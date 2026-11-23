package dev.okmplus.shapeswitch.client;

import java.lang.reflect.Method;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;

import dev.okmplus.shapeswitch.OkmShapeSwitch;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * OneKeyMiner 公开 API 的反射桥。
 *
 * <p>为什么用反射：OKM 发布给玩家的 NeoForge jar 是 Forgix 重命名过的，
 * 形状相关的类名带平台后缀（{@code ChainShape_neoforge}、
 * {@code ShapeContext_neoforge}、{@code ShapeContext$Builder_neoforge}），
 * 而 {@code ShapeContext$Builder_neoforge} 这种名字在 Java 源码里无法直接引用
 * （外层类 {@code ShapeContext} 已被重命名，不存在）。所以附属模组无法用
 * 编译期类型调用形状 API，只能在运行期按名字探测。</p>
 *
 * <p>好处：不依赖编译期 jar，universal / -neoforge / -dev 三种发布包都能工作。</p>
 */
public final class OkmBridge {

    private static final String API_CLASS = "org.xiyu.onekeyminer.api.OneKeyMinerAPI";
    private static final String CONFIG_MANAGER_CLASS = "org.xiyu.onekeyminer.config.ConfigManager";
    private static final String MINER_CONFIG_CLASS = "org.xiyu.onekeyminer.config.MinerConfig";

    private static final Map<Class<?>, Method> PREVIEW_METHODS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Class<?>> BUILDER_CLASSES = new ConcurrentHashMap<>();
    private static final Map<String, Method> BUILDER_METHODS = new ConcurrentHashMap<>();
    private static final Map<String, Method> GETTERS = new ConcurrentHashMap<>();

    private static volatile Boolean available;
    private static Method mRegisteredShapes;
    private static Method mShapeById;
    private static Method mSelectedShape;
    private static Method mSetSelectedShape;
    private static Method mGetConfig;
    private static Class<?> minerConfigClass;
    private static boolean warnedFailure;

    private OkmBridge() {
    }

    /** OKM 是否可用（只判断一次，结果缓存）。 */
    public static boolean available() {
        Boolean cached = available;
        if (cached != null) {
            return cached;
        }
        synchronized (OkmBridge.class) {
            if (available != null) {
                return available;
            }
            boolean ok;
            try {
                Class<?> api = Class.forName(API_CLASS);
                mRegisteredShapes = api.getMethod("getRegisteredShapes");
                mShapeById = api.getMethod("getShape", ResourceLocation.class);
                mSelectedShape = api.getMethod("getSelectedShape");
                mSetSelectedShape = api.getMethod("setSelectedShape", String.class);
                try {
                    mGetConfig = Class.forName(CONFIG_MANAGER_CLASS).getMethod("getConfig");
                    minerConfigClass = Class.forName(MINER_CONFIG_CLASS);
                } catch (Throwable ignored) {
                    mGetConfig = null;
                    minerConfigClass = null;
                }
                ok = true;
                OkmShapeSwitch.LOGGER.info("[ShapeSwitch] 已连接 OneKeyMiner API（来自 {}）", sourceOf(api));
            } catch (Throwable t) {
                ok = false;
                OkmShapeSwitch.LOGGER.error("[ShapeSwitch] 找不到 OneKeyMiner 的 API 类 {}，本模组功能不可用。", API_CLASS, t);
            }
            available = ok;
            return ok;
        }
    }

    /** 全部已注册形状（按 OKM 注册顺序），元素是运行期的 shape 对象。 */
    public static List<?> registeredShapes() {
        if (!available()) {
            return List.of();
        }
        try {
            Object result = mRegisteredShapes.invoke(null);
            return result instanceof List<?> list ? list : List.of();
        } catch (Throwable t) {
            warnOnce("getRegisteredShapes()", t);
            return List.of();
        }
    }

    /** 按 ID 取形状对象，失败返回 null。 */
    public static Object shapeById(String id) {
        if (!available() || id == null || id.isBlank()) {
            return null;
        }
        ResourceLocation parsed = ResourceLocation.tryParse(id);
        if (parsed == null) {
            return null;
        }
        try {
            return mShapeById.invoke(null, parsed);
        } catch (Throwable t) {
            warnOnce("getShape(" + id + ")", t);
            return null;
        }
    }

