package ganymedes01.etfuturum.mixins.early.modernoverworld;

import ganymedes01.etfuturum.configuration.configs.ConfigMapCompatibility;
import ganymedes01.etfuturum.configuration.configs.ConfigWorld;
import ganymedes01.etfuturum.world.generate.terrain.ModernOverworldTerrainGenerator;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.gen.structure.ComponentScatteredFeaturePieces;
import net.minecraft.world.gen.structure.MapGenScatteredFeature;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

/**
 * Grounds legacy desert pyramids against the translated modern terrain surface.
 *
 * <p>1.7.10 DesertPyramid is the odd scattered-feature component that never runs the shared
 * surface-height alignment used by jungle temples and swamp huts: its constructor simply anchors
 * the whole pyramid at physical Y64. With P006's +64 world mapping that leaves the pyramid roughly
 * sixty-four blocks underground. Move that single component to the deterministic P007 surface as
 * soon as its structure start is created.</p>
 */
@Mixin(MapGenScatteredFeature.Start.class)
public abstract class MixinMapGenScatteredFeatureStart {

    @Inject(method = "<init>(Lnet/minecraft/world/World;Ljava/util/Random;II)V", at = @At("RETURN"))
    private void etfu$groundModernDesertPyramid(World world, Random random, int chunkX, int chunkZ, CallbackInfo ci) {
        if (world.provider == null
                || world.provider.dimensionId != 0
                || !ConfigWorld.modernOverworldGeneration
                || !ConfigWorld.extendedWorldHeight
                || ConfigMapCompatibility.isEnabled()) {
            return;
        }

        final StructureStart start = (StructureStart) (Object) this;
        if (start.getComponents().isEmpty()) {
            return;
        }

        final StructureComponent component = start.getComponents().getFirst();
        if (!(component instanceof ComponentScatteredFeaturePieces.DesertPyramid)) {
            return;
        }

        final StructureBoundingBox box = component.getBoundingBox();
        final ModernOverworldTerrainGenerator terrain = new ModernOverworldTerrainGenerator(world.getSeed());
        final boolean amplified = world.getWorldInfo().getTerrainType() == WorldType.AMPLIFIED;

        // Sample the footprint rather than a single centre point so the legacy pyramid remains
        // sensible on the broad slopes that modern terrain permits.
        final int minX = box.minX;
        final int maxX = box.maxX;
        final int minZ = box.minZ;
        final int maxZ = box.maxZ;
        final int midX = (minX + maxX) >> 1;
        final int midZ = (minZ + maxZ) >> 1;
        final int[] xs = new int[] {minX, midX, maxX};
        final int[] zs = new int[] {minZ, midZ, maxZ};

        int total = 0;
        int count = 0;
        for (int x : xs) {
            for (int z : zs) {
                total += terrain.sampleSurfacePhysicalY(x, z, amplified);
                ++count;
            }
        }

        final int targetBaseY = total / count;
        final int deltaY = targetBaseY - box.minY;
        if (deltaY != 0) {
            box.offset(0, deltaY, 0);
            if (start.getBoundingBox() != null) {
                start.getBoundingBox().offset(0, deltaY, 0);
            }
        }
    }
}
