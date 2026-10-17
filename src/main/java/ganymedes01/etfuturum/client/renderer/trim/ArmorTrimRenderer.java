package ganymedes01.etfuturum.client.renderer.trim;

import ganymedes01.etfuturum.Tags;
import ganymedes01.etfuturum.recipes.ModernSmithingRecipes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.*;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.*;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

/** Palette permutations are generated in memory from AssetDirector resources; no Mojang art is bundled. */
public final class ArmorTrimRenderer implements IResourceManagerReloadListener {
    private static final Map<String,ResourceLocation> CACHE=new HashMap<>();
    private static final Set<String> FAILED=new HashSet<>();
    public static BufferedImage recolor(BufferedImage source,BufferedImage key,BufferedImage palette) {
        if(key.getWidth()*key.getHeight()!=palette.getWidth()*palette.getHeight())throw new IllegalArgumentException("Trim palette size mismatch");
        Map<Integer,Integer> colors=new HashMap<>();for(int i=0;i<key.getWidth()*key.getHeight();i++) {
            int original=key.getRGB(i%key.getWidth(),i/key.getWidth());
            if((original>>>24)!=0)colors.put(original&0xffffff,palette.getRGB(i%palette.getWidth(),i/palette.getWidth()));
        }
        BufferedImage result=new BufferedImage(source.getWidth(),source.getHeight(),BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<source.getHeight();y++)for(int x=0;x<source.getWidth();x++) {
            int color=source.getRGB(x,y),alpha=color>>>24;Integer mapped=colors.get(color&0xffffff);
            if(mapped!=null)color=((alpha*(mapped>>>24)/255)<<24)|(mapped&0xffffff);
            result.setRGB(x,y,alpha==0?0:color);
        }return result;
    }
    /** Match NativeImage/STB's raw grayscale values rather than ImageIO's gray-to-sRGB conversion. */
    public static BufferedImage decode(InputStream input) throws IOException {
        BufferedImage source=ImageIO.read(input);
        if(source==null)throw new IOException("Unreadable trim image");
        if(source.getColorModel().getColorSpace().getType()!=java.awt.color.ColorSpace.TYPE_GRAY)return source;
        BufferedImage result=new BufferedImage(source.getWidth(),source.getHeight(),BufferedImage.TYPE_INT_ARGB);
        int bits=source.getSampleModel().getSampleSize(0);
        for(int y=0;y<source.getHeight();y++)for(int x=0;x<source.getWidth();x++) {
            int gray=source.getRaster().getSample(x,y,0);
            gray=bits==16?gray>>>8:gray*255/((1<<bits)-1);
            int alpha=source.getRGB(x,y)>>>24;
            result.setRGB(x,y,(alpha<<24)|(gray<<16)|(gray<<8)|gray);
        }
        return result;
    }
    private static BufferedImage read(String path) throws IOException {
        try(InputStream input=Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation(Tags.MC_ASSET_VER,path)).getInputStream()) {
            return decode(input);
        }
    }
    private static boolean oneOf(Item item,Item... choices) { for(Item choice:choices)if(item==choice)return true;return false; }
    private static String armorAsset(ItemStack stack) {
        Item item=stack.getItem();
        // Equipment assets, not ArmorMaterial: chainmail also uses IRON in 1.7.
        if(oneOf(item,net.minecraft.init.Items.iron_helmet,net.minecraft.init.Items.iron_chestplate,net.minecraft.init.Items.iron_leggings,net.minecraft.init.Items.iron_boots))return "iron";
        if(oneOf(item,net.minecraft.init.Items.golden_helmet,net.minecraft.init.Items.golden_chestplate,net.minecraft.init.Items.golden_leggings,net.minecraft.init.Items.golden_boots))return "gold";
        if(oneOf(item,net.minecraft.init.Items.diamond_helmet,net.minecraft.init.Items.diamond_chestplate,net.minecraft.init.Items.diamond_leggings,net.minecraft.init.Items.diamond_boots))return "diamond";
        if(oneOf(item,ganymedes01.etfuturum.ModItems.NETHERITE_HELMET.get(),ganymedes01.etfuturum.ModItems.NETHERITE_CHESTPLATE.get(),ganymedes01.etfuturum.ModItems.NETHERITE_LEGGINGS.get(),ganymedes01.etfuturum.ModItems.NETHERITE_BOOTS.get()))return "netherite";
        return "";
    }
    private static String palette(ItemStack stack,NBTTagCompound trim) {
        String material=trim.getString("material").replace("minecraft:","");
        return material.equals(armorAsset(stack))?material+"_darker":material;
    }
    /** Actual texture selection, shared by drawing and metadata-to-pattern regression tests. */
    public static String sourcePath(ItemStack stack,boolean icon) {
        NBTTagCompound trim=ModernSmithingRecipes.trim(stack);
        if(trim==null || !(stack.getItem() instanceof ItemArmor))return null;
        int type=((ItemArmor)stack.getItem()).armorType;
        if(type<0 || type>3)return null;
        String pattern=trim.getString("pattern").replace("minecraft:","");
        return icon?"textures/trims/items/"+new String[]{"helmet","chestplate","leggings","boots"}[type]+"_trim.png":"textures/trims/entity/"+(type==2?"humanoid_leggings/":"humanoid/")+pattern+".png";
    }
    private static ResourceLocation texture(ItemStack stack,boolean icon) {
        String source=sourcePath(stack,icon);if(source==null)return null;
        String palette=palette(stack,ModernSmithingRecipes.trim(stack));
        String id=source+"/"+palette;if(CACHE.containsKey(id))return CACHE.get(id);if(FAILED.contains(id))return null;
        try {
            BufferedImage image=recolor(read(source),read("textures/trims/color_palettes/trim_palette.png"),read("textures/trims/color_palettes/"+palette+".png"));
            ResourceLocation target=Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("efr_trim",new DynamicTexture(image));CACHE.put(id,target);return target;
        }catch(IOException failure) { FAILED.add(id);ganymedes01.etfuturum.core.utils.Logger.warn("Unable to resolve armor trim "+id+": "+failure.getMessage());return null; }
    }
    public static void renderArmor(EntityLivingBase entity,int pass,ModelBase model,float limb,float amount,float age,float yaw,float pitch,float scale) {
        ItemStack stack=entity.getEquipmentInSlot(4-pass);if(stack==null || !(model instanceof ModelBiped) || !(stack.getItem() instanceof ItemArmor) || ((ItemArmor)stack.getItem()).armorType!=pass)return;
        ResourceLocation texture=texture(stack,false);if(texture==null)return;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT|GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT|GL11.GL_CURRENT_BIT|GL11.GL_TEXTURE_BIT);
        try { Minecraft.getMinecraft().getTextureManager().bindTexture(texture);GL11.glColor4f(1,1,1,1);GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);GL11.glDepthFunc(GL11.GL_LEQUAL);model.render(entity,limb,amount,age,yaw,pitch,scale); }
        finally { GL11.glPopAttrib(); }
    }
    public static void renderIcon(ItemStack stack,int x,int y,float zLevel) {
        ResourceLocation texture=texture(stack,true);if(texture==null)return;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT|GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT|GL11.GL_CURRENT_BIT|GL11.GL_TEXTURE_BIT);
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(0,0,zLevel+0.01F);
            GL11.glDepthFunc(GL11.GL_LEQUAL);GL11.glDepthMask(false);
            Minecraft.getMinecraft().getTextureManager().bindTexture(texture);GL11.glDisable(GL11.GL_LIGHTING);GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);GL11.glColor4f(1,1,1,1);
            net.minecraft.client.gui.Gui.func_146110_a(x,y,0,0,16,16,16,16);
        }finally { GL11.glPopMatrix();GL11.glPopAttrib(); }
    }
    @Override public void onResourceManagerReload(IResourceManager manager) { for(ResourceLocation texture:CACHE.values())Minecraft.getMinecraft().getTextureManager().deleteTexture(texture);CACHE.clear();FAILED.clear(); }
}
