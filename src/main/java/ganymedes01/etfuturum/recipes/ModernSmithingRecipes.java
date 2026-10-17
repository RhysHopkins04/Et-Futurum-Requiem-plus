package ganymedes01.etfuturum.recipes;

import ganymedes01.etfuturum.*;
import ganymedes01.etfuturum.configuration.configs.*;
import ganymedes01.etfuturum.items.ItemSmithingTemplate;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Container;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;
import java.util.*;

/** Modern template/base/addition semantics; the old two-input recipe API remains available to mods. */
public final class ModernSmithingRecipes {
    private ModernSmithingRecipes() {}
    public static boolean enabled() { return ModBlocks.SMITHING_TABLE.isEnabled() && !ConfigMapCompatibility.mapCompatibilityMode; }
    public static boolean template(ItemStack stack) { return stack!=null && stack.stackSize>0 && ModItems.SMITHING_TEMPLATE.isEnabled() && stack.getItem()==ModItems.SMITHING_TEMPLATE.get() && stack.getItemDamage()>=0 && stack.getItemDamage()<19; }
    public static String pattern(ItemStack template) { return template(template) && template.getItemDamage()>0?ItemSmithingTemplate.NAMES[template.getItemDamage()].replace("_armor_trim_smithing_template",""):""; }
    public static final String[] MATERIALS={"quartz","iron","netherite","redstone","copper","gold","emerald","diamond","lapis","amethyst","resin"};
    public static String material(ItemStack stack) {
        if(stack==null || stack.stackSize<=0)return "";
        if(stack.getItem()==Items.iron_ingot)return "iron";
        if(stack.getItem()==Items.gold_ingot)return "gold";
        if(stack.getItem()==Items.quartz)return "quartz";
        if(stack.getItem()==Items.redstone)return "redstone";
        if(stack.getItem()==Items.emerald)return "emerald";
        if(stack.getItem()==Items.diamond)return "diamond";
        if(stack.getItem()==Items.dye && stack.getItemDamage()==4)return "lapis";
        if(ModItems.AMETHYST_SHARD.isEnabled() && stack.getItem()==ModItems.AMETHYST_SHARD.get())return "amethyst";
        if(ModItems.RESIN_BRICK.isEnabled() && stack.getItem()==ModItems.RESIN_BRICK.get())return "resin";
        if(ModItems.NETHERITE_INGOT.isEnabled() && stack.getItem()==ModItems.NETHERITE_INGOT.get() && stack.getItemDamage()==0)return "netherite";
        if(ModItems.COPPER_INGOT.isEnabled() && stack.getItem()==ModItems.COPPER_INGOT.get() && stack.getItemDamage()==0)return "copper";
        if(!ConfigFunctions.allowModdedTrimMaterials)return "";
        for(String ore:new String[]{"ingotIron","ingotNetherite","ingotCopper","ingotGold"})for(ItemStack candidate:OreDictionary.getOres(ore))if(OreDictionary.itemMatches(candidate,stack,false))return ore.substring(5).toLowerCase(java.util.Locale.ROOT);
        return "";
    }
    public static ItemStack materialExample(String material) {
        switch(material) {
            case "iron":return new ItemStack(Items.iron_ingot);
            case "gold":return new ItemStack(Items.gold_ingot);
            case "netherite":if(ModItems.NETHERITE_INGOT.isEnabled())return ModItems.NETHERITE_INGOT.newItemStack();break;
            case "copper":if(ModItems.COPPER_INGOT.isEnabled())return ModItems.COPPER_INGOT.newItemStack();break;
            case "quartz":return new ItemStack(Items.quartz);
            case "redstone":return new ItemStack(Items.redstone);
            case "emerald":return new ItemStack(Items.emerald);
            case "diamond":return new ItemStack(Items.diamond);
            case "lapis":return new ItemStack(Items.dye,1,4);
            case "amethyst":return ModItems.AMETHYST_SHARD.isEnabled()?ModItems.AMETHYST_SHARD.newItemStack():null;
            case "resin":return ModItems.RESIN_BRICK.isEnabled()?ModItems.RESIN_BRICK.newItemStack():null;
            default:return null;
        }
        if(ConfigFunctions.allowModdedTrimMaterials)for(ItemStack stack:OreDictionary.getOres("ingot"+material.substring(0,1).toUpperCase(java.util.Locale.ROOT)+material.substring(1)))if(material.equals(material(stack)))return stack.copy();
        return null;
    }
    public static boolean trimmable(ItemStack stack) {
        if(stack==null || !(stack.getItem() instanceof ItemArmor))return false;
        if(ConfigFunctions.allowModdedArmorTrims)return true;
        String registry=String.valueOf(Item.itemRegistry.getNameForObject(stack.getItem()));
        return registry.startsWith("minecraft:") || stack.getItem()==ModItems.NETHERITE_HELMET.get() || stack.getItem()==ModItems.NETHERITE_CHESTPLATE.get()
            || stack.getItem()==ModItems.NETHERITE_LEGGINGS.get() || stack.getItem()==ModItems.NETHERITE_BOOTS.get();
    }
    public static NBTTagCompound trim(ItemStack stack) {
        if(stack==null || !stack.hasTagCompound() || !stack.getTagCompound().hasKey("Trim",10))return null;
        NBTTagCompound trim=stack.getTagCompound().getCompoundTag("Trim");String p=trim.getString("pattern").replace("minecraft:",""),m=trim.getString("material").replace("minecraft:","");
        boolean validPattern=false;for(int i=1;i<ItemSmithingTemplate.NAMES.length;i++)if(ItemSmithingTemplate.NAMES[i].equals(p+"_armor_trim_smithing_template"))validPattern=true;
        return validPattern && Arrays.asList(MATERIALS).contains(m)?trim:null;
    }
    public static boolean accepts(ItemStack stack,int slot) {
        if(stack==null || stack.stackSize<=0)return false;
        return slot==0?template(stack):slot==1?trimmable(stack) || SmithingTableRecipes.getInstance().accepts(stack,false):!material(stack).isEmpty() || SmithingTableRecipes.getInstance().accepts(stack,true);
    }
    public static ItemStack result(ItemStack template,ItemStack base,ItemStack addition,World world) {
        if(!enabled() || base==null || base.stackSize<=0 || addition==null || addition.stackSize<=0)return null;
        if(template!=null && !template(template))return null;
        if(template!=null && template.getItemDamage()>0) {
            String pattern=pattern(template),material=material(addition);if(!trimmable(base) || material.isEmpty())return null;
            NBTTagCompound old=trim(base);if(old!=null && old.getString("pattern").equals("minecraft:"+pattern) && old.getString("material").equals("minecraft:"+material))return null;
            ItemStack output=base.copy();output.stackSize=1;NBTTagCompound trim=new NBTTagCompound();trim.setString("pattern","minecraft:"+pattern);trim.setString("material","minecraft:"+material);output.setTagInfo("Trim",trim);return output;
        }
        InventoryCrafting grid=new InventoryCrafting(new Container(){@Override public boolean canInteractWith(EntityPlayer player){return false;}},2,1);
        grid.setInventorySlotContents(0,base.copy());grid.setInventorySlotContents(1,addition.copy());
        for(SmithingTableRecipes.SmithingTableRecipe recipe:SmithingTableRecipes.getInstance().getRecipes()) {
            if(template!=null && !recipe.isBuiltinUpgrade())continue;
            if(template==null && recipe.isBuiltinUpgrade() && ConfigFunctions.requireNetheriteUpgradeTemplate)continue;
            if(recipe.matches(grid,world))return recipe.getCraftingResult(grid);
        }
        return null;
    }
    public static void initDuplication() {
        if(!enabled() || !ModItems.SMITHING_TEMPLATE.isEnabled())return;
        try(java.io.InputStream input=ModernSmithingRecipes.class.getResourceAsStream("/assets/etfuturum/recipes/smithing_1_21_11.json")) {
            if(input==null)throw new IllegalStateException("Missing smithing catalog");
            com.google.gson.JsonObject root=new com.google.gson.JsonParser().parse(new java.io.InputStreamReader(input,"UTF-8")).getAsJsonObject();
            if(!root.get("vanilla_sha1").getAsString().equals("ba2df812c2d12e0219c489c4cd9a5e1f0760f5bd"))throw new IllegalStateException("Unknown smithing catalog");
            for(com.google.gson.JsonElement element:root.getAsJsonArray("templates")) {
                com.google.gson.JsonObject row=element.getAsJsonObject();int meta=row.get("meta").getAsInt();if(meta==0 && !ConfigBlocksItems.enableNetherite)continue;
                for(com.google.gson.JsonElement candidate:row.getAsJsonArray("targets")) {
                    com.google.gson.JsonObject target=candidate.getAsJsonObject();String[] id=target.get("item").getAsString().split(":",2);Item item=cpw.mods.fml.common.registry.GameRegistry.findItem(id[0],id[1]);
                    if(item!=null)cpw.mods.fml.common.registry.GameRegistry.addRecipe(new net.minecraftforge.oredict.ShapedOreRecipe(ModItems.SMITHING_TEMPLATE.newItemStack(2,meta),"#S#","#C#","###",'#',Items.diamond,'S',ModItems.SMITHING_TEMPLATE.newItemStack(1,meta),'C',new ItemStack(item,1,target.get("meta").getAsInt())));
                }
            }
        }catch(Exception failure) { throw new IllegalStateException("Cannot initialize template duplication",failure); }
    }
}
