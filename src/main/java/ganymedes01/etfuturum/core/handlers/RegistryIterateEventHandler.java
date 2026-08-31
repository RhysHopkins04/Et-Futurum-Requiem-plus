package ganymedes01.etfuturum.core.handlers;

import ganymedes01.etfuturum.api.ArmorSoundsRegistry;
import ganymedes01.etfuturum.client.sound.BlockSoundRegisterHelper;
import ganymedes01.etfuturum.configuration.configs.ConfigSounds;
import ganymedes01.etfuturum.recipes.ModTagging;
import net.minecraft.block.Block;
import net.minecraft.item.Item;

/**
 * Performs the registry init pass EFR used to receive from HogUtils events.
 * Running it directly also makes startup ordering explicit and self-contained.
 */
public final class RegistryIterateEventHandler {
    private RegistryIterateEventHandler() {}

    public static void runInitPass() {
        for (Object object : Block.blockRegistry) {
            if (!(object instanceof Block block)) continue;
            String name = String.valueOf(Block.blockRegistry.getNameForObject(block));
            if (ConfigSounds.newBlockSounds) BlockSoundRegisterHelper.registerSoundsDynamic(block, name);
            ModTagging.registerBlockTagsDynamic(block);
        }

        for (Object object : Item.itemRegistry) {
            if (!(object instanceof Item item)) continue;
            String name = String.valueOf(Item.itemRegistry.getNameForObject(item));
            ModTagging.registerItemTagsDynamic(item);
            if (ConfigSounds.armorEquip) ArmorSoundsRegistry.registerDefaults(item, name);
        }
    }
}
