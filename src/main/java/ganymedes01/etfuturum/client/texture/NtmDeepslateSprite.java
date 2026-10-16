package ganymedes01.etfuturum.client.texture;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

/** Combines the installed NTM overlay with the active deepslate texture at atlas load. */
public final class NtmDeepslateSprite extends TextureAtlasSprite {
    private final String overlay;
    private NtmDeepslateSprite(String name,String overlay) {super(name);this.overlay=overlay;}
    public static IIcon register(IIconRegister register,String name,String overlay) {
        if (!(register instanceof TextureMap)) return register.registerIcon(name);
        TextureMap map=(TextureMap)register;
        NtmDeepslateSprite sprite=new NtmDeepslateSprite(name,overlay);
        if (map.setTextureEntry(name,sprite)) return sprite;
        return map.getTextureExtry(name);
    }
    @Override public boolean hasCustomLoader(IResourceManager manager,ResourceLocation location) {return true;}
    @Override public boolean load(IResourceManager manager,ResourceLocation location) {
        try (InputStream host=manager.getResource(new ResourceLocation("minecraft","textures/blocks/deepslate.png")).getInputStream();
             InputStream ore=manager.getResource(new ResourceLocation("hbm","textures/blocks/"+overlay+".png")).getInputStream()) {
            BufferedImage combined=compose(ImageIO.read(host),ImageIO.read(ore));
            loadSprite(new BufferedImage[]{combined},null,false);
            return false; // Forge's TextureMap stitches custom sprites on false.
        } catch(IOException failure) {throw new IllegalStateException("Cannot load NTM deepslate texture "+overlay,failure);}
    }
    public static BufferedImage compose(BufferedImage host,BufferedImage overlay) {
        if(host==null || overlay==null || host.getWidth()!=host.getHeight() || overlay.getWidth()!=overlay.getHeight())
            throw new IllegalArgumentException("Expected square static ore/host textures");
        int size=Math.max(host.getWidth(),overlay.getWidth());
        BufferedImage result=new BufferedImage(size,size,BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics=result.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            graphics.drawImage(host,0,0,size,size,null);graphics.drawImage(overlay,0,0,size,size,null);
        } finally {graphics.dispose();}
        return result;
    }
}
