package ganymedes01.etfuturum.recipes;

import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.ModernMapParityBlocks;
import ganymedes01.etfuturum.configuration.configs.ConfigMapCompatibility;
import ganymedes01.etfuturum.core.utils.RecipeHelper;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import net.minecraftforge.oredict.ShapedOreRecipe;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import java.util.ArrayList;
import java.util.List;

/** Reviewed 1.21.11 crafting routes, with feature-gated companion ingredients. */
public final class RecipeParityCorrections {
    private RecipeParityCorrections() {}

    public static void init() {
        if (ConfigMapCompatibility.mapCompatibilityMode) return;
        if (ModItems.COPPER_NUGGET.isEnabled()) {
            RecipeHelper.addShapelessRecipe(RecipeHelper.Priority.HIGH,ModItems.COPPER_NUGGET.newItemStack(9),"ingotCopper");
            // Use EFR's ingot when enabled; otherwise the pack's registered copper ingot.
            java.util.List<ItemStack> ingots=OreDictionary.getOres("ingotCopper");
            ItemStack ingot=ModItems.COPPER_INGOT.isEnabled()?ModItems.COPPER_INGOT.newItemStack():ingots.isEmpty()?null:ingots.get(0).copy();
            if(ingot!=null) { ingot.stackSize=1; RecipeHelper.addShapedRecipe(RecipeHelper.Priority.HIGH,ingot,"NNN","NNN","NNN",'N',"nuggetCopper"); }
            shaped(ModernMapParityBlocks.COPPER_CHAIN,1,"N","I","N",'N',"nuggetCopper",'I',"ingotCopper");
            shaped(ModernMapParityBlocks.COPPER_TORCH,4,"N","C","S",'N',"nuggetCopper",'C',new ItemStack(Items.coal,1,OreDictionary.WILDCARD_VALUE),'S',Items.stick);
            shaped(ModernMapParityBlocks.COPPER_LANTERN,1,"NNN","NTN","NNN",'N',"nuggetCopper",'T',ModernMapParityBlocks.COPPER_TORCH.get());
        }
        recolouring();
        copperWaxing();
        for (ModernMapParityBlocks entry : new ModernMapParityBlocks[]{ModernMapParityBlocks.PALE_OAK_LOG,
                ModernMapParityBlocks.PALE_OAK_WOOD, ModernMapParityBlocks.STRIPPED_PALE_OAK_LOG,
                ModernMapParityBlocks.STRIPPED_PALE_OAK_WOOD}) {
            if (entry.get() != null) RecipeHelper.addSmelting(new ItemStack(entry.get(), 1, OreDictionary.WILDCARD_VALUE),
                    new ItemStack(Items.coal, 1, 1), 0.15F);
        }
        dye(ModernMapParityBlocks.TORCHFLOWER, 14, 1);
        dye(ModernMapParityBlocks.OPEN_EYEBLOSSOM, 14, 1);
        dye(ModernMapParityBlocks.CLOSED_EYEBLOSSOM, 8, 1);
        dye(ModernMapParityBlocks.PITCHER_PLANT, 6, 2);
        dye(ModernMapParityBlocks.WILDFLOWERS, 11, 1);
        dye(ModernMapParityBlocks.CACTUS_FLOWER, 9, 1);

        shaped(ModernMapParityBlocks.CANDLE, 1, "S", "H", 'S', Items.string, 'H', ModItems.HONEYCOMB.get());
        for (int color = 0; color < ModRecipes.dye_names.length; color++) {
            ModernMapParityBlocks candle = ModernMapParityBlocks.valueOf(ModRecipes.dye_names[color].toUpperCase(java.util.Locale.ROOT) + "_CANDLE");
            if (candle.get() != null && ModernMapParityBlocks.CANDLE.get() != null)
                RecipeHelper.addShapelessRecipe(RecipeHelper.Priority.HIGH, new ItemStack(candle.get()),
                        ModernMapParityBlocks.CANDLE.get(), ModRecipes.ore_dyes[15-color]);
        }
        shaped(ModernMapParityBlocks.CAMPFIRE, 1, " S ", "SCS", "LLL", 'S', Items.stick,
                'C', new ItemStack(Items.coal, 1, OreDictionary.WILDCARD_VALUE), 'L', "logWood");
        shaped(ModernMapParityBlocks.SOUL_CAMPFIRE, 1, " S ", "SCS", "LLL", 'S', Items.stick, 'C', Blocks.soul_sand, 'L', "logWood");
        shaped(ModernMapParityBlocks.SOUL_CAMPFIRE, 1, " S ", "SCS", "LLL", 'S', Items.stick, 'C', ModBlocks.SOUL_SOIL.get(), 'L', "logWood");
        shaped(ModernMapParityBlocks.CRAFTER,1,"###","#C#","RDR",'#',Items.iron_ingot,'C',Blocks.crafting_table,'D',Blocks.dropper,'R',Items.redstone);
        shaped(ModernMapParityBlocks.CALIBRATED_SCULK_SENSOR, 1, " A ", "ASA", 'A', ModItems.AMETHYST_SHARD.get(), 'S', ModernMapParityBlocks.SCULK_SENSOR.get());
        shaped(ModernMapParityBlocks.CHISELED_BOOKSHELF, 1, "PPP", "SSS", "PPP", 'P', "plankWood", 'S', "slabWood");
        shaped(ModernMapParityBlocks.COPPER_CHEST, 1, "CCC", "CXC", "CCC", 'C', ModItems.COPPER_INGOT.get(), 'X', Blocks.chest);
        shaped(ModernMapParityBlocks.SCAFFOLDING, 6, "BSB", "B B", "B B", 'B', ModItems.BAMBOO.get(), 'S', Items.string);
        shaped(ModernMapParityBlocks.COPPER_BARS,16,"CCC","CCC",'C',ModItems.COPPER_INGOT.get());
        shaped(ModernMapParityBlocks.PALE_MOSS_CARPET,3,"MM",'M',ModernMapParityBlocks.PALE_MOSS_BLOCK.get());
        if(ModernMapParityBlocks.SEA_PICKLE.get()!=null) RecipeHelper.addSmelting(
                new ItemStack(ModernMapParityBlocks.SEA_PICKLE.get(),1,OreDictionary.WILDCARD_VALUE),new ItemStack(Items.dye,1,10),0.1F);
        if(ModBlocks.RED_NETHERBRICK.isEnabled() && FurnaceRecipes.smelting().getSmeltingResult(new ItemStack(Blocks.nether_brick))==null)
            RecipeHelper.addSmelting(Blocks.nether_brick,ModBlocks.RED_NETHERBRICK.newItemStack(1,1),0.1F);
        if(ModBlocks.PURPUR_BLOCK.isEnabled() && ModBlocks.PURPUR_PILLAR.isEnabled()) {
            purpur(ModBlocks.PURPUR_SLAB.newItemStack(6),new String[]{"PPP"});
            purpur(ModBlocks.PURPUR_STAIRS.newItemStack(4),new String[]{"P  ","PP ","PPP"});
        }
        if(ModBlocks.SOUL_TORCH.isEnabled()) {
            RecipeHelper.addShapedRecipe(RecipeHelper.Priority.HIGH,ModBlocks.SOUL_TORCH.newItemStack(4),
                    "C","S","B",'C',new ItemStack(Items.coal,1,OreDictionary.WILDCARD_VALUE),'S',Items.stick,'B',Blocks.soul_sand);
            RecipeHelper.addShapedRecipe(RecipeHelper.Priority.HIGH,ModBlocks.SOUL_TORCH.newItemStack(4),
                    "C","S","B",'C',new ItemStack(Items.coal,1,OreDictionary.WILDCARD_VALUE),'S',Items.stick,'B',ModBlocks.SOUL_SOIL.get());
        }
        ItemStack daylight=ganymedes01.etfuturum.configuration.configs.ConfigWorld.tileReplacementMode==-1
                && ModBlocks.DAYLIGHT_DETECTOR.isEnabled() ? ModBlocks.DAYLIGHT_DETECTOR.newItemStack() : new ItemStack(Blocks.daylight_detector);
        RecipeHelper.addShapedRecipe(RecipeHelper.Priority.HIGH,daylight,"GGG","QQQ","SSS",'G',Blocks.glass,'Q',Items.quartz,'S',"slabWood");
        RecipeHelper.addShapedRecipe(RecipeHelper.Priority.HIGH,new ItemStack(Items.saddle)," L ","LIL",'L',Items.leather,'I',Items.iron_ingot);
        if(ModItems.OAK_CHEST_BOAT.isEnabled()) RecipeHelper.addShapelessRecipe(RecipeHelper.Priority.HIGH,
                ModItems.OAK_CHEST_BOAT.newItemStack(),Items.boat,Blocks.chest);

        if (ModItems.HONEY_BOTTLE.isEnabled()) RecipeHelper.addShapelessRecipe(RecipeHelper.Priority.HIGH,
                new ItemStack(Items.sugar, 3), ModItems.HONEY_BOTTLE.get());
        if (ModBlocks.LIGHTNING_ROD.isEnabled()) RecipeHelper.addShapedRecipe(RecipeHelper.Priority.HIGH,
                ModBlocks.LIGHTNING_ROD.newItemStack(), "C", "C", "C", 'C', ModItems.COPPER_INGOT.get());

        minecart(Items.chest_minecart, Blocks.chest);
        minecart(Items.furnace_minecart, Blocks.furnace);
        minecart(Items.hopper_minecart, Blocks.hopper);
        minecart(Items.tnt_minecart, Blocks.tnt);
        removeVanillaShaped(Items.lead);
        RecipeHelper.addShapedRecipe(RecipeHelper.Priority.HIGH, new ItemStack(Items.lead, 2),
                "SS ", "SS ", "  S", 'S', Items.string);
        RecipeHelper.addShapelessRecipe(RecipeHelper.Priority.HIGH, new ItemStack(Blocks.packed_ice),
                Blocks.ice, Blocks.ice, Blocks.ice, Blocks.ice, Blocks.ice, Blocks.ice, Blocks.ice, Blocks.ice, Blocks.ice);

        recycle(new Item[]{Items.golden_helmet, Items.golden_chestplate, Items.golden_leggings, Items.golden_boots,
                Items.golden_sword, Items.golden_pickaxe, Items.golden_axe, Items.golden_shovel, Items.golden_hoe,
                Items.golden_horse_armor}, new ItemStack(Items.gold_nugget));
        if (ModItems.NUGGET_IRON.isEnabled()) recycle(new Item[]{Items.iron_helmet, Items.iron_chestplate,
                Items.iron_leggings, Items.iron_boots, Items.iron_sword, Items.iron_pickaxe, Items.iron_axe,
                Items.iron_shovel, Items.iron_hoe, Items.iron_horse_armor, Items.chainmail_helmet,
                Items.chainmail_chestplate, Items.chainmail_leggings, Items.chainmail_boots}, ModItems.NUGGET_IRON.newItemStack());
    }

