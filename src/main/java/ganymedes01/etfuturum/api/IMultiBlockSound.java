package ganymedes01.etfuturum.api;

import net.minecraft.block.Block;
import net.minecraft.world.World;

/**
 * Allows a block to choose its sound type from world position / metadata.
 * This is Et Futurum Requiem's self-contained replacement for the small
 * interface previously supplied by HogUtils.
 */
public interface IMultiBlockSound {
    Block.SoundType getSoundType(World world, int x, int y, int z, SoundMode mode);

    enum SoundMode {
        BREAK,
        PLACE,
        WALK,
        HIT
    }
}
