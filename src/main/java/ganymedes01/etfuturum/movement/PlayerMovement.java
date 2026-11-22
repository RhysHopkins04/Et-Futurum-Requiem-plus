package ganymedes01.etfuturum.movement;

import ganymedes01.etfuturum.core.utils.ContainedWater;
import ganymedes01.etfuturum.elytra.IElytraPlayer;
import ganymedes01.etfuturum.network.profile.ClientWorldProfile;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraftforge.fluids.IFluidBlock;

public final class PlayerMovement {
    private PlayerMovement() {}

    public static MovementMode mode(EntityPlayer player) {
        return player.worldObj.isRemote ? ClientWorldProfile.server().movement : MovementMode.local();
    }

    public static MovementPose pose(Entity player) {
        return player instanceof MovementPlayer ? ((MovementPlayer) player).etfu$movementPose() : MovementPose.VANILLA;
    }

    public static void resolve(EntityPlayer player) {
        ((MovementPlayer) player).etfu$resolveMovementPose();
    }

    public static boolean vanillaPoseRequired(EntityPlayer player) {
        return player.isDead || player.getHealth() <= 0 || player.isPlayerSleeping()
                || player.isRiding() || player.capabilities.isFlying
                || player.noClip && (!player.worldObj.isRemote || player.isClientWorld())
                || gliding(player);
    }

    public static boolean gliding(EntityPlayer player) {
        return player instanceof IElytraPlayer && ((IElytraPlayer) player).etfu$isElytraFlying();
    }

    public static MovementPose choose(EntityPlayer player, MovementMode mode, MovementPose previous) {
        if (mode == MovementMode.DISABLED || vanillaPoseRequired(player)) return MovementPose.VANILLA;
        if (player.isSprinting() && player.isInWater()
                && (previous == MovementPose.SWIM || eyesInWater(player, previous.eye))
                && fits(player, MovementPose.SWIM)) {
            return MovementPose.SWIM;
        }
        if ((mode == MovementMode.LEGACY_SNEAK || !player.isSneaking()) && fits(player, MovementPose.VANILLA)) {
            return MovementPose.VANILLA;
        }
        if (fits(player, MovementPose.CROUCH)) return MovementPose.CROUCH;
        if (fits(player, MovementPose.CRAWL)) return MovementPose.CRAWL;
        return previous;
    }

    private static boolean fits(EntityPlayer player, MovementPose pose) {
        AxisAlignedBB box = player.boundingBox.copy();
        box.maxY = box.minY + pose.height;
        // Use block collision shapes, including slabs and stairs. An overlapping entity cannot force a crawl.
        return player.worldObj.func_147461_a(box).isEmpty();
    }

    public static float legacyClientEyeOffset(float offset, EntityPlayer player) {
        return pose(player) != MovementPose.VANILLA && !vanillaPoseRequired(player) ? 0.0F : offset;
    }

    public static boolean eyesInWater(EntityPlayer player, float height) {
        double eyeY = player.boundingBox.minY + height;
        int x = MathHelper.floor_double(player.posX);
        int y = MathHelper.floor_double(eyeY);
        int z = MathHelper.floor_double(player.posZ);
        Block block = player.worldObj.getBlock(x, y, z);
        if (block.getMaterial() == Material.water) {
            if (block instanceof IFluidBlock) {
                float fill = ((IFluidBlock) block).getFilledPercentage(player.worldObj, x, y, z);
                return fill < 0 ? eyeY > y + 1 + fill : eyeY < y + fill;
            }
            if (player.worldObj.getBlock(x, y + 1, z).getMaterial() == Material.water
                    || ContainedWater.at(player.worldObj, player.posX, y + 1.01D, player.posZ)) return true;
            return eyeY < y + 1 - BlockLiquid.getLiquidHeightPercent(player.worldObj.getBlockMetadata(x, y, z));
        }
        return ContainedWater.at(player.worldObj, player.posX, eyeY, player.posZ);
    }
}
