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
    private static final int VERSION = 1;
    public static final ServerWorldProfile LEGACY = new ServerWorldProfile(false, false, Collections.emptyMap());
    public final boolean extended, imported;
    public final Map<String, Integer> biomes;

    public ServerWorldProfile(boolean extended, boolean imported, Map<String, Integer> biomes) {
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
                ConfigMapCompatibility.importedEarthMode, ids);
    }

    public void write(ByteBuf buffer) {
        buffer.writeInt(MAGIC).writeByte(VERSION).writeByte((extended ? 1 : 0) | (imported ? 2 : 0));
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
            if (buffer.readInt() != MAGIC || buffer.readUnsignedByte() != VERSION)
                throw new IllegalArgumentException("Unsupported EFR world-profile protocol; update Et Futurum Plus on both sides");
            int flags = buffer.readUnsignedByte(), count = buffer.readUnsignedByte();
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
            return new ServerWorldProfile((flags & 1) != 0, (flags & 2) != 0, ids);
        } catch (IndexOutOfBoundsException truncated) {
            throw new IllegalArgumentException("Truncated EFR world profile", truncated);
        }
    }

    @Override public boolean equals(Object other) {
        if (!(other instanceof ServerWorldProfile)) return false;
        ServerWorldProfile p = (ServerWorldProfile) other;
        return extended == p.extended && imported == p.imported && biomes.equals(p.biomes);
    }
    @Override public int hashCode() { return Objects.hash(extended, imported, biomes); }
}
