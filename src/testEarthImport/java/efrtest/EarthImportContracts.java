package efrtest;

import ganymedes01.etfuturum.api.imported.ImportedBiomeVolume;
import ganymedes01.etfuturum.world.imported.ImportedWorldIdentity;
import net.minecraft.nbt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public final class EarthImportContracts {
    public static void check(boolean result, String message) { if (!result) throw new AssertionError(message); }
    private static void rejects(Runnable action, String message) {
        try { action.run(); } catch (IllegalArgumentException expected) { return; }
        throw new AssertionError(message);
    }
    public static ImportedBiomeVolume fixture() {
        Map<Integer, ImportedBiomeVolume.Section> sections = new LinkedHashMap<>();
        for (int y = -4; y <= 19; y++) {
            byte[] indices = new byte[64];
            String[] names = new String[64];
            for (int i = 0; i < 64; i++) {indices[i] = (byte)i; names[i] = "probe:b_" + i;}
            sections.put(y, new ImportedBiomeVolume.Section(names, indices));
        }
        return new ImportedBiomeVolume(sections);
    }
    public static void main(String[] args) throws Exception {
        NtmTextureContracts.run();
        ganymedes01.etfuturum.world.imported.ImportedWaterProfiles.Profile ocean=ganymedes01.etfuturum.world.imported.ImportedWaterProfiles.named("minecraft:ocean");
        check(ocean.waterColor==0x3f76e4 && ocean.fogColor==0x050533 && ocean.start==-8F && ocean.end==96F,"official ocean water defaults");
        check(ganymedes01.etfuturum.world.imported.ImportedWaterProfiles.named("minecraft:swamp").end<ocean.end,"swamp reduced visibility");
        check(ganymedes01.etfuturum.world.imported.ImportedWaterProfiles.named("minecraft:warm_ocean").fogColor==0x041f33,"warm ocean fog");
        check(ganymedes01.etfuturum.world.imported.ImportedWaterProfiles.named("unknown:biome")==ganymedes01.etfuturum.world.imported.ImportedWaterProfiles.named("minecraft:plains"),"unknown water fallback");
        ImportedBiomeVolume volume = fixture();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        CompressedStreamTools.writeCompressed(volume.toNBT(), bytes);
        ImportedBiomeVolume reload = ImportedBiomeVolume.fromNBT(CompressedStreamTools.readCompressed(new ByteArrayInputStream(bytes.toByteArray())));
        for (int y = 0; y < 384; y += 4) for (int z = 0; z < 16; z += 4) for (int x = 0; x < 16; x += 4)
            check(reload.sourceAt(x,y,z).equals("probe:b_" + (x/4 + 4*(z/4) + 16*((y&15)/4))), "quart roundtrip/axis order");
        check(reload.sourceAt(15,383,15).equals("probe:b_63"), "top edge");
        check(reload.sourceAt(0,-1,0)==null && reload.sourceAt(0,384,0)==null, "out-of-range Y");
        check(new ImportedBiomeVolume(new LinkedHashMap<>()).sourceAt(0,0,0)==null, "sparse volume fallback");
        String[] names={"custom:unknown_biome"}; byte[] indices=new byte[64];
        Map<Integer,ImportedBiomeVolume.Section> section=new LinkedHashMap<>(); section.put(-4,new ImportedBiomeVolume.Section(names,indices));
        names[0]="minecraft:plains";indices[0]=63;
        ImportedBiomeVolume unknown=new ImportedBiomeVolume(section);section.clear();
        check(ImportedBiomeVolume.fromNBT(unknown.toNBT()).sourceAt(0,0,0).equals("custom:unknown_biome"), "defensive copies / unknown identity lost");
        rejects(()->new ImportedBiomeVolume.Section(new String[]{"bad name"},new byte[64]),"invalid name accepted");
        rejects(()->new ImportedBiomeVolume.Section(new String[]{"minecraft:plains"},new byte[63]),"bad sample count");
        byte[] invalid=new byte[64];invalid[63]=(byte)255;
        rejects(()->new ImportedBiomeVolume.Section(new String[]{"minecraft:plains"},invalid),"unsigned bad index");
        NBTTagCompound bad=volume.toNBT();bad.setInteger("Version",2);
        rejects(()->ImportedBiomeVolume.fromNBT(bad),"future schema accepted");
        NBTTagCompound shifted=volume.toNBT();shifted.setInteger("YOffset",0);
        rejects(()->ImportedBiomeVolume.fromNBT(shifted),"wrong vertical transform accepted");
        NBTTagCompound duplicate=volume.toNBT();NBTTagList list=duplicate.getTagList("Sections",10);list.appendTag(list.getCompoundTagAt(0).copy());
        rejects(()->ImportedBiomeVolume.fromNBT(duplicate),"duplicate section accepted");
        NBTTagCompound wrong=volume.toNBT();wrong.getTagList("Sections",10).getCompoundTagAt(0).setString("Y","-4");
        rejects(()->ImportedBiomeVolume.fromNBT(wrong),"wrong NBT type accepted");
        NBTTagCompound identity=new NBTTagCompound();identity.setInteger("Version",1);identity.setInteger("Height",384);identity.setInteger("YOffset",64);
        NBTTagList allocation=new NBTTagList();NBTTagCompound mapped=new NBTTagCompound();mapped.setString("Source","minecraft:cherry_grove");mapped.setInteger("ID",210);allocation.appendTag(mapped);identity.setTag("Biomes",allocation);
        ImportedWorldIdentity saved=new ImportedWorldIdentity("test");saved.readFromNBT(identity);
        Map<String,Integer> actual=new LinkedHashMap<>();actual.put("minecraft:cherry_grove",210);saved.validate(actual);
        actual.put("minecraft:cherry_grove",211);boolean changed=false;try {saved.validate(actual);}catch(IllegalStateException expected){changed=true;}
        check(changed,"changed saved biome ID accepted");
        actual.put("minecraft:cherry_grove",210);actual.put("minecraft:the_void",226);saved.validate(actual);
        NBTTagCompound expanded=new NBTTagCompound();saved.writeToNBT(expanded);
        ImportedWorldIdentity expandedReload=new ImportedWorldIdentity("test");expandedReload.readFromNBT(expanded);expandedReload.validate(actual);
        Map<String,Integer> reused=new LinkedHashMap<>(actual);reused.put("minecraft:new_biome",210);
        boolean collision=false;try{expandedReload.validate(reused);}catch(IllegalStateException expected){collision=true;}
        check(collision,"additive biome allocation reused a saved ID");
        Map<String,Integer> removed=new LinkedHashMap<>(actual);removed.remove("minecraft:cherry_grove");
        boolean disabled=false;try{expandedReload.validate(removed);}catch(IllegalStateException expected){disabled=true;}
        check(disabled,"additive allocation allowed removal of a saved identity");
        System.out.println("EARTH_IMPORT_CONTRACTS PASSED: all 1536 quarts, 24 sections, compressed NBT, malformed data, unknown names, sparse fallback, immutable storage");
    }
}
