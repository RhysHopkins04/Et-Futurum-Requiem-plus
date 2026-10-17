package ganymedes01.etfuturum.recipes;

import com.google.gson.*;
import cpw.mods.fml.common.registry.GameRegistry;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.configuration.configs.ConfigMapCompatibility;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Exact registered-item/metadata routes; no numeric block IDs or crafting substitutions. */
public final class StonecutterRecipes {
    private static List<Recipe> all=Collections.emptyList();
    private StonecutterRecipes() {}
    public static boolean enabled() { return ModBlocks.STONECUTTER.isEnabled() && !ConfigMapCompatibility.mapCompatibilityMode; }
    public static final class Recipe {
        private final String id;
        private final ItemStack input,output;
        private Recipe(String id,ItemStack input,ItemStack output) { this.id=id;this.input=input.copy();this.output=output.copy(); }
        public String getId() { return id; }
        public ItemStack getInput() { return input.copy(); }
        public ItemStack getOutput() { return output.copy(); }
        public boolean matches(ItemStack stack) {
            return stack!=null && stack.stackSize>0 && stack.getItem()==input.getItem() && stack.getItemDamage()==input.getItemDamage();
        }
    }
    public static void init() {
        all=Collections.emptyList();if(!enabled())return;
        List<Recipe> loaded=new ArrayList<>();Set<String> ids=new HashSet<>();
        try(InputStream stream=StonecutterRecipes.class.getResourceAsStream("/assets/etfuturum/recipes/stonecutting_1_21_11.json")) {
            if(stream==null)throw new IllegalStateException("Missing stonecutting catalog");
            JsonObject root=new JsonParser().parse(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
            if(root.get("schema").getAsInt()!=1 || !"ba2df812c2d12e0219c489c4cd9a5e1f0760f5bd".equals(root.get("vanilla_sha1").getAsString()))
                throw new IllegalStateException("Unrecognised stonecutting catalog");
            for(JsonElement element:root.getAsJsonArray("recipes")) {
                JsonObject row=element.getAsJsonObject();String id=row.get("id").getAsString();
                if(!ids.add(id))throw new IllegalStateException("Duplicate stonecutting recipe "+id);
                ItemStack input=read(row.getAsJsonObject("input")),output=read(row.getAsJsonObject("output"));
                if(input!=null && output!=null)loaded.add(new Recipe(id,input,output)); // Disabled registry identities are omitted.
            }
        } catch(Exception failure) { throw new IllegalStateException("Cannot load stonecutting recipes",failure); }
        loaded.sort(Comparator.comparing(Recipe::getId));all=Collections.unmodifiableList(loaded);
        for(Recipe recipe:all)if(find(recipe.input).size()>128)throw new IllegalStateException("Too many stonecutter selections for vanilla button protocol");
    }
    private static ItemStack read(JsonObject object) {
        String[] name=object.get("item").getAsString().split(":",2);
        if(name.length!=2)throw new IllegalArgumentException("Invalid stonecutting item");
        Item item=GameRegistry.findItem(name[0],name[1]);
        int meta=object.get("meta").getAsInt(),count=object.has("count")?object.get("count").getAsInt():1;
        if(meta<0 || meta>32767 || count<1 || count>64)throw new IllegalArgumentException("Invalid stonecutting stack");
        return item==null || item.delegate.name()==null ? null : new ItemStack(item,count,meta);
    }
    public static List<Recipe> getAll() { return all; }
    public static List<Recipe> find(ItemStack input) {
        List<Recipe> choices=new ArrayList<>();for(Recipe recipe:all)if(recipe.matches(input))choices.add(recipe);
        return Collections.unmodifiableList(choices);
    }
}
