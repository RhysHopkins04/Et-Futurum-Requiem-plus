package ganymedes01.etfuturum.core.utils;

/**
 * Explicit modern single/double-chest pairing state mixed into vanilla TileEntityChest.
 *
 * <p>The stored direction points from this chest to its partner. UNKNOWN exists only to migrate
 * pre-Pass-32c worlds which did not yet persist an explicit pair direction.</p>
 */
public interface IChestPairingState {
    byte UNKNOWN = -1;
    byte NONE = 0;
    byte WEST = 1;
    byte EAST = 2;
    byte NORTH = 3;
    byte SOUTH = 4;

    byte etfu$getPairDirection();

    void etfu$setPairDirection(byte direction);

    void etfu$resolvePairing();
}
