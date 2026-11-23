package dev.okmplus.shapeswitch.client;

import java.util.List;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

/**
 * 形状轮盘 HUD：屏幕中央一圈小方块表示全部可轮换形状，中心显示当前形状名与序号。
 *
 * <p>只在“按住 Shift”或“刚切换完的一小段时间”内出现，不干扰正常游戏视野。
 * 全部用 {@code fill}/{@code drawString} 绘制，不操作 PoseStack，避免受 GUI 渲染管线版本差异影响。</p>
 */
public final class ShapeWheelOverlay {

    private static final int MAX_RING_ITEMS = 24;
    private static final int RING_RADIUS = 58;

    private ShapeWheelOverlay() {
    }

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (!ShapeSwitchConfig.isReady()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui || mc.screen != null) {
            return;
        }

        long now = Util.getMillis();
        if (!ShapeSwitcher.wheelVisible(now)) {
            return;
        }

        List<ShapeCatalog.Entry> entries = ShapeCatalog.entries();
        if (entries.isEmpty()) {
            return;
        }

        int index = Math.max(0, ShapeCatalog.indexOfSelected(entries));
        float alpha = fadeAlpha(now, mc);

        int centerX = graphics.guiWidth() / 2;
        int centerY = graphics.guiHeight() / 2 + ShapeSwitchConfig.HUD_OFFSET_Y.get();
        String shapeName = entries.get(index).displayName().getString();

        drawRing(graphics, entries.size(), index, centerX, centerY, alpha);
        drawCenter(graphics, mc, shapeName, index + 1, entries.size(), centerX, centerY, alpha);
    }

    private static float fadeAlpha(long now, Minecraft mc) {
        if (ShiftTracker.isShiftDown(mc)) {
            return 1.0F;
        }
        long hold = ShapeSwitchConfig.WHEEL_HOLD_MS.get();
        long elapsed = now - ShapeSwitcher.lastSwitchMillis();
        if (elapsed < 0L || elapsed >= hold) {
            return 0.0F;
        }
        long fadeStart = hold / 2L;
        if (elapsed <= fadeStart) {
            return 1.0F;
        }
        return 1.0F - (float) (elapsed - fadeStart) / (float) (hold - fadeStart);
    }

    /**
     * 画环。形状数超过 {@link #MAX_RING_ITEMS} 时，环只显示包含当前项的一段窗口，
     * 保证“选中点”始终存在于环上。
     */
    private static void drawRing(GuiGraphics graphics, int total, int selected, int centerX, int centerY, float alpha) {
        int count = Math.min(total, MAX_RING_ITEMS);
        if (count <= 0) {
            return;
        }
        int start = selected >= count ? selected - count + 1 : 0;
        int ringSelected = selected - start;

        double step = 360.0D / count;
        int dimColor = argb(alpha * 0.55F, 0xFFC8D8E8);
        int selectedColor = argb(alpha, 0xFFFFE066);

        for (int i = 0; i < count; i++) {
            double angle = Math.toRadians(-90.0D + step * i);
            int x = centerX + (int) Math.round(Math.cos(angle) * RING_RADIUS);
            int y = centerY + (int) Math.round(Math.sin(angle) * RING_RADIUS);
            if (i == ringSelected) {
                int size = 6;
                graphics.fill(x - size - 2, y - size - 2, x + size + 2, y + size + 2, argb(alpha * 0.35F, 0x000000));
                graphics.fill(x - size, y - size, x + size, y + size, selectedColor);
            } else {
                int size = 3;
                graphics.fill(x - size, y - size, x + size, y + size, dimColor);
            }
        }
    }

    private static void drawCenter(GuiGraphics graphics, Minecraft mc, String shapeName, int number, int total,
            int centerX, int centerY, float alpha) {
        var font = mc.font;
        int nameColor = argb(alpha, 0xFFFFFFFF);
        int infoColor = argb(alpha * 0.85F, 0xFFB9D4E8);
        int hintColor = argb(alpha * 0.6F, 0xFF8FA6B8);

        int panelWidth = Math.max(font.width(shapeName), 84) + 12;
        graphics.fill(centerX - panelWidth / 2, centerY - 11, centerX + panelWidth / 2, centerY + 11,
                argb(alpha * 0.55F, 0x0B1118));
        graphics.drawCenteredString(font, shapeName, centerX, centerY - 9, nameColor);

        String info = number + " / " + total;
        graphics.drawCenteredString(font, info, centerX, centerY + 2, infoColor);

        String hint = Component.translatable("okmshapeswitch.wheel.hint").getString();
        int hintWidth = font.width(hint);
        graphics.fill(centerX - hintWidth / 2 - 3, centerY + 26, centerX + hintWidth / 2 + 3, centerY + 38,
                argb(alpha * 0.5F, 0x0B1118));
        graphics.drawCenteredString(font, hint, centerX, centerY + 28, hintColor);
    }

    private static int argb(float alpha, int rgb) {
        int a = (int) (Math.clamp(alpha, 0.0F, 1.0F) * 255.0F);
        return (a << 24) | (rgb & 0x00FFFFFF);
    }
}
