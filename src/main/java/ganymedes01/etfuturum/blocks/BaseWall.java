package ganymedes01.etfuturum.blocks;

import com.google.common.collect.Maps;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.EtFuturum;
import ganymedes01.etfuturum.core.utils.Utils;
import ganymedes01.etfuturum.lib.RenderIDs;
import net.minecraft.block.Block;
import net.minecraft.block.BlockWall;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.List;
import java.util.Map;

public class BaseWall extends BlockWall implements ISubBlocksBlock {

	private IIcon[] icons;
	private final String[] types;

	private Block mapColorBase;

	public BaseWall(Material material, String... names) {
		super(Blocks.stone); //Dummy value, we really don't care what we put here because we're not using this value
		blockMaterial = material;
		this.types = names;
		setNames(names[0].replace("bricks", "brick").replace("tiles", "tile") + "_wall");
		setCreativeTab(EtFuturum.creativeTabBlocks);
	}

	@Override
	public IIcon[] getIcons() {
		return icons;
	}

	protected void setIcons(IIcon[] icons) {
		this.icons = icons;
	}

	@Override
	public String[] getTypes() {
		return types;
	}

	@Override
	public String getNameFor(ItemStack stack) {
		String type = getTypes()[Math.max(0, (stack.getItemDamage()) % getTypes().length)];
		type = ("".equals(type) ? getUnlocalizedName().replace("tile.", "").replace(getNameDomain() + ".", "") : type);
		if (type.toLowerCase().endsWith("bricks") || type.toLowerCase().endsWith("tiles")) {
			type = type.substring(0, type.length() - 1);
		}
		return type + "_wall";
	}

	@Override
	public int damageDropped(int meta) {
		return meta % getTypes().length;
	}

	@Override
	public void getSubBlocks(Item item, CreativeTabs tab, List<ItemStack> list) {
		for (int i = 0; i < getTypes().length; i++) {
			list.add(new ItemStack(item, 1, i));
		}
	}

	@Override
	public IIcon getIcon(int side, int meta) {
		return getIcons()[meta % icons.length];
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister reg) {
		setIcons(new IIcon[getTypes().length]);
		for (int i = 0; i < getIcons().length; i++) {
			getIcons()[i] = "".equals(getTypes()[i]) ? reg.registerIcon(getTextureName()) :
					reg.registerIcon((getTextureDomain().isEmpty() ? "" : getTextureDomain() + ":")
							+ (getTextureSubfolder().isEmpty() ? "" : getTextureSubfolder() + "/") + getTypes()[i]);
		}
	}

	@Override
	public boolean canConnectWallTo(IBlockAccess world, int x, int y, int z) {
		return ModernWallState.canConnectWallTo(this, world, x, y, z);
	}

	@Override
	public boolean canPlaceTorchOnTop(World world, int x, int y, int z) {
		return true;
	}

	/** Modern walls use an 8px post and neighbour-derived none/low/tall side states. */
	public boolean hasModernPost(IBlockAccess world, int x, int y, int z) {
		return ModernWallState.derive(this, world, x, y, z).up;
	}

