package ganymedes01.etfuturum.world.imported;

import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import java.util.Random;

/** An import identity, not a promise to reproduce modern terrain generation or spawning. */
public final class ImportedSurfaceBiome extends BiomeGenBase {
    public final ImportedBiome definition;
    ImportedSurfaceBiome(int id, ImportedBiome definition) {
        super(id);
        this.definition = definition;
        setBiomeName(definition.sourceName());
        setTemperatureRainfall(definition.temperature, definition.rainfall);
        waterColorMultiplier = definition.waterColor;
        if (definition.temperature <= 0F) setEnableSnow();
        // Preserve usable legacy family spawning; saved-entity migration is unrelated to natural spawning.
        BiomeGenBase fallback = definition.legacyFamily();
        spawnableMonsterList.clear(); spawnableMonsterList.addAll(fallback.getSpawnableList(net.minecraft.entity.EnumCreatureType.monster));
        spawnableCreatureList.clear(); spawnableCreatureList.addAll(fallback.getSpawnableList(net.minecraft.entity.EnumCreatureType.creature));
        spawnableWaterCreatureList.clear(); spawnableWaterCreatureList.addAll(fallback.getSpawnableList(net.minecraft.entity.EnumCreatureType.waterCreature));
        spawnableCaveCreatureList.clear(); spawnableCaveCreatureList.addAll(fallback.getSpawnableList(net.minecraft.entity.EnumCreatureType.ambient));
    }
    @Override public void decorate(World world, Random random, int x, int z) {
        // Deliberately empty: importing a biome never grants ownership of the Earth's surface.
    }
}
