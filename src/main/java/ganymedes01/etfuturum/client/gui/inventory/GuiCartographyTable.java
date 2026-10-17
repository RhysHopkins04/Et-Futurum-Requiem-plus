package ganymedes01.etfuturum.client.gui.inventory;

import ganymedes01.etfuturum.Tags;
import ganymedes01.etfuturum.inventory.ContainerCartographyTable;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.MapData;
import org.lwjgl.opengl.GL11;

/** Uses AssetDirector assets directly; no copied GUI PNGs or client-side saved-map allocation. */
public final class GuiCartographyTable extends GuiContainer {
    private static final ResourceLocation BACKGROUND=new ResourceLocation(Tags.MC_ASSET_VER,"textures/gui/container/cartography_table.png");
    private static final ResourceLocation MAP=new ResourceLocation(Tags.MC_ASSET_VER,"textures/gui/sprites/container/cartography_table/map.png");
    private static final ResourceLocation ERROR=new ResourceLocation(Tags.MC_ASSET_VER,"textures/gui/sprites/container/cartography_table/error.png");
    public GuiCartographyTable(ContainerCartographyTable container) { super(container); }
    @Override protected void drawGuiContainerForegroundLayer(int x,int y) {
        fontRendererObj.drawString(I18n.format("container.cartography_table"),8,4,0x404040);
        fontRendererObj.drawString(I18n.format("container.inventory"),8,73,0x404040);
    }
    @Override protected void drawGuiContainerBackgroundLayer(float ticks,int x,int y) {
        GL11.glColor4f(1,1,1,1);mc.getTextureManager().bindTexture(BACKGROUND);drawTexturedModalRect(guiLeft,guiTop,0,0,xSize,ySize);
        if(inventorySlots.getSlot(0).getHasStack()) {
            boolean valid=inventorySlots.getSlot(2).getHasStack();
            net.minecraft.item.Item extra=inventorySlots.getSlot(1).getHasStack()?inventorySlots.getSlot(1).getStack().getItem():null;
            boolean expand=valid && extra==net.minecraft.init.Items.paper;
            boolean copy=valid && extra==net.minecraft.init.Items.map;
            ResourceLocation map=expand?new ResourceLocation(Tags.MC_ASSET_VER,"textures/gui/sprites/container/cartography_table/scaled_map.png"):MAP;
            mc.getTextureManager().bindTexture(map);func_146110_a(guiLeft+67,guiTop+13,0,0,66,66,66,66);
            MapData data=(MapData)mc.theWorld.loadItemData(MapData.class,"map_"+inventorySlots.getSlot(0).getStack().getItemDamage());
            if(data!=null) {
                GL11.glPushMatrix();GL11.glTranslatef(guiLeft+(expand?85:70),guiTop+(expand?31:16),1);GL11.glScalef(expand?.234375F:.46875F,expand?.234375F:.46875F,1);
                mc.entityRenderer.getMapItemRenderer().func_148250_a(data,false);GL11.glPopMatrix();GL11.glColor4f(1,1,1,1);
            }
            if(copy) {
                mc.getTextureManager().bindTexture(new ResourceLocation(Tags.MC_ASSET_VER,"textures/gui/sprites/container/cartography_table/duplicated_map.png"));
                func_146110_a(guiLeft+83,guiTop+13,0,0,50,66,50,66);
            } else if(valid && extra==net.minecraft.item.Item.getItemFromBlock(net.minecraft.init.Blocks.glass_pane)) {
                mc.getTextureManager().bindTexture(new ResourceLocation(Tags.MC_ASSET_VER,"textures/gui/sprites/container/cartography_table/locked.png"));
                func_146110_a(guiLeft+119,guiTop+62,0,0,10,14,10,14);
            }
        }
        if(inventorySlots.getSlot(0).getHasStack() && inventorySlots.getSlot(1).getHasStack() && !inventorySlots.getSlot(2).getHasStack()) {
            mc.getTextureManager().bindTexture(ERROR);func_146110_a(guiLeft+35,guiTop+31,0,0,28,21,28,21);
        }
    }
}