	@Override
	public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
		ModernWallState.State state = ModernWallState.derive(this, world, x, y, z);
		float minX = state.west.isConnected() ? 0.0F : 0.25F;
		float maxX = state.east.isConnected() ? 1.0F : 0.75F;
		float minZ = state.north.isConnected() ? 0.0F : 0.25F;
		float maxZ = state.south.isConnected() ? 1.0F : 0.75F;
		setBlockBounds(minX, 0.0F, minZ, maxX, state.getVisualMaxY(), maxZ);
	}

	@Override
	public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB mask,
			List<AxisAlignedBB> list, Entity collider) {
		ModernWallState.State state = ModernWallState.derive(this, world, x, y, z);
		if (state.up) addWallCollision(mask, list, x, y, z, 0.25, 0, 0.25, 0.75, 1.5, 0.75);
		if (state.north.isConnected()) addWallCollision(mask, list, x, y, z, 5.0/16.0, 0, 0, 11.0/16.0, 1.5, 0.5);
		if (state.south.isConnected()) addWallCollision(mask, list, x, y, z, 5.0/16.0, 0, 0.5, 11.0/16.0, 1.5, 1);
		if (state.west.isConnected()) addWallCollision(mask, list, x, y, z, 0, 0, 5.0/16.0, 0.5, 1.5, 11.0/16.0);
		if (state.east.isConnected()) addWallCollision(mask, list, x, y, z, 0.5, 0, 5.0/16.0, 1, 1.5, 11.0/16.0);
	}

	private static void addWallCollision(AxisAlignedBB mask, List<AxisAlignedBB> list, int x, int y, int z,
			double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
		AxisAlignedBB box = AxisAlignedBB.getBoundingBox(x + minX, y + minY, z + minZ,
				x + maxX, y + maxY, z + maxZ);
		if (mask == null || box.intersectsWith(mask)) list.add(box);
	}

	@Override
	public AxisAlignedBB getSelectedBoundingBoxFromPool(World world, int x, int y, int z) {
		setBlockBoundsBasedOnState(world, x, y, z);
		return super.getSelectedBoundingBoxFromPool(world, x, y, z);
	}

	@Override public int getRenderType() { return RenderIDs.MODERN_WALL; }
	@Override public boolean renderAsNormalBlock() { return false; }
	@Override public boolean isOpaqueCube() { return false; }


	public BaseWall setUnlocalizedNameWithPrefix(String name) {
		setBlockName((getNameDomain().isEmpty() ? "" : getNameDomain() + ".") + name);
		return this;
	}

	@Override
	public Block setBlockTextureName(String name) {
		return super.setBlockTextureName((getTextureDomain().isEmpty() ? "" : getTextureDomain() + ":")
				+ (getTextureSubfolder().isEmpty() ? "" : getTextureSubfolder() + "/") + name);
	}

	public BaseWall setNames(String name) {
		setUnlocalizedNameWithPrefix(name);
		setBlockTextureName(name);
		return this;
	}

	public BaseWall setToolClass(String toolClass, int level) {
		setHarvestLevel(toolClass, level);
		return this;
	}

	public BaseWall setToolClass(String toolClass, int level, int meta) {
		setHarvestLevel(toolClass, level, meta);
		return this;
	}

	public BaseWall setBlockSound(SoundType type) {
		Utils.setBlockSound(this, type);
		return this;
	}

	public BaseWall setMapColorBaseBlock(Block block) {
		mapColorBase = block;
		return this;
	}

	@Override
	public MapColor getMapColor(int meta) {
		return mapColorBase == null ? super.getMapColor(meta) : mapColorBase.getMapColor(meta);
	}

	private final Map<Integer, Float> hardnesses = Maps.newHashMap();
	private final Map<Integer, Float> resistances = Maps.newHashMap();

	@Override
	public float getBlockHardness(World worldIn, int x, int y, int z) {
		return hardnesses.getOrDefault(worldIn.getBlockMetadata(x, y, z), super.getBlockHardness(worldIn, x, y, z));
	}

	public BaseWall setHardnesses(float hardness, int... metas) {
		if (metas.length == 0) {
			setHardness(hardness);
		} else for (int meta : metas) {
			hardnesses.put(meta, hardness);
		}
		return this;
	}

	@Override
	public float getExplosionResistance(Entity par1Entity, World world, int x, int y, int z, double explosionX, double explosionY, double explosionZ) {
		Float resistance = resistances.get(world.getBlockMetadata(x, y, z));
		if (resistance != null) {
			return resistance / 5.0F;
		}
		return super.getExplosionResistance(par1Entity, world, x, y, z, explosionX, explosionY, explosionZ);
	}

	public BaseWall setResistances(float resistance, int... metas) {
		if (metas.length == 0) {
			setResistance(resistance);
		} else for (int meta : metas) {
			resistances.put(meta, resistance);
		}
		return this;
	}
}
