package ganymedes01.etfuturum.compat.nei;

import ganymedes01.etfuturum.ModernMapParityBlocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import java.util.ArrayList;
import java.util.List;

public class CandleCakeRecipeHandler extends SimpleProcessRecipeHandler {
    @Override public String getOverlayIdentifier() { return "etfuturum.candlecake"; }
    @Override public String getRecipeName() { return StatCollector.translateToLocal("efr.nei.candlecake"); }
    @Override protected String instruction() { return StatCollector.translateToLocal("efr.nei.candlecake.instruction"); }
    @Override protected List<ProcessRecipe> recipes() {
        List<ProcessRecipe> result=new ArrayList<>();
        for(ModernMapParityBlocks candle:ModernMapParityBlocks.values()) {
            ModernMapParityBlocks cake=ModernMapParityBlocks.candleCakeFor(candle);
            if(candle.get()!=null && cake!=null && cake.get()!=null)
                result.add(new ProcessRecipe(new ItemStack(Items.cake),new ItemStack(candle.get()),new ItemStack(cake.get()),null));
        }
        return result;
    }
}