    private static void dye(ModernMapParityBlocks plant, int legacyDye, int count) {
        if (plant.get() != null) RecipeHelper.addShapelessRecipe(RecipeHelper.Priority.HIGH,
                new ItemStack(Items.dye, count, legacyDye), plant.get());
    }

    private static void shaped(ModernMapParityBlocks output, int count, Object... recipe) {
        if (output.get() != null) RecipeHelper.addShapedRecipe(RecipeHelper.Priority.HIGH, new ItemStack(output.get(), count), recipe);
    }

    private static void removeVanillaShaped(Item item) {
        RecipeHelper.removeAllMatchingRecipes(recipe -> recipe.getClass() == ShapedRecipes.class
                && recipe.getRecipeOutput() != null && recipe.getRecipeOutput().getItem() == item);
    }

    private static void minecart(Item output, net.minecraft.block.Block container) {
        removeVanillaShaped(output);
        RecipeHelper.addShapelessRecipe(RecipeHelper.Priority.HIGH, new ItemStack(output), container, Items.minecart);
    }

    private static void recycle(Item[] equipment, ItemStack nugget) {
        for (Item item : equipment) {
            // Do not replace a modpack's existing furnace processing output.
            if (FurnaceRecipes.smelting().getSmeltingResult(new ItemStack(item)) == null)
                RecipeHelper.addSmelting(new ItemStack(item, 1, OreDictionary.WILDCARD_VALUE), nugget.copy(), 0.1F);
        }
    }

