package ganymedes01.etfuturum.client.gui.inventory;

import ganymedes01.etfuturum.Tags;
import ganymedes01.etfuturum.inventory.ContainerCrafter;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public final class GuiCrafter extends GuiContainer {
    private final ContainerCrafter container;
    private static final ResourceLocation TEXTURE=new ResourceLocation(Tags.MC_ASSET_VER,"textures/gui/container/crafter.png");
    private static final ResourceLocation DISABLED=new ResourceLocation(Tags.MC_ASSET_VER,"textures/gui/sprites/container/crafter/disabled_slot.png");
    public GuiCrafter(ContainerCrafter container) { super(container);this.container=container; }
    @Override protected void drawGuiContainerForegroundLayer(int x,int y) {
        fontRendererObj.drawString(I18n.format("container.crafter"),8,6,0x404040);fontRendererObj.drawString(I18n.format("container.inventory"),8,72,0x404040);
    }
    @Override protected void drawGuiContainerBackgroundLayer(float ticks,int x,int y) {
        GL11.glColor4f(1,1,1,1);mc.getTextureManager().bindTexture(TEXTURE);drawTexturedModalRect(guiLeft,guiTop,0,0,xSize,ySize);
        for(int i=0;i<9;i++)if(container.isDisabled(i)) { mc.getTextureManager().bindTexture(DISABLED);func_146110_a(guiLeft+25+(i%3)*18,guiTop+16+(i/3)*18,0,0,18,18,18,18); }
        ResourceLocation power=new ResourceLocation(Tags.MC_ASSET_VER,"textures/gui/sprites/container/crafter/"+(container.powered?"powered_redstone":"unpowered_redstone")+".png");
        mc.getTextureManager().bindTexture(power);func_146110_a(guiLeft+9,guiTop+46,0,0,16,16,16,16);
    }
    @Override protected void mouseClicked(int x,int y,int button) {
        int slotX=(x-guiLeft-26)/18,slotY=(y-guiTop-17)/18;
        if(button==0 && x>=guiLeft+26 && x<guiLeft+80 && y>=guiTop+17 && y<guiTop+71 && mc.thePlayer.inventory.getItemStack()==null) {
            int slot=slotY*3+slotX;
            if(!container.getSlot(slot).getHasStack()) { mc.playerController.sendEnchantPacket(inventorySlots.windowId,slot);return; }
        }
        super.mouseClicked(x,y,button);
    }
}
