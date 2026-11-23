package dev.okmplus.shapeswitch.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import org.joml.Matrix4f;

/**
 * 在世界里画出当前挖掘形状的预览线框。
 *
 * <p>挂在 {@link RenderHighlightEvent.Block} 上：只有玩家准星指向方块时才会触发，
 * 此时 PoseStack 已经是“相机在原点”的空间。</p>
 *
 * <p>注：原版一个帧内会分别以“不透明 / 半透明”两趟触发该事件，这里只取不透明那趟，
 * 避免同一批线框提交两次。线框写进原版提供的 {@link MultiBufferSource}，
 * 由原版在渲染阶段末尾统一 flush，不要在这里手动 {@code endBatch}。</p>
 */
public final class ShapePreviewRenderer {

    private static final float ORIGIN_R = 1.00F;
    private static final float ORIGIN_G = 0.85F;
    private static final float ORIGIN_B = 0.25F;

    private static final float BLOCK_R = 0.30F;
    private static final float BLOCK_G = 0.85F;
    private static final float BLOCK_B = 1.00F;

    /** 线框略微内缩，避免和原版方块描边 z-fighting。 */
    private static final float INSET = 0.002F;

    private ShapePreviewRenderer() {
    }

    public static void onRenderHighlight(RenderHighlightEvent.Block event) {
        if (event.isForTranslucentBlocks()) {
            return;
        }
        if (!ShapeSwitchConfig.isReady() || !ShapeSwitchConfig.previewEnabled()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.options.hideGui || mc.screen != null) {
            return;
        }
        if (ShapeSwitchConfig.previewModeShift() && !ShiftTracker.isShiftDown(mc)) {
            return;
        }

        PreviewGeometry.Result result = PreviewGeometry.current();
        if (result.origin() == null || result.blocks().isEmpty()) {
            return;
        }

        float alpha = ShapeSwitchConfig.PREVIEW_ALPHA.get() / 255.0F;
        if (alpha <= 0.0F) {
            return;
        }

        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource buffers = event.getMultiBufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());

        BlockPos origin = result.origin();
        for (BlockPos pos : result.blocks()) {
            boolean isOrigin = pos.equals(origin);
            float r = isOrigin ? ORIGIN_R : BLOCK_R;
            float g = isOrigin ? ORIGIN_G : BLOCK_G;
            float b = isOrigin ? ORIGIN_B : BLOCK_B;
            float a = isOrigin ? Math.min(1.0F, alpha + 0.25F) : alpha;

            poseStack.pushPose();
            poseStack.translate(pos.getX() - cam.x, pos.getY() - cam.y, pos.getZ() - cam.z);
            Matrix4f matrix = poseStack.last().pose();
            box(lines, matrix, INSET, INSET, INSET, 1.0F - INSET, 1.0F - INSET, 1.0F - INSET, r, g, b, a);
            poseStack.popPose();
        }
    }

    private static void box(VertexConsumer consumer, Matrix4f matrix,
            float x0, float y0, float z0, float x1, float y1, float z1,
            float r, float g, float b, float a) {
        line(consumer, matrix, x0, y0, z0, x1, y0, z0, r, g, b, a);
        line(consumer, matrix, x1, y0, z0, x1, y0, z1, r, g, b, a);
        line(consumer, matrix, x1, y0, z1, x0, y0, z1, r, g, b, a);
        line(consumer, matrix, x0, y0, z1, x0, y0, z0, r, g, b, a);

        line(consumer, matrix, x0, y1, z0, x1, y1, z0, r, g, b, a);
        line(consumer, matrix, x1, y1, z0, x1, y1, z1, r, g, b, a);
        line(consumer, matrix, x1, y1, z1, x0, y1, z1, r, g, b, a);
        line(consumer, matrix, x0, y1, z1, x0, y1, z0, r, g, b, a);

        line(consumer, matrix, x0, y0, z0, x0, y1, z0, r, g, b, a);
        line(consumer, matrix, x1, y0, z0, x1, y1, z0, r, g, b, a);
        line(consumer, matrix, x1, y0, z1, x1, y1, z1, r, g, b, a);
        line(consumer, matrix, x0, y0, z1, x0, y1, z1, r, g, b, a);
    }

    private static void line(VertexConsumer consumer, Matrix4f matrix,
            float x0, float y0, float z0, float x1, float y1, float z1,
            float r, float g, float b, float a) {
        float nx = x1 - x0;
        float ny = y1 - y0;
        float nz = z1 - z0;
        float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (length > 0.0F) {
            nx /= length;
            ny /= length;
            nz /= length;
        }
        consumer.addVertex(matrix, x0, y0, z0).setColor(r, g, b, a).setNormal(nx, ny, nz);
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setNormal(nx, ny, nz);
    }
}
