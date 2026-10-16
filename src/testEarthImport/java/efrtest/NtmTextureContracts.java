package efrtest;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.imageio.ImageIO;
import ganymedes01.etfuturum.client.texture.NtmDeepslateSprite;
import static efrtest.EarthImportContracts.check;

/** Pure CPU texture checks; no client, display or GL context. */
public final class NtmTextureContracts {
    public static void run() throws Exception {
        BufferedImage host=new BufferedImage(2,2,BufferedImage.TYPE_INT_ARGB);
        for(int x=0;x<2;x++)for(int y=0;y<2;y++)host.setRGB(x,y,0xff444444);
        BufferedImage overlay=new BufferedImage(2,2,BufferedImage.TYPE_INT_ARGB);
        overlay.setRGB(0,0,0xffff0000);
        BufferedImage result=NtmDeepslateSprite.compose(host,overlay);
        check(result.getRGB(0,0)==0xffff0000 && result.getRGB(1,1)==0xff444444,"ore overlay/host alpha composition");
        check(host.getRGB(0,0)==0xff444444,"texture source mutated");
        BufferedImage larger=new BufferedImage(4,4,BufferedImage.TYPE_INT_ARGB);
        check(NtmDeepslateSprite.compose(host,larger).getWidth()==4,"resource-pack resolution scaling");
        String[] names={"ore_uranium","ore_asbestos","ore_iron","ore_gold","ore_redstone","ore_lapis","ore_emerald","ore_diamond","cluster_iron","cluster_titanium","cluster_copper","cluster_aluminium"};
        int count=0;
        for(String name:names) {
            try(InputStream stream=NtmTextureContracts.class.getResourceAsStream("/assets/hbm/textures/blocks/"+name+".png")) {
                if(stream==null)continue; // Optional NTM dependency absent in ordinary builds.
                BufferedImage pixels=ImageIO.read(stream);
                BufferedImage composed=NtmDeepslateSprite.compose(host,pixels);
                boolean orePixel=false,hostPixel=false;
                for(int x=0;x<composed.getWidth();x++)for(int y=0;y<composed.getHeight();y++) {
                    int color=composed.getRGB(x,y);
                    check((color>>>24)==255,"transparent hole in ore texture "+name);
                    orePixel|=color!=0xff444444;hostPixel|=color==0xff444444;
                }
                check(orePixel && hostPixel,"native texture lacks ore or exposed host "+name);
                java.io.ByteArrayOutputStream hostBytes=new java.io.ByteArrayOutputStream(),oreBytes=new java.io.ByteArrayOutputStream();
                ImageIO.write(host,"png",hostBytes);ImageIO.write(pixels,"png",oreBytes);
                net.minecraft.client.resources.IResourceManager resources=new net.minecraft.client.resources.IResourceManager() {
                    public java.util.Set getResourceDomains(){return new java.util.HashSet<>(java.util.Arrays.asList("minecraft","hbm"));}
                    public java.util.List getAllResources(net.minecraft.util.ResourceLocation location) throws java.io.IOException {return java.util.Collections.singletonList(getResource(location));}
                    public net.minecraft.client.resources.IResource getResource(net.minecraft.util.ResourceLocation location) {
                        byte[] bytes=location.getResourceDomain().equals("minecraft")?hostBytes.toByteArray():oreBytes.toByteArray();
                        return new net.minecraft.client.resources.IResource() {
                            public InputStream getInputStream(){return new java.io.ByteArrayInputStream(bytes);}
                            public boolean hasMetadata(){return false;}
                            public net.minecraft.client.resources.data.IMetadataSection getMetadata(String key){return null;}
                        };
                    }
                };
                java.lang.reflect.Constructor<NtmDeepslateSprite> constructor=NtmDeepslateSprite.class.getDeclaredConstructor(String.class,String.class);
                constructor.setAccessible(true);
                NtmDeepslateSprite sprite=constructor.newInstance("probe:"+name,name);
                check(!sprite.load(resources,new net.minecraft.util.ResourceLocation("probe",name)),"custom sprite rejected stitching");
                check(sprite.getIconWidth()==composed.getWidth() && sprite.getFrameCount()==1,"atlas dimensions/frame count "+name);
                count++;
            }
        }
        check(count==0 || count==12,"partial NTM texture resources");
        System.out.println("EARTH_IMPORT_NTM_TEXTURES PASSED: alpha, resolution scaling, immutable inputs; "+count+" native overlays");
    }
}
