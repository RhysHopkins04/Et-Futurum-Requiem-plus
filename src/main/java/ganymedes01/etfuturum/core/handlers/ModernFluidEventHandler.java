package ganymedes01.etfuturum.core.handlers;

import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import ganymedes01.etfuturum.ModernMapParityBlocks;
import ganymedes01.etfuturum.ModernPowderSnowBucket;
import ganymedes01.etfuturum.core.utils.PowderSnowPhysics;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.player.FillBucketEvent;

public final class ModernFluidEventHandler {
    @SubscribeEvent public void living(LivingUpdateEvent e) { PowderSnowPhysics.tick(e.entityLiving); }
    @SubscribeEvent public void bucket(FillBucketEvent e) {
        if (e.target == null || e.target.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || e.current == null) return;
        int x=e.target.blockX,y=e.target.blockY,z=e.target.blockZ;
        if (!e.world.canMineBlock(e.entityPlayer,x,y,z) || !e.entityPlayer.canPlayerEdit(x,y,z,e.target.sideHit,e.current)) return;
        if (e.current.getItem()==Items.bucket) {
            if (e.world.getBlock(x,y,z)==ModernMapParityBlocks.POWDER_SNOW.get()) {
                if (!e.world.isRemote) e.world.setBlockToAir(x,y,z);
                e.result=new ItemStack(ModernPowderSnowBucket.get());
                e.setResult(Event.Result.ALLOW);
                e.world.playSoundEffect(x+0.5,y+0.5,z+0.5,"minecraft_1.21.11:item.bucket.fill_powder_snow",1,1);
            } else if (ModernMapParityBlocks.containsWater(e.world,x,y,z)) {
                if (!e.world.isRemote) ModernMapParityBlocks.drainContainedWater(e.world,x,y,z);
                e.result=new ItemStack(Items.water_bucket);
                e.setResult(Event.Result.ALLOW);
            }
        } else if (e.current.getItem()==Items.water_bucket && ModernMapParityBlocks.fillContainedWater(e.world,x,y,z)) {
            e.result=new ItemStack(Items.bucket);
            e.setResult(Event.Result.ALLOW);
        }
    }
}
