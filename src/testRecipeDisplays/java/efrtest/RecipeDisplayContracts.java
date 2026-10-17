package efrtest;

import codechicken.nei.recipe.TemplateRecipeHandler;
import ganymedes01.etfuturum.compat.nei.FilteredFurnaceRecipeHandler;
import ganymedes01.etfuturum.configuration.configs.ConfigFunctions;
import ganymedes01.etfuturum.core.utils.ItemStackMap;
import ganymedes01.etfuturum.recipes.SmokerRecipes;
import ganymedes01.etfuturum.recipes.BlastFurnaceRecipes;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;
import java.util.Collections;
import java.util.List;

/** Executes actual NEI queries without creating a Minecraft window or rendering. */
public final class RecipeDisplayContracts {
    private static void check(boolean ok,String why) { if(!ok)throw new AssertionError(why); }
    public static class Smoker extends FilteredFurnaceRecipeHandler {
        @Override protected ItemStack lookup(ItemStack input) { return SmokerRecipes.smelting().getSmeltingResult(input); }
        @Override protected ItemStack machine() { return new ItemStack(Items.stick); }
        @Override protected ItemStackMap<ItemStack> explicitRecipes() { return SmokerRecipes.smelting().smeltingList; }
        @Override public String getOverlayIdentifier() { return "probe.smoker"; }
        @Override public List<Class<? extends GuiContainer>> getRecipeTransferRectGuis() { return Collections.emptyList(); }
    }
    public static class Blast extends FilteredFurnaceRecipeHandler {
        @Override protected ItemStack lookup(ItemStack input) { return BlastFurnaceRecipes.smelting().getSmeltingResult(input); }
        @Override protected ItemStack machine() { return new ItemStack(Items.blaze_rod); }
        @Override protected ItemStackMap<ItemStack> explicitRecipes() { return BlastFurnaceRecipes.smelting().smeltingList; }
        @Override public String getOverlayIdentifier() { return "probe.blast"; }
        @Override public List<Class<? extends GuiContainer>> getRecipeTransferRectGuis() { return Collections.emptyList(); }
    }
    public static class Interaction extends ganymedes01.etfuturum.compat.nei.SimpleProcessRecipeHandler {
        @Override public String getOverlayIdentifier() { return "probe.interaction"; }
        @Override public String getRecipeName() { return "Test interaction"; }
        @Override protected String instruction() { return "No fuel"; }
        @Override public List<Class<? extends GuiContainer>> getRecipeTransferRectGuis() { return Collections.emptyList(); }
        @Override protected List<ProcessRecipe> recipes() {
            List<ProcessRecipe> result=new java.util.ArrayList<>();
            result.add(new ProcessRecipe(new ItemStack(Items.beef),null,new ItemStack(Items.cooked_beef),null));
            return result;
        }
    }
    private static void result(TemplateRecipeHandler h,ItemStack output) {
        check(h.numRecipes()>0,"missing result "+output);
        for(int i=0;i<h.numRecipes();i++)check(h.getResultStack(i).item.getItem()==output.getItem(),"foreign cached recipe for "+output);
    }
    public static void main(String[] args) throws Exception {
        net.minecraft.launchwrapper.Launch.blackboard.put("fml.deobfuscatedEnvironment",true);
        net.minecraft.launchwrapper.Launch.minecraftHome=new java.io.File("build/recipe-display-tests");
        net.minecraft.launchwrapper.Launch.minecraftHome.mkdirs();
        java.lang.reflect.Method inject=cpw.mods.fml.relauncher.FMLInjectionData.class.getDeclaredMethod("build",java.io.File.class,net.minecraft.launchwrapper.LaunchClassLoader.class);
        inject.setAccessible(true);inject.invoke(null,net.minecraft.launchwrapper.Launch.minecraftHome,net.minecraft.launchwrapper.Launch.classLoader);
        cpw.mods.fml.common.Loader.injectData(cpw.mods.fml.relauncher.FMLInjectionData.data());
        for(Class<?> owner:new Class<?>[]{cpw.mods.fml.relauncher.FMLRelaunchLog.class,cpw.mods.fml.relauncher.FMLLaunchHandler.class}) {
            java.lang.reflect.Field side=owner.getDeclaredField("side");side.setAccessible(true);side.set(null,cpw.mods.fml.relauncher.Side.CLIENT);
        }
        Bootstrap.func_151354_b();
        ConfigFunctions.enableAutoAddSmoker=true;ConfigFunctions.enableAutoAddBlastFurnace=true;
        OreDictionary.registerOre("oreIron",new ItemStack(Blocks.iron_ore));
        Smoker smoker=new Smoker();
        smoker.loadCraftingRecipes("item",new ItemStack(Items.cooked_beef));result(smoker,new ItemStack(Items.cooked_beef));
        smoker.arecipes.clear();smoker.loadCraftingRecipes("item",new ItemStack(Items.baked_potato));result(smoker,new ItemStack(Items.baked_potato));
        smoker.arecipes.clear();smoker.loadCraftingRecipes(new ItemStack(Blocks.glass));check(smoker.numRecipes()==0,"smoker displays glass");
        smoker.loadUsageRecipes(new ItemStack(Items.beef));result(smoker,new ItemStack(Items.cooked_beef));
        smoker.arecipes.clear();smoker.loadUsageRecipes(new ItemStack(Items.potato));result(smoker,new ItemStack(Items.baked_potato));
        smoker.arecipes.clear();smoker.loadUsageRecipes(new ItemStack(Blocks.sand));check(smoker.numRecipes()==0,"smoker displays sand usage");
        smoker.loadCraftingRecipes("smelting");check(smoker.numRecipes()==0,"generic furnace tab leaks into smoker");
        Blast blast=new Blast();blast.loadCraftingRecipes("item",new ItemStack(Items.iron_ingot));result(blast,new ItemStack(Items.iron_ingot));
        blast.arecipes.clear();blast.loadUsageRecipes(new ItemStack(Items.beef));check(blast.numRecipes()==0,"blast furnace displays beef");
        blast.loadCraftingRecipes("smelting");check(blast.numRecipes()==0,"generic furnace tab leaks into blast furnace");
        BlastFurnaceRecipes.smelting().addRecipe(new ItemStack(Blocks.sand),new ItemStack(Items.iron_ingot),0);
        blast.loadUsageRecipes(new ItemStack(Blocks.sand));result(blast,new ItemStack(Items.iron_ingot));
        blast.arecipes.clear();BlastFurnaceRecipes.smelting().removeRecipe(new ItemStack(Blocks.sand));
        blast.loadUsageRecipes(new ItemStack(Blocks.sand));check(blast.numRecipes()==0,"blacklisted recipe remained visible");
        java.util.Map<String,Integer> order=new java.util.HashMap<>();
        ganymedes01.etfuturum.compat.nei.NEIProcessOrdering.defaults(order);
        check(order.get("etfuturum.smithing")<0,"smithing not preferred to default fuel page");
        order.put("etfuturum.smithing",25);ganymedes01.etfuturum.compat.nei.NEIProcessOrdering.defaults(order);
        check(order.get("etfuturum.smithing")==25,"user handler ordering overwritten");
        order.clear();order.put("ganymedes01.etfuturum.compat.nei.SmithingRecipeHandler",30);
        ganymedes01.etfuturum.compat.nei.NEIProcessOrdering.defaults(order);
        check(!order.containsKey("etfuturum.smithing"),"class-based user ordering overwritten");
        Interaction interaction=new Interaction();
        interaction.loadCraftingRecipes("item",new ItemStack(Items.cooked_beef));result(interaction,new ItemStack(Items.cooked_beef));
        check(interaction.getIngredientStacks(0).size()==1 && interaction.getOtherStacks(0).isEmpty(),"fuel slot leaked into interaction display");
        interaction.arecipes.clear();interaction.loadUsageRecipes(new ItemStack(Items.beef));result(interaction,new ItemStack(Items.cooked_beef));
        interaction.arecipes.clear();interaction.loadCraftingRecipes("smelting");check(interaction.numRecipes()==0,"generic furnace tab leaks into interaction category");
        java.lang.reflect.Field namedMods=cpw.mods.fml.common.Loader.class.getDeclaredField("namedMods");namedMods.setAccessible(true);namedMods.set(cpw.mods.fml.common.Loader.instance(),new java.util.HashMap<>());
        ganymedes01.etfuturum.configuration.configs.ConfigBlocksItems.enableCartographyTable=true;
        ganymedes01.etfuturum.configuration.configs.ConfigBlocksItems.enableSmithingTable=true;
        ganymedes01.etfuturum.configuration.configs.ConfigBlocksItems.enableNetherite=true;
        net.minecraft.block.Block.blockRegistry.addObject(4090,"minecraft:probe_cartography",ganymedes01.etfuturum.ModBlocks.CARTOGRAPHY_TABLE.get());
        net.minecraft.item.Item.itemRegistry.addObject(4090,"minecraft:probe_cartography",new net.minecraft.item.ItemBlock(ganymedes01.etfuturum.ModBlocks.CARTOGRAPHY_TABLE.get()));
        ganymedes01.etfuturum.compat.nei.CartographyRecipeHandler maps=new ganymedes01.etfuturum.compat.nei.CartographyRecipeHandler();
        maps.loadCraftingRecipes("etfuturum.cartography");check(maps.numRecipes()==3,"cartography operation examples missing");
        check(maps.getResultStack(0).item.stackSize==2 && maps.getResultStack(1).item.stackSize==1 && maps.getResultStack(2).item.stackSize==1,"cartography NEI counts");
        maps.arecipes.clear();maps.loadUsageRecipes(new ItemStack(Items.paper));check(maps.numRecipes()==1,"paper shows foreign cartography operations");
        maps.arecipes.clear();maps.loadUsageRecipes(new ItemStack(Blocks.glass_pane));check(maps.numRecipes()==1,"pane lock operation missing");
        maps.arecipes.clear();maps.loadCraftingRecipes("smelting");check(maps.numRecipes()==0,"furnace leaked into cartography");
        check(order.get("etfuturum.cartography")<0,"cartography not preferred to fuel page");
        net.minecraft.item.Item.itemRegistry.addObject(4091,"etfuturum:probe_smithing_template",ganymedes01.etfuturum.ModItems.SMITHING_TEMPLATE.get());
        ganymedes01.etfuturum.recipes.SmithingTableRecipes.init();
        ganymedes01.etfuturum.compat.nei.SmithingRecipeHandler smithing=new ganymedes01.etfuturum.compat.nei.SmithingRecipeHandler();
        smithing.loadCraftingRecipes("etfuturum.smithing");check(smithing.numRecipes()>=9,"modern smithing NEI routes missing");
        check(smithing.getIngredientStacks(0).size()==3 && smithing.getIngredientStacks(0).get(0).item.getItem()==ganymedes01.etfuturum.ModItems.SMITHING_TEMPLATE.get(),"NEI missing template input");
        smithing.arecipes.clear();smithing.loadCraftingRecipes("smelting");check(smithing.numRecipes()==0,"generic furnace leaked into smithing");
        try { WorkstationAssetContracts.run();
        BookSigningContracts.run();
        ArmorTrimAssetContracts.run();
        SmithingScreenContracts.run();
        TrimNamingContracts.run(); }catch(Exception invalid) { throw new AssertionError("AssetDirector routing failed",invalid); }
        System.out.println("RECIPE_DISPLAY_CONTRACTS PASSED: actual NEI item dispatch, sequential result/usage queries, category isolation and registry overrides/removals");
    }
}
