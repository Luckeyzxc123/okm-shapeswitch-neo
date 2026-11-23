package dev.okmplus.shapeswitch.client;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.Util;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.xiyu.onekeyminer.api.OneKeyMinerAPI;
import org.xiyu.onekeyminer.config.ConfigManager;
import org.xiyu.onekeyminer.config.MinerConfig;
import org.xiyu.onekeyminer.shape.ChainShape;
import org.xiyu.onekeyminer.shape.ShapeContext;

/**
 * 在客户端把“当前选中的形状 + 准星目标方块”算成一组预览坐标。
 *
 * <p>形状算法本身来自 OKM（{@link ChainShape#getPreviewPositions(ShapeContext)}），
 * 因此预览结果与实际连锁挖掘的形状一致，并且自定义形状同样支持
 * （不模拟工具限制与黑白名单）。</p>
 */
public final class PreviewGeometry {

    /** 预览结果。origin 单独给出，便于用不同颜色高亮。 */
    public record Result(BlockPos origin, List<BlockPos> blocks) {
        public static final Result EMPTY = new Result(null, List.of());
    }

    private static final long CACHE_MILLIS = 100L;

    /** 同一帧内多次调用时复用结果。 */
    private static String cacheKey = "";
    private static long cacheTime = Long.MIN_VALUE;
    private static Result cache = Result.EMPTY;

    private PreviewGeometry() {
    }

    public static Result current() {
        if (!ShapeSwitchConfig.isReady()) {
            return Result.EMPTY;
        }
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

        ChainShape shape = lookupShape(shapeId);
        if (shape == null) {
            return Result.EMPTY;
        }

        int cap = ShapeSwitchConfig.PREVIEW_MAX_BLOCKS.get();
        int maxBlocks = 64;
        int maxDistance = 16;
        boolean allowDiagonal = true;
        if (ShapeSwitchConfig.PREVIEW_MATCH_OKM_LIMITS.get()) {
            Integer okmMaxBlocks = readOkmInt("maxBlocks");
            if (okmMaxBlocks != null && okmMaxBlocks > 0) {
                maxBlocks = okmMaxBlocks;
            }
            Integer okmMaxDistance = readOkmInt("maxDistance");
            if (okmMaxDistance != null && okmMaxDistance > 0) {
                maxDistance = okmMaxDistance;
            }
            Boolean okmDiagonal = readOkmAllowDiagonal();
            if (okmDiagonal != null) {
                allowDiagonal = okmDiagonal;
            }
        }
        // 先限流再计算：否则大形状每 100ms 都可能遍历上万个方块。
        maxBlocks = Math.max(1, Math.min(maxBlocks, cap));

        List<BlockPos> positions;
        try {
            ShapeContext context = new ShapeContext.Builder()
                    .level(level)
                    .originPos(origin)
                    .originState(state)
                    .playerFacing(player.getDirection())
                    .playerLookingVertical(verticalDirection(player))
                    .maxBlocks(maxBlocks)
                    .maxDistance(maxDistance)
                    .allowDiagonal(allowDiagonal)
                    .blockMatcher((originState, target) -> target != null
                            && !target.isAir()
                            && target.getBlock() == originState.getBlock())
                    .build();
            positions = shape.getPreviewPositions(context);
        } catch (RuntimeException | LinkageError e) {
            return Result.EMPTY;
        }
        if (positions == null || positions.isEmpty()) {
            return new Result(origin, List.of());
        }

        Set<BlockPos> unique = new LinkedHashSet<>(Math.min(positions.size() + 1, cap));
        if (ShapeSwitchConfig.PREVIEW_SHOW_ORIGIN.get()) {
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

    private static ChainShape lookupShape(String shapeId) {
        try {
            ResourceLocation id = ResourceLocation.tryParse(shapeId);
            return id == null ? null : OneKeyMinerAPI.getShape(id);
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
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

    // OKM 的配置字段在 1.6.x 各版本间保持一致，但这里仍然容错：
    // 主模组换了实现或者配置还没加载时，预览退化成默认值即可。
    private static Integer readOkmInt(String field) {
        try {
            MinerConfig config = ConfigManager.getConfig();
            return switch (field) {
                case "maxBlocks" -> config.maxBlocks;
                case "maxDistance" -> config.maxDistance;
                default -> null;
            };
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    private static Boolean readOkmAllowDiagonal() {
        try {
            return ConfigManager.getConfig().allowDiagonal;
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }
}
