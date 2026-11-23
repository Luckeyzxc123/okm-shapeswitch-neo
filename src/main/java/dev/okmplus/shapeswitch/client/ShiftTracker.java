package dev.okmplus.shapeswitch.client;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.Window;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** 判断 Shift 是否按下，供轮盘显示与滚轮切换共用。 */
public final class ShiftTracker {

    private ShiftTracker() {
    }

    public static boolean isShiftDown(Minecraft mc) {
        if (Screen.hasShiftDown()) {
            return true;
        }
        Window window = mc.getWindow();
        if (window == null) {
            return false;
        }
        long handle = window.getWindow();
        return handle != 0L
                && (GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                        || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS);
    }
}
