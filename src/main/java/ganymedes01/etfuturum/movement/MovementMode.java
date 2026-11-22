package ganymedes01.etfuturum.movement;

import ganymedes01.etfuturum.configuration.configs.ConfigMixins;

public enum MovementMode {
    DISABLED(0),
    MODERN(1),
    LEGACY_SNEAK(2);

    public final int wireId;

    MovementMode(int wireId) {
        this.wireId = wireId;
    }

    public static MovementMode local() {
        if (!ConfigMixins.modernPlayerMovement) return DISABLED;
        return ConfigMixins.legacySneakInModernMovement ? LEGACY_SNEAK : MODERN;
    }

    public static MovementMode fromWire(int id) {
        switch (id) {
            case 0: return DISABLED;
            case 1: return MODERN;
            case 2: return LEGACY_SNEAK;
            default: throw new IllegalArgumentException("Unsupported EFR movement mode: " + id);
        }
    }
}
