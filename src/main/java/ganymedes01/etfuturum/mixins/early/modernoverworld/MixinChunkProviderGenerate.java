package ganymedes01.etfuturum.mixins.early.modernoverworld;

import cpw.mods.fml.common.eventhandler.Event;
import ganymedes01.etfuturum.configuration.configs.ConfigMapCompatibility;
import ganymedes01.etfuturum.configuration.configs.ConfigWorld;
import ganymedes01.etfuturum.core.utils.WorldHeightCompat;
import ganymedes01.etfuturum.world.generate.terrain.ModernOverworldCaveGenerator;
import ganymedes01.etfuturum.world.generate.terrain.ModernOverworldRavineGenerator;
import ganymedes01.etfuturum.world.generate.terrain.ModernOverworldTerrainGenerator;
import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.gen.ChunkProviderGenerate;
import net.minecraft.world.gen.MapGenBase;
import net.minecraft.world.gen.NoiseGeneratorPerlin;
import net.minecraft.init.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.ChunkProviderEvent;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.event.terraingen.TerrainGen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

/**
 * P007: feeds the vanilla ChunkProviderGenerate pipeline a 384-high modern base terrain while
 * preserving its biome array, structure-start, lighting and population flow. P007a substitutes
 * only the final Block[] -> Chunk materialisation because vanilla's bitwise index assumes height 256.
 */
@Mixin(ChunkProviderGenerate.class)
public abstract class MixinChunkProviderGenerate {

    @Shadow private World worldObj;
    @Shadow private Random rand;
    @Shadow private NoiseGeneratorPerlin field_147430_m;
    @Shadow private double[] stoneNoise;
    @Shadow private MapGenBase caveGenerator;
    @Shadow private MapGenBase ravineGenerator;

    @Unique private ModernOverworldTerrainGenerator etfu$modernTerrain;

    @Unique
    private boolean etfu$isModernOverworld() {
        return this.worldObj != null
                && this.worldObj.provider != null
                && this.worldObj.provider.dimensionId == 0
                && ConfigWorld.extendedWorldHeight
                && ConfigWorld.modernOverworldGeneration
                && !ConfigMapCompatibility.isEnabled();
    }

    @Unique
    private ModernOverworldTerrainGenerator etfu$terrain() {
        if (this.etfu$modernTerrain == null) {
            this.etfu$modernTerrain = new ModernOverworldTerrainGenerator(this.worldObj.getSeed());
        }
        return this.etfu$modernTerrain;
    }

    /** Allocate 16x16x384 Block/metadata buffers only for the modern Overworld path. */
    @ModifyConstant(method = "provideChunk", constant = @Constant(intValue = 65536))
    private int etfu$extendModernChunkBuffers(int original) {
        return etfu$isModernOverworld() ? 256 * WorldHeightCompat.EXTENDED_HEIGHT : original;
    }

    /**
     * Vanilla Chunk's Block[]+metadata constructor computes its source index with bitwise OR:
     * {@code x * height * 16 | z * height | y}. That is only equivalent to addition while the
     * vertical stride is a power of two (256 in vanilla). At P007's 384-block stride those bit
     * ranges overlap, aliasing many X/Z columns onto the same source entries and producing the
     * characteristic comb/rib world corruption seen in the first runtime test.
     *
     * <p>Keep the vanilla provider pipeline, but replace only its final Chunk construction in the
     * modern Overworld path with an arithmetic-stride copy into the already-extended 24 section
     * storage array. Non-modern paths retain the original constructor unchanged.</p>
     */
    @Redirect(method = "provideChunk", at = @At(value = "NEW", target = "net/minecraft/world/chunk/Chunk"))
    private Chunk etfu$constructModernChunk(World world, Block[] blocks, byte[] metadata, int chunkX, int chunkZ) {
        if (!etfu$isModernOverworld()) {
            return new Chunk(world, blocks, metadata, chunkX, chunkZ);
        }

        final int height = blocks.length / 256;
        if (height != WorldHeightCompat.EXTENDED_HEIGHT || metadata.length != blocks.length) {
            throw new IllegalArgumentException("Modern Overworld chunk construction requires matching 16x16x384 buffers");
        }

        final Chunk chunk = new Chunk(world, chunkX, chunkZ);
        final ExtendedBlockStorage[] sections = chunk.getBlockStorageArray();
        if (sections.length < WorldHeightCompat.EXTENDED_SECTION_COUNT) {
            throw new IllegalStateException("Extended-height Chunk storage was not active during modern terrain construction");
        }

        final boolean hasSky = !world.provider.hasNoSky;
        for (int localX = 0; localX < 16; ++localX) {
            for (int localZ = 0; localZ < 16; ++localZ) {
                final int columnBase = (localX * 16 + localZ) * height;
                for (int y = 0; y < height; ++y) {
                    final int sourceIndex = columnBase + y;
                    final Block block = blocks[sourceIndex];
                    if (block == null || block == Blocks.air) {
                        continue;
                    }

                    final int sectionIndex = y >> 4;
                    ExtendedBlockStorage section = sections[sectionIndex];
                    if (section == null) {
                        section = new ExtendedBlockStorage(sectionIndex << 4, hasSky);
                        sections[sectionIndex] = section;
                    }

                    section.func_150818_a(localX, y & 15, localZ, block);
                    section.setExtBlockMetadata(localX, y & 15, localZ, metadata[sourceIndex]);
                }
            }
        }

        return chunk;
    }