    /** shape#getId() -> "onekeyminer:cube" */
    public static String shapeId(Object shape) {
        Object value = callGetter(shape, "getId");
        return value == null ? "" : value.toString();
    }

    /** shape#getTranslationKey() */
    public static String shapeTranslationKey(Object shape) {
        Object value = callGetter(shape, "getTranslationKey");
        return value == null ? "" : value.toString();
    }

    public static String selectedShape() {
        if (!available()) {
            return "";
        }
        try {
            Object value = mSelectedShape.invoke(null);
            return value == null ? "" : value.toString();
        } catch (Throwable t) {
            warnOnce("getSelectedShape()", t);
            return "";
        }
    }

    public static boolean setSelectedShape(String id) {
        if (!available() || id == null) {
            return false;
        }
        try {
            Object value = mSetSelectedShape.invoke(null, id);
            return Boolean.TRUE.equals(value);
        } catch (Throwable t) {
            warnOnce("setSelectedShape(" + id + ")", t);
            return false;
        }
    }

    /** 读 OKM 的 MinerConfig 字段（int）。 */
    public static int configInt(String field, int fallback) {
        Object value = configField(field);
        return value instanceof Number number ? number.intValue() : fallback;
    }

    /** 读 OKM 的 MinerConfig 字段（boolean）。 */
    public static boolean configBool(String field, boolean fallback) {
        Object value = configField(field);
        return value instanceof Boolean flag ? flag : fallback;
    }

    /**
     * 用 OKM 自己的形状算法算预览坐标。
     *
     * @return 预览方块列表；任何一步失败都返回空列表（不会抛给渲染线程）
     */
    public static List<BlockPos> previewPositions(Object shape, Level level, BlockPos pos, BlockState state,
            Direction facing, Direction vertical, int maxBlocks, int maxDistance, boolean allowDiagonal) {
        if (!available() || shape == null || level == null || pos == null || state == null) {
            return List.of();
        }
        try {
            Method preview = PREVIEW_METHODS.get(shape.getClass());
            if (preview == null) {
                preview = findPreviewMethod(shape.getClass());
                if (preview == null) {
                    warnOnce("getPreviewPositions 方法不存在", null);
                    return List.of();
                }
                PREVIEW_METHODS.put(shape.getClass(), preview);
            }

            Class<?> contextClass = preview.getParameterTypes()[0];
            Object builder = newBuilder(contextClass);
            if (builder == null) {
                warnOnce("找不到 ShapeContext Builder（" + contextClass.getName() + "）", null);
                return List.of();
            }

            invokeBuilder(builder, "level", level);
            invokeBuilder(builder, "originPos", pos);
            invokeBuilder(builder, "originState", state);
            invokeBuilder(builder, "playerFacing", facing);
            invokeBuilder(builder, "playerLookingVertical", vertical);
            invokeBuilder(builder, "maxBlocks", maxBlocks);
            invokeBuilder(builder, "maxDistance", maxDistance);
            invokeBuilder(builder, "allowDiagonal", allowDiagonal);
            applyBlockMatcher(builder);

            Object context = invokeZeroArg(builder, "build");
            if (context == null) {
                return List.of();
            }
            Object result = preview.invoke(shape, context);
            if (!(result instanceof List<?> raw)) {
                return List.of();
            }
            List<BlockPos> out = new ArrayList<>(raw.size());
            for (Object element : raw) {
                if (element instanceof BlockPos blockPos) {
                    out.add(blockPos);
                }
            }
            return out;
        } catch (Throwable t) {
            warnOnce("计算形状预览", t);
            return List.of();
        }
    }

    /* ------------------------------- 内部实现 ------------------------------- */

