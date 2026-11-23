package dev.okmplus.shapeswitch;

import com.mojang.logging.LogUtils;
import dev.okmplus.shapeswitch.client.ClientBootstrap;
import dev.okmplus.shapeswitch.client.ShapeSwitchConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * OneKeyMiner ShapeSwitch Plus (Neo) —— 一键连锁(Neo)Forge 的附属模组。
 *
 * <p>功能：Shift + 鼠标滚轮 循环切换挖掘形状；在准星所指方块上渲染当前形状的
 * 挖掘预览；切换时显示形状轮盘 HUD。</p>
 *
 * <p>只依赖 OKM 的公开 API（{@code org.xiyu.onekeyminer.api.OneKeyMinerAPI}），
 * 不使用反射、不注册 Mixin，也没有任何服务端逻辑，因此声明为客户端模组。</p>
 */
@Mod(value = OkmShapeSwitch.MOD_ID, dist = Dist.CLIENT)
public final class OkmShapeSwitch {

    public static final String MOD_ID = "okmshapeswitch";
    public static final String OKM_MOD_ID = "onekeyminer";
    public static final Logger LOGGER = LogUtils.getLogger();

    public OkmShapeSwitch(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, ShapeSwitchConfig.SPEC, "okm-shapeswitch-client.toml");
        ClientBootstrap.init(modBus, container);
    }
}
