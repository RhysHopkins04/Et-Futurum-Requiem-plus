package ganymedes01.etfuturum.compat;

import cpw.mods.ironchest.ChestChangerType;
import cpw.mods.ironchest.IronChest;
import cpw.mods.ironchest.IronChestType;
import cpw.mods.ironchest.ItemChestChanger;
import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.items.ItemBarrelUpgrade;
import ganymedes01.etfuturum.items.ItemShulkerBoxUpgrade;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import ganymedes01.etfuturum.core.utils.RecipeHelper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

public class CompatIronChests {
	private static final Map<String, ItemChestChanger> upgradeItems = new Object2ObjectLinkedOpenHashMap<>();
	private static final Map<String, ChestChangerType> upgradeTypes = new Object2ObjectLinkedOpenHashMap<>();
	private static final Map<ChestChangerType, Pair<IronChestType, IronChestType>> upgradeMappings = new Reference2ObjectOpenHashMap<>();
	private static final Map<String, IronChestType> tiers = new Object2ObjectLinkedOpenHashMap<>();
	private static double renderDistance;
	static {
		// Collects all enabled chest upgrade typee
		renderDistance = getStaticFieldOrDefault(IronChest.class, "TRANSPARENT_RENDER_INSIDE", Boolean.class, true)
				? getStaticFieldOrDefault(IronChest.class, "TRANSPARENT_RENDER_DISTANCE", Double.class, 128D) : 0F;
		for(IronChestType type : IronChestType.values()) {
			if(callNoArgOrDefault(type, "isEnabled", Boolean.class, true)) {
				tiers.put(type.name(), type);
			}
		}
		for(ChestChangerType type : ChestChangerType.values()) {
			IronChestType source = getFieldOrDefault(type, "source", IronChestType.class, null);
			IronChestType target = getFieldOrDefault(type, "target", IronChestType.class, null);
			boolean isEnabled = source != null && target != null
					&& getFieldOrDefault(type, "isAllowed", Boolean.class, true)
					&& tierExists(source.name()) && tierExists(target.name());
			if(isEnabled) {
				upgradeTypes.put(type.name(), type);
				upgradeMappings.put(type, Pair.of(source, target));
			}
		}
	}

	public static boolean upgradeExists(String from, String to) {
		return upgradeTypes.containsKey(from+to);
	}

	public static boolean tierExists(String type) {
		return tiers.containsKey(type);
	}

	public static void init() {
		for(ChestChangerType type : upgradeTypes.values()) {
			ItemChestChanger item = getFieldOrDefault(type, "item", ItemChestChanger.class, null);
			boolean isEnabled = item != null && item.delegate.name() != null;
			if(isEnabled) {
				upgradeItems.put(type.name(), item);
			}
		}
	}

	public static void registerRecipes() {
		if(ModItems.BARREL_UPGRADE.isEnabled()) {
			ItemBarrelUpgrade upgrade = ((ItemBarrelUpgrade) ModItems.BARREL_UPGRADE.get());
			for (int i = 0; i < upgrade.types.length; i++) {
				Item icUpgrade = upgradeItems.get(upgrade.getSource(i) + upgrade.getTarget(i));
				RecipeHelper.addShapedRecipe(ModItems.BARREL_UPGRADE.newItemStack(1, i), "X", 'X', new ItemStack(icUpgrade));
				RecipeHelper.addShapedRecipe(new ItemStack(icUpgrade), "X", 'X', ModItems.BARREL_UPGRADE.newItemStack(1, i));
			}
		}
		if(ModItems.SHULKER_BOX_UPGRADE.isEnabled()) {
			ItemShulkerBoxUpgrade upgrade = ((ItemShulkerBoxUpgrade) ModItems.SHULKER_BOX_UPGRADE.get());
			for (int i = 0; i < upgrade.types.length; i++) {
				Item icUpgrade = upgradeItems.get(upgrade.getSource(i) + upgrade.getTarget(i));
					RecipeHelper.addShapelessRecipe(RecipeHelper.Priority.NORMAL, ModItems.SHULKER_BOX_UPGRADE.newItemStack(1, i), ModItems.SHULKER_SHELL.newItemStack(), new ItemStack(icUpgrade));
					RecipeHelper.addShapedRecipe(RecipeHelper.Priority.NORMAL, new ItemStack(icUpgrade), "X", 'X', ModItems.SHULKER_BOX_UPGRADE.newItemStack(1, i));
			}
		}
	}

