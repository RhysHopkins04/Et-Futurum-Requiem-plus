package ganymedes01.etfuturum.blocks;

import ganymedes01.etfuturum.EtFuturum;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.client.sound.ModSounds;
import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.Random;

/** Modern-style moss block with bone-meal spreading and lush vegetation. */
public class BlockMoss extends BaseBlock implements IGrowable {

    public BlockMoss() {
        super(Material.grass);
        setHardness(0.1F);
        setResistance(0.1F);
        setNames("moss_block");
        setHarvestLevel("hoe", 0);
        setBlockSound(ModSounds.soundMoss);
        setCreativeTab(EtFuturum.creativeTabBlocks);
    }

    /** MCP name: canFertilize. Moss requires an open block above the source. */
    @Override
    public boolean func_149851_a(World world, int x, int y, int z, boolean isClient) {
        return world.isAirBlock(x, y + 1, z);
    }

    /** MCP name: shouldFertilize. */
    @Override
    public boolean func_149852_a(World world, Random rand, int x, int y, int z) {
        return true;
    }

    /**
     * MCP name: fertilize.
     *
     * Modern moss can affect a 7x7 footprint (excluding the four far corners) and
     * follows nearby terrain vertically. This backport finds the exposed surface in
     * each column around the source, converts supported vanilla/EFR base stone or dirt
     * to moss, then uses the modern lush-vegetation weighting.
     */
    @Override
    public void func_149853_b(World world, Random rand, int x, int y, int z) {
        if (world.isRemote) {
            return;
        }

        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (Math.abs(dx) == 3 && Math.abs(dz) == 3) {
                    continue;
                }

                int sx = x + dx;
                int sz = z + dz;
                int sy = findSurface(world, sx, y, sz);
                if (sy == Integer.MIN_VALUE) {
                    continue;
                }

                Block surface = world.getBlock(sx, sy, sz);
                boolean source = sx == x && sy == y && sz == z;
                if (!source) {
                    if (!isMossReplaceable(surface) || rand.nextFloat() > 0.78F) {
                        continue;
                    }
                    world.setBlock(sx, sy, sz, ModBlocks.MOSS_BLOCK.get(), 0, 2);
                }

                if (rand.nextFloat() < 0.60F) {
                    growVegetation(world, rand, sx, sy + 1, sz);
                }
            }
        }
    }

    private static int findSurface(World world, int x, int originY, int z) {
        // Search the same vertical span modern moss uses, preferring the highest exposed
        // replaceable surface. This lets a patch climb cave ledges without crossing air gaps.
        for (int y = Math.min(world.getHeight() - 2, originY + 5); y >= Math.max(1, originY - 5); y--) {
            Block block = world.getBlock(x, y, z);
            Block above = world.getBlock(x, y + 1, z);
            if ((block == ModBlocks.MOSS_BLOCK.get() || isMossReplaceable(block))
                    && (above.isAir(world, x, y + 1, z) || above.isReplaceable(world, x, y + 1, z))) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    private static boolean isMossReplaceable(Block block) {
        if (block == Blocks.stone || block == Blocks.dirt || block == Blocks.grass) {
            return true;
        }
        return ModBlocks.DEEPSLATE.isEnabled() && block == ModBlocks.DEEPSLATE.get();
    }

    private static void growVegetation(World world, Random rand, int x, int y, int z) {
        if (!world.isAirBlock(x, y, z)) {
            return;
        }

        // Modern configured weights: short grass 50, tall grass 10, moss carpet 25,
        // azalea 7, flowering azalea 4.
        int roll = rand.nextInt(96);
        if (roll < 50) {
            if (Blocks.tallgrass.canBlockStay(world, x, y, z)) {
                world.setBlock(x, y, z, Blocks.tallgrass, 1, 2);
            }
        } else if (roll < 60) {
            if (y + 1 < world.getHeight() && world.isAirBlock(x, y + 1, z)
                    && Blocks.double_plant.canBlockStay(world, x, y, z)) {
                Blocks.double_plant.func_149889_c(world, x, y, z, 2, 2);
            }
        } else if (roll < 85) {
            if (ModBlocks.MOSS_CARPET.get().canBlockStay(world, x, y, z)) {
                world.setBlock(x, y, z, ModBlocks.MOSS_CARPET.get(), 0, 2);
            }
        } else if (roll < 92) {
            if (ModBlocks.AZALEA.get().canBlockStay(world, x, y, z)) {
                world.setBlock(x, y, z, ModBlocks.AZALEA.get(), 0, 2);
            }
        } else if (ModBlocks.AZALEA.get().canBlockStay(world, x, y, z)) {
            world.setBlock(x, y, z, ModBlocks.AZALEA.get(), 1, 2);
        }
    }

    @Override
    public boolean canSustainPlant(IBlockAccess world, int x, int y, int z, ForgeDirection direction, IPlantable plant) {
        return Blocks.dirt.canSustainPlant(world, x, y, z, direction, plant);
    }
}
