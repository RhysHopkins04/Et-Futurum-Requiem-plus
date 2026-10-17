package ganymedes01.etfuturum.client.gui.inventory;

import ganymedes01.etfuturum.inventory.ContainerGrindstone;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public final class GuiGrindstone extends GuiContainer {
    private static final ResourceLocation TEXTURE=new ResourceLocation("textures/gui/container/grindstone.png");
    public GuiGrindstone(ContainerGrindstone container) { super(container); }
    @Override protected void drawGuiContainerForegroundLayer(int x,int y) {
        fontRendererObj.drawString(I18n.format("container.grindstone_title"),8,6,0x404040);
        fontRendererObj.drawString(I18n.format("container.inventory"),8,72,0x404040);
    }
    @Override protected void drawGuiContainerBackgroundLayer(float ticks,int x,int y) {
        GL11.glColor4f(1,1,1,1);mc.getTextureManager().bindTexture(TEXTURE);drawTexturedModalRect(guiLeft,guiTop,0,0,xSize,ySize);
        if((inventorySlots.getSlot(0).getHasStack() || inventorySlots.getSlot(1).getHasStack()) && !inventorySlots.getSlot(2).getHasStack())
            { mc.getTextureManager().bindTexture(new ResourceLocation("textures/gui/sprites/container/grindstone/error.png"));
              func_146110_a(guiLeft+92,guiTop+31,0,0,28,21,28,21); }
    }
}