    private static Object configField(String field) {
        if (!available() || mGetConfig == null || minerConfigClass == null) {
            return null;
        }
        try {
            Object config = mGetConfig.invoke(null);
            return config == null ? null : minerConfigClass.getField(field).get(config);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object callGetter(Object shape, String name) {
        if (shape == null) {
            return null;
        }
        try {
            String key = shape.getClass().getName() + "#" + name;
            Method method = GETTERS.get(key);
            if (method == null) {
                method = shape.getClass().getMethod(name);
                GETTERS.put(key, method);
            }
            return method.invoke(shape);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Method findPreviewMethod(Class<?> type) {
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            Method found = oneArgMethodNamed(c.getMethods(), "getPreviewPositions");
            if (found != null) {
                return found;
            }
        }
        for (Class<?> iface : type.getInterfaces()) {
            Method found = oneArgMethodNamed(iface.getMethods(), "getPreviewPositions");
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static Method oneArgMethodNamed(Method[] methods, String name) {
        for (Method method : methods) {
            if (method.getName().equals(name) && method.getParameterCount() == 1) {
                return method;
            }
        }
        return null;
    }

    private static Object newBuilder(Class<?> contextClass) {
        try {
            Class<?> builderClass = BUILDER_CLASSES.get(contextClass);
            if (builderClass == null) {
                builderClass = resolveBuilderClass(contextClass);
                if (builderClass == null) {
                    return null;
                }
                BUILDER_CLASSES.put(contextClass, builderClass);
            }
            return builderClass.getDeclaredConstructor().newInstance();
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Forgix 重命名后，Builder 可能出现在这些位置：
     * {@code ShapeContext_neoforge$Builder}、{@code ShapeContext$Builder_neoforge}、
     * {@code ShapeContext$Builder}、{@code ShapeContext_neoforge$Builder_neoforge}。
     */
    private static Class<?> resolveBuilderClass(Class<?> contextClass) {
        String name = contextClass.getName();
        boolean suffixed = name.endsWith("_neoforge");
        String outer = suffixed ? name.substring(0, name.length() - "_neoforge".length()) : name;
        String suffix = suffixed ? "_neoforge" : "";
        String[] candidates = {
                name + "$Builder",
                outer + "$Builder" + suffix,
                name + "$Builder" + suffix,
                outer + "$Builder",
        };
        for (String candidate : candidates) {
            try {
                return Class.forName(candidate);
            } catch (Throwable ignored) {
                // try next
            }
        }
        return null;
    }

    private static Object invokeBuilder(Object builder, String name, Object argument) {
        try {
            String key = builder.getClass().getName() + "#" + name;
            Method method = BUILDER_METHODS.get(key);
            if (method == null) {
                method = oneArgMethodNamed(builder.getClass().getMethods(), name);
                if (method == null) {
                    return null;
                }
                BUILDER_METHODS.put(key, method);
            }
            return method.invoke(builder, argument);
        } catch (Throwable t) {
            // 单个 setter 失败不致命（例如某些版本没有 playerLookingVertical）
            return null;
        }
    }

    private static Object invokeZeroArg(Object target, String name) {
        try {
            Method method = target.getClass().getMethod(name);
            return method.invoke(target);
        } catch (Throwable t) {
            return null;
        }
    }

    private static void applyBlockMatcher(Object builder) {
        try {
            Method method = oneArgMethodNamed(builder.getClass().getMethods(), "blockMatcher");
            if (method == null) {
                return;
            }
            BiPredicate<BlockState, BlockState> matcher = (origin, target) -> target != null
                    && !target.isAir()
                    && origin != null
                    && target.getBlock() == origin.getBlock();
            method.invoke(builder, matcher);
        } catch (Throwable ignored) {
            // 没有 blockMatcher 就用 OKM 默认匹配
        }
    }

    private static String sourceOf(Class<?> type) {
        try {
            CodeSource source = type.getProtectionDomain().getCodeSource();
            return source == null || source.getLocation() == null ? "unknown" : source.getLocation().toString();
        } catch (Throwable t) {
            return "unknown";
        }
    }

    private static void warnOnce(String where, Throwable error) {
        if (warnedFailure) {
            return;
        }
        warnedFailure = true;
        if (error == null) {
            OkmShapeSwitch.LOGGER.warn("[ShapeSwitch] OKM 桥接失败：{}", where);
        } else {
            OkmShapeSwitch.LOGGER.error("[ShapeSwitch] OKM 桥接失败：" + where, error);
        }
    }
}
