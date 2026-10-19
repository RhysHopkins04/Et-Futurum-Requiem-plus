package efrtest;

import java.io.*;
import java.util.*;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.*;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.tileentity.TileEntity;
import static efrtest.EarthImportContracts.check;

/** Focused native chain neighbour/loot checks against immutable captured geometry. */
final class NtmStructureDetailProbe {
    static void run(WorldServer server) throws Exception {
        File root=new File(System.getProperty("efr.structureDetailCaptures"));
        Block chain=(Block)Block.blockRegistry.getObject("hbm:tile.dungeon_chain");
        check(chain!=null,"native chain absent");
        for(int rotation=0;rotation<4;rotation++) {
          for(int offset:new int[]{0,63,224}) {
            NBTTagCompound capture;
            try(InputStream input=new FileInputStream(new File(root,"factory-r"+rotation+".nbt"))) {capture=CompressedStreamTools.readCompressed(input);}
            NtmRecordingWorld world=new NtmRecordingWorld(1,BiomeGenBase.plains,false,offset==0?64:128);
            NBTTagList cells=capture.getTagList("cells",10);List<int[]> chains=new ArrayList<>();
            for(int i=0;i<cells.tagCount();i++) {
                NBTTagCompound cell=cells.getCompoundTagAt(i);int x=cell.getInteger("x"),y=cell.getInteger("y")+offset,z=cell.getInteger("z");
                Block block=(Block)Block.blockRegistry.getObject(cell.getString("block"));
                world.setBlock(x,y,z,block,cell.getInteger("meta"),2);
                if(block==chain)chains.add(new int[]{x,y,z,cell.getInteger("meta")});
                if(cell.hasKey("tile",10)) {
                    TileEntity te=TileEntity.createAndLoadEntity(cell.getCompoundTag("tile"));
                    if(te!=null)world.setTileEntity(x,y,z,te);
                }
            }
            for(NtmRecordingWorld.Position pos:new ArrayList<>(world.written)) {
                TileEntity tile=world.getTileEntity(pos.x,pos.y,pos.z);
                if(tile!=null && tile.getClass().getName().contains("TileEntityWandLoot"))tile.updateEntity();
            }
            int lost=0;
            for(int pass=0;pass<3;pass++)for(int[] p:chains) {
                chain.onNeighborBlockChange(world,p[0],p[1],p[2],Blocks.stone);
                if(world.getBlock(p[0],p[1],p[2])!=chain)lost++;
            }
            System.out.println("NTM_CHAIN_DETAIL rotation="+rotation+" offset="+offset+" chains="+chains.size()+" lost="+lost+" entities="+world.entities);
            check(lost==0,"native factory chain fails neighbour/loot updates");
            // Deliberately removing an actual anchor must still break the chain.
            int[] top=chains.stream().filter(p->p[3]==0 && world.getBlock(p[0],p[1]+1,p[2])!=chain).findFirst().get();
            world.setBlockToAir(top[0],top[1]+1,top[2]);
            chain.onNeighborBlockChange(world,top[0],top[1],top[2],Blocks.air);
            check(world.getBlock(top[0],top[1],top[2])!=chain,"unsupported chain stopped obeying native physics");
          }
        }
        verifyNativeCollapse(root, "factory-r0");
        verifyNativeCollapse(root, "crane-r3");
        System.out.println("NTM_STRUCTURE_DETAILS PASSED");
    }
    /** Exercise the native scripted action omitted by the neighbour-only check. */
    @SuppressWarnings("unchecked")
    private static void verifyNativeCollapse(File root,String name) throws Exception {
        NBTTagCompound capture;
        try(InputStream in=new FileInputStream(new File(root,name+".nbt"))) {capture=CompressedStreamTools.readCompressed(in);}
        NtmRecordingWorld world=new NtmRecordingWorld(1,BiomeGenBase.plains,false,128);
        NBTTagList cells=capture.getTagList("cells",10);TileEntity trigger=null;
        for(int i=0;i<cells.tagCount();i++) {
            NBTTagCompound cell=cells.getCompoundTagAt(i);
            int x=cell.getInteger("x"),y=cell.getInteger("y")+63,z=cell.getInteger("z");
            world.setBlock(x,y,z,(Block)Block.blockRegistry.getObject(cell.getString("block")),cell.getInteger("meta"),2);
            if(cell.hasKey("tile",10)) {
                NBTTagCompound data=(NBTTagCompound)cell.getCompoundTag("tile").copy();
                data.setInteger("x",x);data.setInteger("y",y);data.setInteger("z",z);
                TileEntity tile=TileEntity.createAndLoadEntity(data);
                if(tile!=null)world.setTileEntity(x,y,z,tile);
                if("COLLAPSE_ROOF_RAD_5".equals(data.getString("actionID")))trigger=tile;
            }
        }
        check(trigger!=null,"missing native collapse trigger "+name);
        java.util.function.Consumer<Object> action=(java.util.function.Consumer<Object>)Class.forName("com.hbm.world.gen.util.LogicBlockActions").getField("COLLAPSE_ROOF_RAD_5").get(null);
        action.accept(trigger);
        check(world.entities.isEmpty(),"untriggered native trap collapsed");
        trigger.getClass().getField("phase").setInt(trigger,1);
        action.accept(trigger);
        check(!world.entities.isEmpty(),"activated native trap did not spawn falling blocks");
        check(world.entities.stream().allMatch(n->n.equals("com.hbm.entity.item.EntityFallingBlockNT")),"unexpected collapse entity");
        check(world.getBlock(trigger.xCoord,trigger.yCoord,trigger.zCoord)==Blocks.air,"native trigger not consumed");
        System.out.println("NTM_NATIVE_COLLAPSE "+name+" falling_entities="+world.entities.size()+" PASSED");
    }

}
