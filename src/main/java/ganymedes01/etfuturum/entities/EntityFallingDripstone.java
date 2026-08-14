package ganymedes01.etfuturum.entities;

import java.util.List;

import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.blocks.BlockPointedDripstone;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

/** A whole unsupported stalactite column falling as one entity. */
public class EntityFallingDripstone extends EntityFallingBlock implements IEntityAdditionalSpawnData {

    private int fallHurtMax = 40;
    private float fallHurtAmount = 6.0F;
    private boolean hurtEntities = true;
    private int count = 1;

    public EntityFallingDripstone(World world) {
        super(world);
    }

    public EntityFallingDripstone(World world, double x, double y, double z, int meta, int count) {
        super(world, x, y, z, ModBlocks.POINTED_DRIPSTONE.get(), meta);
        this.count = Math.max(1, count);
        this.fallHurtAmount = Math.max(this.count, 6);
        resizeForColumn();
        this.setPosition(x, y, z);
    }

    private void resizeForColumn() {
        this.yOffset = this.count - 0.5F;
        this.setSize(0.98F, this.count);
    }

    public int getCount() {
        return count;
    }

    @Override
    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        ++this.field_145812_b;
        this.motionY -= 0.04D;
        this.moveEntity(this.motionX, this.motionY, this.motionZ);
        this.motionX *= 0.98D;
        this.motionY *= 0.98D;
        this.motionZ *= 0.98D;

        if (this.worldObj.isRemote) {
            return;
        }

        int y = MathHelper.floor_double(this.posY);
        if (this.onGround) {
            this.motionX *= 0.7D;
            this.motionZ *= 0.7D;
            this.motionY *= -0.5D;
            dropColumnItems();
            this.setDead();
            return;
        }

        int maxHeight = Math.max(256, this.worldObj.getActualHeight());
        if ((this.field_145812_b > 100 && (y < -32 || y > maxHeight + 32)) || this.field_145812_b > 600) {
            dropColumnItems();
            this.setDead();
        }
    }

    private void dropColumnItems() {
        if (!this.field_145813_c || !ModBlocks.POINTED_DRIPSTONE.isEnabled()) {
            return;
        }
        Block block = ModBlocks.POINTED_DRIPSTONE.get();
        this.entityDropItem(new ItemStack(block, this.count, 0), 0.5F);
    }

    @Override
    protected void fall(float distance) {
        if (!this.hurtEntities) {
            return;
        }
        int blocksFallen = MathHelper.ceiling_float_int(distance - 1.0F);
        if (blocksFallen <= 0) {
            return;
        }

        float baseDamage = Math.min(blocksFallen * this.fallHurtAmount, this.fallHurtMax);
        @SuppressWarnings("unchecked")
        List<Entity> entities = this.worldObj.getEntitiesWithinAABBExcludingEntity(this, this.boundingBox);
        for (Entity entity : entities) {
            float damage = baseDamage;
            if (entity instanceof EntityLivingBase) {
                EntityLivingBase living = (EntityLivingBase) entity;
                ItemStack helmet = living.getEquipmentInSlot(4);
                if (helmet != null) {
                    damage *= 0.75F;
                    helmet.damageItem(2, living);
                }
            }
            entity.attackEntityFrom(BlockPointedDripstone.STALACTITE_DAMAGE, damage);
        }
    }

    @Override
    public void writeSpawnData(ByteBuf data) {
        data.writeInt(this.count);
        data.writeInt(this.field_145814_a);
    }

    @Override
    public void readSpawnData(ByteBuf data) {
        this.count = Math.max(1, data.readInt());
        this.field_145814_a = data.readInt();
        resizeForColumn();
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setBoolean("HurtEntities", this.hurtEntities);
        tag.setFloat("FallHurtAmount", this.fallHurtAmount);
        tag.setInteger("FallHurtMax", this.fallHurtMax);
        tag.setInteger("Count", this.count);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        this.hurtEntities = !tag.hasKey("HurtEntities") || tag.getBoolean("HurtEntities");
        this.fallHurtMax = tag.hasKey("FallHurtMax") ? tag.getInteger("FallHurtMax") : 40;
        this.count = tag.hasKey("Count") ? Math.max(1, tag.getInteger("Count")) : 1;
        this.fallHurtAmount = Math.max(this.count, 6);
        resizeForColumn();
    }
}
