package ganymedes01.etfuturum.mixins.late.ntm;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import ganymedes01.etfuturum.compat.NtmOreCompat;
import net.minecraft.block.Block;
import net.minecraftforge.event.world.BlockEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/** Let the native dust handler recognise the corresponding deepslate sources. */
@Pseudo
@Mixin(targets="com.hbm.main.ModEventHandler",remap=false)
public abstract class MixinCoalDustBreakEffects {
    @WrapOperation(method="onBlockBreak",at=@At(value="FIELD",target="Lnet/minecraftforge/event/world/BlockEvent$BreakEvent;block:Lnet/minecraft/block/Block;"),remap=false,require=1)
    private Block efr$breakEffectSource(BlockEvent.BreakEvent event,Operation<Block> original) {
        return NtmOreCompat.breakEffectSource(original.call(event),event.blockMetadata);
    }
}
