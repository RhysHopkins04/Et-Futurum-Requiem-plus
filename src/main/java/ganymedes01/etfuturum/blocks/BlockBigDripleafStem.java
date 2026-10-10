package ganymedes01.etfuturum.blocks;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.client.sound.ModSounds;
import ganymedes01.etfuturum.core.utils.Utils;
import ganymedes01.etfuturum.lib.RenderIDs;
import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

/** Internal, non-inventory stem block for big dripleaf columns. */
public class BlockBigDripleafStem extends Block implements IGrowable {

    public static final int WATERLOGGED_BIT = 4;

    private IIcon stemIcon;

    public BlockBigDripleafStem() {
        super(Material.plants);
        setBlockName(Utils.getUnlocalisedName("big_dripleaf_stem"));
        setBlockTextureName("big_dripleaf_stem");
        Utils.setBlockSound(this, ModSounds.soundBigDripleaf);
        setHardness(0.1F);
        setBlockBounds(0.25F, 0.0F, 0.25F, 0.75F, 1.0F, 0.75F);
    }

    @Override
    public boolean canRenderInPass(int pass) { return pass == 0 || pass == 1; }

    @Override
    @SideOnly(Side.CLIENT)
    public int getRenderBlockPass() { return 1; }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public int getRenderType() {
        return RenderIDs.BIG_DRIPLEAF;
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return null;
    }

    @Override
    public boolean isReplaceable(IBlockAccess world, int x, int y, int z) {
        return false;
    }

    @Override
    public int onBlockPlaced(World world, int x, int y, int z, int side,
                              float hitX, float hitY, float hitZ, int metadata) {
        Block b = world.getBlock(x, y, z);
        return (metadata & 3) | ((b == net.minecraft.init.Blocks.water || b == net.minecraft.init.Blocks.flowing_water)
                && (world.getBlockMetadata(x, y, z) & 15) == 0 ? WATERLOGGED_BIT : 0);
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        // Same footprint as BlockBigDripleafRenderer.renderStem (rotated model
        // coordinates x=5..11, z=9..15 in 1/16ths). Avoid the oversized central
        // selection cube that did not match the visible stem.
        int facing = world.getBlockMetadata(x, y, z) & 3;
        if (facing == 0) setBlockBounds(5F / 16F, 0, 9F / 16F, 11F / 16F, 1, 15F / 16F);
        else if (facing == 1) setBlockBounds(9F / 16F, 0, 5F / 16F, 15F / 16F, 1, 11F / 16F);
        else if (facing == 2) setBlockBounds(5F / 16F, 0, 1F / 16F, 11F / 16F, 1, 7F / 16F);
        else setBlockBounds(1F / 16F, 0, 5F / 16F, 7F / 16F, 1, 11F / 16F);
    }

    @Override
    public AxisAlignedBB getSelectedBoundingBoxFromPool(World world, int x, int y, int z) {
        setBlockBoundsBasedOnState(world, x, y, z);
        return super.getSelectedBoundingBoxFromPool(world, x, y, z);
    }

    @Override
    public boolean canBlockStay(World world, int x, int y, int z) {
        Block below = world.getBlock(x, y - 1, z);
        if (below != this && !BlockBigDripleaf.isValidBigDripleafGround(world, x, y - 1, z))
            return false;
        // Modern big dripleaf is one connected plant: after the top is removed,
        // its lower stem sections must also collapse, dropping an item per part.
        int topY = y + 1;
        while (topY < world.getHeight() && world.getBlock(x, topY, z) == this) topY++;
        return topY < world.getHeight() && world.getBlock(x, topY, z) instanceof BlockBigDripleaf;
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (!world.isRemote && !canBlockStay(world, x, y, z)) {
            // Drop one Big Dripleaf per stem, including neighbour-driven collapse.
            world.func_147480_a(x, y, z, true);
        }
    }

    @Override
    public Item getItemDropped(int meta, Random random, int fortune) {
        // There is no standalone stem item in modern Minecraft; breaking a stem yields
        // the normal Big Dripleaf item instead.
        return Item.getItemFromBlock(ModBlocks.BIG_DRIPLEAF.get());
    }

    @Override
    public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z, EntityPlayer player) {
        return ModBlocks.BIG_DRIPLEAF.newItemStack();
    }

    private int findTop(World world, int x, int y, int z) {
        int scanY = y;
        while (scanY < world.getHeight() - 1 && world.getBlock(x, scanY, z) == this) {
            scanY++;
        }
        return world.getBlock(x, scanY, z) instanceof BlockBigDripleaf ? scanY : -1;
    }

    /** MCP: canFertilize */
    @Override
    public boolean func_149851_a(World world, int x, int y, int z, boolean isClient) {
        int topY = findTop(world, x, y, z);
        return topY >= 0 && ((BlockBigDripleaf) world.getBlock(x, topY, z))
                .func_149851_a(world, x, topY, z, isClient);
    }

    /** MCP: shouldFertilize */
    @Override
    public boolean func_149852_a(World world, Random random, int x, int y, int z) {
        return true;
    }

    /** MCP: fertilize */
    @Override
    public void func_149853_b(World world, Random random, int x, int y, int z) {
        int topY = findTop(world, x, y, z);
        if (topY >= 0 && world.getBlock(x, topY, z) instanceof BlockBigDripleaf) {
            ((BlockBigDripleaf) world.getBlock(x, topY, z)).growOne(world, x, topY, z);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister reg) {
        stemIcon = reg.registerIcon("big_dripleaf_stem");
        blockIcon = stemIcon;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getStemIcon() {
        return stemIcon;
    }
}
