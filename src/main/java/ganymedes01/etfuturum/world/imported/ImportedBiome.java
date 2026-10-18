package ganymedes01.etfuturum.world.imported;

import net.minecraftforge.common.BiomeDictionary.Type;

/** Stable source identities; ordinal is used only for initial configuration defaults, never persistence. */
public enum ImportedBiome {
    CHERRY_GROVE("cherry_grove", .5F, .8F, 0x3F76E4, Type.FOREST, Type.MOUNTAIN),
    GROVE("grove", -.2F, .8F, 0x3F76E4, Type.CONIFEROUS, Type.FOREST, Type.SNOWY, Type.COLD, Type.MOUNTAIN),
    SNOWY_SLOPES("snowy_slopes", -.3F, .9F, 0x3F76E4, Type.SNOWY, Type.COLD, Type.MOUNTAIN),
    JAGGED_PEAKS("jagged_peaks", -.7F, .9F, 0x3F76E4, Type.SNOWY, Type.COLD, Type.MOUNTAIN),
    FROZEN_PEAKS("frozen_peaks", -.7F, .9F, 0x3F76E4, Type.SNOWY, Type.COLD, Type.MOUNTAIN),
    STONY_PEAKS("stony_peaks", 1F, .3F, 0x3F76E4, Type.MOUNTAIN),
    MEADOW("meadow", .5F, .8F, 0x0E4ECF, Type.PLAINS, Type.MOUNTAIN),
    MANGROVE_SWAMP("mangrove_swamp", .8F, .9F, 0x3A7A6A, Type.SWAMP, Type.WET),
    BAMBOO_JUNGLE("bamboo_jungle", .95F, .9F, 0x3F76E4, Type.JUNGLE, Type.HOT, Type.WET),
    WARM_OCEAN("warm_ocean", .5F, .5F, 0x43D5EE, Type.OCEAN),
    LUKEWARM_OCEAN("lukewarm_ocean", .5F, .5F, 0x45ADF2, Type.OCEAN),
    COLD_OCEAN("cold_ocean", .5F, .5F, 0x3D57D6, Type.OCEAN, Type.COLD),
    DEEP_LUKEWARM_OCEAN("deep_lukewarm_ocean", .5F, .5F, 0x45ADF2, Type.OCEAN),
    DEEP_COLD_OCEAN("deep_cold_ocean", .5F, .5F, 0x3D57D6, Type.OCEAN, Type.COLD),
    DEEP_FROZEN_OCEAN("deep_frozen_ocean", 0F, .5F, 0x3938C9, Type.OCEAN, Type.COLD, Type.SNOWY),
    PALE_GARDEN("pale_garden", .7F, .8F, 0x76889D, Type.FOREST),
    THE_VOID("the_void", .5F, .5F, 0x3F76E4, Type.DEAD);

    public final String path;
    public final float temperature, rainfall;
    public final int waterColor;
    private final Type[] types;
    ImportedBiome(String path, float temperature, float rainfall, int waterColor, Type... types) {
        this.path = path; this.temperature = temperature; this.rainfall = rainfall;
        this.waterColor = waterColor; this.types = types;
    }
    public net.minecraft.world.biome.BiomeGenBase legacyFamily() {
        switch (this) {
            case CHERRY_GROVE: return net.minecraft.world.biome.BiomeGenBase.forest;
            case PALE_GARDEN: return net.minecraft.world.biome.BiomeGenBase.roofedForest;
            case GROVE: return net.minecraft.world.biome.BiomeGenBase.coldTaiga;
            case SNOWY_SLOPES: case JAGGED_PEAKS: case FROZEN_PEAKS: return net.minecraft.world.biome.BiomeGenBase.iceMountains;
            case STONY_PEAKS: return net.minecraft.world.biome.BiomeGenBase.extremeHills;
            case MEADOW: return net.minecraft.world.biome.BiomeGenBase.plains;
            case MANGROVE_SWAMP: return net.minecraft.world.biome.BiomeGenBase.swampland;
            case BAMBOO_JUNGLE: return net.minecraft.world.biome.BiomeGenBase.jungle;
            case DEEP_FROZEN_OCEAN: return net.minecraft.world.biome.BiomeGenBase.frozenOcean;
            default: return net.minecraft.world.biome.BiomeGenBase.ocean;
        }
    }
    public String sourceName() { return "minecraft:" + path; }
    public Type[] types() { return types.clone(); }
}
