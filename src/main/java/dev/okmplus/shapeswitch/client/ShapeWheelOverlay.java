package dev.okmplus.shapeswitch.client;

import java.util.List;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.Util;

/**
 * 褰㈢姸杞洏 HUD锛氬睆骞曚腑澶竴鍦堝皬鏂瑰潡琛ㄧず鍏ㄩ儴鍙疆鎹㈠舰鐘讹紝涓績鏄剧ず褰撳墠褰㈢姸鍚嶄笌搴忓彿銆? *
 * <p>鍙湪鈥滄寜浣?Shift鈥濇垨鈥滃垰鍒囨崲瀹岀殑涓€灏忔鏃堕棿鈥濆唴鍑虹幇锛屼笉骞叉壈姝ｅ父娓告垙瑙嗛噹銆? * 鍏ㄩ儴鐢?{@code fill}/{@code drawString} 缁樺埗锛屼笉鎿嶄綔 PoseStack锛岄伩鍏嶅彈 GUI 娓叉煋绠＄嚎鐗堟湰宸紓褰卞搷銆?/p>
 */
public final class ShapeWheelOverlay {

    private static final int MAX_RING_ITEMS = 24;
    private static final int RING_RADIUS = 58;

    private ShapeWheelOverlay() {
    }

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
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
        int centerY = graphics.guiHeight() / 2 + ShapeSwitchConfig.wheelOffsetY();
        String shapeName = entries.get(index).displayName().getString();

        drawRing(graphics, entries.size(), index, centerX, centerY, alpha);
        drawCenter(graphics, mc, shapeName, index + 1, entries.size(), centerX, centerY, alpha);
    }

    private static float fadeAlpha(long now, Minecraft mc) {
        if (ShiftTracker.isShiftDown(mc)) {
            return 1.0F;
        }
        long hold = ShapeSwitchConfig.wheelHoldMillis();
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
     * 鐢荤幆銆傚舰鐘舵暟瓒呰繃 {@link #MAX_RING_ITEMS} 鏃讹紝鐜彧鏄剧ず鍖呭惈褰撳墠椤圭殑涓€娈电獥鍙ｏ紝
     * 淇濊瘉鈥滈€変腑鐐光€濆缁堝瓨鍦ㄤ簬鐜笂銆?     */
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
