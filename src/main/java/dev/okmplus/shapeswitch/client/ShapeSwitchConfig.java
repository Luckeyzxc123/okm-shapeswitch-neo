package dev.okmplus.shapeswitch.client;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 客户端配置。全部为 {@code ModConfig.Type.CLIENT}，不会同步到服务器。
 *
 * <p>所有读取入口都会先检查 {@link #isReady()}：配置文件语法错误 / 重载失败时
 * {@code ModConfigSpec} 的值会抛 {@code IllegalStateException}，直接读会让渲染线程崩掉。</p>
 */
public final class ShapeSwitchConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("总开关：是否允许 Shift + 滚轮 切换挖掘形状。",
                    "Master switch for Shift + mouse wheel shape cycling.")
            .define("enabled", true);

    public static final ModConfigSpec.BooleanValue REQUIRE_SHIFT = BUILDER
            .comment("切换形状时必须按住 Shift。设为 false 时，单独滚动滚轮即可切换（会顶掉快捷栏滚动）。",
                    "Require Shift to be held while scrolling. When false, a bare wheel scroll cycles shapes (this steals hotbar scrolling).")
            .define("requireShift", true);

    public static final ModConfigSpec.BooleanValue INVERT_SCROLL = BUILDER
            .comment("反转滚轮方向（默认上滚 = 下一个形状）。",
                    "Invert wheel direction (default: scroll up = next shape).")
            .define("invertScroll", false);

    public static final ModConfigSpec.BooleanValue SHOW_WHEEL = BUILDER
            .comment("切换形状时显示形状轮盘；按住 Shift 时也会显示。",
                    "Show the shape wheel while switching, and whenever Shift is held.")
            .define("showWheel", true);

    public static final ModConfigSpec.IntValue WHEEL_HOLD_MS = BUILDER
            .comment("松开 Shift 后轮盘继续显示的时长（毫秒）。",
                    "How long the wheel stays visible after the last switch, in milliseconds.")
            .defineInRange("wheelHoldMillis", 1200, 200, 10_000);

    public static final ModConfigSpec.IntValue HUD_OFFSET_Y = BUILDER
            .comment("轮盘中心相对于屏幕中心的垂直偏移（像素，正数向下）。",
                    "Vertical offset of the wheel centre from the screen centre, in pixels (positive = down).")
            .defineInRange("wheelOffsetY", 0, -200, 200);

    public static final ModConfigSpec.BooleanValue SHOW_ACTION_BAR = BUILDER
            .comment("切换形状时在物品栏上方提示当前形状名称。",
                    "Show the current shape name above the hotbar when switching.")
            .define("showActionBar", true);

    public static final ModConfigSpec.ConfigValue<String> PREVIEW_MODE = BUILDER
            .comment("挖掘形状预览的显示时机：always（始终）、shift（按住 Shift）、off（关闭）。",
                    "When to draw the mining-shape preview: always / shift / off.")
            .defineInList("previewMode", "always", List.of("always", "shift", "off"));

    public static final ModConfigSpec.IntValue PREVIEW_MAX_BLOCKS = BUILDER
            .comment("预览最多计算/渲染多少个方块（防止大形状卡顿）。",
                    "Maximum number of previewed blocks to compute and render.")
            .defineInRange("previewMaxBlocks", 512, 1, 4096);

    public static final ModConfigSpec.IntValue PREVIEW_ALPHA = BUILDER
            .comment("预览线框不透明度（0-255）。",
                    "Preview outline alpha (0-255).")
            .defineInRange("previewAlpha", 150, 0, 255);

    public static final ModConfigSpec.BooleanValue PREVIEW_SHOW_ORIGIN = BUILDER
            .comment("把准星所指的原方块也用高亮色画进预览。",
                    "Also outline the origin block the crosshair points at.")
            .define("previewShowOrigin", true);

    public static final ModConfigSpec.BooleanValue PREVIEW_MATCH_OKM_LIMITS = BUILDER
            .comment("预览使用 OKM 配置里的 maxBlocks / maxDistance / allowDiagonal，尽量与实际挖掘一致。",
                    "Use OneKeyMiner's configured maxBlocks / maxDistance / allowDiagonal so the preview matches what is actually mined.")
            .define("previewUseOkmLimits", true);

    public static final ModConfigSpec.ConfigValue<String> EXCLUDED_SHAPES = BUILDER
            .comment("不参与轮换的形状 ID，用英文逗号分隔，例如 \"onekeyminer:amorphous\"。",
                    "Comma separated shape IDs excluded from cycling, e.g. \"onekeyminer:amorphous\".")
            .define("excludedShapes", "");

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ShapeSwitchConfig() {
    }

    /** 配置是否已经加载完成（未加载时读取任何值都会抛异常）。 */
    public static boolean isReady() {
        return SPEC.isLoaded();
    }

    public static boolean previewModeAlways() {
        return "always".equals(previewMode());
    }

    public static boolean previewModeShift() {
        return "shift".equals(previewMode());
    }

    public static boolean previewEnabled() {
        String mode = previewMode();
        return "always".equals(mode) || "shift".equals(mode);
    }

    public static String previewMode() {
        if (!isReady()) {
            return "always";
        }
        String raw = PREVIEW_MODE.get();
        return raw == null ? "always" : raw.trim().toLowerCase(Locale.ROOT);
    }

    public static Set<String> excludedShapeIds() {
        if (!isReady()) {
            return Set.of();
        }
        String raw = EXCLUDED_SHAPES.get();
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        Set<String> ids = new LinkedHashSet<>();
        for (String part : raw.split(",")) {
            String id = part.trim();
            if (!id.isEmpty()) {
                ids.add(id);
            }
        }
        return ids;
    }
}
