package ganymedes01.etfuturum.api;

/** Saved-map identity owns locking, so every copy of the same map ID shares the state. */
public interface LockedMapData {
    boolean etfuturum$isLocked();
    void etfuturum$setLocked(boolean locked);
}
