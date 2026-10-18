package efrtest;

import com.google.gson.JsonObject;
import cpw.mods.fml.common.Loader;
import ganymedes01.etfuturum.world.imported.NtmImportResources;
import net.minecraft.block.Block;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;

public final class NtmResourcesProbe {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void run(WorldServer world, boolean restart) throws Exception {
        if (!Loader.isModLoaded("hbm")) return;
        restart = restart && new java.io.File("ntm-resource-fixture-created").exists();
        JsonObject info = NtmImportResources.describe(world);
        check(info.get("available").getAsBoolean(), "NTM native export failed: " + info);
        Class<?> modItems = Class.forName("com.hbm.items.ModItems");
        Item resource = (Item)modItems.getField("bedrock_ore_base").get(null);
        Block node = (Block)Block.blockRegistry.getObject("hbm:tile.ore_bedrock");
        for (int tier = 1; tier <= 4; tier++) {
            int x = -176 + tier, z = -176;
            JsonObject row = info.getAsJsonArray("nodeTemplates").get(tier-1).getAsJsonObject();
            if (restart) {
                TileEntity saved = world.getTileEntity(x, 0, z);
                check(saved != null, "bedrock node missing on restart");
                NBTTagCompound data = new NBTTagCompound(); saved.writeToNBT(data);
                check(data.getInteger("tier") == tier && data.getInteger("0id") == Item.getIdFromItem(resource), "bedrock node payload changed on restart");
            }
            NBTTagCompound nbt = new NBTTagCompound();
            nbt.setString("id", row.get("id").getAsString());
            for (String key : new String[]{"0id", "fluid", "amount", "tier", "color", "shape"})
                if (row.has(key)) nbt.setInteger(key, row.get(key).getAsInt());
            nbt.setByte("size", (byte)row.get("size").getAsInt()); nbt.setShort("meta", (short)row.get("meta").getAsInt());
            nbt.setInteger("x",x); nbt.setInteger("y",0); nbt.setInteger("z",z);
            TileEntity tile = TileEntity.createAndLoadEntity(nbt);
            check(tile != null, "native bedrock tile cannot load exported schema");
            world.setBlock(x,0,z,node,0,2); world.setTileEntity(x,0,z,tile);
            Class<?> excavator = Class.forName("com.hbm.tileentity.machine.TileEntityMachineExcavator");
            TileEntity machine = (TileEntity)excavator.getConstructor().newInstance();
            machine.setWorldObj(world); machine.xCoord=-160; machine.yCoord=16; machine.zCoord=-160;
            IInventory inv = (IInventory)machine;
            inv.setInventorySlotContents(4, new ItemStack((Item)modItems.getField("drillbit").get(null),1,9));
            Object acid = tile.getClass().getField("acidRequirement").get(tile);
            if (acid != null) {
                Object type = acid.getClass().getField("type").get(acid);
                Object tank = excavator.getField("tank").get(machine);
                tank.getClass().getMethod("setTankType",type.getClass()).invoke(tank,type);
                tank.getClass().getMethod("setFill",int.class).invoke(tank,10000);
            }
            Class<?> pos = Class.forName("com.hbm.util.fauxpointtwelve.BlockPos");
            java.lang.reflect.Method collect = excavator.getDeclaredMethod("collectBedrock",pos); collect.setAccessible(true);
            collect.invoke(machine,pos.getConstructor(int.class,int.class,int.class).newInstance(x,0,z));
            ItemStack output = inv.getStackInSlot(5);
            check(output != null && output.getItem()==resource && output.hasTagCompound(), "native excavator cannot produce graded bedrock resource at tier " + tier);
        }
        for (String name : new String[]{"ore_oil","ore_bedrock_oil"}) {
            Block block = (Block)Block.blockRegistry.getObject("hbm:tile."+name);
            check(((Boolean)block.getClass().getMethod("requiresFracking").invoke(block)) == name.equals("ore_bedrock_oil"), "oil/fracking contract");
            check(((Number)block.getClass().getMethod("getPrimaryFluidAmount",int.class).invoke(block,0)).intValue()>0, "empty petroleum extraction yield");
        }
        NtmFloraProbe.run(world, restart);
        String converted = System.getProperty("efr.convertedResourceChunk");
        if (converted != null) {
            NBTTagCompound chunk;
            try (java.io.InputStream input = new java.io.FileInputStream(converted)) {
                chunk = net.minecraft.nbt.CompressedStreamTools.readCompressed(input);
            }
            net.minecraft.nbt.NBTTagList list = chunk.getCompoundTag("Level").getTagList("TileEntities",10);
            int loaded=0;
            for (int i=0;i<list.tagCount();i++) {
                NBTTagCompound nbt=list.getCompoundTagAt(i);
                if (!nbt.getString("id").equals(info.getAsJsonArray("nodeTemplates").get(0).getAsJsonObject().get("id").getAsString())) continue;
                TileEntity tile=TileEntity.createAndLoadEntity(nbt);
                check(tile!=null,"converter emitted an unknown tile ID");
                ItemStack stack=(ItemStack)tile.getClass().getField("resource").get(tile);
                check(stack!=null && stack.getItem()==resource,"converter emitted wrong native resource item");
                NBTTagCompound written=new NBTTagCompound();tile.writeToNBT(written);
                check(written.equals(nbt),"converter NBT does not round-trip through actual NTM: "+nbt+" -> "+written);
                loaded++;
            }
            check(loaded>0,"converted test chunk contained no mineral nodes");
            System.out.println("NTM_CONVERTED_NBT PASSED: "+loaded+" actual converter node payloads round-trip through NTM");
        }
        System.out.println("NTM_NATIVE_RESOURCES PASSED: four node tiers load, extract and persist; petroleum/fracking contracts");
        new java.io.File("ntm-resource-fixture-created").createNewFile();
    }
}
