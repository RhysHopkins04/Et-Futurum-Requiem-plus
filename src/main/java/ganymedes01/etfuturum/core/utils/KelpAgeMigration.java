package ganymedes01.etfuturum.core.utils;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

/** Rewrites old head state before Anvil creates any tile entities. Idempotent, chunk-local. */
public final class KelpAgeMigration {
    private KelpAgeMigration() { }

    public static boolean migrate(NBTTagCompound level, int lowId, int highId) {
        if (lowId <= 0 || highId <= 0 || lowId == highId) return false;
        NBTTagList tiles = level.getTagList("TileEntities", 10);
        NBTTagList sections = level.getTagList("Sections", 10);
        boolean changed = false;
        for (int t = tiles.tagCount() - 1; t >= 0; t--) {
            NBTTagCompound tile = tiles.getCompoundTagAt(t);
            if (!"etfuturum:modern_parity_kelp_state".equals(tile.getString("id"))) continue;
            int x = tile.getInteger("x"), y = tile.getInteger("y"), z = tile.getInteger("z");
            if (y < 0 || y > 255 || (x >> 4) != level.getInteger("xPos") || (z >> 4) != level.getInteger("zPos")) continue;
            for (int n = 0; n < sections.tagCount(); n++) {
                NBTTagCompound section = sections.getCompoundTagAt(n);
                if ((section.getByte("Y") & 255) != y >> 4) continue;
                byte[] blocks = section.getByteArray("Blocks"), add = section.getByteArray("Add"), data = section.getByteArray("Data");
                if (blocks.length != 4096 || data.length != 2048 || (add.length != 0 && add.length != 2048)) break;
                int index = ((y & 15) << 8) | ((z & 15) << 4) | (x & 15);
                int id = (blocks[index] & 255) | (add.length == 0 ? 0 : nibble(add, index) << 8);
                if (id != lowId) break; // Never alter unrelated blocks or stale mismatched records.
                int age = Math.max(0, Math.min(25, tile.getByte("Age") & 255));
                int target = age < 16 ? lowId : highId;
                blocks[index] = (byte)target;
                if (add.length == 0 && target > 255) add = new byte[2048];
                if (add.length != 0) { putNibble(add, index, target >> 8); section.setByteArray("Add", add); }
                putNibble(data, index, age & 15);
                tiles.removeTag(t);
                changed = true;
                break;
            }
        }
        return changed;
    }

    private static int nibble(byte[] a, int i) { return (a[i >> 1] >> ((i & 1) * 4)) & 15; }
    private static void putNibble(byte[] a, int i, int value) {
        int shift = (i & 1) * 4;
        a[i >> 1] = (byte)((a[i >> 1] & ~(15 << shift)) | ((value & 15) << shift));
    }
}
