package ganymedes01.etfuturum.network.profile;

import ganymedes01.etfuturum.configuration.configs.ConfigMapCompatibility;
import ganymedes01.etfuturum.configuration.configs.ConfigWorld;
import ganymedes01.etfuturum.world.imported.ImportedBiome;
import ganymedes01.etfuturum.world.imported.ImportedBiomes;
import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Bounded, versioned login contract. Never reads or writes a client's configuration. */
public final class ServerWorldProfile {
    private static final int MAGIC = 0x45465250; // EFRP
    private static final int VERSION = 2;
    public static final ServerWorldProfile LEGACY = new ServerWorldProfile(false, false, Collections.emptyMap());
    public final boolean extended, imported;
    public final int height;
    public final int packetVersion;
    public final Map<String, Integer> biomes;

    public ServerWorldProfile(boolean extended, boolean imported, Map<String, Integer> biomes) {
        this(extended, imported, extended ? 384 : 256, biomes);
    }

    public ServerWorldProfile(boolean extended, boolean imported, int height, Map<String, Integer> biomes) {
        this(extended, imported, height, biomes, extended ? VERSION : 1);
    }

    private ServerWorldProfile(boolean extended, boolean imported, int height, Map<String, Integer> biomes, int packetVersion) {
        this.packetVersion = packetVersion;
        if ((!extended && (height != 256 || packetVersion != 1))
                || (extended && (height < 384 || height > 512 || (height & 15) != 0)))
            throw new IllegalArgumentException("Invalid EFR world height (384..512 in 16-block sections)");
        this.height = height;
        if (imported && !extended) throw new IllegalArgumentException("Earth requires the 384-block engine");
        if (biomes.size() > ImportedBiome.values().length) throw new IllegalArgumentException("Too many imported biomes");
        Set<Integer> ids = new HashSet<>();
        Set<String> names = new HashSet<>();
        for (ImportedBiome biome : ImportedBiome.values()) names.add(biome.sourceName());
        for (Map.Entry<String, Integer> entry : biomes.entrySet()) {
            Integer id = entry.getValue();
            if (!names.contains(entry.getKey()) || id == null || id < 0 || id >= 255 || !ids.add(id))
                throw new IllegalArgumentException("Unknown imported biome or invalid/reused biome ID");
        }
        this.extended = extended;
        this.imported = imported;
        this.biomes = Collections.unmodifiableMap(new LinkedHashMap<>(biomes));
    }

    public static ServerWorldProfile local() {
        Map<String, Integer> ids = new LinkedHashMap<>();
        ImportedBiomes.registered().forEach((name, biome) -> ids.put(name, biome.biomeID));
        return new ServerWorldProfile(ConfigWorld.extendedWorldHeight && ConfigMapCompatibility.allowsExtendedHeight(),
                ConfigMapCompatibility.importedEarthMode,
                ConfigWorld.extendedWorldHeight && ConfigMapCompatibility.allowsExtendedHeight() ? ConfigWorld.extendedWorldHeightBlocks : 256, ids);
    }

    public void write(ByteBuf buffer) {
        buffer.writeInt(MAGIC).writeByte(packetVersion).writeByte((extended ? 1 : 0) | (imported ? 2 : 0));
        if (packetVersion == 2) buffer.writeShort(height);
        buffer.writeByte(biomes.size());
        biomes.forEach((name, id) -> {
            byte[] utf8 = name.getBytes(StandardCharsets.UTF_8);
            buffer.writeByte(utf8.length).writeBytes(utf8).writeByte(id);
        });
    }

    /** No extension means a legacy peer. A present but malformed extension is never downgraded. */
    public static ServerWorldProfile read(ByteBuf buffer) {
        if (!buffer.isReadable()) return LEGACY;
        try {
            if (buffer.readInt() != MAGIC)
                throw new IllegalArgumentException("Invalid EFR world-profile magic");
            int version = buffer.readUnsignedByte();
            if (version != 1 && version != VERSION)
                throw new IllegalArgumentException("Unsupported EFR world-profile protocol; update Et Futurum Plus on both sides");
            int flags = buffer.readUnsignedByte();
            int height = version == 2 ? buffer.readUnsignedShort() : ((flags & 1) != 0 ? 384 : 256);
            int count = buffer.readUnsignedByte();
            if ((flags & ~3) != 0 || count > ImportedBiome.values().length)
                throw new IllegalArgumentException("Invalid EFR world profile");
            Map<String, Integer> ids = new LinkedHashMap<>();
            for (int i = 0; i < count; i++) {
                int length = buffer.readUnsignedByte();
                if (length < 1 || length > 64) throw new IllegalArgumentException("Invalid imported biome name length");
                String name = buffer.toString(buffer.readerIndex(), length, StandardCharsets.UTF_8);
                buffer.skipBytes(length);
                if (ids.put(name, (int)buffer.readUnsignedByte()) != null)
                    throw new IllegalArgumentException("Duplicate imported biome name");
            }
            if (buffer.isReadable()) throw new IllegalArgumentException("Trailing EFR profile data");
            return new ServerWorldProfile((flags & 1) != 0, (flags & 2) != 0, height, ids, version);
        } catch (IndexOutOfBoundsException truncated) {
            throw new IllegalArgumentException("Truncated EFR world profile", truncated);
        }
    }

    @Override public boolean equals(Object other) {
        if (!(other instanceof ServerWorldProfile)) return false;
        ServerWorldProfile p = (ServerWorldProfile) other;
        return extended == p.extended && imported == p.imported && height == p.height && packetVersion == p.packetVersion && biomes.equals(p.biomes);
    }
    @Override public int hashCode() { return Objects.hash(extended, imported, height, packetVersion, biomes); }
}
