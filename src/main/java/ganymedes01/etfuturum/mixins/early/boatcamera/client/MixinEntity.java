package ganymedes01.etfuturum.mixins.early.boatcamera.client;

import ganymedes01.etfuturum.entities.EntityNewBoat;
import ganymedes01.etfuturum.entities.EntityNewBoatSeat;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class MixinEntity {

	@Inject(method = "setAngles", at = @At("TAIL"))
	private void etfuturum$limitNewBoatCameraYaw(float yaw, float pitch, CallbackInfo ci) {
		if ((Object) this instanceof EntityClientPlayerMP player) {
			if (player.ridingEntity instanceof EntityNewBoat boat) {
				boat.updatePassengerCamera(player);
			} else if (player.ridingEntity instanceof EntityNewBoatSeat seat && seat.getBoat() != null) {
				seat.getBoat().updatePassengerCamera(player);
			}
		}
	}
}
