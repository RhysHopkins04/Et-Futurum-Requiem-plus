package ganymedes01.etfuturum.mixins.early.cartography;

import ganymedes01.etfuturum.api.LockedMapData;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemMap.class)
public abstract class MixinItemMap {
    @Inject(method="updateMapData",at=@At("HEAD"),cancellable=true)
    private void etfuturum$lockedPixels(World world,Entity entity,MapData data,CallbackInfo ci) {
        if(((LockedMapData)data).etfuturum$isLocked())ci.cancel();
    }
    @Inject(method="onCreated",at=@At("HEAD"),cancellable=true)
    private void etfuturum$lockedScaling(ItemStack stack,World world,EntityPlayer player,CallbackInfo ci) {
        if(stack.hasTagCompound() && stack.getTagCompound().getBoolean("map_is_scaling")) {
            MapData data=(MapData)world.loadItemData(MapData.class,"map_"+stack.getItemDamage());
            if(data!=null && ((LockedMapData)data).etfuturum$isLocked()) { stack.getTagCompound().removeTag("map_is_scaling");ci.cancel(); }
        }
    }
}
