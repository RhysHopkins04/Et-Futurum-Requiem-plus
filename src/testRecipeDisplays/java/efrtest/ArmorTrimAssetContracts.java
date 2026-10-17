package efrtest;

import ganymedes01.etfuturum.client.renderer.trim.ArmorTrimRenderer;
import ganymedes01.etfuturum.client.ModernAssetResourcePack;
import ganymedes01.etfuturum.Tags;
import net.minecraft.util.ResourceLocation;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.zip.ZipFile;
import java.io.*;
import java.util.*;

public final class ArmorTrimAssetContracts {
    private static void check(boolean ok,String why) { if(!ok)throw new AssertionError(why); }
    private static BufferedImage image(ZipFile zip,String path) throws Exception { try(InputStream input=zip.getInputStream(zip.getEntry("assets/minecraft/"+path))) { return ArmorTrimRenderer.decode(input); } }
    public static void run() throws Exception {
        String path=System.getProperty("efr.test.vanillaJar");if(path==null) { System.out.println("ARMOR_TRIM_ASSETS SKIPPED: pass -PsmithingReferenceJar for pinned-asset coverage");return; }
        java.security.MessageDigest digest=java.security.MessageDigest.getInstance("SHA-1");try(InputStream stream=new FileInputStream(path)) { byte[] buffer=new byte[65536];int n;while((n=stream.read(buffer))>=0)digest.update(buffer,0,n); }
        StringBuilder sha=new StringBuilder();for(byte b:digest.digest())sha.append(String.format("%02x",b&255));check(sha.toString().equals("ba2df812c2d12e0219c489c4cd9a5e1f0760f5bd"),"wrong official reference jar");
        java.lang.reflect.Method select=ArmorTrimRenderer.class.getDeclaredMethod("palette",net.minecraft.item.ItemStack.class,net.minecraft.nbt.NBTTagCompound.class);select.setAccessible(true);
        net.minecraft.nbt.NBTTagCompound trim=new net.minecraft.nbt.NBTTagCompound();trim.setString("material","minecraft:iron");
        check(select.invoke(null,new net.minecraft.item.ItemStack(net.minecraft.init.Items.iron_chestplate),trim).equals("iron_darker"),"matching iron armor missed darker palette");
        check(select.invoke(null,new net.minecraft.item.ItemStack(net.minecraft.init.Items.chainmail_chestplate),trim).equals("iron"),"chainmail incorrectly uses iron armor's darker palette");
        trim.setString("material","minecraft:gold");
        check(select.invoke(null,new net.minecraft.item.ItemStack(net.minecraft.init.Items.golden_chestplate),trim).equals("gold_darker"),"matching gold armor missed darker palette");
        check(select.invoke(null,new net.minecraft.item.ItemStack(net.minecraft.init.Items.iron_chestplate),trim).equals("gold"),"gold on iron uses wrong palette");
        trim.setString("material","minecraft:netherite");
        check(select.invoke(null,ganymedes01.etfuturum.ModItems.NETHERITE_CHESTPLATE.newItemStack(),trim).equals("netherite_darker"),"EFR netherite material name missed darker palette");
        int permutations=0;
        try(ZipFile zip=new ZipFile(path)) {
            String[] equipment={"leather","chainmail","iron","gold","diamond","netherite"};
            for(String material:ganymedes01.etfuturum.recipes.ModernSmithingRecipes.MATERIALS) {
                com.google.gson.JsonObject data;try(InputStream json=zip.getInputStream(zip.getEntry("data/minecraft/trim_material/"+material+".json"))) { data=new com.google.gson.JsonParser().parse(new InputStreamReader(json,"UTF-8")).getAsJsonObject(); }
                trim.setString("material","minecraft:"+material);
                for(int i=0;i<equipment.length;i++) {
                    String expected=data.get("asset_name").getAsString();
                    if(data.has("override_armor_assets") && data.getAsJsonObject("override_armor_assets").has("minecraft:"+equipment[i]))expected=data.getAsJsonObject("override_armor_assets").get("minecraft:"+equipment[i]).getAsString();
                    for(String slot:new String[]{"helmet","chestplate","leggings","boots"}) {
                        net.minecraft.item.Item armor=equipment[i].equals("netherite")?ganymedes01.etfuturum.ModItems.valueOf("NETHERITE_"+slot.toUpperCase(java.util.Locale.ROOT)).get():(net.minecraft.item.Item)net.minecraft.init.Items.class.getField((equipment[i].equals("gold")?"golden":equipment[i])+"_"+slot).get(null);
                        check(select.invoke(null,new net.minecraft.item.ItemStack(armor),trim).equals(expected),"palette differs from vanilla trim material JSON for "+equipment[i]+" "+slot+" "+material);
                    }
                }
            }
            BufferedImage key=image(zip,"textures/trims/color_palettes/trim_palette.png");
            try(InputStream raw=zip.getInputStream(zip.getEntry("assets/minecraft/textures/trims/color_palettes/trim_palette.png"))) {
                BufferedImage decodedByImageIO=ImageIO.read(raw);int differences=0;
                for(int y=0;y<key.getHeight();y++)for(int x=0;x<key.getWidth();x++) {
                    int gray=decodedByImageIO.getRaster().getSample(x,y,0),expected=(gray<<16)|(gray<<8)|gray;
                    check((key.getRGB(x,y)&0xffffff)==expected,"grayscale trim palette was gamma-converted");
                    if(key.getRGB(x,y)!=decodedByImageIO.getRGB(x,y))differences++;
                }
                check(differences>0,"reference no longer exercises ImageIO grayscale conversion");
            }
            List<String> sources=new ArrayList<>();for(String name:ganymedes01.etfuturum.items.ItemSmithingTemplate.NAMES)if(name.contains("_armor_trim_"))for(String layer:new String[]{"humanoid","humanoid_leggings"})sources.add("textures/trims/entity/"+layer+"/"+name.replace("_armor_trim_smithing_template","")+".png");
            for(String armor:new String[]{"helmet","chestplate","leggings","boots"})sources.add("textures/trims/items/"+armor+"_trim.png");
            List<String> palettes=new ArrayList<>(Arrays.asList(ganymedes01.etfuturum.recipes.ModernSmithingRecipes.MATERIALS));palettes.addAll(Arrays.asList("iron_darker","gold_darker","diamond_darker","netherite_darker","copper_darker"));
            for(String source:sources)for(String palette:palettes) {
                BufferedImage input=image(zip,source),colors=image(zip,"textures/trims/color_palettes/"+palette+".png"),out=ArmorTrimRenderer.recolor(input,key,colors);check(out.getWidth()==input.getWidth() && out.getHeight()==input.getHeight(),"trim dimensions changed");int visible=0;
                for(int y=0;y<input.getHeight();y++)for(int x=0;x<input.getWidth();x++) { int original=input.getRGB(x,y),actual=out.getRGB(x,y);check((original>>>24)==(actual>>>24),"trim alpha lost");if((actual>>>24)>0)visible++; }
                int changed=0;
                for(int y=0;y<input.getHeight();y++)for(int x=0;x<input.getWidth();x++) {
                    int original=input.getRGB(x,y),expected=original;
                    if((original>>>24)==0)expected=0;
                    else for(int i=0;i<key.getWidth()*key.getHeight();i++)if((key.getRGB(i%key.getWidth(),i/key.getWidth())>>>24)!=0 && (key.getRGB(i%key.getWidth(),i/key.getWidth())&0xffffff)==(original&0xffffff)) {
                        int mapped=colors.getRGB(i%colors.getWidth(),i/colors.getWidth());expected=(((original>>>24)*(mapped>>>24)/255)<<24)|(mapped&0xffffff);
                    }
                    check(out.getRGB(x,y)==expected,"wrong palette color or unmapped-pixel behavior: "+source+" "+palette);
                    if((original>>>24)>0 && out.getRGB(x,y)!=original)changed++;
                }
                check(changed>0,"trim did not change to material color: "+source+" "+palette);
                check(visible>0,"empty trim permutation "+source+" "+palette);permutations++;
            }
        }
        BufferedImage input=new BufferedImage(2,1,BufferedImage.TYPE_INT_ARGB),key=new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB),palette=new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB);input.setRGB(0,0,0x80112233);input.setRGB(1,0,0);key.setRGB(0,0,0xff112233);palette.setRGB(0,0,0xffabcdef);
        BufferedImage output=ArmorTrimRenderer.recolor(input,key,palette);check(output.getRGB(0,0)==0x80abcdef && output.getRGB(1,0)==0,"palette mapping/transparent pixel mismatch");
        key.setRGB(0,0,0x00112233);output=ArmorTrimRenderer.recolor(input,key,palette);
        check(output.getRGB(0,0)==input.getRGB(0,0),"transparent key entry incorrectly recolored a pixel");
        java.lang.reflect.Method target=ModernAssetResourcePack.class.getDeclaredMethod("toModernLocation",ResourceLocation.class);target.setAccessible(true);
        for(String name:ganymedes01.etfuturum.items.ItemSmithingTemplate.NAMES) {
            String legacy="textures/items/"+name+".png";check(ModernAssetResourcePack.getAliasedPaths().contains(legacy),"template alias unregistered");ResourceLocation route=(ResourceLocation)target.invoke(null,new ResourceLocation("minecraft",legacy));check(route.getResourceDomain().equals(Tags.MC_ASSET_VER) && route.getResourcePath().equals("textures/item/"+name+".png"),"template art misrouted");
            check(ArmorTrimAssetContracts.class.getResource("/assets/minecraft/"+legacy)==null,"template art copied into resources");
        }
        System.out.println("ARMOR_TRIM_ASSETS PASSED: "+permutations+" actual AssetDirector-reference palette permutations, alpha mapping and all 19 real bridge aliases; no window/GL context");
    }
}