	@Nullable
	public static String getUpgradeName(String from, Item item) {
		if(item instanceof ItemChestChanger changer && changer.getType().canUpgrade(tiers.get(from))) {
			Pair<IronChestType, IronChestType> types = upgradeMappings.get(changer.getType());
			if(types.first().name().equals(from.toUpperCase())) {
				return types.second().name();
			}
		}
		return null;
	}

	public static String getNextBarrelUpgrade(String current, ItemStack stack) {
		if(ModItems.BARREL_UPGRADE.isEnabled()) {
			if(stack.getItem() instanceof ItemBarrelUpgrade upgrade && upgrade.getSource(stack.getItemDamage()).equals(current)
					&& upgradeTypes.containsKey(current+upgrade.getTarget(stack.getItemDamage()))) {
				return upgrade.getTarget(stack.getItemDamage());
			}
			return null;
		}
		return CompatIronChests.getUpgradeName(current, stack.getItem());
	}

	public static String getNextShulkerUpgrade(String current, ItemStack stack) {
		if(ModItems.SHULKER_BOX_UPGRADE.isEnabled()) {
			if(stack.getItem() instanceof ItemShulkerBoxUpgrade upgrade && upgrade.getSource(stack.getItemDamage()).equals(current)
					&& upgradeTypes.containsKey(current+upgrade.getTarget(stack.getItemDamage()))) {
				return upgrade.getTarget(stack.getItemDamage());
			}
			return null;
		}
		return CompatIronChests.getUpgradeName(current.replace("VANILLA", "WOOD"), stack.getItem());
	}

	public static boolean enableCrystalRendering() {
		return crystalRenderDistance() > 0;
	}

	public static double crystalRenderDistance() {
		return renderDistance;
	}

	private static Field findField(Class<?> owner, String name) throws NoSuchFieldException {
		Class<?> current = owner;
		while(current != null) {
			try {
				Field field = current.getDeclaredField(name);
				field.setAccessible(true);
				return field;
			} catch(NoSuchFieldException ignored) {
				current = current.getSuperclass();
			}
		}
		throw new NoSuchFieldException(name);
	}

	private static Method findNoArgMethod(Class<?> owner, String name) throws NoSuchMethodException {
		Class<?> current = owner;
		while(current != null) {
			try {
				Method method = current.getDeclaredMethod(name);
				method.setAccessible(true);
				return method;
			} catch(NoSuchMethodException ignored) {
				current = current.getSuperclass();
			}
		}
		throw new NoSuchMethodException(name);
	}

	private static <T> T getStaticFieldOrDefault(Class<?> owner, String name, Class<T> cast, T def) {
		try {
			Object value = findField(owner, name).get(null);
			return value == null ? def : cast.cast(value);
		} catch(Exception e) {
			return def;
		}
	}

	private static <T> T getFieldOrDefault(Object target, String name, Class<T> cast, T def) {
		try {
			Object value = findField(target.getClass(), name).get(target);
			return value == null ? def : cast.cast(value);
		} catch(Exception e) {
			return def;
		}
	}

	private static <T> T callNoArgOrDefault(Object target, String name, Class<T> cast, T def) {
		try {
			Object value = findNoArgMethod(target.getClass(), name).invoke(target);
			return value == null ? def : cast.cast(value);
		} catch(Exception e) {
			return def;
		}
	}
}
