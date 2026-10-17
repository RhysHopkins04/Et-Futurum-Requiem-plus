package ganymedes01.etfuturum.mixins.early.cartography;

import ganymedes01.etfuturum.api.LockedMapData;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.storage.MapData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MapData.class)
public abstract class MixinMapData implements LockedMapData {
    @Unique private boolean etfuturum$locked;
    @Override public boolean etfuturum$isLocked() { return etfuturum$locked; }
    @Override public void etfuturum$setLocked(boolean locked) { etfuturum$locked=locked;((MapData)(Object)this).markDirty(); }
    @Inject(method="readFromNBT",at=@At("TAIL"))
    private void etfuturum$read(NBTTagCompound tag,CallbackInfo ci) { etfuturum$locked=tag.getBoolean("etfuturum:locked"); }
    @Inject(method="writeToNBT",at=@At("TAIL"))
    private void etfuturum$write(NBTTagCompound tag,CallbackInfo ci) { tag.setBoolean("etfuturum:locked",etfuturum$locked); }
}
