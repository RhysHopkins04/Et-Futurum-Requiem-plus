package ganymedes01.etfuturum.client.renderer.block;

import com.gtnewhorizons.angelica.api.ThreadSafeISBRH;
import ganymedes01.etfuturum.blocks.ModernWallState;
import ganymedes01.etfuturum.lib.RenderIDs;
import net.minecraft.block.Block;
import net.minecraft.block.BlockWall;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.world.IBlockAccess;

/** Renders vanilla BlockWall and BaseWall variants with shared modern post/arm proportions. */
@ThreadSafeISBRH(perThread = false)
public final class BlockModernWallRenderer extends BlockModelBase {
    public BlockModernWallRenderer() {
        super(RenderIDs.MODERN_WALL);
    }

    @Override
    protected void renderInventoryModel(Block block, int meta, int modelId, RenderBlocks renderer,
            double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        renderStandardInventoryCube(block, meta, modelId, renderer, 0.25, 0, 0.25, 0.75, 1, 0.75);
        renderStandardInventoryCube(block, meta, modelId, renderer, 5.0/16.0, 0, 0, 11.0/16.0, 14.0/16.0, 0.5);
        renderStandardInventoryCube(block, meta, modelId, renderer, 5.0/16.0, 0, 0.5, 11.0/16.0, 14.0/16.0, 1);
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block,
            int modelId, RenderBlocks renderer) {
        if (!(block instanceof BlockWall)) return false;
        ModernWallState.State state = ModernWallState.derive(block, world, x, y, z);
        boolean rendered = false;
        if (state.up) rendered |= renderStandardWorldCube(world, x, y, z, block, modelId, renderer, 0.25, 0, 0.25, 0.75, 1, 0.75);
        if (state.north.isConnected()) rendered |= renderStandardWorldCube(world, x, y, z, block, modelId, renderer,
                5.0/16.0, 0, 0, 11.0/16.0, state.north == ModernWallState.Side.TALL ? 1.0 : 14.0/16.0, 0.5);
        if (state.south.isConnected()) rendered |= renderStandardWorldCube(world, x, y, z, block, modelId, renderer,
                5.0/16.0, 0, 0.5, 11.0/16.0, state.south == ModernWallState.Side.TALL ? 1.0 : 14.0/16.0, 1);
        if (state.west.isConnected()) rendered |= renderStandardWorldCube(world, x, y, z, block, modelId, renderer,
                0, 0, 5.0/16.0, 0.5, state.west == ModernWallState.Side.TALL ? 1.0 : 14.0/16.0, 11.0/16.0);
        if (state.east.isConnected()) rendered |= renderStandardWorldCube(world, x, y, z, block, modelId, renderer,
                0.5, 0, 5.0/16.0, 1, state.east == ModernWallState.Side.TALL ? 1.0 : 14.0/16.0, 11.0/16.0);
        renderer.setRenderBounds(0, 0, 0, 1, 1, 1);
        return rendered;
    }
}
