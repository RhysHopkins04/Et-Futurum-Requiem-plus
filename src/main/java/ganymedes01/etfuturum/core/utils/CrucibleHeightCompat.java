package ganymedes01.etfuturum.core.utils;

import net.minecraft.entity.player.EntityPlayer;

/** Invoked only by the verified Crucible block-click height branch. No Bukkit linkage. */
public final class CrucibleHeightCompat {
    private CrucibleHeightCompat() { }
    public static int maximumInteractionY(Object player, Object action, int original) {
        if (!(player instanceof EntityPlayer) || !(action instanceof Enum)) return original;
        EntityPlayer entity = (EntityPlayer)player;
        if (!WorldHeightCompat.extended(entity.worldObj)) return original;
        String name = ((Enum<?>)action).name();
        // Bukkit's AIR overload deliberately uses Y=256 to clear its clicked block. Keep that sentinel.
        return "LEFT_CLICK_BLOCK".equals(name) || "RIGHT_CLICK_BLOCK".equals(name)
                ? entity.worldObj.getHeight() - 1 : original;
    }
}