    /** Replace legacy biome-height terrain density with the P007 modern terrain-shaping fields. */
    @Inject(method = "func_147424_a", at = @At("HEAD"), cancellable = true)
    private void etfu$generateModernBaseTerrain(int chunkX, int chunkZ, Block[] blocks, CallbackInfo ci) {
        if (!etfu$isModernOverworld()) {
            return;
        }

        etfu$terrain().generateBaseTerrain(this.worldObj, chunkX, chunkZ, blocks);
        ci.cancel();
    }

    /**
     * Re-run the existing biome-specific surface rules through a translated logical Y0..255
     * window. Forge's ReplaceBiomeBlocks event is retained for compatibility.
     */
    @Inject(method = "replaceBlocksForBiome", at = @At("HEAD"), cancellable = true)
    private void etfu$replaceModernSurface(int chunkX, int chunkZ, Block[] blocks, byte[] metadata,
            BiomeGenBase[] biomes, CallbackInfo ci) {
        if (!etfu$isModernOverworld()) {
            return;
        }

        ChunkProviderEvent.ReplaceBiomeBlocks event = new ChunkProviderEvent.ReplaceBiomeBlocks(
                (ChunkProviderGenerate) (Object) this, chunkX, chunkZ, blocks, metadata, biomes, this.worldObj);
        MinecraftForge.EVENT_BUS.post(event);

        if (event.getResult() != Event.Result.DENY) {
            final double scale = 0.03125D;
            this.stoneNoise = this.field_147430_m.func_151599_a(this.stoneNoise,
                    (double) (chunkX * 16), (double) (chunkZ * 16), 16, 16,
                    scale * 2.0D, scale * 2.0D, 1.0D);
            etfu$terrain().applyTranslatedBiomeSurface(this.worldObj, this.rand, chunkX, chunkZ,
                    blocks, metadata, biomes, this.stoneNoise);
        }

        ci.cancel();
    }

    /**
     * Modern Java removed the legacy water-lake population feature in favour of aquifers, and the
     * old 1.7 lava-lake call also samples the legacy physical-Y range. Keep firing Forge's populate
     * event for compatibility, but suppress both old lake generators because P008b local aquifers now
     * own modern underground water/lava bodies. This also prevents duplicate elevated legacy ponds.
     */
    @Redirect(method = "populate", at = @At(value = "INVOKE",
            target = "Lnet/minecraftforge/event/terraingen/TerrainGen;populate(Lnet/minecraft/world/chunk/IChunkProvider;Lnet/minecraft/world/World;Ljava/util/Random;IIZLnet/minecraftforge/event/terraingen/PopulateChunkEvent$Populate$EventType;)Z",
            remap = false))
    private boolean etfu$gateLegacyLakePopulation(IChunkProvider provider, World world, Random rand,
            int chunkX, int chunkZ, boolean hasVillageGenerated, PopulateChunkEvent.Populate.EventType type) {
        final boolean allowed = TerrainGen.populate(provider, world, rand, chunkX, chunkZ, hasVillageGenerated, type);
        if (etfu$isModernOverworld()
                && (type == PopulateChunkEvent.Populate.EventType.LAKE
                        || type == PopulateChunkEvent.Populate.EventType.LAVA)) {
            return false;
        }
        return allowed;
    }

    /**
     * P008b-c installs both the 384-height noise cave field and its translated canyon/ravine
     * companion. Legacy and Map Compatibility worlds retain the original 1.7 carvers unchanged.
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void etfu$installModernCarvers(World world, long seed, boolean mapFeatures, CallbackInfo ci) {
        if (world.provider != null
                && world.provider.dimensionId == 0
                && ConfigWorld.modernOverworldGeneration
                && !ConfigMapCompatibility.isEnabled()) {
            this.caveGenerator = new ModernOverworldCaveGenerator(seed);
            this.ravineGenerator = new ModernOverworldRavineGenerator(seed);
        }
    }
}
