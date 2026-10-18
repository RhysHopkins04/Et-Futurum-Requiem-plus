package efrtest;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Random;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.*;
import static efrtest.EarthImportContracts.check;

/** Exercise transformed vanilla foundation sampling below and above the imported spawn reference. */
final class ImportedStructureGroundProbe {
    static void run(WorldServer world) throws Exception {
        check(world.provider.getAverageGroundLevel()==128,"imported spawn reference changed");
        Field bounds=StructureComponent.class.getDeclaredField("boundingBox");bounds.setAccessible(true);
        Method village=StructureVillagePieces.Village.class.getDeclaredMethod("getAverageGroundLevel",World.class,StructureBoundingBox.class);
        village.setAccessible(true);
        Class<?> feature=Class.forName("net.minecraft.world.gen.structure.ComponentScatteredFeaturePieces$Feature");
        Method scattered=feature.getDeclaredMethod("func_74935_a",World.class,StructureBoundingBox.class,int.class);
        scattered.setAccessible(true);
        for(int surface:new int[]{64,180}) {
            for(int x=320;x<=322;x++) for(int z=320;z<=322;z++) {
                for(int y=surface;y<world.getHeight();y++)
                    if(!world.isAirBlock(x,y,z))world.setBlock(x,y,z,Blocks.air,0,2);
                world.setBlock(x,surface-1,z,Blocks.stone,0,2);
            }
            StructureBoundingBox clip=new StructureBoundingBox(320,0,320,335,383,335);
            StructureVillagePieces.House1 house=new StructureVillagePieces.House1();
            bounds.set(house,new StructureBoundingBox(320,64,320,322,69,322));
            check(((Integer)village.invoke(house,world,clip))==surface,"village raised above actual terrain at "+surface);
            ComponentScatteredFeaturePieces.JunglePyramid temple=new ComponentScatteredFeaturePieces.JunglePyramid(new Random(1),320,320);
            bounds.set(temple,new StructureBoundingBox(320,64,320,322,69,322));
            check((Boolean)scattered.invoke(temple,world,clip,0),"temple failed to sample terrain");
            check(temple.getBoundingBox().minY==surface,"temple raised above actual terrain at "+surface);
            if(cpw.mods.fml.common.Loader.isModLoaded("hbm")) {
                Class<?> component=Class.forName("com.hbm.world.gen.component.Component");
                Class<?> houseType=Class.forName("com.hbm.world.gen.component.CivilianFeatures$NTMHouse1");
                Object ntm=houseType.getConstructor().newInstance();
                StructureBoundingBox area=new StructureBoundingBox(320,64,320,322,69,322);
                bounds.set(ntm,area);
                Method mean=component.getDeclaredMethod("getAverageHeight",World.class,StructureBoundingBox.class,StructureBoundingBox.class,int.class);
                mean.setAccessible(true);
                check(((Integer)mean.invoke(null,world,area,clip,64))==surface,"NTM static ground clamp at "+surface);
                Method align=component.getDeclaredMethod("setAverageHeight",World.class,StructureBoundingBox.class,int.class);
                align.setAccessible(true);
                check((Boolean)align.invoke(ntm,world,clip,64),"NTM ground sampling failed");
                check(((StructureComponent)ntm).getBoundingBox().minY==surface,"NTM foundation shifted above terrain at "+surface);
                boolean imported=ganymedes01.etfuturum.configuration.configs.ConfigMapCompatibility.importedEarthMode;
                try {
                    ganymedes01.etfuturum.configuration.configs.ConfigMapCompatibility.importedEarthMode=false;
                    check(((Integer)mean.invoke(null,world,area,clip,64))==Math.max(surface,world.provider.getAverageGroundLevel()),"NTM original height policy changed outside imported mode");
                } finally {ganymedes01.etfuturum.configuration.configs.ConfigMapCompatibility.importedEarthMode=imported;}
                System.out.println("NTM_STRUCTURE_GROUND PASSED at Y"+surface+": static and instance native samplers, config-off policy retained");
            }
        }
        System.out.println("IMPORTED_STRUCTURE_GROUND PASSED: native village/temple sampling at Y64 and Y180, spawn reference unchanged");
    }
}
