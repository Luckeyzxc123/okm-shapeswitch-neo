package dev.okmplus.shapeswitch.client;

import dev.okmplus.shapeswitch.OkmShapeSwitch;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

/**
 * 客户端入口：注册 HUD 图层、输入/渲染事件与配置界面。
 *
 * <p>只在物理客户端加载，专用服务器不会触碰这里的任何类。</p>
 */
public final class ClientBootstrap {

    private static final ResourceLocation WHEEL_LAYER = ResourceLocation.fromNamespaceAndPath(
            OkmShapeSwitch.MOD_ID, "shape_wheel");

    private ClientBootstrap() {
    }

    public static void init(IEventBus modBus, ModContainer container) {
        OkmShapeSwitch.LOGGER.info("[ShapeSwitch] 客户端初始化，OKM 已加载={}",
                ModList.get().isLoaded(OkmShapeSwitch.OKM_MOD_ID));

        modBus.addListener(ClientBootstrap::onRegisterGuiLayers);
        modBus.addListener(ClientBootstrap::onConfigEvent);
        modBus.addListener(ShapeSwitchKeys::register);

        NeoForge.EVENT_BUS.addListener(ShapeScrollHandler::onScroll);
        NeoForge.EVENT_BUS.addListener(ShapePreviewRenderer::onRenderHighlight);
       
        NeoForge.EVENT_BUS.addListener(PreviewToggle::onClientTick);

        // 模组列表里的“配置”按钮 -> NeoForge 通用配置界面。
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (modContainer, parent) -> new ConfigurationScreen(modContainer, parent));
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, WHEEL_LAYER, ShapeWheelOverlay::render);
        OkmShapeSwitch.LOGGER.info("[ShapeSwitch] 形状轮盘 HUD 已注册");
    }

    private static void onConfigEvent(ModConfigEvent event) {
        PreviewGeometry.invalidate();
    }
}
