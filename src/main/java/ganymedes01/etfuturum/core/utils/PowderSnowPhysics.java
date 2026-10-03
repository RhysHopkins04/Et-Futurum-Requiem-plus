package ganymedes01.etfuturum.core.utils;

import ganymedes01.etfuturum.ModernMapParityBlocks;
import ganymedes01.etfuturum.entities.EntityFox;
import ganymedes01.etfuturum.entities.EntityEndermite;
import ganymedes01.etfuturum.entities.EntityRabbit;
import ganymedes01.etfuturum.entities.EntityPolarBear;
import ganymedes01.etfuturum.entities.EntityStray;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.monster.*;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.monster.EntitySnowman;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import java.util.UUID;

public final class PowderSnowPhysics {
    public static final String FROZEN = "etfuturum:TicksFrozen";
    private static final UUID FROST_SPEED = UUID.fromString("6d62ae7c-b384-4675-893a-55a731c7516d");
    public static final DamageSource FREEZE = new DamageSource("freeze").setDamageBypassesArmor();
    private PowderSnowPhysics() { }
    public static boolean boots(EntityLivingBase e) { ItemStack s=e.getEquipmentInSlot(1); return s != null && s.getItem()==Items.leather_boots; }
    public static boolean canWalk(Entity e) { return e instanceof EntityFox || e instanceof EntityRabbit || e instanceof EntitySilverfish
            || e instanceof EntityEndermite || e instanceof EntityLivingBase && boots((EntityLivingBase)e); }
    public static boolean atFeet(Entity e) {
        return e.worldObj.getBlock(MathHelper.floor_double(e.posX),MathHelper.floor_double(e.boundingBox.minY),MathHelper.floor_double(e.posZ)) == ModernMapParityBlocks.POWDER_SNOW.get();
    }
    public static boolean stuck(Entity e) {
        if (e instanceof EntityLivingBase) return atFeet(e);
        net.minecraft.util.AxisAlignedBB box=e.boundingBox;
        for (int x=MathHelper.floor_double(box.minX+0.001D);x<=MathHelper.floor_double(box.maxX-0.001D);x++)
            for (int y=MathHelper.floor_double(box.minY+0.001D);y<=MathHelper.floor_double(box.maxY-0.001D);y++)
                for (int z=MathHelper.floor_double(box.minZ+0.001D);z<=MathHelper.floor_double(box.maxZ-0.001D);z++)
                    if (e.worldObj.getBlock(x,y,z)==ModernMapParityBlocks.POWDER_SNOW.get()) return true;
        return false;
    }
    public static boolean canFreeze(EntityLivingBase e) {
        if (e instanceof EntitySnowman || e instanceof EntityPolarBear || e instanceof EntityStray || e instanceof EntityWither
                || e instanceof EntityPlayer && ((EntityPlayer)e).capabilities.disableDamage) return false;
        for (int slot=1; slot<=4; slot++) {
            ItemStack s=e.getEquipmentInSlot(slot);
            if (s != null && (s.getItem()==Items.leather_boots || s.getItem()==Items.leather_leggings
                    || s.getItem()==Items.leather_chestplate || s.getItem()==Items.leather_helmet)) return false;
        }
        return true;
    }
    public static int nextFrozen(int ticks,boolean inside,boolean susceptible) {
        return inside && susceptible ? Math.min(140,ticks+1) : Math.max(0,ticks-2);
    }
    public static void tick(EntityLivingBase e) {
        if (e.worldObj.isRemote) return;
        int old=e.getEntityData().getInteger(FROZEN);
        boolean susceptible=canFreeze(e), inside=atFeet(e);
        int next=nextFrozen(old,inside,susceptible);
        if (old != next) e.getEntityData().setInteger(FROZEN,next);
        if (e instanceof net.minecraft.entity.player.EntityPlayerMP && (old>0 || next>0 || e.ticksExisted==1) && (e.ticksExisted%20==0 || e.ticksExisted==1 || old!=next && (next%10==0 || next==140)))
            ganymedes01.etfuturum.EtFuturum.networkWrapper.sendTo(new ganymedes01.etfuturum.network.FrozenStateMessage(next),(net.minecraft.entity.player.EntityPlayerMP)e);
        if (old==0 && next==0) return;
        if (!e.worldObj.isRemote) {
            net.minecraft.entity.ai.attributes.IAttributeInstance speed=e.getEntityAttribute(SharedMonsterAttributes.movementSpeed);
            AttributeModifier existing=speed.getModifier(FROST_SPEED);
            if (existing != null) speed.removeModifier(existing);
            if (next>0 && !e.worldObj.isAirBlock(MathHelper.floor_double(e.posX),MathHelper.floor_double(e.boundingBox.minY)-1,MathHelper.floor_double(e.posZ)))
                speed.applyModifier(new AttributeModifier(FROST_SPEED,"Powder snow frost",-0.05D*next/140.0D,0).setSaved(false));
            if (next >= 140 && susceptible && e.ticksExisted%40==0)
                e.attackEntityFrom(FREEZE,e instanceof EntityBlaze || e instanceof EntityMagmaCube ? 5.0F : 1.0F);
        }
    }
}
