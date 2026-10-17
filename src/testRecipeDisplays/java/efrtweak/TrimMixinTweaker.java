package efrtweak;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;
import java.io.File;
import java.util.List;

/** Apply the production trim mixins to actual Forge renderer classes, without opening a window. */
public final class TrimMixinTweaker implements ITweaker {
    private org.spongepowered.asm.launch.MixinTweaker mixin;
    @Override public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        System.setProperty("mixin.env.side", "CLIENT");
        mixin = new org.spongepowered.asm.launch.MixinTweaker();
        mixin.acceptOptions(args, gameDir, assetsDir, profile);
        org.spongepowered.asm.mixin.MixinEnvironment.getDefaultEnvironment().setSide(org.spongepowered.asm.mixin.MixinEnvironment.Side.CLIENT);
        org.spongepowered.asm.mixin.Mixins.addConfiguration("mixins.efr_trim_contracts.json");
        com.llamalad7.mixinextras.MixinExtrasBootstrap.init();
    }
    @Override public void injectIntoClassLoader(LaunchClassLoader loader) { mixin.injectIntoClassLoader(loader); }
    @Override public String getLaunchTarget() { return "efrtest.TrimMixinContracts"; }
    @Override public String[] getLaunchArguments() { return new String[0]; }
}
