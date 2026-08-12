package ganymedes01.etfuturum.blocks;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.EtFuturum;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.client.sound.ModSounds;
import ganymedes01.etfuturum.core.utils.Utils;
import ganymedes01.etfuturum.lib.RenderIDs;
import ganymedes01.etfuturum.world.generate.decorate.WorldGenAzaleaTree;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.IGrowable;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.EnumPlantType;
import net.minecraftforge.event.terraingen.TerrainGen;

import java.util.List;
import java.util.Random;

public class BlockAzalea extends BlockBush implements ISubBlocksBlock, IGrowable {

	public IIcon[] sideIcons;
	public IIcon[] topIcons;
	public int meta;

	private final String[] types = new String[]{"azalea", "flowering_azalea"};

	public BlockAzalea() {
		super(Material.wood);
		setHardness(0.0F);
		setResistance(0.0F);
		Utils.setBlockSound(this, ModSounds.soundAzalea);
		setBlockName(Utils.getUnlocalisedName("azalea"));
		setBlockTextureName("azalea");
		setCreativeTab(EtFuturum.creativeTabBlocks);
		setBlockBounds(0, 0, 0, 1, 1, 1);
	}

	@Override
	public boolean isOpaqueCube() {
		return false;
	}

	@Override
	public EnumPlantType getPlantType(IBlockAccess world, int x, int y, int z) {
		return EnumPlantType.Plains;
	}

	@Override
	public boolean canBlockStay(World world, int x, int y, int z) {
		Block ground = world.getBlock(x, y - 1, z);
		return ground == Blocks.dirt
				|| ground == Blocks.grass
				|| ground == Blocks.mycelium
				|| ground == Blocks.sand
				|| ground == Blocks.hardened_clay
				|| ground == Blocks.stained_hardened_clay
				|| ground == Blocks.snow
				|| (ModBlocks.COARSE_DIRT.isEnabled() && ground == ModBlocks.COARSE_DIRT.get())
				|| (ModBlocks.MOSS_BLOCK.isEnabled() && ground == ModBlocks.MOSS_BLOCK.get())
				|| (ModBlocks.ROOTED_DIRT.isEnabled() && ground == ModBlocks.ROOTED_DIRT.get())
				|| (ModBlocks.MUD.isEnabled() && ground == ModBlocks.MUD.get())
				|| (ModBlocks.MUDDY_MANGROVE_ROOTS.isEnabled() && ground == ModBlocks.MUDDY_MANGROVE_ROOTS.get());
	}

	@Override
	public void addCollisionBoxesToList(World worldIn, int x, int y, int z, AxisAlignedBB mask, List<AxisAlignedBB> list, Entity collider) {
		setBlockBounds(0.0F, 0.5F, 0.0F, 1.0F, 1.0F, 1.0F);
		super.addCollisionBoxesToList(worldIn, x, y, z, mask, list, collider);
		setBlockBounds(0.4375F, 0.5F, 0.4375F, 0.5625F, 1.0F, 0.5625F);
		super.addCollisionBoxesToList(worldIn, x, y, z, mask, list, collider);

		this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
	}

	@Override
	public AxisAlignedBB getSelectedBoundingBoxFromPool(World world, int x, int y, int z) {
		return AxisAlignedBB.getBoundingBox(x + 0.0F, y + 0.5F, z + 0.0F, x + 1.0F, y + 1.0F, z + 1.0F);
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
		return AxisAlignedBB.getBoundingBox(x + 0.0F, y + 0.5F, z + 0.0F, x + 1.0F, y + 1.0F, z + 1.0F);
	}

	@Override
	public void getSubBlocks(Item item, CreativeTabs tab, List<ItemStack> list) {
		for (int i = 0; i < getTypes().length; i++) {
			list.add(new ItemStack(item, 1, i));
		}
	}

	@Override
	public boolean isReplaceable(IBlockAccess world, int x, int y, int z) {
		return false;
	}

	@Override
	public int getRenderType() {
		return RenderIDs.AZALEA;
	}

	@Override
	public boolean shouldSideBeRendered(IBlockAccess worldIn, int x, int y, int z, int side) {
		return side != 0 && super.shouldSideBeRendered(worldIn, x, y, z, side);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister reg) {
		this.blockIcon = reg.registerIcon(this.getTextureName() + "_plant");

		sideIcons = new IIcon[2];
		topIcons = new IIcon[2];
		sideIcons[0] = reg.registerIcon(this.getTextureName() + "_side");
		sideIcons[1] = reg.registerIcon("flowering_" + this.getTextureName() + "_side");
		topIcons[0] = reg.registerIcon(this.getTextureName() + "_top");
		topIcons[1] = reg.registerIcon("flowering_" + this.getTextureName() + "_top");
	}

	@Override
	public int damageDropped(int meta) {
		return meta % getTypes().length;
	}

	@Override
	public IIcon[] getIcons() {
		return sideIcons;
	}

	@Override
	public IIcon getIcon(int side, int meta) {
		if (side == 0) {
			return this.blockIcon;
		}
		if (side == 1) {
			return topIcons[meta % topIcons.length];
		}
		return sideIcons[meta % topIcons.length];
	}

	@Override
	public String[] getTypes() {
		return types;
	}

	@Override
	public String getNameFor(ItemStack stack) {
		return getTypes()[stack.getItemDamage() % types.length];
	}

	/** MCP name: canFertilize. */
	@Override
	public boolean func_149851_a(World world, int x, int y, int z, boolean isClient) {
		return true;
	}

	/** MCP name: shouldFertilize. Java Edition azaleas succeed on roughly 45% of bone-meal attempts. */
	@Override
	public boolean func_149852_a(World world, Random rand, int x, int y, int z) {
		return rand.nextFloat() < 0.45F;
	}

	/** MCP name: fertilize. */
	@Override
	public void func_149853_b(World world, Random rand, int x, int y, int z) {
		if (world.isRemote || !TerrainGen.saplingGrowTree(world, rand, x, y, z)) {
			return;
		}

		int meta = world.getBlockMetadata(x, y, z);
		world.setBlockToAir(x, y, z);
		WorldGenAzaleaTree tree = new WorldGenAzaleaTree(true);
		if (!tree.generate(world, rand, x, y, z)) {
			world.setBlock(x, y, z, this, meta, 2);
		}
	}

	@Override
	public MapColor getMapColor(int meta) {
		return MapColor.grassColor;
	}
}