package dev.okmplus.shapeswitch.client;

import dev.okmplus.shapeswitch.OkmShapeSwitch;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** 处理“开关形状预览”热键。 */
public final class PreviewToggle {

    private PreviewToggle() {
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        while (ShapeSwitchKeys.TOGGLE_PREVIEW.consumeClick()) {
            toggle(mc);
        }
    }

    private static void toggle(Minecraft mc) {
        boolean enable = !ShapeSwitchConfig.previewEnabled();
        String mode = enable ? "always" : "off";
        try {
            ShapeSwitchConfig.PREVIEW_MODE.set(mode);
            ShapeSwitchConfig.SPEC.save();
        } catch (RuntimeException e) {
            // 配置写入失败不影响本次开关生效（内存中的值已改）。
            OkmShapeSwitch.LOGGER.warn("[ShapeSwitch] 保存预览开关到配置文件失败", e);
        }
        PreviewGeometry.invalidate();
        if (mc.player != null) {
            mc.player.displayClientMessage(
                    Component.translatable(enable
                            ? "okmshapeswitch.message.preview_on"
                            : "okmshapeswitch.message.preview_off"),
                    true);
        }
    }
}
