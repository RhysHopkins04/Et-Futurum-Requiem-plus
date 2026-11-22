package ganymedes01.etfuturum.movement;

public enum MovementPose {
    VANILLA(0, 1.8F, 1.62F),
    CROUCH(1, 1.5F, 1.27F),
    CRAWL(2, 0.6F, 0.4F),
    SWIM(3, 0.6F, 0.4F);

    public final byte wireId;
    public final float height;
    public final float eye;

    MovementPose(int wireId, float height, float eye) {
        this.wireId = (byte) wireId;
        this.height = height;
        this.eye = eye;
    }

    public boolean horizontal() {
        return this == CRAWL || this == SWIM;
    }

    public static MovementPose fromWire(byte id) {
        switch (id) {
            case 1: return CROUCH;
            case 2: return CRAWL;
            case 3: return SWIM;
            default: return VANILLA;
        }
    }
}
