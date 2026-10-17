package efrtweak;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;
import java.io.File;
import java.util.List;

/** Give Forge's bootstrap its required LaunchClassLoader, without starting Minecraft. */
public final class RecipeDisplayTweaker implements ITweaker {
    @Override public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {}
    @Override public void injectIntoClassLoader(LaunchClassLoader loader) {}
    @Override public String getLaunchTarget() { return "efrtest.RecipeDisplayContracts"; }
    @Override public String[] getLaunchArguments() { return new String[0]; }
}
