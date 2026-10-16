package ganymedes01.etfuturum.blocks.rawore.modded;

import com.google.common.collect.Lists;
import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.Tags;
import ganymedes01.etfuturum.blocks.BaseSubtypesBlock;
import ganymedes01.etfuturum.core.utils.IInitAction;
import ganymedes01.etfuturum.core.utils.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.apache.commons.lang3.StringUtils;
import ganymedes01.etfuturum.core.utils.DummyWorld;

import java.util.List;

public class BlockGeneralModdedRawOre extends BaseSubtypesBlock implements IInitAction {

	public final String[] ores;
	private final float[] hardnesses;
	private final float[] resistances;

	public BlockGeneralModdedRawOre(String... names) {
		super(Material.rock, names);
		ores = new String[names.length];
		for (int i = 0; i < names.length; i++) {
			ores[i] = "block" + StringUtils.capitalize(names[i].replaceFirst("^raw_", "").replace("_block", ""));
		}
		hardnesses = new float[ores.length];
		resistances = new float[ores.length];
		setNames("modded_raw_ore");
		setCreativeTab(null);
		java.util.Arrays.fill(hardnesses, 5F);
		java.util.Arrays.fill(resistances, 6F);
		setHardness(5);
		setResistance(6);
		setHarvestLevel("pickaxe", 1); //TODO: Config for this since some mods use different harvest levels for these materials
	}

	@Override
	public void getSubBlocks(Item item, CreativeTabs tab, List<ItemStack> list) {
		// Hidden legacy save identity.
	}

	@Override
	public String getTextureDomain() {
		return Tags.MOD_ID;
	}

	@Override
	public float getBlockHardness(World world, int x, int y, int z) {
		return hardnesses[Math.min(world.getBlockMetadata(x, y, z), hardnesses.length - 1)];
	}

	@Override
	public float getExplosionResistance(Entity par1Entity, World world, int x, int y, int z, double explosionX, double explosionY, double explosionZ) {
		return resistances[Math.min(world.getBlockMetadata(x, y, z), resistances.length - 1)];
	}

	public static final List<BlockGeneralModdedRawOre> loaded = Lists.newLinkedList();

	@Override
	public void postInitAction() {
		// No active registrations.
	}

	@Override
	public void onLoadAction() {
		// Retired forms do not resolve or register modded materials.
	}
}
