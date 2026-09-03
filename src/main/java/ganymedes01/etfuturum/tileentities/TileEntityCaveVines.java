package ganymedes01.etfuturum.tileentities;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

import java.util.Random;

/**
 * Persistent state for a Cave Vine tip.
 *
 * <p>Pass 32 makes {@code Age} (0..25) the canonical Backporter-facing modern state. The older
 * {@code MaxLength}/{@code TipSheared} fields remain readable/writable so existing EFR worlds keep
 * loading without an NBT migration step.</p>
 */
public class TileEntityCaveVines extends TileEntity
{
    private int maxLength;
    private boolean tipSheared = false;
    private int age = 0;

    public TileEntityCaveVines()
    {
        Random rand = new Random();
        maxLength = rand.nextInt(26) + 2;
    }

    public int getMaxLength()
    {
        return maxLength;
    }

    public void setMaxLength(int length)
    {
        maxLength = Math.max(2, length);
        markDirtyAndSync();
    }

    public int getAge()
    {
        return age;
    }

    public void setAge(int value)
    {
        age = Math.max(0, Math.min(25, value));
        markDirtyAndSync();
    }

    public boolean getTipSheared()
    {
        return tipSheared;
    }

    public void setTipSheared(boolean value)
    {
        tipSheared = value;
        markDirtyAndSync();
    }

    private void markDirtyAndSync()
    {
        markDirty();
        if (worldObj != null) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger("Age", age);
        compound.setInteger("MaxLength", maxLength);
        compound.setBoolean("TipSheared", tipSheared);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        boolean hasMaxLength = compound.hasKey("MaxLength");
        this.maxLength = hasMaxLength ? Math.max(2, compound.getInteger("MaxLength")) : 27;
        this.tipSheared = compound.getBoolean("TipSheared");
        if (compound.hasKey("Age")) {
            this.age = Math.max(0, Math.min(25, compound.getInteger("Age")));
        } else {
            // Old EFR saves used a random maximum length rather than modern age. There is no exact
            // inverse, so derive a stable conservative age while retaining MaxLength itself.
            this.age = Math.max(0, Math.min(25, 27 - this.maxLength));
        }
    }

    @Override
    public Packet getDescriptionPacket()
    {
        NBTTagCompound tag = new NBTTagCompound();
        writeToNBT(tag);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet)
    {
        readFromNBT(packet.func_148857_g());
        if (worldObj != null) worldObj.markBlockRangeForRenderUpdate(xCoord, yCoord, zCoord, xCoord, yCoord, zCoord);
    }

    @Override
    public boolean canUpdate()
    {
        return false;
    }
}
