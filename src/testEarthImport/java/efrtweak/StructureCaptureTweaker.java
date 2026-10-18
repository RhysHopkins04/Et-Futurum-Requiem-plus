package efrtweak;

import java.io.File;
import java.util.List;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

/** Opt-in development instrumentation; never packaged in the release jar. */
public final class StructureCaptureTweaker implements ITweaker {
    private org.spongepowered.asm.launch.MixinTweaker mixin;
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        mixin=new org.spongepowered.asm.launch.MixinTweaker();
        mixin.acceptOptions(args,gameDir,assetsDir,profile);
        org.spongepowered.asm.mixin.Mixins.addConfiguration("mixins.efr_structure_capture.json");
    }
    public void injectIntoClassLoader(LaunchClassLoader loader){mixin.injectIntoClassLoader(loader);}
    public String getLaunchTarget(){return "net.minecraft.server.MinecraftServer";}
    public String[] getLaunchArguments(){return new String[0];}
}
