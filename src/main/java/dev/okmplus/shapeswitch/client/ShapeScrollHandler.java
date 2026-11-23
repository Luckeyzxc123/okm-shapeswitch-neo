package dev.okmplus.shapeswitch.client;

import dev.okmplus.shapeswitch.OkmShapeSwitch;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.InputEvent;

/**
 * Shift + 鼠标滚轮 切换挖掘形状。
 *
 * <p>NeoForge 的 {@link InputEvent.MouseScrollingEvent} 在原版处理快捷栏滚动之前触发，
 * 取消它就能拦下“切快捷栏”，这正是原版留给模组的挂点。</p>
 */
public final class ShapeScrollHandler {

    private static boolean loggedFirstEvent;

    private ShapeScrollHandler() {
    }

    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        if (!ShapeSwitchConfig.enabled()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null) {
            return;
        }
        if (!loggedFirstEvent) {
            loggedFirstEvent = true;
            OkmShapeSwitch.LOGGER.info("[ShapeSwitch] 已收到滚轮事件（shiftDown={}，形状数={}）",
                    ShiftTracker.isShiftDown(mc), ShapeCatalog.entries().size());
        }
        if (ShapeSwitchConfig.requireShift() && !ShiftTracker.isShiftDown(mc)) {
            return;
        }

        double delta = event.getScrollDeltaY();
        if (delta == 0.0D) {
            return;
        }

        boolean switched;
        try {
            switched = ShapeSwitcher.cycle(delta > 0.0D ? 1 : -1);
        } catch (RuntimeException | LinkageError e) {
            OkmShapeSwitch.LOGGER.error("[ShapeSwitch] 切换形状时发生异常", e);
            return;
        }

        if (switched) {
            // 拦下原版快捷栏滚动，避免切形状的同时换物品。
            event.setCanceled(true);
        }
    }
}
