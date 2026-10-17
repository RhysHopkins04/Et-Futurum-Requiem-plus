package ganymedes01.etfuturum.mixins.early.cartography;

import ganymedes01.etfuturum.api.LockedMapData;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.RecipesMapExtending;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RecipesMapExtending.class)
public abstract class MixinRecipesMapExtending {
    @Inject(method="matches",at=@At("HEAD"),cancellable=true)
    private void etfuturum$locked(InventoryCrafting grid,World world,CallbackInfoReturnable<Boolean> cir) {
        for(int i=0;i<grid.getSizeInventory();i++) {
            ItemStack stack=grid.getStackInSlot(i);
            if(stack!=null && stack.getItem()==Items.filled_map) {
                MapData data=(MapData)world.loadItemData(MapData.class,"map_"+stack.getItemDamage());
                if(data!=null && ((LockedMapData)data).etfuturum$isLocked()) { cir.setReturnValue(false);return; }
            }
        }
    }
}
