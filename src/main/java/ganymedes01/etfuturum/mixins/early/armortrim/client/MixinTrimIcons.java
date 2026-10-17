package ganymedes01.etfuturum.mixins.early.armortrim.client;

import ganymedes01.etfuturum.client.renderer.trim.ArmorTrimRenderer;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderItem.class)
public abstract class MixinTrimIcons {
    @Shadow public float zLevel;
    // Forge's normal inventory path invokes this boolean overload directly. The
    // five-argument wrapper delegates here, so both paths receive exactly one overlay.
    @Inject(method="renderItemIntoGUI(Lnet/minecraft/client/gui/FontRenderer;Lnet/minecraft/client/renderer/texture/TextureManager;Lnet/minecraft/item/ItemStack;IIZ)V",at=@At("RETURN"),remap=false)
    private void etfuturum$trim(FontRenderer font,TextureManager textures,ItemStack stack,int x,int y,boolean renderEffect,CallbackInfo ci) { if(stack!=null)ArmorTrimRenderer.renderIcon(stack,x,y,zLevel); }
}
