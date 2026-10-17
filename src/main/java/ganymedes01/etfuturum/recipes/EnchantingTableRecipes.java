package ganymedes01.etfuturum.recipes;

import ganymedes01.etfuturum.ModEnchantments;
import ganymedes01.etfuturum.api.GrindstoneRegistry;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.WeightedRandom;
import java.util.*;

/** Table-only eligibility. Loot/anvil/book registries retain their existing treasure entries. */
public final class EnchantingTableRecipes {
    private static final Set<Enchantment> TREASURES=Collections.newSetFromMap(new IdentityHashMap<Enchantment,Boolean>());
    private EnchantingTableRecipes() {}
    public static void registerTreasure(Enchantment enchantment) { if(enchantment==null)throw new IllegalArgumentException("null enchantment");TREASURES.add(enchantment); }
    public static boolean allowed(Enchantment enchantment) {
        return enchantment!=null && enchantment!=ModEnchantments.mending && enchantment!=ModEnchantments.frostWalker
            && enchantment!=ModEnchantments.swiftSneak && !GrindstoneRegistry.isCurse(enchantment) && !TREASURES.contains(enchantment);
    }
    public static List<EnchantmentData> build(Random random,ItemStack stack,int level) {
        int enchantability=stack.getItem().getItemEnchantability(stack);if(enchantability<=0)return null;
        int spread=enchantability/4+1;
        int modified=level+1+random.nextInt(spread)+random.nextInt(spread);
        float variation=(random.nextFloat()+random.nextFloat()-1F)*.15F;
        modified=Math.max(1,Math.round(modified*(1F+variation)));
        Map<Integer,EnchantmentData> raw=EnchantmentHelper.mapEnchantmentData(modified,stack);
        if(raw==null)return null;
        Map<Integer,EnchantmentData> candidates=new HashMap<>(raw);
        for(Iterator<EnchantmentData> it=candidates.values().iterator();it.hasNext();)if(!allowed(it.next().enchantmentobj))it.remove();
        if(candidates.isEmpty())return null;
        List<EnchantmentData> selected=new ArrayList<>();
        selected.add((EnchantmentData)WeightedRandom.getRandomItem(random,candidates.values()));
        for(int chance=modified;random.nextInt(50)<=chance;chance>>=1) {
            for(Iterator<EnchantmentData> it=candidates.values().iterator();it.hasNext();) {
                Enchantment candidate=it.next().enchantmentobj;boolean compatible=true;
                for(EnchantmentData previous:selected)if(!previous.enchantmentobj.canApplyTogether(candidate) || !candidate.canApplyTogether(previous.enchantmentobj)) { compatible=false;break; }
                if(!compatible)it.remove();
            }
            if(candidates.isEmpty())break;
            selected.add((EnchantmentData)WeightedRandom.getRandomItem(random,candidates.values()));
        }
        return selected;
    }
}
