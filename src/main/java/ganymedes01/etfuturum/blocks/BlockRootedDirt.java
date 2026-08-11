package ganymedes01.etfuturum.blocks;

import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.client.sound.ModSounds;
import net.minecraft.block.IGrowable;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

import java.util.Random;

/**
 * Rooted dirt from the lush-caves block family.
 *
 * Natural lush-cave generation is intentionally out of scope for this fork profile; the block is
 * registered as content and supports its small vanilla-style bonemeal interaction.
 */
public class BlockRootedDirt extends BaseBlock implements IGrowable {

    public BlockRootedDirt() {
        super(Material.ground);
        setNames("rooted_dirt");
        setBlockSound(ModSounds.soundRootedDirt);
        setHardness(0.5F);
        setToolClass("shovel", 0);
        setMapColorBaseBlock(Blocks.dirt);
    }

    @Override
    public boolean func_149851_a(World world, int x, int y, int z, boolean isClient) {
        return ModBlocks.HANGING_ROOTS.isEnabled() && world.isAirBlock(x, y - 1, z);
    }

    @Override
    public boolean func_149852_a(World world, Random rand, int x, int y, int z) {
        return true;
    }

    @Override
    public void func_149853_b(World world, Random rand, int x, int y, int z) {
        if (ModBlocks.HANGING_ROOTS.isEnabled() && world.isAirBlock(x, y - 1, z)) {
            world.setBlock(x, y - 1, z, ModBlocks.HANGING_ROOTS.get(), 0, 3);
        }
    }
}
