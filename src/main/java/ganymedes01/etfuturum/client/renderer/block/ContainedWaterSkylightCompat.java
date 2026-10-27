package ganymedes01.etfuturum.client.renderer.block;

import ganymedes01.etfuturum.core.utils.Logger;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

/** Retain vanilla liquid skylight while Angelica computes AO and block lighting. */
public final class ContainedWaterSkylightCompat {
    private static final AtomicBoolean WARNED = new AtomicBoolean();
    private static final class Holder { private static final Bridge BRIDGE = Bridge.find(); }
    private ContainedWaterSkylightCompat() { }

    /** Four sky bytes plus a validity bit. No per-quad allocation or world mutation. */
    public static long capture(Object builder) {
        if (!ContainedWaterShaderCompat.isWaterFluidContext(builder)) return 0;
        Bridge bridge = Holder.BRIDGE;
        if (bridge == null || !bridge.builder.isInstance(builder)) return 0;
        try {
            Object[] vertices = (Object[]) bridge.vertices.get(builder);
            if (vertices == null || vertices.length < 4) return 0;
            long packed = 1L << 32;
            for (int i = 0; i < 4; i++)
                packed |= (long) ((bridge.light.getInt(vertices[i]) >>> 16) & 255) << (i * 8);
            return packed;
        } catch (ReflectiveOperationException | RuntimeException failure) { warn(failure); return 0; }
    }

    public static void restore(Object builder, long sky) {
        if ((sky & (1L << 32)) == 0) return;
        Bridge bridge = Holder.BRIDGE;
        if (bridge == null) return;
        try {
            int[] lightmap = (int[]) bridge.lm.get(bridge.quadLightData.get(builder));
            if (lightmap == null || lightmap.length < 4) return;
            for (int i = 0; i < 4; i++)
                lightmap[i] = (lightmap[i] & ~0x00FF0000) | ((int) ((sky >>> (i * 8)) & 255) << 16);
        } catch (ReflectiveOperationException | RuntimeException failure) { warn(failure); }
    }

    private static void warn(Throwable failure) {
        if (WARNED.compareAndSet(false, true)) Logger.warn("Optional water skylight compatibility unavailable: " + failure);
    }

    private static final class Bridge {
        final Class<?> builder;
        final Field vertices, light, quadLightData, lm;
        Bridge(Class<?> builder, Field vertices, Field light, Field quadLightData, Field lm) {
            this.builder = builder; this.vertices = vertices; this.light = light;
            this.quadLightData = quadLightData; this.lm = lm;
        }
        static Bridge find() {
            try {
                Class<?> builder = Class.forName("com.gtnewhorizons.angelica.rendering.celeritas.AngelicaChunkBuildContext", false,
                        ContainedWaterSkylightCompat.class.getClassLoader());
                Field vertices = builder.getDeclaredField("vertices");
                Field quad = builder.getDeclaredField("quadLightData");
                Field light = vertices.getType().getComponentType().getField("light");
                Field lm = quad.getType().getField("lm");
                vertices.setAccessible(true); quad.setAccessible(true);
                return new Bridge(builder, vertices, light, quad, lm);
            } catch (ReflectiveOperationException | LinkageError | RuntimeException olderAngelica) {
                warn(olderAngelica); return null;
            }
        }
    }
}
