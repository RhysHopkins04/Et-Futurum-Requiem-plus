package ganymedes01.etfuturum.core.utils;

/** Water overlay queries for render views that do not expose their source chunks. */
public interface WaterloggedAccess {
    boolean etfu$isWaterlogged(int x, int y, int z);
}
