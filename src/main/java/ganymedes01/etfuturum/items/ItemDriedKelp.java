package ganymedes01.etfuturum.items;

import net.minecraft.item.ItemStack;

/** Modern dried kelp: one hunger point and half the ordinary eating duration. */
public final class ItemDriedKelp extends BaseFood {
    public ItemDriedKelp() { super(1,0.3F,false); setNames("dried_kelp"); }
    @Override public int getMaxItemUseDuration(ItemStack stack) { return 16; }
}
