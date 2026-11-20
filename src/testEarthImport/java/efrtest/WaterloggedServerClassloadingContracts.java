package efrtest;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.jar.JarFile;

import cpw.mods.fml.relauncher.FMLLaunchHandler;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.launchwrapper.IClassTransformer;

/** Runs the supplied Crucible side transformer against the actual packaged handler. */
public final class WaterloggedServerClassloadingContracts {
    private static final String HANDLER = "ganymedes01.etfuturum.network.WaterloggedChunkHandler";

    public static void main(String[] args) throws Exception {
        Field side = FMLLaunchHandler.class.getDeclaredField("side");
        side.setAccessible(true);
        side.set(null, Side.SERVER);
        URL server = Paths.get(args[0]).toUri().toURL();
        try (URLClassLoader crucible = new URLClassLoader(new URL[] {server},
                WaterloggedServerClassloadingContracts.class.getClassLoader()) {
            @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.equals("cpw.mods.fml.common.asm.transformers.SideTransformer")) {
                    Class<?> type = findLoadedClass(name);
                    if (type == null) type = findClass(name);
                    if (resolve) resolveClass(type);
                    return type;
                }
                return super.loadClass(name, resolve);
            }
        }) {
            IClassTransformer transformer = (IClassTransformer) crucible.loadClass(
                    "cpw.mods.fml.common.asm.transformers.SideTransformer").newInstance();
            if (args.length > 2) {
                try {
                    instantiate(args[2], transformer);
                    throw new AssertionError("Old handler unexpectedly loaded without client classes");
                } catch (LinkageError expected) {
                    System.out.println("Previous candidate reproduced client classloading failure: " + expected);
                }
            }
            instantiate(args[1], transformer);
            System.out.println("WATERLOGGED_SERVER_CLASSLOADING PASSED: packaged handler instantiates with client classes forbidden using "
                    + crucible.loadClass("cpw.mods.fml.common.asm.transformers.SideTransformer")
                            .getProtectionDomain().getCodeSource().getLocation());
        }
    }

    private static void instantiate(String archive, IClassTransformer transformer) throws Exception {
        try (JarFile jar = new JarFile(archive)) {
            ClassLoader isolated = new ClassLoader(WaterloggedServerClassloadingContracts.class.getClassLoader()) {
                @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                    if (name.startsWith("net.minecraft.client.") || name.startsWith("ganymedes01.etfuturum.client."))
                        throw new ClassNotFoundException("Client class forbidden on dedicated server: " + name);
                    if (name.startsWith("ganymedes01.etfuturum.network.WaterloggedChunk")) {
                        Class<?> type = findLoadedClass(name);
                        if (type == null) {
                            try (InputStream in = jar.getInputStream(jar.getJarEntry(name.replace('.', '/') + ".class"))) {
                                java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
                                byte[] block = new byte[4096];
                                int count;
                                while ((count = in.read(block)) != -1) buffer.write(block, 0, count);
                                byte[] bytes = transformer.transform(name, name, buffer.toByteArray());
                                type = defineClass(name, bytes, 0, bytes.length);
                            } catch (java.io.IOException e) {
                                throw new ClassNotFoundException(name, e);
                            }
                        }
                        if (resolve) resolveClass(type);
                        return type;
                    }
                    return super.loadClass(name, resolve);
                }
            };
            // Matches SimpleNetworkWrapper.instantiate's reflective construction in the crash report.
            isolated.loadClass(HANDLER).newInstance();
        }
    }
}
