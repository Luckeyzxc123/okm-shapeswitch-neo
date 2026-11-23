package dev.okmplus.shapeswitch.client;

import java.util.List;

import dev.okmplus.shapeswitch.OkmShapeSwitch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.Util;
import org.xiyu.onekeyminer.api.OneKeyMinerAPI;

/**
 * 形状轮换逻辑：读取当前形状 -> 计算下一个 -> 交给 OKM API 保存并同步。
 *
 * <p>OKM 的 {@code setSelectedShape} 已经负责写配置、发同步包、触发配置变更事件，
 * 所以这里不碰任何网络与配置文件。</p>
 */
public final class ShapeSwitcher {

    /** 上一次切换的时间，用于轮盘淡出与切换反馈。 */
    private static long lastSwitchMillis = Long.MIN_VALUE;

    private ShapeSwitcher() {
    }

    public static long lastSwitchMillis() {
        return lastSwitchMillis;
    }

    /** 轮盘是否应该显示（按住 Shift，或刚切换过）。 */
    public static boolean wheelVisible(long now) {
        if (!ShapeSwitchConfig.isReady() || !ShapeSwitchConfig.SHOW_WHEEL.get()) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            return false;
        }
        if (ShiftTracker.isShiftDown(mc)) {
            return true;
        }
        if (lastSwitchMillis == Long.MIN_VALUE) {
            return false;
        }
        return now - lastSwitchMillis < ShapeSwitchConfig.WHEEL_HOLD_MS.get();
    }

    /**
     * 切换形状。
     *
     * @param direction +1 下一个，-1 上一个
     * @return 是否成功切换
     */
    public static boolean cycle(int direction) {
        if (!ShapeSwitchConfig.isReady() || !ShapeSwitchConfig.ENABLED.get()) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return false;
        }

        List<ShapeCatalog.Entry> entries = ShapeCatalog.entries();
        if (entries.size() < 2) {
            // 只有一个形状时没有可切换的目标，但仍然给个提示。
            if (entries.size() == 1) {
                notify(mc, entries.get(0));
            }
            return false;
        }

        int step = direction >= 0 ? 1 : -1;
        if (ShapeSwitchConfig.INVERT_SCROLL.get()) {
            step = -step;
        }
        int current = ShapeCatalog.indexOfSelected(entries);
        if (current < 0) {
            // 当前形状不在可轮换列表里：向下轮从第一个开始，向上轮从最后一个开始。
            current = step > 0 ? -1 : 0;
        }
        int next = Math.floorMod(current + step, entries.size());
        if (next == current) {
            return false;
        }

        ShapeCatalog.Entry target = entries.get(next);
        boolean applied;
        try {
            applied = OneKeyMinerAPI.setSelectedShape(target.id());
        } catch (RuntimeException | LinkageError e) {
            OkmShapeSwitch.LOGGER.error("[ShapeSwitch] 无法切换形状到 {}", target.id(), e);
            return false;
        }
        if (!applied) {
            OkmShapeSwitch.LOGGER.warn("[ShapeSwitch] OKM 拒绝切换到 {}", target.id());
            return false;
        }

        lastSwitchMillis = Util.getMillis();
        notify(mc, target);
        return true;
    }

    private static void notify(Minecraft mc, ShapeCatalog.Entry entry) {
        LocalPlayer player = mc.player;
        if (player == null || !ShapeSwitchConfig.SHOW_ACTION_BAR.get()) {
            return;
        }
        List<ShapeCatalog.Entry> entries = ShapeCatalog.entries();
        int index = Math.max(0, ShapeCatalog.indexOfSelected(entries));
        player.displayClientMessage(
                Component.translatable("okmshapeswitch.actionbar.switched",
                        entry.displayName(), index + 1, entries.size()),
                true);
    }
}
