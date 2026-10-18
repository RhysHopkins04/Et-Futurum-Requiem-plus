package ganymedes01.etfuturum.world.imported;

import com.google.gson.*;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.GameRegistry;
import java.lang.reflect.*;
import java.util.*;
import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;
import net.minecraftforge.oredict.OreDictionary;

/** Optional AE2 import contracts; no generation or mutations of the export world. */
public final class Ae2ImportResources {
    private Ae2ImportResources() {}
    public static String encoded(NBTTagCompound nbt) throws java.io.IOException {
        java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();
        CompressedStreamTools.writeCompressed(nbt,out);
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }
    public static NBTTagCompound nativeChest(WorldServer world,long seed) throws Exception {
        return nativeChest(world,seed,null);
    }
    private static NBTTagCompound nativeChest(WorldServer world,long seed,Map<String,String> cells) throws Exception {
        final Block stone=(Block)Block.blockRegistry.getObject("minecraft:stone");
        final TileEntity chest=(TileEntity)Class.forName("appeng.tile.storage.TileSkyChest").getConstructor().newInstance();
        chest.setWorldObj(world);chest.xCoord=0;chest.yCoord=64;chest.zCoord=0;
        Class<?> iface=Class.forName("appeng.worldgen.meteorite.IMeteoriteWorld");
        Object fake=Proxy.newProxyInstance(iface.getClassLoader(),new Class<?>[]{iface},(proxy,method,args)->{
            switch(method.getName()) {
                case "getWorld": return world;
                case "getBlock": return stone;
                case "getBlockMetadata": return 0;
                case "getTileEntity": return chest;
                case "canBlockSeeTheSky": return false;
                case "minX": case "minZ": case "maxX": case "maxZ": return (Integer)args[0];
                case "setBlock":
                    if(cells!=null)cells.put(args[0]+","+args[1]+","+args[2],Block.blockRegistry.getNameForObject(args[3]));
                    return method.getReturnType()==boolean.class?Boolean.TRUE:null;
                case "done": return null;
                default: throw new UnsupportedOperationException(method.toString());
            }
        });
        Class<?> placer=Class.forName("appeng.worldgen.MeteoritePlacer");
        Object instance=placer.getConstructor(iface,long.class,int.class,int.class,int.class).newInstance(fake,seed,0,64,0);
        Method place=placer.getDeclaredMethod("placeMeteorite",iface,int.class,int.class,int.class);place.setAccessible(true);
        place.invoke(instance,fake,0,64,0);
        NBTTagCompound nbt=new NBTTagCompound();chest.writeToNBT(nbt);return nbt;
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    public static JsonObject describe(WorldServer world) {
        JsonObject out=new JsonObject();out.addProperty("schemaVersion",1);out.addProperty("available",false);
        if(!Loader.isModLoaded("appliedenergistics2"))return out;
        try {
            Class<?> configClass=Class.forName("appeng.core.AEConfig");Object config=configClass.getField("instance").get(null);
            Class<?> feature=Class.forName("appeng.core.features.AEFeature");
            out.addProperty("spawnPresses",(Boolean)configClass.getMethod("isFeatureEnabled",feature).invoke(config,Enum.valueOf((Class)feature,"SpawnPressesInMeteorites")));
            Class<?> type=Class.forName("appeng.api.features.IWorldGen$WorldGenType");
            Class<?> registry=Class.forName("appeng.core.features.registries.WorldGenRegistry");
            out.addProperty("enabled",(Boolean)registry.getMethod("isWorldGenEnabled",type,net.minecraft.world.World.class).invoke(registry.getField("INSTANCE").get(null),Enum.valueOf((Class)type,"Meteorites"),world));
            Gson gson=new Gson();
            for(String key:new String[]{"minMeteoriteDistance","meteoriteSpawnChance"})out.add(key,gson.toJsonTree(configClass.getField(key).get(config)));
            Object json=Class.forName("appeng.core.AEJSONConfig").getField("instance").get(null);
            Method tables=json.getClass().getDeclaredMethod("getTablesForDimension",int.class);tables.setAccessible(true);
            JsonArray all=new JsonArray();
            for(Object table:(Iterable<?>)tables.invoke(json,0)) {
                JsonArray rows=new JsonArray();
                for(Object entry:(Iterable<?>)table) {
                    JsonObject row=gson.toJsonTree(entry).getAsJsonObject();String name=row.get("item").getAsString();
                    ArrayList<ItemStack> variants=new ArrayList<>();boolean ore=name.startsWith("ore#");
                    if(ore) variants.addAll(OreDictionary.getOres(name.substring(4)));
                    else {String[] parts=name.split(":");if(parts.length==2){Item item=GameRegistry.findItem(parts[0],parts[1]);if(item!=null)variants.add(new ItemStack(item,1,row.get("meta_data").getAsInt()));}}
                    boolean fallback=variants.isEmpty();if(fallback)variants.add(new ItemStack(Items.glowstone_dust));
                    row.addProperty("ore",ore);row.addProperty("fallback",fallback);JsonArray stacks=new JsonArray();
                    for(ItemStack variant:variants) {
                        ItemStack copy=variant.copy();copy.stackSize=1;NBTTagCompound nbt=new NBTTagCompound();copy.writeToNBT(nbt);
                        JsonObject v=new JsonObject();v.addProperty("nbt",encoded(nbt));v.addProperty("item",Item.itemRegistry.getNameForObject(copy.getItem()));v.addProperty("itemId",Item.getIdFromItem(copy.getItem()));v.addProperty("maxStack",copy.getMaxStackSize());stacks.add(v);
                    }
                    row.add("variants",stacks);rows.add(row);
                }
                all.add(rows);
            }
            out.add("lootTables",all);
            JsonArray samples=new JsonArray();
            for(long seed:new long[]{1,2,3,42,-1,-431,1234567,987654321}) {
                TreeMap<String,String> cells=new TreeMap<>();
                JsonObject row=new JsonObject();row.addProperty("seed",seed);row.addProperty("chest",encoded(nativeChest(world,seed,cells)));
                StringBuilder canonical=new StringBuilder();for(Map.Entry<String,String> cell:cells.entrySet())canonical.append(cell.getKey()).append(":").append(cell.getValue()).append(";");
                byte[] digest=java.security.MessageDigest.getInstance("SHA-256").digest(canonical.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                StringBuilder hex=new StringBuilder();for(byte b:digest)hex.append(String.format("%02x",b&255));
                row.addProperty("shapeHash",hex.toString());row.addProperty("shapeCount",cells.size());samples.add(row);
            }
            out.add("samples",samples);
            JsonArray gridSamples=new JsonArray();
            Method seedGrid=Class.forName("appeng.util.Platform").getMethod("seedFromGrid",Random.class,long.class,long.class,long.class);
            for(long seed:new long[]{1,-1,1234567})for(int[] grid:new int[][]{{-2,-1},{0,0},{3,-5}}) {
                Random rng=new Random();seedGrid.invoke(null,rng,seed,(long)grid[0],(long)grid[1]);
                rng.nextDouble();int x=grid[0]*707+rng.nextInt(567)+70;int z=grid[1]*707+rng.nextInt(567)+70;
                rng.nextInt(20);long meteorSeed=rng.nextLong();while(meteorSeed==0)meteorSeed=rng.nextLong();
                JsonObject row=new JsonObject();row.addProperty("seed",seed);row.addProperty("gx",grid[0]);row.addProperty("gz",grid[1]);row.addProperty("x",x);row.addProperty("z",z);row.addProperty("meteorSeed",meteorSeed);gridSamples.add(row);
            }
            out.add("gridSamples",gridSamples);out.addProperty("available",true);
        } catch(Exception | LinkageError ex) {out.addProperty("error",ex.toString());}
        return out;
    }
}
