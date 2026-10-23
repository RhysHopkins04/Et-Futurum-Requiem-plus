package ganymedes01.etfuturum.network.profile;

import ganymedes01.etfuturum.world.imported.ImportedBiome;
import ganymedes01.etfuturum.world.imported.ImportedSurfaceBiome;
import net.minecraft.world.biome.BiomeGenBase;

/** Published atomically before Forge completes login; server/integrated-server state stays separate. */
public final class ClientWorldProfile {
    private static volatile ClientWorldProfile active = new ClientWorldProfile(ServerWorldProfile.LEGACY);
    public final ServerWorldProfile profile;
    private final net.minecraft.network.NetworkManager owner;
    private boolean disconnected;
    private final BiomeGenBase[] biomes = new BiomeGenBase[256];

    private ClientWorldProfile(ServerWorldProfile profile) { this(profile, null); }
    private ClientWorldProfile(ServerWorldProfile profile, net.minecraft.network.NetworkManager owner) {
        this.profile = profile;
        this.owner = owner;
        for (ImportedBiome definition : ImportedBiome.values()) {
            Integer id = profile.biomes.get(definition.sourceName());
            if (id != null) biomes[id] = ImportedSurfaceBiome.forConnection(id, definition, profile.imported);
        }
    }
    public static ServerWorldProfile server() { return active.profile; }
    public static void accept(ServerWorldProfile profile) { accept(profile, null); }
    public static synchronized void accept(ServerWorldProfile profile, net.minecraft.network.NetworkManager owner) {
        active = new ClientWorldProfile(profile, owner);
    }
    public static synchronized void clear() { active = new ClientWorldProfile(ServerWorldProfile.LEGACY); }
    public static synchronized void disconnected(net.minecraft.network.NetworkManager owner) {
        if (active.owner == owner) active.disconnected = true;
    }
    /** Called on the game thread after a failed login; active worlds clear when unloaded. */
    public static synchronized void clearDisconnected() { if (active.disconnected) clear(); }
    public static BiomeGenBase biome(int id) { return active.biomes[id & 255]; }
}
