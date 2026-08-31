package ganymedes01.etfuturum.core.utils;

import ganymedes01.etfuturum.compat.ModsList;
import net.minecraft.item.ItemDye;
import net.minecraftforge.oredict.OreDictionary;

import java.util.Arrays;

/** EFR-local utility subset formerly supplied by HogUtils. */
public final class GenericUtils {
    private static Integer maxMeta;
    private static Integer minMeta;

    private GenericUtils() {}

    public static int getMaxBlockMetadata() {
        if (maxMeta == null) {
            if (ModsList.NOT_ENOUGH_IDS.isLoaded() && ModsList.NOT_ENOUGH_IDS.isVersionNewerOrEqual("2.0.0")) maxMeta = (int)Short.MAX_VALUE;
            else if (ModsList.ENDLESS_IDS_BLOCKITEM.isLoaded()) maxMeta = 65536;
            else maxMeta = 15;
        }
        return maxMeta;
    }

    public static int getMinBlockMetadata() {
        if (minMeta == null) {
            minMeta = ModsList.NOT_ENOUGH_IDS.isLoaded() && ModsList.NOT_ENOUGH_IDS.isVersionNewerOrEqual("2.0.0")
                ? (int)Short.MIN_VALUE : 0;
        }
        return minMeta;
    }

    public static boolean isBlockMetaInBounds(int meta) {
        return meta >= getMinBlockMetadata() && meta <= getMaxBlockMetadata();
    }

    public static boolean isBlockMetaInBoundsIgnoreWildcard(int meta) {
        return meta == OreDictionary.WILDCARD_VALUE || isBlockMetaInBounds(meta);
    }

    public static final class Constants {
        public static final String[] MODERN_COLORS_SNAKE_CASE;
        static {
            MODERN_COLORS_SNAKE_CASE = Arrays.copyOf(ItemDye.field_150921_b, 16);
            MODERN_COLORS_SNAKE_CASE[7] = "light_gray";
        }
        private Constants() {}
    }
}
