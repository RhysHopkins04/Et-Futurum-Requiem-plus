package ganymedes01.etfuturum.api.mappings;

import net.minecraft.block.Block;
import net.minecraftforge.oredict.OreDictionary;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public class BlockMetaPair extends ObjMetaPair<Block> {
    private static final Map<Block, Map<Integer, BlockMetaPair>> INTERNER = new IdentityHashMap<>();

    public BlockMetaPair(Block block, int meta) {
        super(block, meta);
    }

    public static synchronized BlockMetaPair intern(Block block, int meta) {
        return INTERNER.computeIfAbsent(block, ignored -> new HashMap<>())
            .computeIfAbsent(meta, ignored -> new BlockMetaPair(block, meta));
    }

    public static BlockMetaPair intern(Block block) {
        return intern(block, OreDictionary.WILDCARD_VALUE);
    }

    public static BlockMetaPair of(Block block, int meta) {
        return new BlockMetaPair(block, meta);
    }
}
