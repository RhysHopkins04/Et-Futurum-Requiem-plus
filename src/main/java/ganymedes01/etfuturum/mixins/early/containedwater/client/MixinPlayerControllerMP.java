package ganymedes01.etfuturum.mixins.early.containedwater.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import ganymedes01.etfuturum.core.handlers.ModernFluidEventHandler;
import net.minecraft.block.Block;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerControllerMP.class)
public abstract class MixinPlayerControllerMP {
    @WrapOperation(method = "onPlayerRightClick", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/block/Block;onBlockActivated(Lnet/minecraft/world/World;IIILnet/minecraft/entity/player/EntityPlayer;IFFF)Z"))
    private boolean etfu$predictBucketUse(Block block, World world, int x, int y, int z, EntityPlayer player,
            int side, float hitX, float hitY, float hitZ, Operation<Boolean> original) {
        return ModernFluidEventHandler.canUseOnHost(player, world, player.getHeldItem(), x, y, z)
                || original.call(block, world, x, y, z, player, side, hitX, hitY, hitZ);
    }
}
