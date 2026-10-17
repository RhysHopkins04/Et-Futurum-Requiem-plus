package ganymedes01.etfuturum.recipes;

import ganymedes01.etfuturum.ModernMapParityBlocks;
import ganymedes01.etfuturum.api.GrindstoneRegistry;
import ganymedes01.etfuturum.configuration.configs.ConfigMapCompatibility;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import java.util.*;

/** Pure preview calculation; only the server result slot consumes inputs or awards XP. */
public final class GrindstoneRecipes {
    private GrindstoneRecipes() {}
    public static boolean enabled() { return ModernMapParityBlocks.GRINDSTONE.get()!=null && !ConfigMapCompatibility.mapCompatibilityMode; }
    private static NBTTagList enchantments(ItemStack stack) {
        if(stack==null || !stack.hasTagCompound())return new NBTTagList();
        return stack.getTagCompound().getTagList(stack.getItem()==Items.enchanted_book?"StoredEnchantments":"ench",10);
    }
    private static Enchantment enchantment(int id) { return id>=0 && id<Enchantment.enchantmentsList.length?Enchantment.enchantmentsList[id]:null; }
    private static boolean retained(int id) {
        Enchantment enchantment=enchantment(id);
        // Removed/disabled mod enchantments must not silently destroy saved item data.
        return enchantment==null || GrindstoneRegistry.isCurse(enchantment);
    }
    public static boolean accepts(ItemStack stack) { return stack!=null && stack.stackSize>0 && (stack.isItemStackDamageable() || enchantments(stack).tagCount()>0 || stack.getItem()==Items.enchanted_book); }
    public static ItemStack result(ItemStack first,ItemStack second) {
        if(first!=null && first.stackSize!=1 || second!=null && second.stackSize!=1)return null;
        if(first==null && second==null)return null;
        ItemStack source=first==null?second:first,output=source.copy();output.stackSize=1;
        if(first==null || second==null) {
            if(enchantments(source).tagCount()==0)return null;
        } else {
            if(first.getItem()!=second.getItem())return null;
            if(first.isItemStackDamageable() && second.isItemStackDamageable()) {
                int max=Math.max(first.getMaxDamage(),second.getMaxDamage());
                long remaining=(long)first.getMaxDamage()-first.getItemDamage()+second.getMaxDamage()-second.getItemDamage()+max*5L/100;
                output.setItemDamage((int)Math.max(0,max-remaining));
            } else {
                if(first.getMaxStackSize()<2 || !first.isItemEqual(second) || !ItemStack.areItemStackTagsEqual(first,second))return null;
                output.stackSize=2;
            }
        }
        Map<Integer,NBTTagCompound> kept=new LinkedHashMap<>();
        for(ItemStack input:new ItemStack[]{source,first!=null && second!=null?second:null}) {
            NBTTagList list=enchantments(input);
            for(int i=0;i<list.tagCount();i++) {
                NBTTagCompound entry=list.getCompoundTagAt(i);int id=entry.getShort("id");
                if(!retained(id))continue;
                NBTTagCompound old=kept.get(id);
                if(old==null || entry.getShort("lvl")>old.getShort("lvl"))kept.put(id,(NBTTagCompound)entry.copy());
            }
        }
        if(output.hasTagCompound()) { output.getTagCompound().removeTag("ench");output.getTagCompound().removeTag("StoredEnchantments");output.getTagCompound().removeTag("RepairCost"); }
        if(output.getItem()==Items.enchanted_book && kept.isEmpty()) { output.func_150996_a(Items.book);output.setItemDamage(0); }
        if(!kept.isEmpty()) {
            NBTTagList list=new NBTTagList();for(NBTTagCompound entry:kept.values())list.appendTag(entry);
            output.setTagInfo(output.getItem()==Items.enchanted_book?"StoredEnchantments":"ench",list);
            // Modern prior-work penalty is rebuilt from retained enchantment count.
            int cost=0;for(int i=0;i<kept.size();i++)cost=cost>(Integer.MAX_VALUE-1)/2?Integer.MAX_VALUE:cost*2+1;
            output.setRepairCost(cost);
        }
        if(output.hasTagCompound() && output.getTagCompound().hasNoTags())output.setTagCompound(null);
        return output;
    }
    public static int experienceValue(ItemStack first,ItemStack second) {
        long total=0;
        for(ItemStack input:new ItemStack[]{first,second}) {
            NBTTagList list=enchantments(input);
            for(int i=0;i<list.tagCount();i++) {
                NBTTagCompound entry=list.getCompoundTagAt(i);int id=entry.getShort("id");Enchantment enchantment=enchantment(id);
                if(enchantment!=null && !GrindstoneRegistry.isCurse(enchantment) && entry.getShort("lvl")>0)
                    total+=Math.max(0,enchantment.getMinEnchantability(entry.getShort("lvl")));
            }
        }
        return (int)Math.min(total,Integer.MAX_VALUE-1L);
    }
}
