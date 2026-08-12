package ganymedes01.etfuturum.mixins.early.extendedheight;

import ganymedes01.etfuturum.core.utils.WorldHeightCompat;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Removes ItemBlock's legacy solid-block stop at physical Y=255 for the extended Overworld.
 *
 * Vanilla performs this check after resolving the clicked face, so without this hook normal block
 * placement can never cross the old section-15 ceiling even though World/Chunk storage accepts
 * physical Y=256..383. Non-Overworld dimensions retain the original Y=255 guard.
 */
@Mixin(ItemBlock.class)
public class MixinItemBlock {

    @ModifyConstant(method = "onItemUse", constant = @Constant(intValue = WorldHeightCompat.LEGACY_MAX_Y))
    private int etfu$moveLegacySolidPlacementGuard(
            int original, ItemStack stack, EntityPlayer player, World world,
            int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        return world.provider != null && world.provider.dimensionId == 0
                ? WorldHeightCompat.EXTENDED_HEIGHT
                : original;
    }
}
