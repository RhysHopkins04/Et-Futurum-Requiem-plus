package ganymedes01.etfuturum.world.imported;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import cpw.mods.fml.common.Loader;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/** Optional native resource contracts. Reflection keeps NTM an optional dependency. */
public final class NtmImportResources {
    private NtmImportResources() {}

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static JsonObject describe(WorldServer world) {
        JsonObject out = new JsonObject();
        out.addProperty("schemaVersion", 1);
        out.addProperty("available", false);
        if (!Loader.isModLoaded("hbm")) return out;
        try {
            Class<?> fluids = Class.forName("com.hbm.inventory.fluid.Fluids");
            JsonObject ids = new JsonObject();
            for (String name : new String[]{"NONE", "WATER", "SULFURIC_ACID", "SOLVENT"}) {
                Object fluid = fluids.getField(name).get(null);
                ids.addProperty(name, (Number)fluid.getClass().getMethod("getID").invoke(fluid));
            }
            out.add("fluidIds", ids);
            Class<?> bedrock = Class.forName("com.hbm.world.feature.BedrockOre");
            Class<?> tileClass = Class.forName("com.hbm.blocks.generic.BlockBedrockOreTE$TileEntityBedrockOre");
            Item item = (Item)Class.forName("com.hbm.items.ModItems").getField("bedrock_ore_base").get(null);
            out.addProperty("resourceItem", Item.itemRegistry.getNameForObject(item));
            out.addProperty("resourceItemId", Item.getIdFromItem(item));
            JsonArray templates = new JsonArray();
            for (int tier = 1; tier <= 4; tier++) {
                TileEntity tile = (TileEntity)tileClass.getConstructor().newInstance();
                tileClass.getField("resource").set(tile, new ItemStack(item));
                tileClass.getField("acidRequirement").set(tile, bedrock.getField("BORE_TIER_" + tier).get(null));
                tileClass.getField("tier").setInt(tile, tier);
                tileClass.getField("color").setInt(tile, 0xD78A16);
                tileClass.getField("shape").setInt(tile, 0);
                NBTTagCompound nbt = new NBTTagCompound(); tile.writeToNBT(nbt);
                JsonObject row = new JsonObject(); row.addProperty("id", nbt.getString("id"));
                for (String key : new String[]{"0id", "fluid", "amount", "tier", "color", "shape"})
                    if (nbt.hasKey(key)) row.addProperty(key, nbt.getInteger(key));
                row.addProperty("size", nbt.getByte("size")); row.addProperty("meta", nbt.getShort("meta"));
                templates.add(row);
            }
            out.add("nodeTemplates", templates);
            Class<?> bodyClass = Class.forName("com.hbm.dim.SolarSystem$Body");
            Object earth = Enum.valueOf((Class)bodyClass, "KERBIN");
            Class<?> ores = Class.forName("com.hbm.items.special.ItemBedrockOreNew$CelestialBedrockOre");
            Object registry = ores.getMethod("get", bodyClass).invoke(null, earth);
            Object[] types = (Object[])ores.getField("types").get(registry);
            Class<?> base = Class.forName("com.hbm.items.special.ItemBedrockOreBase");
            JsonArray indices = new JsonArray();
            for (Object type : types) {
                JsonObject row = new JsonObject();
                row.addProperty("index", type.getClass().getField("index").getInt(type));
                row.addProperty("suffix", (String)type.getClass().getField("suffix").get(type));
                indices.add(row);
            }
            out.add("earthBedrockTypes", indices);
            JsonArray samples = new JsonArray();
            for (int x : new int[]{-10000, -3000, -10, 0, 1974, 10000}) {
                int z = x / 2 - 137;
                JsonObject row = new JsonObject(); row.addProperty("x", x); row.addProperty("z", z);
                JsonArray levels = new JsonArray(); double total = 0;
                for (Object type : types) {
                    double level = ((Number)base.getMethod("getOreLevel", World.class, int.class, int.class, type.getClass())
                        .invoke(null, world, x, z, type)).doubleValue();
                    levels.add(new com.google.gson.JsonPrimitive(level)); total += level;
                }
                row.add("levels", levels); row.addProperty("tier", (Number)bedrock.getMethod("getTier", double.class).invoke(null, total / types.length));
                samples.add(row);
            }
            out.addProperty("worldSeed", world.getSeed()); out.add("densitySamples", samples);
            JsonObject flora = new JsonObject();
            flora.addProperty("schemaVersion", 1);
            Class<?> tomClass = Class.forName("com.hbm.saveddata.TomSaveData");
            net.minecraft.world.WorldSavedData tom = world.perWorldStorage.loadData((Class)tomClass, "impactData");
            flora.addProperty("impacted", tom != null && tomClass.getField("impact").getBoolean(tom));
            Class<?> flowers = Class.forName("com.hbm.blocks.generic.BlockNTMFlower$EnumFlowerType");
            JsonObject metadata = new JsonObject();
            for (Object value : flowers.getEnumConstants()) {
                Enum<?> entry = (Enum<?>)value;
                metadata.addProperty(entry.name(), entry.ordinal());
            }
            flora.add("flowerMetadata", metadata);
            JsonObject names = new JsonObject();
            Class<?> blocks = Class.forName("com.hbm.blocks.ModBlocks");
            for (String field : new String[]{"plant_flower", "reeds", "dirt_oily"})
                names.addProperty(field, net.minecraft.block.Block.blockRegistry.getNameForObject(blocks.getField(field).get(null)));
            flora.add("blockNames", names);
            out.add("floraContracts", flora);
            out.addProperty("available", true);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ex) {
            out.addProperty("error", ex.toString());
        }
        return out;
    }
}
