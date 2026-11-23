package dev.okmplus.shapeswitch.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.xiyu.onekeyminer.api.OneKeyMinerAPI;
import org.xiyu.onekeyminer.shape.ChainShape;

import dev.okmplus.shapeswitch.OkmShapeSwitch;

/**
 * 当前所有可轮换的挖掘形状。
 *
 * <p>直接读取 OKM 的 {@link OneKeyMinerAPI#getRegisteredShapes()}，因此其它附属模组
 * 注册的自定义形状也会自动出现在轮盘里，不需要额外适配。</p>
 */
public final class ShapeCatalog {

    /** 一个可轮换的形状条目。 */
    public record Entry(String id, String translationKey) {

        /** 本地化显示名；OKM 没有对应语言项时退化为 id 的可读形式。 */
        public Component displayName() {
            return Component.translatableWithFallback(translationKey, humanize(id));
        }
    }

    /** HUD 每帧都会问一次，避免每帧重建列表。 */
    private static List<Entry> cached = List.of();
    private static String cachedSignature = "";
    private static boolean warnedMergedJar;

    private ShapeCatalog() {
    }

    /**
     * 按 OKM 的注册顺序返回全部形状（已过滤配置中排除的 ID）。
     *
     * <p>配置或形状注册表变化时自动重建缓存。</p>
     */
    public static List<Entry> entries() {
        List<ChainShape> shapes;
        try {
            shapes = OneKeyMinerAPI.getRegisteredShapes();
        } catch (RuntimeException | LinkageError e) {
            return List.of();
        }
        if (shapes == null || shapes.isEmpty()) {
            return List.of();
        }

        Set<String> excluded = ShapeSwitchConfig.excludedShapeIds();
        String signature = shapes.size() + "|" + excluded;
        if (signature.equals(cachedSignature)) {
            return cached;
        }

        try {
            List<Entry> out = new ArrayList<>(shapes.size());
            for (ChainShape shape : shapes) {
                if (shape == null) {
                    continue;
                }
                ResourceLocation id;
                try {
                    id = shape.getId();
                } catch (RuntimeException | LinkageError e) {
                    continue;
                }
                if (id == null) {
                    continue;
                }
                String idString = id.toString();
                if (excluded.contains(idString)) {
                    continue;
                }
                String key;
                try {
                    key = shape.getTranslationKey();
                } catch (RuntimeException | LinkageError e) {
                    key = null;
                }
                if (key == null || key.isBlank()) {
                    key = "shape." + id.getNamespace() + "." + id.getPath();
                }
                out.add(new Entry(idString, key));
            }
            List<Entry> result = List.copyOf(out);
            cachedSignature = signature;
            cached = result;
            return result;
        } catch (ClassCastException e) {
            // 玩家装的是 Forgix 三平台合并包（onekeyminer-1.6.6-1.21.7.jar）时，
            // OKM 返回的是 ChainShape_neoforge，和编译期的 ChainShape 不是同一个类。
            if (!warnedMergedJar) {
                warnedMergedJar = true;
                OkmShapeSwitch.LOGGER.error(
                        "[ShapeSwitch] 检测到 OneKeyMiner 三平台合并包，形状系统无法使用。"
                                + "请改用 onekeyminer-neoforge-<版本>-<MC>.jar（GitHub Releases 里的 NeoForge 专用版）。");
            }
            return List.of();
        } catch (RuntimeException | LinkageError e) {
            return List.of();
        }
    }

    /** 当前选中的形状 ID（读 OKM 配置）。 */
    public static String selectedShapeId() {
        try {
            String id = OneKeyMinerAPI.getSelectedShape();
            return Objects.requireNonNullElse(id, "");
        } catch (RuntimeException | LinkageError e) {
            return "";
        }
    }

    /** 当前选中形状在列表中的下标；找不到时返回 -1。 */
    public static int indexOfSelected(List<Entry> entries) {
        if (entries.isEmpty()) {
            return -1;
        }
        String selected = selectedShapeId();
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).id().equals(selected)) {
                return i;
            }
        }
        return -1;
    }

    /** 把 {@code onekeyminer:small_tunnel} 变成 {@code Small Tunnel}。 */
    public static String humanize(String id) {
        int colon = id.indexOf(':');
        String path = colon >= 0 ? id.substring(colon + 1) : id;
        String[] parts = path.split("[_\\-]+");
        StringBuilder sb = new StringBuilder(path.length());
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                sb.append(part.substring(1));
            }
        }
        return sb.isEmpty() ? id : sb.toString();
    }
}