    private static void recolouring() {
        for (int color=0;color<16;color++) {
            String dye=ModRecipes.ore_dyes[15-color];
            List<ItemStack> wool=new ArrayList<>(), carpet=new ArrayList<>(), beds=new ArrayList<>();
            for (int source=0;source<16;source++) if (source!=color) {
                wool.add(new ItemStack(Blocks.wool,1,source));
                carpet.add(new ItemStack(Blocks.carpet,1,source));
                Item bed=bed(source);if(bed!=null) beds.add(new ItemStack(bed));
            }
            recolour(new ItemStack(Blocks.wool,1,color),wool,dye);
            recolour(new ItemStack(Blocks.carpet,1,color),carpet,dye);
            Item target=bed(color);if(target!=null) recolour(new ItemStack(target),beds,dye);
            RecipeHelper.addShapedRecipe(RecipeHelper.Priority.HIGH,new ItemStack(Blocks.stained_glass_pane,8,color),
                    "PPP","PDP","PPP",'P',Blocks.glass_pane,'D',dye);
        }
    }

    private static Item bed(int color) {
        return color==14 ? Items.bed : GameRegistry.findItem("etfuturum",ModRecipes.dye_names[color]+"_bed");
    }

    private static void recolour(ItemStack output,List<ItemStack> sources,String dye) {
        if(sources.isEmpty())return;
        ShapelessOreRecipe recipe=new ShapelessOreRecipe(output,sources.get(0),dye);
        recipe.getInput().set(0,sources);
        GameRegistry.addRecipe(recipe);
    }

