package ganymedes01.etfuturum.mixins.early.commandblockstate;

import ganymedes01.etfuturum.configuration.configs.ConfigBlocksItems;
import ganymedes01.etfuturum.core.utils.IModernCommandBlockState;
import ganymedes01.etfuturum.lib.RenderIDs;
import net.minecraft.block.BlockCommandBlock;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Minimal visual-state extension only; vanilla command execution remains untouched. */
@Mixin(BlockCommandBlock.class)
public abstract class MixinBlockCommandBlock {
    /** Mixin method merge overrides the inherited Block render ID for this concrete block. */
    public int getRenderType() {
        return ConfigBlocksItems.enableModernMapParityBlocks ? RenderIDs.MODERN_MAP_PARITY : 0;
    }

    @Inject(method = "onBlockPlacedBy", at = @At("TAIL"))
    private void etfu$setPlacedFacing(World world, int x, int y, int z, EntityLivingBase placer,
            ItemStack stack, CallbackInfo ci) {
        if (!ConfigBlocksItems.enableModernMapParityBlocks) return;
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof IModernCommandBlockState)) return;
        int facing;
        if (placer.rotationPitch > 60.0F) facing = 1; // up, opposite the player's downward look
        else if (placer.rotationPitch < -60.0F) facing = 0; // down
        else {
            int q = MathHelper.floor_double(placer.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;
            // q: south/west/north/east; block face points away from placer.
            int[] six = {2, 5, 3, 4};
            facing = six[q];
        }
        ((IModernCommandBlockState) tile).etfu$setModernState(facing, false);
    }
}
