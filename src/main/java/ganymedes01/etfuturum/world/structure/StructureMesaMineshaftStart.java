package ganymedes01.etfuturum.world.structure;

import ganymedes01.etfuturum.configuration.configs.ConfigMapCompatibility;
import ganymedes01.etfuturum.configuration.configs.ConfigWorld;
import ganymedes01.etfuturum.core.utils.WorldHeightCompat;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureMineshaftStart;

import java.util.Random;

public class StructureMesaMineshaftStart extends StructureMineshaftStart {
	public StructureMesaMineshaftStart() {
	}

	public StructureMesaMineshaftStart(World p_i2039_1_, Random p_i2039_2_, int p_i2039_3_, int p_i2039_4_) {
		this.field_143024_c = p_i2039_3_; // chunkPosX
		this.field_143023_d = p_i2039_4_; // chunkPosZ
		StructureMesaMineshaftPieces.MesaRoom room = new StructureMesaMineshaftPieces.MesaRoom(0, p_i2039_2_, (p_i2039_3_ << 4) + 2, (p_i2039_4_ << 4) + 2);
		this.components.add(room);
		room.buildComponent(room, this.components, p_i2039_2_);

		this.updateBoundingBox();

		// Badlands mineshafts intentionally remain surface-biased. In the modern 384-high Overworld,
		// anchor that special path to the translated sea level rather than legacy physical Y63.
		// Legacy/non-modern/map-compat worlds keep the original 1.7 anchor exactly.
		int placementSeaLevel = getPlacementSeaLevel(p_i2039_1_);
		int j = placementSeaLevel - this.boundingBox.maxY + this.boundingBox.getYSize() / 2 + 5;
		this.boundingBox.offset(0, j, 0);

		for (StructureComponent structurecomponent : this.components) {
			if (structurecomponent instanceof StructureMesaMineshaftPieces.Piece) {
				((StructureMesaMineshaftPieces.Piece) structurecomponent).offset(0, j, 0);
			}
		}
	}

	private static int getPlacementSeaLevel(World world) {
		if (world != null && world.provider != null && world.provider.dimensionId == 0
				&& ConfigWorld.extendedWorldHeight && ConfigWorld.modernOverworldGeneration
				&& !ConfigMapCompatibility.isEnabled()) {
			return WorldHeightCompat.PHYSICAL_SEA_LEVEL;
		}
		return 63;
	}

}
