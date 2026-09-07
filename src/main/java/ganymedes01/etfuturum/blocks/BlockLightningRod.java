package ganymedes01.etfuturum.blocks;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.ModernMapParityBlocks;
import ganymedes01.etfuturum.client.model.ModernJsonModelBridge;
import ganymedes01.etfuturum.client.sound.ModSounds;
import ganymedes01.etfuturum.lib.RenderIDs;
import ganymedes01.etfuturum.tileentities.TileEntityLightningRod;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

public class BlockLightningRod extends BaseBlock implements IDegradable {

	public BlockLightningRod() {
		super(Material.iron);
		setHardness(3);
		setResistance(6);
		setHarvestLevel("pickaxe", 1);
		setNames("lightning_rod");
		setBlockSound(ModSounds.soundCopper);
		setTickRandomly(true);
		setBlockBounds(0.375F, 0.0F, 0.375F, 0.625F, 1.0F, 0.625F);
	}

	@Override
	public int onBlockPlaced(World world, int x, int y, int z, int side,
			float hitX, float hitY, float hitZ, int meta) {
		// Same compact facing convention used by the modern lightning-rod parity variants:
		// 0 down, 1 up, 2 north, 3 south, 4 west, 5 east. The legacy clicked-side
		// convention renders horizontal modern rod models 180 degrees inward, so invert only
		// wall placement; floor/ceiling placement is already correct.
		switch (side) {
			case 2: return 3;
			case 3: return 2;
			case 4: return 5;
			case 5: return 4;
			case 0: return 0;
			case 1: return 1;
			default: return 1;
		}
	}

	@Override
	public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
		int facing = world.getBlockMetadata(x, y, z) & 7;
		if (facing == 2 || facing == 3) {
			setBlockBounds(0.375F, 0.375F, 0.0F, 0.625F, 0.625F, 1.0F);
		} else if (facing == 4 || facing == 5) {
			setBlockBounds(0.0F, 0.375F, 0.375F, 1.0F, 0.625F, 0.625F);
		} else {
			setBlockBounds(0.375F, 0.0F, 0.375F, 0.625F, 1.0F, 0.625F);
		}
	}

	@Override
	public void updateTick(World world, int x, int y, int z, Random random) {
		tickDegradation(world, x, y, z, random);
	}

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side,
			float hitX, float hitY, float hitZ) {
		return tryWaxOnWaxOff(world, x, y, z, player);
	}

	@Override
	public int getCopperMeta(int meta) {
		return 0;
	}

	@Override
	public Block getCopperBlockFromMeta(int meta) {
		return ModernMapParityBlocks.getPass36CopperBlock(this, meta);
	}

	@Override
	public int getFinalCopperMeta(IBlockAccess world, int x, int y, int z, int meta, int worldMeta) {
		return worldMeta;
	}

	@Override
	public int getRenderType() {
		// Reuse the exact AssetDirector JSON model path used by the modern oxidisation/wax variants.
		return RenderIDs.MODERN_MAP_PARITY;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister reg) {
		// Default copper and waxed-default copper are visually identical. Reusing the already-audited
		// waxed lightning-rod model eliminates the old incomplete 1.7 texture/cube fallback entirely.
		blockIcon = reg.registerIcon("minecraft:stone");
		ModernJsonModelBridge.PreparedModels prepared = ModernJsonModelBridge.prepare(ModernMapParityBlocks.WAXED_LIGHTNING_ROD, reg);
		IIcon fallback = ModernJsonModelBridge.getFallbackIcon(prepared);
		if (fallback != null) blockIcon = fallback;
	}

	public TileEntity createNewTileEntity(World worldIn, int meta) {
		return new TileEntityLightningRod();
	}

	/**
	 * Is this block (a) opaque and (b) a full 1m cube?  This determines whether or not to render the shared face of two
	 * adjacent blocks and also whether the player can attach torches, redstone wire, etc to this block.
	 */
	@Override
	public boolean isOpaqueCube() {
		return false;
	}

	/**
	 * If this block doesn't render as an ordinary block it will return False (examples: signs, buttons, stairs, etc)
	 */
	@Override
	public boolean renderAsNormalBlock() {
		return false;
	}
}
