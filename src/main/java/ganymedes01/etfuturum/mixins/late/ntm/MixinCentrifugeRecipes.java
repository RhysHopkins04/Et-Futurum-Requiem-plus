package ganymedes01.etfuturum.mixins.late.ntm;

import ganymedes01.etfuturum.compat.NtmOreCompat;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Normalise the exact input for both machine and tool callers, retaining native recipes. */
@Pseudo
@Mixin(targets = "com.hbm.inventory.recipes.CentrifugeRecipes", remap = false)
public abstract class MixinCentrifugeRecipes {
    @ModifyVariable(method = "getOutput", at = @At("HEAD"), argsOnly = true, ordinal = 0, remap = false, require = 1)
    private static ItemStack efr$nativeOreInput(ItemStack input) {
        return NtmOreCompat.processingInput(input);
    }
}
