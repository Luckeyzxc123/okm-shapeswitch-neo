package dev.okmplus.shapeswitch.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/** 本模组的按键绑定。 */
public final class ShapeSwitchKeys {

    /** 开关形状预览（可在“选项 → 控制”里改键）。 */
    public static final KeyMapping TOGGLE_PREVIEW = new KeyMapping(
            "key.okmshapeswitch.toggle_preview",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "key.categories.okmshapeswitch");

    private ShapeSwitchKeys() {
    }

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_PREVIEW);
    }
}
