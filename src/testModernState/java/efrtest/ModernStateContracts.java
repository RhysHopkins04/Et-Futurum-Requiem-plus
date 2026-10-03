package efrtest;

import ganymedes01.etfuturum.core.utils.KelpAgeMigration;
import ganymedes01.etfuturum.core.utils.PowderSnowPhysics;
import net.minecraft.nbt.*;
import java.io.*;

public final class ModernStateContracts {
    public static void check(boolean test,String message) { if (!test) throw new AssertionError(message); }
    public static NBTTagCompound fixture(int low,int high) {
        NBTTagCompound level=new NBTTagCompound(); level.setInteger("xPos",-2); level.setInteger("zPos",3);
        NBTTagList sections=new NBTTagList(),tiles=new NBTTagList();
        NBTTagCompound section=new NBTTagCompound(); section.setByte("Y",(byte)4);
        byte[] blocks=new byte[4096],data=new byte[2048],add=new byte[2048];
        for (int age=0;age<=25;age++) {
            blocks[age]=(byte)low; put(add,age,low>>8);
            NBTTagCompound tile=new NBTTagCompound(); tile.setString("id","etfuturum:modern_parity_kelp_state");
            tile.setInteger("x",-32+(age&15)); tile.setInteger("y",64); tile.setInteger("z",48+(age>>4)); tile.setByte("Age",(byte)age);
            tiles.appendTag(tile);
        }
        blocks[26]=7; put(add,26,0); put(data,26,13);
        section.setByteArray("Blocks",blocks);section.setByteArray("Data",data);section.setByteArray("Add",add);
        sections.appendTag(section);level.setTag("Sections",sections);level.setTag("TileEntities",tiles);
        return level;
    }
    private static void put(byte[] a,int i,int n) { int shift=(i&1)*4; a[i>>1]=(byte)((a[i>>1]&~(15<<shift))|((n&15)<<shift)); }
    public static void verify(NBTTagCompound level,int low,int high) {
        NBTTagCompound s=level.getTagList("Sections",10).getCompoundTagAt(0);
        byte[] b=s.getByteArray("Blocks"),a=s.getByteArray("Add"),d=s.getByteArray("Data");
        for (int age=0;age<=25;age++) {
            int shift=(age&1)*4,id=(b[age]&255)|(((a[age>>1]>>shift)&15)<<8),meta=(d[age>>1]>>shift)&15;
            check(id==(age<16?low:high) && meta==(age&15),"age "+age+" corrupted");
        }
        check(b[26]==7 && ((d[13])&15)==13,"neighbor nibble changed");
        check(level.getTagList("TileEntities",10).tagCount()==0,"migration left head TEs");
    }
    public static void main(String[] args) throws Exception {
        for (int[] ids:new int[][]{{240,3500},{3001,3011},{4000,33}}) {
            NBTTagCompound level=fixture(ids[0],ids[1]);
            check(level.getTagList("TileEntities",10).getCompoundTagAt(25).getTag("Age") instanceof NBTTagByte,"Age is not BYTE");
            ByteArrayOutputStream bytes=new ByteArrayOutputStream(); CompressedStreamTools.writeCompressed(level,bytes);
            level=CompressedStreamTools.readCompressed(new ByteArrayInputStream(bytes.toByteArray()));
            check(KelpAgeMigration.migrate(level,ids[0],ids[1]),"migration did not run"); verify(level,ids[0],ids[1]);
            bytes=new ByteArrayOutputStream(); CompressedStreamTools.writeCompressed(level,bytes);
            level=CompressedStreamTools.readCompressed(new ByteArrayInputStream(bytes.toByteArray())); verify(level,ids[0],ids[1]);
            check(!KelpAgeMigration.migrate(level,ids[0],ids[1]),"migration not idempotent");
        }
        NBTTagCompound level=fixture(3001,3011);level.getTagList("TileEntities",10).getCompoundTagAt(0).setInteger("x",9000);
        check(KelpAgeMigration.migrate(level,3001,3011),"remaining valid migration failed");
        check(level.getTagList("TileEntities",10).tagCount()==1,"out-of-chunk TE was changed");
        int ticks=0;for(int i=0;i<140;i++)ticks=PowderSnowPhysics.nextFrozen(ticks,true,true);
        check(ticks==140,"freeze threshold");check(PowderSnowPhysics.nextFrozen(ticks,true,true)==140,"unbounded freezing");
        check(PowderSnowPhysics.nextFrozen(ticks,false,true)==138,"thaw rate");
        check(PowderSnowPhysics.nextFrozen(ticks,true,false)==138,"leather/immunity thaw");
        check(PowderSnowPhysics.nextFrozen(0,false,true)==0,"negative frozen ticks");
        System.out.println("Modern state contracts PASSED: 78 migrated ages, high/low IDs, compressed NBT reload, idempotence, neighbor preservation, freezing/thawing.");
    }
}
