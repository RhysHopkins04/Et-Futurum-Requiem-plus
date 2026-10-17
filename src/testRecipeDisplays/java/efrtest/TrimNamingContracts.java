package efrtest;

import com.google.gson.*;
import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.client.renderer.trim.ArmorTrimRenderer;
import ganymedes01.etfuturum.items.ItemSmithingTemplate;
import ganymedes01.etfuturum.recipes.ModernSmithingRecipes;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import net.minecraft.util.*;
import java.io.*;
import java.util.*;
import java.util.zip.ZipFile;

/** Actual localized names, tooltips and template-to-recipe-to-renderer paths against vanilla data. */
public final class TrimNamingContracts {
    private static void check(boolean value,String why) { if(!value)throw new AssertionError(why); }
    private static JsonObject json(InputStream input) throws Exception {
        try(InputStream stream=input) { return new JsonParser().parse(new InputStreamReader(stream,"UTF-8")).getAsJsonObject(); }
    }
    public static void run() throws Exception {
        String path=System.getProperty("efr.test.vanillaJar");
        if(path==null) { System.out.println("TRIM_NAMING SKIPPED: provide smithingReferenceJar");return; }
        Map<String,String> translations=new HashMap<>();
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(TrimNamingContracts.class.getResourceAsStream("/assets/etfuturum/lang/en_US.lang"),"UTF-8"))) {
            String line;while((line=reader.readLine())!=null) {
                int equals=line.indexOf('=');if(equals>0 && !line.startsWith("#"))translations.put(line.substring(0,equals),line.substring(equals+1));
            }
        }
        // Only this disposable headless process's translation map is replaced.
        StringTranslate.replaceWith(translations);
        ItemSmithingTemplate item=(ItemSmithingTemplate)ModItems.SMITHING_TEMPLATE.get();
        Set<String> names=new HashSet<>(),wornPaths=new HashSet<>(),patternImages=new HashSet<>();int routes=0;
        try(ZipFile vanilla=new ZipFile(path)) {
            JsonObject language=json(vanilla.getInputStream(vanilla.getEntry("assets/minecraft/lang/en_us.json")));
            JsonObject catalog=json(TrimNamingContracts.class.getResourceAsStream("/assets/etfuturum/recipes/smithing_1_21_11.json"));
            for(JsonElement entry:catalog.getAsJsonArray("templates")) {
                JsonObject row=entry.getAsJsonObject();int meta=row.get("meta").getAsInt();String id=row.get("name").getAsString();
                ItemStack template=new ItemStack(item,1,meta);String expected=language.get("item.minecraft."+id+".new").getAsString();
                check(item.getItemStackDisplayName(template).equals(expected),"wrong template name at metadata "+meta+": "+item.getItemStackDisplayName(template));
                check(names.add(expected),"templates share an indistinguishable name");
                EnumRarity rarity=id.startsWith("silence_")?EnumRarity.epic:
                        id.startsWith("ward_") || id.startsWith("eye_") || id.startsWith("vex_") || id.startsWith("spire_")?EnumRarity.rare:EnumRarity.uncommon;
                check(item.getRarity(template)==rarity,"wrong vanilla template name color/rarity: "+id);
                List<String> tooltip=new ArrayList<>();item.addInformation(template,null,tooltip,false);
                String prefix="item.minecraft.smithing_template."+(meta==0?"netherite_upgrade":"armor_trim");
                check(tooltip.size()==6 && EnumChatFormatting.getTextWithoutFormattingCodes(tooltip.get(0)).equals(language.get("item.minecraft.smithing_template").getAsString()),"missing Smithing Template subtitle");
                check(tooltip.get(1).isEmpty() && EnumChatFormatting.getTextWithoutFormattingCodes(tooltip.get(2)).equals(language.get("item.minecraft.smithing_template.applies_to").getAsString()),"missing applies-to heading");
                check(EnumChatFormatting.getTextWithoutFormattingCodes(tooltip.get(3)).equals(" "+language.get(prefix+".applies_to").getAsString()),"wrong template applicability");
                check(EnumChatFormatting.getTextWithoutFormattingCodes(tooltip.get(4)).equals(language.get("item.minecraft.smithing_template.ingredients").getAsString()) && EnumChatFormatting.getTextWithoutFormattingCodes(tooltip.get(5)).equals(" "+language.get(prefix+".ingredients").getAsString()),"wrong ingredient tooltip");
                if(meta==0)continue;
                String pattern=id.replace("_armor_trim_smithing_template","");
                check(translations.get("efr.trim."+pattern).equals(language.get("trim_pattern.minecraft."+pattern).getAsString()),"applied trim pattern named incorrectly");
                JsonObject definition=json(vanilla.getInputStream(vanilla.getEntry("data/minecraft/trim_pattern/"+pattern+".json")));
                String asset=definition.get("asset_id").getAsString().replace("minecraft:","");
                Item[] armor={Items.iron_helmet,Items.iron_chestplate,Items.iron_leggings,Items.iron_boots};
                for(int type=0;type<armor.length;type++) {
                    ItemStack result=ModernSmithingRecipes.result(template,new ItemStack(armor[type]),new ItemStack(Items.gold_ingot),null);
                    check(result!=null && ModernSmithingRecipes.trim(result).getString("pattern").equals("minecraft:"+pattern),"named template produces a different trim");
                    String actual=ArmorTrimRenderer.sourcePath(result,false),expectedPath="textures/trims/entity/"+(type==2?"humanoid_leggings/":"humanoid/")+asset+".png";
                    check(expectedPath.equals(actual),"named trim selects wrong worn texture");wornPaths.add(actual);
                    String icon=ArmorTrimRenderer.sourcePath(result,true);
                    check(icon.equals("textures/trims/items/"+new String[]{"helmet","chestplate","leggings","boots"}[type]+"_trim.png"),"wrong inventory trim mask");
                    check(vanilla.getEntry("assets/minecraft/"+actual)!=null && vanilla.getEntry("assets/minecraft/"+icon)!=null,"selected trim art missing");routes++;
                }
                try(InputStream pixels=vanilla.getInputStream(vanilla.getEntry("assets/minecraft/textures/trims/entity/humanoid/"+asset+".png"))) {
                    java.security.MessageDigest digest=java.security.MessageDigest.getInstance("SHA-256");byte[] buffer=new byte[4096];int n;while((n=pixels.read(buffer))>=0)digest.update(buffer,0,n);
                    patternImages.add(Arrays.toString(digest.digest()));
                }
            }
            for(String material:ModernSmithingRecipes.MATERIALS)check(translations.get("efr.trim.material."+material).equals(language.get("trim_material.minecraft."+material).getAsString()),"trim material named incorrectly");
        }
        check(names.size()==19 && wornPaths.size()==36 && routes==72 && patternImages.size()==18,"missing template names or unique worn trim patterns");
        System.out.println("TRIM_NAMING PASSED: 19 actual localized names/tooltips, 18 distinct pattern images, 72 template-to-recipe-to-renderer routes and all pattern/material labels match vanilla");
    }
}
