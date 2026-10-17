package ganymedes01.etfuturum.compat.nei;

import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.recipes.StonecutterRecipes;
import net.minecraft.util.StatCollector;
import java.util.ArrayList;
import java.util.List;

public final class StonecutterRecipeHandler extends SimpleProcessRecipeHandler {
    @Override public String getOverlayIdentifier() { return "etfuturum.stonecutting"; }
    @Override public String getRecipeName() { return StatCollector.translateToLocal("tile.etfuturum.stonecutter.name"); }
    @Override protected String instruction() { return StatCollector.translateToLocal("efr.nei.stonecutter.instruction"); }
    @Override protected List<ProcessRecipe> recipes() {
        List<ProcessRecipe> rows=new ArrayList<>();
        for(StonecutterRecipes.Recipe recipe:StonecutterRecipes.getAll())
            rows.add(new ProcessRecipe(recipe.getInput(),null,recipe.getOutput(),ModBlocks.STONECUTTER.newItemStack()));
        return rows;
    }
}
