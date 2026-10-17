package ganymedes01.etfuturum.mixins.early.bookinput.client;

import net.minecraft.client.gui.GuiScreenBook;
import net.minecraft.util.ChatAllowedCharacters;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** lwjgl3ify's committed-text hook must target the title while the signing screen is open. */
@Mixin(GuiScreenBook.class)
public abstract class MixinGuiScreenBook {
    @Shadow private boolean field_146480_s;
    @Shadow @org.spongepowered.asm.mixin.Final private boolean bookIsUnsigned;
    @Shadow private boolean field_146481_r;
    @Shadow private String bookTitle;
    @Shadow protected abstract void updateButtons();
    @Unique private Boolean etfuturum$committedInput;
    @Unique private boolean etfuturum$hasCommittedInput() {
        if(etfuturum$committedInput==null) {
            etfuturum$committedInput=false;
            // Detect the actually applied hook, not merely the presence/version of the optional mod.
            for(java.lang.reflect.Method method:getClass().getMethods())
                if(method.getName().equals("onTextEvent") && method.getParameterTypes().length==1
                    && method.getParameterTypes()[0].getName().equals("me.eigenraven.lwjgl3ify.api.InputEvents$TextEvent"))etfuturum$committedInput=true;
        }
        return etfuturum$committedInput;
    }
    @Inject(method="func_146460_c",at=@At("HEAD"),cancellable=true)
    private void etfuturum$titleKey(char character,int key,CallbackInfo ci) {
        // Retain backspace/enter control keys; printable input arrives once through committed text.
        if(etfuturum$hasCommittedInput() && key!=14 && key!=28 && key!=156 && ChatAllowedCharacters.isAllowedCharacter(character))ci.cancel();
    }
    @Inject(method="func_146459_b",at=@At("HEAD"),cancellable=true)
    private void etfuturum$committedTitle(String text,CallbackInfo ci) {
        if(!bookIsUnsigned) { ci.cancel();return; }
        if(!field_146480_s)return;
        ci.cancel();
        StringBuilder title=new StringBuilder(bookTitle);
        for(int i=0;i<text.length() && title.length()<16;i++)
            if(ChatAllowedCharacters.isAllowedCharacter(text.charAt(i)))title.append(text.charAt(i));
        if(!title.toString().equals(bookTitle)) { bookTitle=title.toString();field_146481_r=true;updateButtons(); }
    }
}
