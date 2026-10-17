package ganymedes01.etfuturum.client.gui.inventory;

import ganymedes01.etfuturum.inventory.ContainerStonecutter;
import ganymedes01.etfuturum.recipes.StonecutterRecipes;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.opengl.GL12;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import java.util.List;

/** Modern four-column selection layout with wheel and draggable-scrollbar support. */
public final class GuiStonecutter extends GuiContainer {
    private static final ResourceLocation BACKGROUND=new ResourceLocation("textures/gui/container/stonecutter.png");
    private final ContainerStonecutter container;
    private List<StonecutterRecipes.Recipe> lastChoices;
    private int row;
    private boolean scrolling;
    public GuiStonecutter(ContainerStonecutter container) { super(container);this.container=container;xSize=176;ySize=166; }
    private int maxRow() { return Math.max(0,(container.getRecipes().size()+3)/4-3); }
    private void clamp() {
        if(lastChoices!=container.getRecipes()) { lastChoices=container.getRecipes();row=0; }
        row=Math.max(0,Math.min(row,maxRow()));
    }
    private int hovered(int mouseX,int mouseY) {
        int x=mouseX-guiLeft-52,y=mouseY-guiTop-14;
        return x>=0 && x<64 && y>=0 && y<54 ? row*4+(y/18)*4+x/16 : -1;
    }
    private void sprite(String name,int x,int y,int width,int height) {
        mc.getTextureManager().bindTexture(new ResourceLocation("textures/gui/sprites/container/stonecutter/"+name+".png"));
        func_146110_a(x,y,0,0,width,height,width,height);
    }
    @Override protected void drawGuiContainerBackgroundLayer(float partialTicks,int mouseX,int mouseY) {
        clamp();if(!Mouse.isButtonDown(0))scrolling=false;
        GL11.glColor4f(1,1,1,1);mc.getTextureManager().bindTexture(BACKGROUND);
        drawTexturedModalRect(guiLeft,guiTop,0,0,xSize,ySize);
        List<StonecutterRecipes.Recipe> choices=container.getRecipes();int hovered=hovered(mouseX,mouseY);
        for(int index=row*4;index<Math.min(choices.size(),row*4+12);index++) {
            int cell=index-row*4,x=guiLeft+52+cell%4*16,y=guiTop+14+cell/4*18;
            sprite(index==container.getSelected()?"recipe_selected":index==hovered?"recipe_highlighted":"recipe",x,y,16,18);
        }
        sprite(maxRow()>0?"scroller":"scroller_disabled",guiLeft+119,guiTop+15+(maxRow()==0?0:row*39/maxRow()),12,15);
        RenderHelper.enableGUIStandardItemLighting();
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,240F,240F);
        GL11.glColor4f(1,1,1,1);
        for(int index=row*4;index<Math.min(choices.size(),row*4+12);index++) {
            int cell=index-row*4,x=guiLeft+52+cell%4*16,y=guiTop+15+cell/4*18;
            ItemStack output=choices.get(index).getOutput();
            itemRender.renderItemAndEffectIntoGUI(fontRendererObj,mc.getTextureManager(),output,x,y);
        }
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        RenderHelper.disableStandardItemLighting();GL11.glDisable(GL11.GL_LIGHTING);
    }
    @Override protected void drawGuiContainerForegroundLayer(int mouseX,int mouseY) {
        fontRendererObj.drawString(I18n.format("tile.etfuturum.stonecutter.name"),8,6,0x404040);
        fontRendererObj.drawString(I18n.format("container.inventory"),8,72,0x404040);
    }
    @Override public void drawScreen(int mouseX,int mouseY,float partialTicks) {
        super.drawScreen(mouseX,mouseY,partialTicks);
        int index=hovered(mouseX,mouseY);
        if(index>=0 && index<container.getRecipes().size())
            func_146283_a(container.getRecipes().get(index).getOutput().getTooltip(mc.thePlayer,mc.gameSettings.advancedItemTooltips),mouseX,mouseY);
    }
    private void scrollTo(int mouseY) {
        row=Math.max(0,Math.min(maxRow(),Math.round((mouseY-guiTop-15-7.5F)/39F*maxRow())));
    }
    @Override protected void mouseClicked(int mouseX,int mouseY,int button) {
        clamp();
        if(button==0) {
            if(maxRow()>0 && mouseX>=guiLeft+119 && mouseX<guiLeft+131 && mouseY>=guiTop+15 && mouseY<guiTop+69) {
                scrolling=true;scrollTo(mouseY);return;
            }
            int index=hovered(mouseX,mouseY);
            if(index>=0 && index<container.getRecipes().size()) {
                if(container.enchantItem(mc.thePlayer,index))mc.playerController.sendEnchantPacket(inventorySlots.windowId,index);
                return;
            }
        }
        super.mouseClicked(mouseX,mouseY,button);
    }
    @Override protected void mouseClickMove(int mouseX,int mouseY,int button,long duration) {
        if(scrolling && button==0 && Mouse.isButtonDown(0))scrollTo(mouseY);
        else super.mouseClickMove(mouseX,mouseY,button,duration);
    }
    @Override public void handleMouseInput() {
        super.handleMouseInput();int wheel=Mouse.getEventDWheel();clamp();
        int x=Mouse.getEventX()*width/mc.displayWidth,y=height-Mouse.getEventY()*height/mc.displayHeight-1;
        if(wheel!=0 && x>=guiLeft+52 && x<guiLeft+132 && y>=guiTop+14 && y<guiTop+69)
            row=Math.max(0,Math.min(maxRow(),row+(wheel>0?-1:1)));
    }
}
