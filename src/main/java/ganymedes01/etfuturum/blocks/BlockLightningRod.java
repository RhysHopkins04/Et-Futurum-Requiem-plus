package ganymedes01.etfuturum.blocks;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.ModernMapParityBlocks;
import ganymedes01.etfuturum.client.model.ModernJsonModelBridge;
import ganymedes01.etfuturum.client.sound.ModSounds;
import ganymedes01.etfuturum.lib.RenderIDs;
import ganymedes01.etfuturum.tileentities.TileEntityLightningRod;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockLightningRod extends BaseBlock {

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
		// 0 down, 1 up, 2 north, 3 south, 4 west, 5 east.
		return side >= 0 && side <= 5 ? side : 1;
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
