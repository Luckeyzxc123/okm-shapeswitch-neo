package dev.okmplus.shapeswitch.client;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * 在客户端把“当前选中的形状 + 准星目标方块”算成一组预览坐标。
 *
 * <p>形状算法本身由 OKM 提供（通过 {@link OkmBridge} 反射调用
 * {@code getPreviewPositions}），因此预览结果与连锁挖掘一致，自定义形状同样支持。</p>
 */
public final class PreviewGeometry {

    /** 预览结果。origin 单独给出，便于用不同颜色高亮。 */
    public record Result(BlockPos origin, List<BlockPos> blocks) {
        public static final Result EMPTY = new Result(null, List.of());
    }

    private static final long CACHE_MILLIS = 100L;

    private static String cacheKey = "";
    private static long cacheTime = Long.MIN_VALUE;
    private static Result cache = Result.EMPTY;

    private PreviewGeometry() {
    }

    public static Result current() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return Result.EMPTY;
        }
        HitResult hit = mc.hitResult;
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            return Result.EMPTY;
        }
        BlockPos origin = blockHit.getBlockPos();
        BlockState state = mc.level.getBlockState(origin);
        if (state.isAir()) {
            return Result.EMPTY;
        }

        String shapeId = ShapeCatalog.selectedShapeId();
        String key = shapeId + '|' + origin.asLong() + '|' + mc.player.getDirection().get3DDataValue()
                + '|' + verticalDirection(mc.player) + '|' + state.getBlock().hashCode();
        long now = Util.getMillis();
        if (key.equals(cacheKey) && cacheTime != Long.MIN_VALUE && now - cacheTime < CACHE_MILLIS) {
            return cache;
        }
        cacheKey = key;
        cacheTime = now;
        cache = compute(mc, origin, state, shapeId);
        return cache;
    }

    /** 清除缓存（配置变化后调用）。 */
    public static void invalidate() {
        cacheKey = "";
        cacheTime = Long.MIN_VALUE;
        cache = Result.EMPTY;
    }

    private static Result compute(Minecraft mc, BlockPos origin, BlockState state, String shapeId) {
        Level level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null) {
            return Result.EMPTY;
        }

        Object shape = OkmBridge.shapeById(shapeId);
        if (shape == null) {
            return Result.EMPTY;
        }

        int cap = ShapeSwitchConfig.previewMaxBlocks();
        int maxBlocks = 64;
        int maxDistance = 16;
        boolean allowDiagonal = true;
        if (ShapeSwitchConfig.useOkmLimits()) {
            maxBlocks = OkmBridge.configInt("maxBlocks", maxBlocks);
            maxDistance = OkmBridge.configInt("maxDistance", maxDistance);
            allowDiagonal = OkmBridge.configBool("allowDiagonal", allowDiagonal);
        }
        // 先限流再计算：否则大形状每 100ms 都可能遍历上万个方块。
        maxBlocks = Math.max(1, Math.min(maxBlocks, cap));

        List<BlockPos> positions = OkmBridge.previewPositions(shape, level, origin, state,
                player.getDirection(), verticalDirection(player), maxBlocks, maxDistance, allowDiagonal);
        if (positions.isEmpty()) {
            return new Result(origin, List.of());
        }

        Set<BlockPos> unique = new LinkedHashSet<>(Math.min(positions.size() + 1, cap));
        if (ShapeSwitchConfig.previewShowOrigin()) {
            unique.add(origin.immutable());
        }
        for (BlockPos pos : positions) {
            if (pos == null) {
                continue;
            }
            unique.add(pos.immutable());
            if (unique.size() >= cap) {
                break;
            }
        }
        return new Result(origin, List.copyOf(new ArrayList<>(unique)));
    }

    /** 预览的包围盒尺寸（单位：方块），用于轮盘上显示“几×几×几”。 */
    public record Size(int x, int y, int z, int count) {
    }

    /**
     * 当前预览的尺寸摘要；没有目标方块或预览为空时返回 {@code null}。
     *
     * <p>复用 {@link #current()} 的缓存，因此可以每帧调用。</p>
     */
    public static Size lastSize() {
        Result result = current();
        if (result == null || result.blocks().isEmpty()) {
            return null;
        }
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (BlockPos pos : result.blocks()) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        if (minX > maxX) {
            return null;
        }
        return new Size(maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1, result.blocks().size());
    }

    private static Direction verticalDirection(LocalPlayer player) {
        float pitch = player.getXRot();
        if (pitch < -45.0F) {
            return Direction.UP;
        }
        if (pitch > 45.0F) {
            return Direction.DOWN;
        }
        return null;
    }
}
