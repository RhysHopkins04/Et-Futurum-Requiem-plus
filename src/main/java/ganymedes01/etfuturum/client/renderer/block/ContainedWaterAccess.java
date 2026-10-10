package ganymedes01.etfuturum.client.renderer.block;

import ganymedes01.etfuturum.ModernMapParityBlocks;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.util.ForgeDirection;

/** Render-only fluid view: lets vanilla water share heights/occlusion across occupied sources. */
public final class ContainedWaterAccess implements IBlockAccess {
    private final IBlockAccess source;
    public ContainedWaterAccess(IBlockAccess source) { this.source = source; }
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
