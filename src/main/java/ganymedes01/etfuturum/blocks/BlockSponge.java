package ganymedes01.etfuturum.blocks;

import ganymedes01.etfuturum.EtFuturum;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.ModernMapParityBlocks;
import ganymedes01.etfuturum.Tags;
import ganymedes01.etfuturum.client.sound.ModSounds;
import ganymedes01.etfuturum.configuration.configs.ConfigSounds;
import ganymedes01.etfuturum.configuration.configs.ConfigWorld;
import ganymedes01.etfuturum.core.utils.Utils;
import ganymedes01.etfuturum.world.WorldCoord;
import lombok.NonNull;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.util.ForgeDirection;
import org.apache.commons.lang3.ArrayUtils;
import ganymedes01.etfuturum.api.IMultiBlockSound;

import java.util.*;

public class BlockSponge extends BaseSubtypesBlock implements IMultiBlockSound {

	private static final int ABSORB_RADIUS = 6;
	private static final int MAX_ABSORBED_CELLS = 64;

	public BlockSponge() {
		super(Material.sponge, "sponge", "wet_sponge");
		setHardness(0.6F);
		setBlockSound(ModSounds.soundSponge);
		setBlockTextureName("sponge");
		setBlockName(Utils.getUnlocalisedName("sponge"));
		setCreativeTab(EtFuturum.creativeTabBlocks);
	}

	@Override
	public String getNameFor(ItemStack stack) {
		return stack.getItemDamage() == 1 ? getTypes()[1] : Blocks.sponge.getUnlocalizedName();
	}

	@Override
	public void onBlockAdded(World world, int x, int y, int z) {
		tryAbsorb(world, x, y, z, world.getBlockMetadata(x, y, z) == 1);
	}

	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block neighborBlock) {
		tryAbsorb(world, x, y, z, world.getBlockMetadata(x, y, z) == 1);
		super.onNeighborBlockChange(world, x, y, z, neighborBlock);
	}

	protected void tryAbsorb(World worldIn, int x, int y, int z, boolean wet) {
		if (worldIn.isRemote) return;
		if (!ArrayUtils.contains(BiomeDictionary.getTypesForBiome(worldIn.getBiomeGenForCoords(x, z)), BiomeDictionary.Type.NETHER)) {
			if (!wet) {
				List<WorldCoord> absorbed = absorb(worldIn, x, y, z);
				if (absorbed.isEmpty()) return;
				worldIn.setBlockMetadataWithNotify(x, y, z, 1, 2);
				// Finish the traversal before neighboring updates can activate this sponge again.
				for (WorldCoord cell : absorbed) {
					worldIn.notifyBlocksOfNeighborChange(cell.x, cell.y, cell.z, Blocks.air);
				}
				if (ConfigSounds.newBlockSounds) {
					worldIn.playSoundEffect(x + .5D, y + .5D, z + .5D, Tags.MC_ASSET_VER + ":block.sponge.absorb", 1, 1);
				}
			}
		} else if (wet) {
			worldIn.playSoundEffect(x + .5D, y + .5D, z + .5D, "random.fizz", 1, 1);
			worldIn.setBlockMetadataWithNotify(x, y, z, 0, 2);
		}
	}

	private List<WorldCoord> absorb(World world, int x, int y, int z) {
		Queue<WorldCoord> frontier = new ArrayDeque<>();
		List<WorldCoord> absorbed = new ArrayList<>();
		frontier.add(new WorldCoord(x, y, z));

		for (int radius = 1; radius <= ABSORB_RADIUS && !frontier.isEmpty(); radius++) {
			int layerSize = frontier.size();
			for (int index = 0; index < layerSize; index++) {
				WorldCoord from = frontier.remove();
				for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
					WorldCoord target = from.add(direction);
					if (world.blockExists(target.x, target.y, target.z)
							&& ModernMapParityBlocks.absorbWater(world, target.x, target.y, target.z)) {
						absorbed.add(target);
						if (absorbed.size() == MAX_ABSORBED_CELLS) return absorbed;
						frontier.add(target);
					}
				}
			}
		}
		return absorbed;
	}

	@Override
	public void randomDisplayTick(World world, int x, int y, int z, Random rand) {
		if (world.getBlockMetadata(x, y, z) == 1) {
			ForgeDirection dir = getRandomDirection(rand);

			if (dir != ForgeDirection.UP && !World.doesBlockHaveSolidTopSurface(world, x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ)) {
				double d0 = x;
				double d1 = y;
				double d2 = z;

				if (dir == ForgeDirection.DOWN) {
					d1 -= 0.05D;
					d0 += rand.nextDouble();
					d2 += rand.nextDouble();
				} else {
					d1 += rand.nextDouble() * 0.8D;

					if (dir == ForgeDirection.EAST || dir == ForgeDirection.WEST) {
						d2 += rand.nextDouble();

						if (dir == ForgeDirection.EAST)
							d0++;
						else
							d0 += 0.05D;
					} else {
						d0 += rand.nextDouble();

						if (dir == ForgeDirection.SOUTH)
							d2++;
						else
							d2 += 0.05D;
					}
				}

				world.spawnParticle("dripWater", d0, d1, d2, 0.0D, 0.0D, 0.0D);
			}
		}
	}

	@Override
	public void getSubBlocks(Item item, CreativeTabs tab, List<ItemStack> list) {
		list.add(new ItemStack(item, 1, 1));
	}

	private ForgeDirection getRandomDirection(Random rand) {
		return ForgeDirection.VALID_DIRECTIONS[rand.nextInt(ForgeDirection.VALID_DIRECTIONS.length)];
	}

	@Override
	public Item getItem(World world, int x, int y, int z) {
		int meta = world.getBlockMetadata(x, y, z);
		return Item.getItemFromBlock(ConfigWorld.tileReplacementMode == -1 || meta == 1 ? ModBlocks.SPONGE.get() : Blocks.sponge);
	}

	@Override
	public Item getItemDropped(int meta, Random rand, int fortune) {
		return Item.getItemFromBlock(ConfigWorld.tileReplacementMode == -1 || meta == 1 ? ModBlocks.SPONGE.get() : Blocks.sponge);
	}

	@Override
	public @NonNull SoundType getSoundType(World world, int i, int i1, int i2, SoundMode soundMode) {
		return world.getBlockMetadata(i, i1, i2) == 1 ? ModSounds.soundWetSponge : stepSound;
	}
}
