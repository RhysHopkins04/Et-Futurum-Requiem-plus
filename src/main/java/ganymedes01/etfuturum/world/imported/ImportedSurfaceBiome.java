package ganymedes01.etfuturum.world.imported;

import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import java.util.Random;

/** An import identity, not a promise to reproduce modern terrain generation or spawning. */
public final class ImportedSurfaceBiome extends BiomeGenBase {
    public final ImportedBiome definition;
    public final boolean importedAltitude;
    ImportedSurfaceBiome(int id, ImportedBiome definition) {
        this(id, definition, true, ganymedes01.etfuturum.configuration.configs.ConfigMapCompatibility.importedEarthMode);
    }
    public static ImportedSurfaceBiome forConnection(int id, ImportedBiome definition, boolean importedAltitude) {
        return new ImportedSurfaceBiome(id, definition, false, importedAltitude);
    }
    private ImportedSurfaceBiome(int id, ImportedBiome definition, boolean register, boolean importedAltitude) {
        super(id, register);
        this.definition = definition;
        this.importedAltitude = importedAltitude;
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
        if(definition==ImportedBiome.THE_VOID) {
            setDisableRain();
            spawnableMonsterList.clear();spawnableCreatureList.clear();
            spawnableWaterCreatureList.clear();spawnableCaveCreatureList.clear();
        }
    }
    @Override public void decorate(World world, Random random, int x, int z) {
        // Deliberately empty: importing a biome never grants ownership of the Earth's surface.
    }
}
