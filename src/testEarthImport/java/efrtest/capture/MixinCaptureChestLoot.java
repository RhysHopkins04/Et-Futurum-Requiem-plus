package efrtest.capture;

import java.util.Random;
import net.minecraft.inventory.IInventory;
import net.minecraft.util.WeightedRandomChestContent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observes native loot calls without consuming RNG or changing their results. */
@Mixin(WeightedRandomChestContent.class)
public abstract class MixinCaptureChestLoot {
    @Inject(method="generateChestContents",at=@At("HEAD"))
    private static void before(Random rng,WeightedRandomChestContent[] content,IInventory inventory,int count,CallbackInfo ci){
        efrtest.NtmLootCapture.before(rng,content,inventory,count);
    }
    @Inject(method="generateChestContents",at=@At("RETURN"))
    private static void after(Random rng,WeightedRandomChestContent[] content,IInventory inventory,int count,CallbackInfo ci){
        efrtest.NtmLootCapture.after(inventory);
    }
}
