package ganymedes01.etfuturum.compat.nei;

import codechicken.lib.gui.GuiDraw;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.recipes.CartographyRecipes;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import java.util.*;

/** Operation examples do not allocate map IDs or saved data. Live usage queries respect known map state. */
public final class CartographyRecipeHandler extends SimpleProcessRecipeHandler {
    private final class Row extends ProcessRecipe {
        final String operation;
        Row(ItemStack map,ItemStack addition,String operation) {
            super(map,addition,output(map,operation),ModBlocks.CARTOGRAPHY_TABLE.newItemStack());this.operation=operation;
        }
    }
    private static ItemStack output(ItemStack map,String operation) { ItemStack out=map.copy();out.stackSize=operation.equals("copy")?2:1;return out; }
    @Override public String getOverlayIdentifier() { return "etfuturum.cartography"; }
    @Override public String getRecipeName() { return StatCollector.translateToLocal("container.cartography_table"); }
    @Override protected String instruction() { return ""; }
    private List<ProcessRecipe> rows(ItemStack map,boolean example) {
        List<ProcessRecipe> rows=new ArrayList<>();if(!CartographyRecipes.enabled())return rows;
        net.minecraft.client.Minecraft client=example?null:net.minecraft.client.Minecraft.getMinecraft();
        net.minecraft.world.World world=client==null?null:client.theWorld;
        map=map.copy();map.stackSize=1;
        ItemStack[] additions={new ItemStack(Items.map),new ItemStack(Items.paper),new ItemStack(Blocks.glass_pane)};
        String[] operations={"copy","expand","lock"};
        for(int i=0;i<3;i++)if(example || world==null || CartographyRecipes.data(world,map)==null || operations[i].equals(CartographyRecipes.operation(world,map,additions[i])))rows.add(new Row(map,additions[i],operations[i]));
        return rows;
    }
    @Override protected List<ProcessRecipe> recipes() { return rows(new ItemStack(Items.filled_map),true); }
    @Override public void loadUsageRecipes(ItemStack input) {
        if(input==null || !CartographyRecipes.enabled())return;
        if(input.getItem()==Items.filled_map)arecipes.addAll(rows(input,false));else super.loadUsageRecipes(input);
    }
    @Override public void drawExtras(int recipe) {
        super.drawExtras(recipe);
        GuiDraw.drawString(StatCollector.translateToLocal("efr.nei.cartography."+((Row)arecipes.get(recipe)).operation),4,64,0x404040,false);
    }
}
