package ganymedes01.etfuturum.core.utils;

/**
 * Central vertical-coordinate contract for the Et Futurum Requiem Plus extended-height prototype.
 *
 * Minecraft 1.7.10 remains positive-Y internally. Modern Overworld coordinates are represented by
 * shifting them upward by 64 blocks:
 *
 * modern -64 -> physical 0
 * modern   0 -> physical 64
 * modern  63 -> physical 127
 * modern 319 -> physical 383
 *
 * Keeping physical coordinates non-negative avoids introducing negative chunk-section indices into
 * the 1.7.10 engine while still giving map conversion and future modern terrain generation a stable
 * one-to-one vertical mapping.
 */
public final class WorldHeightCompat {

    public static final int LEGACY_HEIGHT = 256;
    public static final int LEGACY_MAX_Y = LEGACY_HEIGHT - 1;
    public static final int LEGACY_SECTION_COUNT = LEGACY_HEIGHT / 16;

    public static final int EXTENDED_HEIGHT = 384;
    public static final int EXTENDED_MAX_Y = EXTENDED_HEIGHT - 1;
    public static final int EXTENDED_SECTION_COUNT = EXTENDED_HEIGHT / 16;

    public static final int MODERN_Y_OFFSET = 64;

    // Logical modern Overworld coordinate contract (1.18+ style vertical range).
    public static final int MODERN_MIN_Y = -64;
    public static final int MODERN_MAX_Y = 319;
    public static final int MODERN_HEIGHT = MODERN_MAX_Y - MODERN_MIN_Y + 1;
    public static final int MODERN_SEA_LEVEL = 63;
    public static final int MODERN_CLOUD_HEIGHT = 192;
    public static final int MODERN_AVERAGE_GROUND_LEVEL = 64;

    // Positive physical coordinates used by the 1.7.10 engine.
    public static final int PHYSICAL_MIN_Y = 0;
    public static final int PHYSICAL_ZERO_Y = MODERN_Y_OFFSET;
    public static final int PHYSICAL_SEA_LEVEL = MODERN_SEA_LEVEL + MODERN_Y_OFFSET;
    public static final int PHYSICAL_CLOUD_HEIGHT = MODERN_CLOUD_HEIGHT + MODERN_Y_OFFSET;
    public static final int PHYSICAL_AVERAGE_GROUND_LEVEL = MODERN_AVERAGE_GROUND_LEVEL + MODERN_Y_OFFSET;

    public static final int FULL_SECTION_MASK = (1 << EXTENDED_SECTION_COUNT) - 1;

    /**
     * Upper bound for the uncompressed S21/S26 chunk payload when all 24 sections use Add/MSB data.
     * This deliberately has headroom above the exact theoretical payload size.
     */
    public static final int MAX_CHUNK_DATA_BYTES = 393728;

    private WorldHeightCompat() {}

    public static int modernToPhysicalY(int modernY) {
        return modernY + MODERN_Y_OFFSET;
    }

    public static int physicalToModernY(int physicalY) {
        return physicalY - MODERN_Y_OFFSET;
    }

    public static boolean isModernYInRange(int modernY) {
        return modernY >= MODERN_MIN_Y && modernY <= MODERN_MAX_Y;
    }

    public static boolean isPhysicalYInRange(int physicalY) {
        return physicalY >= PHYSICAL_MIN_Y && physicalY <= EXTENDED_MAX_Y;
    }
}
