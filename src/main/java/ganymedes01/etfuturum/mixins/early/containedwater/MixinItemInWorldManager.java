package ganymedes01.etfuturum.mixins.early.containedwater;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import ganymedes01.etfuturum.core.handlers.ModernFluidEventHandler;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.management.ItemInWorldManager;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemInWorldManager.class)
public abstract class MixinItemInWorldManager {
    @WrapOperation(method = "activateBlockOrUseItem", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/block/Block;onBlockActivated(Lnet/minecraft/world/World;IIILnet/minecraft/entity/player/EntityPlayer;IFFF)Z"))
    private boolean etfu$useBucketBeforeOpening(Block block, World world, int x, int y, int z, EntityPlayer player,
            int side, float hitX, float hitY, float hitZ, Operation<Boolean> original, @Local PlayerInteractEvent event) {
        return (ModernFluidEventHandler.isItemUseAllowed(event)
                && ModernFluidEventHandler.useOnHost(player, world, player.getHeldItem(), x, y, z, side, hitX, hitY, hitZ))
                || original.call(block, world, x, y, z, player, side, hitX, hitY, hitZ);
    }
}