    private static void copperWaxing() {
        if(!ModItems.HONEYCOMB.isEnabled())return;
        for(Object key:Block.blockRegistry.getKeys()) {
            String name=key.toString();
            if(!name.startsWith("etfuturum:waxed_"))continue;
            Block waxed=(Block)Block.blockRegistry.getObject(key);
            Block plain=(Block)Block.blockRegistry.getObject("etfuturum:"+name.substring("etfuturum:waxed_".length()));
            ModernMapParityBlocks parity=ModernMapParityBlocks.fromBlock(plain);
            if(!(plain instanceof ganymedes01.etfuturum.blocks.BlockCopperBulb)
                    && plain!=ModBlocks.LIGHTNING_ROD.get()
                    && (parity==null || !parity.isPass36CopperLifecycleIdentity()))continue;
            if(Item.getItemFromBlock(plain)!=null && Item.getItemFromBlock(waxed)!=null)
                RecipeHelper.addShapelessRecipe(RecipeHelper.Priority.HIGH,new ItemStack(waxed),plain,ModItems.HONEYCOMB.get());
        }
        if(ModBlocks.COPPER_BULB.isEnabled()) for(int stage=0;stage<4;stage++)
            RecipeHelper.addShapelessRecipe(RecipeHelper.Priority.HIGH,ModBlocks.COPPER_BULB.newItemStack(1,8+stage),
                    ModBlocks.COPPER_BULB.newItemStack(1,stage),ModItems.HONEYCOMB.get());
    }

    private static void purpur(ItemStack output,String[] pattern) {
        if(!RecipeHelper.validateItems(output))return;
        List<ItemStack> choices=new ArrayList<>();choices.add(ModBlocks.PURPUR_BLOCK.newItemStack());
        choices.add(ModBlocks.PURPUR_PILLAR.newItemStack(1,OreDictionary.WILDCARD_VALUE));
        ShapedOreRecipe recipe=new ShapedOreRecipe(output,pattern,'P',ModBlocks.PURPUR_BLOCK.get());
        for(int i=0;i<recipe.getInput().length;i++) if(recipe.getInput()[i]!=null) recipe.getInput()[i]=choices;
        GameRegistry.addRecipe(recipe);
    }
}
