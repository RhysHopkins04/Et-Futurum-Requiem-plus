package ganymedes01.etfuturum.blocks;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.EtFuturum;
import ganymedes01.etfuturum.ModernMapParityBlocks;
import ganymedes01.etfuturum.client.model.ModernJsonModelBridge;
import ganymedes01.etfuturum.lib.RenderIDs;
import ganymedes01.etfuturum.client.sound.ModSounds;
import ganymedes01.etfuturum.configuration.configs.ConfigBlocksItems;
import ganymedes01.etfuturum.world.generate.decorate.WorldGenCherryTrees;
import lombok.NonNull;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSapling;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;
import net.minecraftforge.event.terraingen.TerrainGen;
import ganymedes01.etfuturum.api.IMultiBlockSound;

import java.util.List;
import java.util.Random;

public class BlockModernSapling extends BlockSapling implements ISubBlocksBlock, IMultiBlockSound {
	private final String[] types = new String[]{"mangrove_propagule", "cherry_sapling"};
	private final IIcon[] icons = new IIcon[types.length];

	public BlockModernSapling() {
		setStepSound(Block.soundTypeGrass);
		setCreativeTab(EtFuturum.creativeTabBlocks);
	}

	@Override
	public void getSubBlocks(Item itemIn, CreativeTabs tab, List<ItemStack> list) {
		if (ConfigBlocksItems.enableMangroveWoodFamily) {
			list.add(new ItemStack(itemIn, 1, 0));
		}
		if (ConfigBlocksItems.enableCherryBlocks) {
			list.add(new ItemStack(itemIn, 1, 1));
		}
	}

	@Override
	public IIcon getIcon(int side, int meta) {
		return icons[(meta & 7) % icons.length];
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister reg) {
		for (int i = 0; i < icons.length; ++i) {
			icons[i] = reg.registerIcon(types[i]);
		}
		if (ConfigBlocksItems.enableMangroveWoodFamily) ModernJsonModelBridge.prepare(ModernMapParityBlocks.MANGROVE_PROPAGULE, reg);
	}

	@Override
	public int getRenderType() {
		return RenderIDs.MODERN_MAP_PARITY;
	}

	public static int getMangroveAge(IBlockAccess world, int x, int y, int z) {
		TileEntity tile = world == null ? null : world.getTileEntity(x, y, z);
		return tile instanceof MangrovePropaguleStateTileEntity ? ((MangrovePropaguleStateTileEntity) tile).getAge() : 0;
	}

	public static boolean isMangroveHanging(IBlockAccess world, int x, int y, int z) {
		TileEntity tile = world == null ? null : world.getTileEntity(x, y, z);
		return tile instanceof MangrovePropaguleStateTileEntity && ((MangrovePropaguleStateTileEntity) tile).isHanging();
	}

	@Override
	public boolean hasTileEntity(int metadata) {
		return ConfigBlocksItems.enableMangroveWoodFamily && (metadata & 7) == 0;
	}

	@Override
	public TileEntity createTileEntity(World world, int metadata) {
		return hasTileEntity(metadata) ? new MangrovePropaguleStateTileEntity() : null;
	}

	@Override
	public void updateTick(World world, int x, int y, int z, Random random) {
		if ((world.getBlockMetadata(x, y, z) & 7) == 0 && isMangroveHanging(world, x, y, z)) return;
		super.updateTick(world, x, y, z, random);
	}

	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
		if ((world.getBlockMetadata(x, y, z) & 7) == 0 && isMangroveHanging(world, x, y, z)) return;
		super.onNeighborBlockChange(world, x, y, z, neighbor);
	}

	public static final class MangrovePropaguleStateTileEntity extends TileEntity {
		private boolean hanging;
		private int age;

		public boolean isHanging() { return hanging; }
		public int getAge() { return age; }
		public void setVisualState(boolean hanging, int age) {
			this.hanging = hanging;
			this.age = Math.max(0, Math.min(4, age));
			markDirty();
			if (worldObj != null) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
		}
		@Override public void writeToNBT(NBTTagCompound tag) {
			super.writeToNBT(tag); tag.setBoolean("Hanging", hanging); tag.setByte("Age", (byte) age);
		}
		@Override public void readFromNBT(NBTTagCompound tag) {
			super.readFromNBT(tag); hanging = tag.getBoolean("Hanging"); age = Math.max(0, Math.min(4, tag.getByte("Age")));
		}
		@Override public Packet getDescriptionPacket() {
			NBTTagCompound tag = new NBTTagCompound(); writeToNBT(tag);
			return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 34, tag);
		}
		@Override public void onDataPacket(NetworkManager network, S35PacketUpdateTileEntity packet) {
			readFromNBT(packet.func_148857_g());
			if (worldObj != null) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
		}
	}

	private static final WorldGenAbstractTree cherry = new WorldGenCherryTrees(true);

	/**
	 * MCP name: {@code growTree}
	 */
	@Override
	public void func_149878_d(World p_149878_1_, int p_149878_2_, int p_149878_3_, int p_149878_4_, Random p_149878_5_) {
		if (!TerrainGen.saplingGrowTree(p_149878_1_, p_149878_5_, p_149878_2_, p_149878_3_, p_149878_4_)) {
			return;
		}

		int l = p_149878_1_.getBlockMetadata(p_149878_2_, p_149878_3_, p_149878_4_) & 7;
		WorldGenAbstractTree tree = null;

		switch (l) {
			case 1:
				if (ConfigBlocksItems.enableCherryBlocks) {
					tree = cherry;
				}
				break;
		}

		if (tree != null) {
			Block block = p_149878_1_.getBlock(p_149878_2_, p_149878_3_, p_149878_4_);
			int meta = p_149878_1_.getBlockMetadata(p_149878_2_, p_149878_3_, p_149878_4_);
			p_149878_1_.setBlock(p_149878_2_, p_149878_3_, p_149878_4_, Blocks.air);
			boolean success = tree.generate(p_149878_1_, p_149878_5_, p_149878_2_, p_149878_3_, p_149878_4_);
			if (!success) {
				p_149878_1_.setBlock(p_149878_2_, p_149878_3_, p_149878_4_, block, meta, 2);
			}
		}
	}

	@Override
	public IIcon[] getIcons() {
		return icons;
	}

	@Override
	public String[] getTypes() {
		return types;
	}

	@Override
	public String getNameFor(ItemStack stack) {
		return types[stack.getItemDamage() % types.length];
	}

	@Override
	@NonNull
	public Block.SoundType getSoundType(World world, int x, int y, int z, SoundMode type) {
		int meta = world.getBlockMetadata(x, y, z);
		return meta == 1 || meta == 9 ? ModSounds.soundCherrySapling : stepSound;
	}
}
