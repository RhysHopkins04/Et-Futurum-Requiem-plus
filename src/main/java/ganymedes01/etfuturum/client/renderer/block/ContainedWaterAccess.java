package ganymedes01.etfuturum.client.renderer.block;

import ganymedes01.etfuturum.ModernMapParityBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.util.ForgeDirection;

/** Render-only fluid view: lets vanilla water share heights/occlusion across occupied sources. */
public final class ContainedWaterAccess implements IBlockAccess {
    private final IBlockAccess source;
    public ContainedWaterAccess(IBlockAccess source) { this.source = source; }
    public IBlockAccess source() { return source; }

    public float chestWaterHeight(int x, int y, int z, float sampledHeight) {
        float sourceHeight = 1.0F - BlockLiquid.getLiquidHeightPercent(0);
        if (sampledHeight >= sourceHeight) return sampledHeight;

        // Keep blended corners where the chest joins other water. On its own,
        // air beyond a chest's corners must not tilt the contained source.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if ((dx != 0 || dz != 0) && getBlock(x + dx, y, z + dz).getMaterial() == Material.water)
                    return sampledHeight;
                if (getBlock(x + dx, y + 1, z + dz).getMaterial() == Material.water)
                    return sampledHeight;
            }
        }
        return sourceHeight;
    }

    public Block getBlock(int x,int y,int z) { return ModernMapParityBlocks.containsWater(source,x,y,z) ? Blocks.water : source.getBlock(x,y,z); }
    public int getBlockMetadata(int x,int y,int z) { return ModernMapParityBlocks.containsWater(source,x,y,z) ? 0 : source.getBlockMetadata(x,y,z); }
    public TileEntity getTileEntity(int x,int y,int z) { return source.getTileEntity(x,y,z); }
    public int getLightBrightnessForSkyBlocks(int x,int y,int z,int light) { return source.getLightBrightnessForSkyBlocks(x,y,z,light); }
    public int isBlockProvidingPowerTo(int x,int y,int z,int side) { return source.isBlockProvidingPowerTo(x,y,z,side); }
    public boolean isAirBlock(int x,int y,int z) { return source.isAirBlock(x,y,z); }
    public BiomeGenBase getBiomeGenForCoords(int x,int z) { return source.getBiomeGenForCoords(x,z); }
    public int getHeight() { return ganymedes01.etfuturum.core.utils.WorldHeightCompat.heightOf(source); }
    public boolean extendedLevelsInChunkCache() { return source.extendedLevelsInChunkCache(); }
    public boolean isSideSolid(int x,int y,int z,ForgeDirection side,boolean fallback) { return source.isSideSolid(x,y,z,side,fallback); }
}
