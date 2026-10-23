package ganymedes01.etfuturum.client.renderer.block;

import ganymedes01.etfuturum.core.utils.Logger;
import net.minecraft.init.Blocks;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/** Optional Angelica 2.x per-vertex material override; no shader-pack IDs are hard-coded. */
public final class ContainedWaterShaderCompat {
    private static final Bridge BRIDGE = Bridge.find();
    private static final AtomicBoolean WARNED = new AtomicBoolean();
    private static final ThreadLocal<Scope> ACTIVE = new ThreadLocal<>();
    private static final Scope NONE = new Scope(null, null);

    private ContainedWaterShaderCompat() { }

    public static Scope water(Object tessellator) {
        if (BRIDGE == null || !BRIDGE.tessellatorType.isInstance(tessellator)) return NONE;
        try {
            // Read the current map on every scope: shader reloads replace this map.
            Map<?, ?> mappings = (Map<?, ?>) BRIDGE.mappings.invokeExact();
            if (mappings == null) return NONE;
            Object values = mappings.get(Blocks.water);
            if (!(values instanceof Map)) values = mappings.get(Blocks.flowing_water);
            if (!(values instanceof Map)) return NONE;
            Object value = ((Map<?, ?>) values).get(Integer.valueOf(0));
            if (!(value instanceof Number)) return NONE;
            short id = ((Number) value).shortValue();
            if (id == -1) return NONE;
            Scope scope = new Scope(tessellator, ACTIVE.get());
            scope.id = id;
            BRIDGE.override.invokeExact(tessellator, id);
            ACTIVE.set(scope);
            return scope;
        } catch (Throwable failure) {
            warn(failure);
            return NONE;
        }
    }

    private static void warn(Throwable failure) {
        if (WARNED.compareAndSet(false, true))
            Logger.warn("Contained-water shader material override unavailable: " + failure);
    }

    public static final class Scope implements AutoCloseable {
        private final Object tessellator;
        private final Scope previous;
        private short id;
        private boolean closed;
        private Scope(Object tessellator, Scope previous) { this.tessellator = tessellator; this.previous = previous; }
        @Override public void close() {
            if (tessellator == null || closed) return;
            closed = true;
            try {
                // -1 is Angelica's normal "use the enclosing block material" sentinel.
                short restore = previous != null && previous.tessellator == tessellator ? previous.id : -1;
                BRIDGE.override.invokeExact(tessellator, restore);
            } catch (Throwable failure) { warn(failure); }
            finally {
                if (previous == null) ACTIVE.remove(); else ACTIVE.set(previous);
            }
        }
    }

    private static final class Bridge {
        private final Class<?> tessellatorType;
        private final MethodHandle mappings;
        private final MethodHandle override;
        private Bridge(Class<?> type, MethodHandle mappings, MethodHandle override) {
            this.tessellatorType = type; this.mappings = mappings; this.override = override;
        }
        private static Bridge find() {
            try {
                ClassLoader loader = ContainedWaterShaderCompat.class.getClassLoader();
                Class<?> type = Class.forName("com.gtnewhorizons.angelica.rendering.StateAwareTessellator", false, loader);
                Class<?> settings = Class.forName("net.coderbot.iris.block_rendering.BlockRenderingSettings", false, loader);
                Object instance = settings.getField("INSTANCE").get(null);
                MethodHandles.Lookup lookup = MethodHandles.publicLookup();
                MethodHandle mappings = lookup.unreflect(settings.getMethod("getBlockMetaMatches"))
                        .bindTo(instance).asType(MethodType.methodType(Map.class));
                MethodHandle override = lookup.unreflect(type.getMethod("angelica$setShaderOverrideBlockId", short.class))
                        .asType(MethodType.methodType(void.class, Object.class, short.class));
                return new Bridge(type, mappings, override);
            } catch (ClassNotFoundException | NoSuchMethodException absentOrOlderAngelica) {
                return null;
            } catch (ReflectiveOperationException | LinkageError failure) {
                Logger.warn("Could not initialize optional contained-water shader support: " + failure);
                return null;
            }
        }
    }
}
