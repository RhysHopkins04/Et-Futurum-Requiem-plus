package ganymedes01.etfuturum.mixins.early.bedexit;

import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S0APacketUseBed;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(S0APacketUseBed.class)
public abstract class MixinS0APacketUseBed {

	@Shadow private int field_149096_c;

	@Inject(method = "readPacketData", at = @At("RETURN"))
	private void etfu$readBedHeight(PacketBuffer data, CallbackInfo ci) {
		// Vanilla sign-extends the byte, placing a sleeper at Y 128 in the void at Y -128.
		field_149096_c &= 0xFF;
		if (data.readableBytes() == 1) {
			field_149096_c |= data.readUnsignedByte() << 8;
		}
	}

	@Inject(method = "writePacketData", at = @At("RETURN"))
	private void etfu$writeUpperBedHeight(PacketBuffer data, CallbackInfo ci) {
		// Keep the vanilla packet for Y 0..255. Upper beds need one additional byte.
		if (field_149096_c > 0xFF) {
			data.writeByte(field_149096_c >>> 8);
		}
	}
}
