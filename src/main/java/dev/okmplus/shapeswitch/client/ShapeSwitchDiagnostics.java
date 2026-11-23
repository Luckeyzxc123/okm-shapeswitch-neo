package dev.okmplus.shapeswitch.client;

import java.util.List;

import dev.okmplus.shapeswitch.OkmShapeSwitch;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * 一次性诊断输出：玩家进入世界后往日志里写几行关键状态，
 * 方便判断“为什么没反应”（OKM 不可用 / 形状列表为空 / 配置未读）。
 */
public final class ShapeSwitchDiagnostics {

    private static boolean reported;

    private ShapeSwitchDiagnostics() {
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        if (reported) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        reported = true;

        List<ShapeCatalog.Entry> entries = List.of();
        try {
            entries = ShapeCatalog.entries();
        } catch (Throwable t) {
            OkmShapeSwitch.LOGGER.error("[ShapeSwitch] 读取形状列表失败", t);
        }

        OkmShapeSwitch.LOGGER.info(
                "[ShapeSwitch] 诊断: okmLoaded={}, okmApi={}, configReady={}, shapes={}, selected='{}', preview='{}'",
                ModList.get().isLoaded(OkmShapeSwitch.OKM_MOD_ID),
                OkmBridge.available(),
                ShapeSwitchConfig.isReady(),
                entries.size(),
                ShapeCatalog.selectedShapeId(),
                ShapeSwitchConfig.previewMode());

        if (!OkmBridge.available()) {
            OkmShapeSwitch.LOGGER.warn("[ShapeSwitch] 没能连接 OKM 的 API，功能不可用（上面的 error 有完整栈）。");
        } else if (entries.isEmpty()) {
            OkmShapeSwitch.LOGGER.warn("[ShapeSwitch] OKM 已连接但形状列表为空，"
                    + "Shift+滚轮 与轮盘都不会生效。");
        }
    }
}
