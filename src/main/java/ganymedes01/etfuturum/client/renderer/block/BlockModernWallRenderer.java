package ganymedes01.etfuturum.client.renderer.block;

import com.gtnewhorizons.angelica.api.ThreadSafeISBRH;
import ganymedes01.etfuturum.blocks.BaseWall;
import ganymedes01.etfuturum.lib.RenderIDs;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.world.IBlockAccess;

/** Renders BaseWall variants with modern post/arm proportions while retaining their metadata textures. */
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
        if (!(block instanceof BaseWall)) return false;
        BaseWall wall = (BaseWall) block;
        boolean north = wall.canConnectWallTo(world, x, y, z - 1);
        boolean south = wall.canConnectWallTo(world, x, y, z + 1);
        boolean west = wall.canConnectWallTo(world, x - 1, y, z);
        boolean east = wall.canConnectWallTo(world, x + 1, y, z);
        boolean rendered = false;
        if (wall.hasModernPost(world, x, y, z)) rendered |= renderStandardWorldCube(world, x, y, z, block, modelId, renderer, 0.25, 0, 0.25, 0.75, 1, 0.75);
        if (north) rendered |= renderStandardWorldCube(world, x, y, z, block, modelId, renderer, 5.0/16.0, 0, 0, 11.0/16.0, 14.0/16.0, 0.5);
        if (south) rendered |= renderStandardWorldCube(world, x, y, z, block, modelId, renderer, 5.0/16.0, 0, 0.5, 11.0/16.0, 14.0/16.0, 1);
        if (west) rendered |= renderStandardWorldCube(world, x, y, z, block, modelId, renderer, 0, 0, 5.0/16.0, 0.5, 14.0/16.0, 11.0/16.0);
        if (east) rendered |= renderStandardWorldCube(world, x, y, z, block, modelId, renderer, 0.5, 0, 5.0/16.0, 1, 14.0/16.0, 11.0/16.0);
        renderer.setRenderBounds(0, 0, 0, 1, 1, 1);
        return rendered;
    }
}
