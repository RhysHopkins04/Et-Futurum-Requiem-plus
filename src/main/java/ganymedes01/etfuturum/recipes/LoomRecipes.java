package ganymedes01.etfuturum.recipes;

import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.blocks.itemblocks.ItemBlockBanner;
import ganymedes01.etfuturum.configuration.configs.ConfigMapCompatibility;
import ganymedes01.etfuturum.core.utils.RecipeHelper;
import ganymedes01.etfuturum.core.utils.Utils;
import ganymedes01.etfuturum.lib.EnumColor;
import ganymedes01.etfuturum.tileentities.TileEntityBanner;
import ganymedes01.etfuturum.tileentities.TileEntityBanner.EnumBannerPattern;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import java.util.*;

/** Loom choices use the existing persisted banner IDs and legacy wool-color ordering. */
public final class LoomRecipes {
    private LoomRecipes() {}
    public static final EnumBannerPattern[] TEMPLATES={EnumBannerPattern.CURLY_BORDER,EnumBannerPattern.CREEPER,
        EnumBannerPattern.BRICKS,EnumBannerPattern.FLOWER,EnumBannerPattern.MOJANG,EnumBannerPattern.SKULL,
        EnumBannerPattern.GLOBE,EnumBannerPattern.PIGLIN,EnumBannerPattern.FLOW,EnumBannerPattern.GUSTER};
    public static boolean enabled() { return ModBlocks.LOOM.isEnabled() && ModBlocks.BANNER.isEnabled() && !ConfigMapCompatibility.mapCompatibilityMode; }
    public static void init() {
        if(!enabled() || !ModItems.BANNER_PATTERN.isEnabled())return;
        Object[] ingredients={Blocks.vine,new ItemStack(Items.skull,1,4),Blocks.brick_block,
            new ItemStack(Blocks.red_flower,1,8),new ItemStack(Items.golden_apple,1,1),new ItemStack(Items.skull,1,1)};
        for(int i=0;i<ingredients.length;i++)RecipeHelper.addShapelessRecipe(RecipeHelper.Priority.HIGH,
            new ItemStack(ModItems.BANNER_PATTERN.get(),1,i),Items.paper,ingredients[i]);
    }
    public static int dye(ItemStack stack) {
        if(stack!=null && stack.stackSize>0)for(String ore:Utils.getOreNames(stack))for(EnumColor color:EnumColor.VALUES)
            if(ore.equals(color.getOreName()))return color.getDamage();
        return -1;
    }
    public static boolean template(ItemStack stack) {
        return stack!=null && stack.stackSize>0 && ModItems.BANNER_PATTERN.isEnabled()
            && stack.getItem()==ModItems.BANNER_PATTERN.get() && stack.getItemDamage()>=0 && stack.getItemDamage()<TEMPLATES.length;
    }
    public static List<EnumBannerPattern> choices(ItemStack banner,ItemStack dye,ItemStack template) {
        if(!enabled() || banner==null || banner.stackSize<=0 || banner.getItem()!=ModBlocks.BANNER.getItem()
            || TileEntityBanner.getPatterns(banner)>=6 || dye(dye)<0)return Collections.emptyList();
        if(template!=null)return template(template)?Collections.singletonList(TEMPLATES[template.getItemDamage()]):Collections.emptyList();
        List<EnumBannerPattern> list=new ArrayList<>();
        for(EnumBannerPattern p:EnumBannerPattern.VALUES)if(p!=EnumBannerPattern.BASE && p.hasValidCrafting() && !p.hasCraftingStack())list.add(p);
        return Collections.unmodifiableList(list);
    }
    public static ItemStack apply(ItemStack banner,int color,EnumBannerPattern pattern) {
        if(banner==null || banner.getItem()!=ModBlocks.BANNER.getItem() || banner.stackSize<=0
            || color<0 || color>15 || pattern==null || pattern==EnumBannerPattern.BASE || TileEntityBanner.getPatterns(banner)>=6)return null;
        ItemStack output=banner.copy();output.stackSize=1;
        NBTTagCompound tag=ItemBlockBanner.getSubTag(output,"BlockEntityTag",true);
        NBTTagList patterns=tag.getTagList("Patterns",10);
        NBTTagCompound layer=new NBTTagCompound();layer.setString("Pattern",pattern.getPatternID());layer.setInteger("Color",color);
        patterns.appendTag(layer);tag.setTag("Patterns",patterns);return output;
    }
}
