package ganymedes01.etfuturum.tileentities;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntitySign;

/**
 * EFR sign tile data with a small modern-sign compatibility layer.
 *
 * <p>The inherited {@link #signText} array remains the front side so old worlds and the legacy
 * 1.7 sign packet format stay readable. The additional back side, per-side colour/glow state and
 * shared wax lock are persisted as extra NBT fields.</p>
 */
public class TileEntityWoodSign extends TileEntitySign {

    private final String[] backText = new String[]{"", "", "", ""};
    private int frontTextColour = 0x000000;
    private int backTextColour = 0x000000;
    private boolean frontGlowing;
    private boolean backGlowing;
    private boolean waxed;

    /** Client-only editor hint used by the TESRs; harmless on a dedicated server. */
    private transient boolean editingBack;

    public String[] getText(boolean back) {
        return back ? backText : signText;
    }

    public void setText(boolean back, String[] lines) {
        String[] target = getText(back);
        for (int i = 0; i < 4; i++) {
            String value = lines != null && i < lines.length && lines[i] != null ? lines[i] : "";
            target[i] = value.length() > 15 ? value.substring(0, 15) : value;
        }
        markDirtyAndSync();
    }

    public int getTextColour(boolean back) {
        return back ? backTextColour : frontTextColour;
    }

    public void setTextColour(boolean back, int colour) {
        if (back) backTextColour = colour & 0xFFFFFF;
        else frontTextColour = colour & 0xFFFFFF;
        markDirtyAndSync();
    }

    public boolean isGlowing(boolean back) {
        return back ? backGlowing : frontGlowing;
    }

    public void setGlowing(boolean back, boolean glowing) {
        if (back) backGlowing = glowing;
        else frontGlowing = glowing;
        markDirtyAndSync();
    }

    public boolean isWaxed() {
        return waxed;
    }

    public void setWaxed(boolean waxed) {
        this.waxed = waxed;
        markDirtyAndSync();
    }

    public boolean isEditingBack() {
        return editingBack;
    }

    public void setEditingBack(boolean editingBack) {
        this.editingBack = editingBack;
    }

    /** Modern signs remain re-editable after save/reload until waxed. */
    @Override
    public boolean func_145914_a() {
        return !waxed;
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        for (int i = 0; i < 4; i++) compound.setString("BackText" + (i + 1), backText[i]);
        compound.setInteger("FrontTextColour", frontTextColour);
        compound.setInteger("BackTextColour", backTextColour);
        compound.setBoolean("FrontGlowing", frontGlowing);
        compound.setBoolean("BackGlowing", backGlowing);
        compound.setBoolean("Waxed", waxed);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        for (int i = 0; i < 4; i++) {
            String value = compound.getString("BackText" + (i + 1));
            backText[i] = value.length() > 15 ? value.substring(0, 15) : value;
        }
        frontTextColour = compound.hasKey("FrontTextColour") ? compound.getInteger("FrontTextColour") & 0xFFFFFF : 0x000000;
        backTextColour = compound.hasKey("BackTextColour") ? compound.getInteger("BackTextColour") & 0xFFFFFF : 0x000000;
        frontGlowing = compound.getBoolean("FrontGlowing");
        backGlowing = compound.getBoolean("BackGlowing");
        waxed = compound.getBoolean("Waxed");
    }

    private void markDirtyAndSync() {
        markDirty();
        if (worldObj != null) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound tag = new NBTTagCompound();
        writeToNBT(tag);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet) {
        readFromNBT(packet.func_148857_g());
    }
}
