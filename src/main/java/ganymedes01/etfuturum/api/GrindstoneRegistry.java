package ganymedes01.etfuturum.api;

import net.minecraft.enchantment.Enchantment;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** Allows 1.7 mods to identify curses, which have no vanilla 1.7 Enchantment API flag. */
public final class GrindstoneRegistry {
    private static final Set<Enchantment> CURSES=Collections.newSetFromMap(new IdentityHashMap<Enchantment,Boolean>());
    private GrindstoneRegistry() {}
    public static void registerCurse(Enchantment curse) { if(curse==null)throw new IllegalArgumentException("null curse");CURSES.add(curse); }
    public static boolean isCurse(Enchantment enchantment) {
        if(enchantment==null)return false;
        if(CURSES.contains(enchantment))return true;
        String name=enchantment.getName();
        return "enchantment.binding_curse".equals(name) || "enchantment.vanishing_curse".equals(name)
            || "enchantment.curse_binding".equals(name) || "enchantment.curse_vanishing".equals(name);
    }
}
