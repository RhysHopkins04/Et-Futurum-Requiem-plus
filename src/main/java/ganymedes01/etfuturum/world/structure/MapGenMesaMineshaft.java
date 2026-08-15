package ganymedes01.etfuturum.world.structure;

import ganymedes01.etfuturum.configuration.configs.ConfigWorld;
import net.minecraft.world.gen.structure.MapGenMineshaft;
import net.minecraft.world.gen.structure.StructureMineshaftStart;
import net.minecraft.world.gen.structure.StructureStart;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;
import org.apache.commons.lang3.ArrayUtils;

/**
 * This still allows regular mineshafts to generate as well as Mesa ones.
 * We do this so mixins are not required for overriding normal Mineshafts
 *
 * @author roadhog360
 */
public class MapGenMesaMineshaft extends MapGenMineshaft {

	@Override
	protected boolean canSpawnStructureAtCoords(int chunkX, int chunkZ) {
		if (ConfigWorld.modernOverworldGeneration && worldObj != null && worldObj.provider != null
				&& worldObj.provider.dimensionId == 0) {
			// Remove 1.7's distance-from-origin suppression while keeping approximately the same
			// global rarity. This avoids both spawn-area deserts and the far-world clustering bias.
			return this.rand.nextDouble() < 0.004D;
		}
		return super.canSpawnStructureAtCoords(chunkX, chunkZ);
	}

	@Override
	protected StructureStart getStructureStart(int p_75049_1_, int p_75049_2_) {
		if (ArrayUtils.contains(BiomeDictionary.getTypesForBiome(this.worldObj.getBiomeGenForCoords((p_75049_1_ << 4) + 8, (p_75049_2_ << 4) + 8)), Type.MESA)) {
			return new StructureMesaMineshaftStart(this.worldObj, this.rand, p_75049_1_, p_75049_2_);
		}
		if (ConfigWorld.modernOverworldGeneration && this.worldObj != null && this.worldObj.provider != null
				&& this.worldObj.provider.dimensionId == 0) {
			return new StructureModernMineshaftStart(this.worldObj, this.rand, p_75049_1_, p_75049_2_);
		}
		return new StructureMineshaftStart(this.worldObj, this.rand, p_75049_1_, p_75049_2_);
	}
}
