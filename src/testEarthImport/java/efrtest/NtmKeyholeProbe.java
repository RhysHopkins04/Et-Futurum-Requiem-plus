package efrtest;

import net.minecraft.block.Block;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;

/** Native player activation remains available after importing the hidden block. */
final class NtmKeyholeProbe {
    static void run(WorldServer server)throws Exception{
        Block keyhole=NtmLegacyCapture.block("stone_keyhole"),door=NtmLegacyCapture.block("door_red");
        Item key=(Item)Class.forName("com.hbm.items.ModItems").getField("key_red_cracked").get(null);
        FakePlayer player=FakePlayerFactory.getMinecraft(server);int selected=player.inventory.currentItem;
        ItemStack previous=player.inventory.getCurrentItem();
        try{
            for(int y:new int[]{12,272})for(int side=2;side<=5;side++){
                NtmRecordingWorld world=new NtmRecordingWorld(12345+side,BiomeGenBase.plains,false,288);
                world.setBlock(0,y,0,keyhole);player.inventory.setInventorySlotContents(selected,new ItemStack(key));
                if(!keyhole.onBlockActivated(world,0,y,0,player,side,0.5F,0.5F,0.5F)||player.inventory.getCurrentItem().stackSize!=0
                    ||world.getBlock(0,y-1,0)!=door||world.written.size()<300)throw new AssertionError("Imported keyhole lost native room/door/key activation");
                for(NtmRecordingWorld.Position pos:world.written){
                    TileEntity tile=world.getTileEntity(pos.x,pos.y,pos.z);if(tile==null)continue;
                    NBTTagCompound state=new NBTTagCompound();tile.writeToNBT(state);TileEntity loaded=TileEntity.createAndLoadEntity(state);
                    if(loaded==null||loaded.getClass()!=tile.getClass())throw new AssertionError("Native keyhole room tile cannot reload");
                }
            }
        }finally{player.inventory.setInventorySlotContents(selected,previous);}
        System.out.println("NTM_KEYHOLE_ACTIVATION PASSED: native key consumption, all four orientations, rooms, doors and tile reload at Y12/Y272");
    }
}
