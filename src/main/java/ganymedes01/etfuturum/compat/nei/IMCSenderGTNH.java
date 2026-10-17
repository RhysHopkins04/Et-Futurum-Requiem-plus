package ganymedes01.etfuturum.compat.nei;

import cpw.mods.fml.common.event.FMLInterModComms;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.Tags;
import net.minecraft.nbt.NBTTagCompound;

public class IMCSenderGTNH {

	public static void IMCSender() {

		if (ModBlocks.SMOKER.isEnabled()) {
			sendHandler("ganymedes01.etfuturum.compat.nei.SmokerRecipeHandler", "etfuturum:smoker");
			sendCatalyst("etfuturum.smoker", "etfuturum:smoker");

			sendCatalyst("fuel", "etfuturum:smoker");
		}

		if (ModBlocks.BLAST_FURNACE.isEnabled()) {
			sendHandler("ganymedes01.etfuturum.compat.nei.BlastFurnaceRecipeHandler", "etfuturum:blast_furnace");
			sendCatalyst("etfuturum.blastfurnace", "etfuturum:blast_furnace");

			sendCatalyst("fuel", "etfuturum:blast_furnace");
		}

        if (ganymedes01.etfuturum.ModernMapParityBlocks.CAMPFIRE.get() != null) {
            sendHandler("ganymedes01.etfuturum.compat.nei.CampfireRecipeHandler", "etfuturum:campfire", 2, false);
            sendCatalyst("etfuturum.campfire", "etfuturum:campfire");
            sendCatalyst("etfuturum.campfire", "etfuturum:soul_campfire");
            sendHandler("ganymedes01.etfuturum.compat.nei.CandleCakeRecipeHandler", "etfuturum:candle", 2, false);
        }
        if (ModBlocks.SMITHING_TABLE.isEnabled()) {
            sendHandler("ganymedes01.etfuturum.compat.nei.SmithingRecipeHandler", "etfuturum:smithing_table", 2, false);
            sendCatalyst("etfuturum.smithing", "etfuturum:smithing_table");
        }
        if (ganymedes01.etfuturum.recipes.StonecutterRecipes.enabled()) {
            sendHandler("ganymedes01.etfuturum.compat.nei.StonecutterRecipeHandler","etfuturum:stonecutter",2,false);
            sendCatalyst("etfuturum.stonecutting","etfuturum:stonecutter");
        }
        if (ganymedes01.etfuturum.recipes.LoomRecipes.enabled()) {
            sendHandler("ganymedes01.etfuturum.compat.nei.LoomRecipeHandler","etfuturum:loom",2,false);
            sendCatalyst("etfuturum.loom","etfuturum:loom");
        }
        if (ganymedes01.etfuturum.recipes.GrindstoneRecipes.enabled()) {
            sendHandler("ganymedes01.etfuturum.compat.nei.GrindstoneRecipeHandler","etfuturum:grindstone",2,false);
            sendCatalyst("etfuturum.grinding","etfuturum:grindstone");
        }
        if (ganymedes01.etfuturum.recipes.CartographyRecipes.enabled()) {
            sendHandler("ganymedes01.etfuturum.compat.nei.CartographyRecipeHandler","etfuturum:cartography_table",2,false);
            sendCatalyst("etfuturum.cartography","etfuturum:cartography_table");
        }
		if (ModBlocks.BANNER.isEnabled()) {
			sendHandler("ganymedes01.etfuturum.compat.nei.BannerPatternHandler", "etfuturum:banner");
		}

		if (ModBlocks.COMPOSTER.isEnabled()) {
			sendHandler("ganymedes01.etfuturum.compat.nei.ComposterHandler", "etfuturum:composter", 1, false);
			sendCatalyst("etfuturum.composter", "etfuturum:composter");
		}
	}

	/*
	 * These are based on methods from GTNewHorizons/GoodGenerator (Fork of GlodBlock/GoodGenerator)
	 * Author: GlodBlock
	 */

	private static void sendHandler(String name, String block) {
		sendHandler(name, block, 5, true);
	}

	private static void sendHandler(String name, String block, int maxRecipesPerPage, boolean showButtons) {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setString("handler", name);
		nbt.setString("modName", Tags.MOD_NAME);
		nbt.setString("modId", Tags.MOD_ID);
		nbt.setBoolean("modRequired", true);
		nbt.setString("itemName", block);
		nbt.setInteger("handlerHeight", maxRecipesPerPage == 2 ? 82 : 65);
		nbt.setInteger("handlerWidth", 166);
		nbt.setInteger("maxRecipesPerPage", maxRecipesPerPage);
		nbt.setInteger("yShift", 6);
		nbt.setBoolean("showFavoritesButton", showButtons);
		nbt.setBoolean("showOverlayButton", showButtons);
		FMLInterModComms.sendMessage("NotEnoughItems", "registerHandlerInfo", nbt);
	}

	private static void sendCatalyst(String name, String stack) {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setString("handlerID", name);
		nbt.setString("itemName", stack);
		nbt.setInteger("priority", 0);
		FMLInterModComms.sendMessage("NotEnoughItems", "registerCatalystInfo", nbt);
	}
}
