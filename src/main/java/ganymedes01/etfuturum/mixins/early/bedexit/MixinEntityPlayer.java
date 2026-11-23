package ganymedes01.etfuturum.mixins.early.bedexit;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayer {

	@WrapOperation(method = "wakeUpPlayer", at = @At(value = "INVOKE", remap = false,
			target = "Lnet/minecraft/block/Block;getBedSpawnPosition(Lnet/minecraft/world/IBlockAccess;IIILnet/minecraft/entity/player/EntityPlayer;)Lnet/minecraft/util/ChunkCoordinates;"))
	private ChunkCoordinates etfu$findClearBedExit(Block bed, IBlockAccess access, int x, int y, int z,
			EntityPlayer player, Operation<ChunkCoordinates> original) {
		ChunkCoordinates exit = original.call(bed, access, x, y, z, player);
		World world = player.worldObj;
		AxisAlignedBB bounds = exit == null ? etfu$wakeBounds(player, x, y + 1, z)
				: etfu$wakeBounds(player, exit.posX, exit.posY, exit.posZ);
		if (world.getCollidingBoundingBoxes(player, bounds).isEmpty()) return exit;

		// Try the bed's level and the floor below it before looking for a clear spot above the bed.
		ChunkCoordinates clearExit = etfu$findExitOnLevel(player, x, y, z);
		if (clearExit == null && y > 1) clearExit = etfu$findExitOnLevel(player, x, y - 1, z);
		for (int level = y + 1; clearExit == null && level <= world.getHeight(); level++) {
			clearExit = etfu$findExitOnLevel(player, x, level, z);
		}
		return clearExit == null ? exit : clearExit;
	}

	@Unique
	private static ChunkCoordinates etfu$findExitOnLevel(EntityPlayer player, int x, int y, int z) {
		World world = player.worldObj;
		for (int px = x - 2; px <= x + 2; px++) {
			for (int pz = z - 2; pz <= z + 2; pz++) {
				if (!world.blockExists(px, y - 1, pz) || !World.doesBlockHaveSolidTopSurface(world, px, y - 1, pz)) continue;
				AxisAlignedBB bounds = etfu$wakeBounds(player, px, y, pz);
				if (world.getCollidingBoundingBoxes(player, bounds).isEmpty()
						&& !world.isAnyLiquid(bounds) && !world.func_147470_e(bounds)) {
					return new ChunkCoordinates(px, y, pz);
				}
			}
		}
		return null;
	}

	@Unique
	private static AxisAlignedBB etfu$wakeBounds(EntityPlayer player, int x, int y, int z) {
		// wakeUpPlayer has restored standing size and the normal yOffset at this point.
		double centerX = x + 0.5F;
		double centerZ = z + 0.5F;
		double feetY = (double) (y + player.yOffset + 0.1F) - player.yOffset;
		double halfWidth = player.width / 2.0F;
		// The player position packet clears the step offset after the initial wake-up placement.
		return AxisAlignedBB.getBoundingBox(centerX - halfWidth, feetY + Math.min(0.0F, player.ySize), centerZ - halfWidth,
				centerX + halfWidth, feetY + Math.max(0.0F, player.ySize) + player.height, centerZ + halfWidth);
	}
}
