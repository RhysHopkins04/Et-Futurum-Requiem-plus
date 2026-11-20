package ganymedes01.etfuturum.compat;

import com.gtnewhorizons.liquidlogged.api.FluidHolderPredicate;
import com.gtnewhorizons.liquidlogged.api.LiquidLoggedAPI;
import ganymedes01.etfuturum.ModBlocks;
import net.minecraftforge.fluids.FluidRegistry;

/** Water-only registration using the approach from GTNH Et Futurum Requiem PR #147. */
final class CompatLiquidLogged {
	private static final FluidHolderPredicate WATER_ONLY = (world, x, y, z, block, meta, fluid) ->
			fluid == FluidRegistry.WATER;

	private CompatLiquidLogged() {}

	static void register() {
		register(ModBlocks.PRISMARINE_STAIRS);
		register(ModBlocks.PRISMARINE_STAIRS_BRICK);
		register(ModBlocks.PRISMARINE_STAIRS_DARK);
	}

	private static void register(ModBlocks entry) {
		if (entry.isEnabled()) {
			LiquidLoggedAPI.registerPredicate(entry.get(), WATER_ONLY);
		}
	}
}
