package ganymedes01.etfuturum.client.renderer.tileentity;

import ganymedes01.etfuturum.tileentities.TileEntityLectern;
import net.minecraft.client.model.ModelBook;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/** The standard book mesh, fixed open on the lectern's authored sloping surface. */
public final class TileEntityLecternRenderer extends TileEntitySpecialRenderer {
    private static final ResourceLocation TEXTURE=new ResourceLocation("textures/entity/enchanting_table_book.png");
    private final ModelBook book=new ModelBook();
    @Override public void renderTileEntityAt(TileEntity tile,double x,double y,double z,float ticks) {
        if(!(tile instanceof TileEntityLectern) || !((TileEntityLectern)tile).hasBook())return;
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef((float)x+.5F,(float)y+1.0625F,(float)z+.5F);
            int facing=tile.getBlockMetadata()&3;
            GL11.glRotatef(-(facing==0?270:facing==1?0:facing==2?90:180),0,1,0);GL11.glRotatef(67.5F,0,0,1);GL11.glTranslatef(0,-.125F,0);
            GL11.glColor4f(1,1,1,1);bindTexture(TEXTURE);book.render(null,0,.1F,.9F,1.2F,0,.0625F);
        } finally { GL11.glPopMatrix(); }
    }
}
