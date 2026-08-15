package ganymedes01.etfuturum.world.structure;

import ganymedes01.etfuturum.core.utils.WorldHeightCompat;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureMineshaftStart;

import java.util.Random;

/**
 * Repositions a normal vanilla 1.7 mineshaft piece graph into the expanded modern underground.
 *
 * <p>The legacy StructureMineshaftStart first applies its old sea-level-63 height adjustment.
 * Plus then replaces only that vertical placement result. Normal modern mineshafts are kept below
 * the translated sea level with the same ten-block headroom used by the modern structure rule,
 * while the expanded physical minimum Y=0 lets the complete piece graph reach the deeper world.
 * This intentionally does not sample {@code World#getHeightValue}: modern regular placement is a
 * below-sea-level rule, and forcing a 1.7 structure start to provide terrain neighbours here can
 * re-enter chunk generation.</p>
 *
 * <p>Badlands/mesa mineshafts remain on their separate surface-biased path and do not use this
 * normal-mineshaft correction.</p>
 */
public final class StructureModernMineshaftStart extends StructureMineshaftStart {

    private static final int SEA_LEVEL_HEADROOM = 10;

    public StructureModernMineshaftStart() {
        super();
    }

    public StructureModernMineshaftStart(World world, Random rand, int chunkX, int chunkZ) {
        super(world, rand, chunkX, chunkZ);

        final int maxTopY = WorldHeightCompat.PHYSICAL_SEA_LEVEL - SEA_LEVEL_HEADROOM;
        final int minimumTopY = WorldHeightCompat.PHYSICAL_MIN_Y + this.boundingBox.getYSize() + 1;

        int targetTopY = minimumTopY;
        if (targetTopY < maxTopY) {
            targetTopY += rand.nextInt(maxTopY - targetTopY);
        }

        final int offsetY = targetTopY - this.boundingBox.maxY;
        this.boundingBox.offset(0, offsetY, 0);
        for (Object object : this.components) {
            ((StructureComponent) object).getBoundingBox().offset(0, offsetY, 0);
        }
    }
}
