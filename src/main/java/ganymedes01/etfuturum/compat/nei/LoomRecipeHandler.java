package ganymedes01.etfuturum.compat.nei;

import codechicken.nei.PositionedStack;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.recipes.LoomRecipes;
import ganymedes01.etfuturum.tileentities.TileEntityBanner.EnumBannerPattern;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import java.util.*;

/** White-base examples; the actual loom preserves any banner base and existing layers. */
public final class LoomRecipeHandler extends SimpleProcessRecipeHandler {
    @Override public String getOverlayIdentifier() { return "etfuturum.loom"; }
    @Override public String getRecipeName() { return StatCollector.translateToLocal("tile.etfuturum.loom.name"); }
    @Override protected String instruction() { return StatCollector.translateToLocal("efr.nei.loom.instruction"); }
    @Override protected List<ProcessRecipe> recipes() { return recipesFor(ModBlocks.BANNER.newItemStack(1,0)); }
    private List<ProcessRecipe> recipesFor(ItemStack base) {
        List<ProcessRecipe> rows=new ArrayList<>();if(!LoomRecipes.enabled())return rows;
        for(int color=0;color<16;color++)for(int template=-1;template<LoomRecipes.TEMPLATES.length;template++) {
            ItemStack banner=base.copy();banner.stackSize=1;ItemStack dye=new ItemStack(Items.dye,1,color);
            ItemStack pattern=template<0?null:new ItemStack(ModItems.BANNER_PATTERN.get(),1,template);
            for(EnumBannerPattern choice:LoomRecipes.choices(banner,dye,pattern)) {
                ProcessRecipe row=new ProcessRecipe(banner,net.minecraftforge.oredict.OreDictionary.getOres(ganymedes01.etfuturum.lib.EnumColor.VALUES[color].getOreName()),LoomRecipes.apply(banner,15-color,choice),ModBlocks.LOOM.newItemStack());
                if(pattern!=null)row.other.add(new PositionedStack(pattern,82,18));
                rows.add(row);
            }
        }
        return rows;
    }
    @Override public void loadUsageRecipes(ItemStack input) {
        if(input!=null && input.getItem()==ModBlocks.BANNER.getItem())arecipes.addAll(recipesFor(input));
        else super.loadUsageRecipes(input);
    }
    @Override public void loadCraftingRecipes(ItemStack result) {
        if(result==null || result.getItem()!=ModBlocks.BANNER.getItem())return;
        net.minecraft.nbt.NBTTagCompound tag=ganymedes01.etfuturum.blocks.itemblocks.ItemBlockBanner.getSubTag(result,"BlockEntityTag",false);
        if(tag==null || tag.getTagList("Patterns",10).tagCount()==0) { super.loadCraftingRecipes(result);return; }
        ItemStack input=result.copy();input.stackSize=1;
        net.minecraft.nbt.NBTTagCompound inputTag=ganymedes01.etfuturum.blocks.itemblocks.ItemBlockBanner.getSubTag(input,"BlockEntityTag",false);
        net.minecraft.nbt.NBTTagList layers=inputTag.getTagList("Patterns",10);layers.removeTag(layers.tagCount()-1);
        ItemStack expected=result.copy();expected.stackSize=1;
        for(ProcessRecipe row:recipesFor(input))if(ItemStack.areItemStacksEqual(row.output.item,expected))arecipes.add(row);
    }
}
