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
}
