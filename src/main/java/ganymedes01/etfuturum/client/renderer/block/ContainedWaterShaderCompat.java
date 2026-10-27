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
    private static final ThreadLocal<FluidScope> ACTIVE_FLUID = new ThreadLocal<>();
    private static final Scope NONE = new Scope(null, null);

    private ContainedWaterShaderCompat() { }

    public static Scope water(Object tessellator) {
        if (BRIDGE == null || !BRIDGE.tessellatorType.isInstance(tessellator)) return NONE;
        try {
            Short mapped = waterId();
            if (mapped == null) return NONE;
            short id = mapped.shortValue();
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

    /** Resolve the current pack, never a hard-coded shader material ID. */
    private static Short waterId() throws Throwable {
        if (BRIDGE == null) return null;
        // Shader reloads replace this map.
        Map<?, ?> mappings = (Map<?, ?>) BRIDGE.mappings.invokeExact();
        if (mappings == null) return null;
        Short still = sourceWaterId(mappings.get(Blocks.water));
        return still != null ? still : sourceWaterId(mappings.get(Blocks.flowing_water));
    }

    private static Short sourceWaterId(Object values) {
        if (!(values instanceof Map)) return null;
        Object value = ((Map<?, ?>) values).get(Integer.valueOf(0));
        if (!(value instanceof Number) || ((Number) value).shortValue() == -1) return null;
        return Short.valueOf(((Number) value).shortValue());
    }

    /**
     * Angelica encodes the completed block after renderBlockLiquid has returned.
     * Only a batch entirely emitted with our water override may use fluid lighting.
     * Plant/mixed batches and unmapped packs retain the original context.
     */
    public static FluidScope fluidBatch(Object builder, int[] overrides, int vertexCount) {
        if (overrides == null || vertexCount <= 0 || (vertexCount & 3) != 0
                || vertexCount > overrides.length || overrides[0] == -1) return FluidScope.NONE;
        try {
            Short mapped = waterId();
            if (mapped == null) return FluidScope.NONE;
            short id = mapped.shortValue();
            for (int i = 0; i < vertexCount; i++) if (overrides[i] != id) return FluidScope.NONE;
            FluidBridge bridge = FluidBridgeHolder.BRIDGE;
            if (bridge == null || !bridge.builderType.isInstance(builder)) return FluidScope.NONE;
            Object context = (Object) bridge.context.invokeExact(builder);
            short previous = (short) bridge.getType.invokeExact(context);
            // prepareToRenderFluid uses renderType=1, independently of the block's render ID.
            bridge.setType.invokeExact(context, (short) 1);
            return new FluidScope(context, previous, bridge);
        } catch (Throwable failure) { warn(failure); return FluidScope.NONE; }
    }

    /** Only verified contained-water scopes or native water IDs may retain liquid skylight. */
    public static boolean isWaterFluidContext(Object builder) {
        try {
            FluidBridge bridge = FluidBridgeHolder.BRIDGE;
            if (bridge == null || !bridge.builderType.isInstance(builder)) return false;
            Object context = (Object) bridge.context.invokeExact(builder);
            if ((short) bridge.getType.invokeExact(context) != 1) return false;
            FluidScope active = ACTIVE_FLUID.get();
            if (active != null && active.context == context && !active.closed) return true;
            Short mapped = waterId();
            return mapped != null && (short) bridge.getId.invokeExact(context) == mapped.shortValue();
        } catch (Throwable failure) { warn(failure); return false; }
    }

    public static final class FluidScope implements AutoCloseable {
        private static final FluidScope NONE = new FluidScope(null, (short) 0, null);
        private final Object context;
        private final short previous;
        private final FluidBridge bridge;
        private final FluidScope previousScope;
        private boolean closed;
        private FluidScope(Object context, short previous, FluidBridge bridge) {
            this.context = context; this.previous = previous; this.bridge = bridge;
            this.previousScope = context == null ? null : ACTIVE_FLUID.get();
            if (context != null) ACTIVE_FLUID.set(this);
        }
        /** Keep verified water batches in the translucent shader pass, even for opaque atlas pixels. */
        public boolean preservesWaterPass() { return context != null; }
        @Override public void close() {
            if (context == null || closed) return;
            closed = true;
            try { bridge.setType.invokeExact(context, previous); }
            catch (Throwable failure) { warn(failure); }
            finally {
                if (previousScope == null) ACTIVE_FLUID.remove(); else ACTIVE_FLUID.set(previousScope);
            }
        }
    }

    private static final class FluidBridgeHolder { private static final FluidBridge BRIDGE = FluidBridge.find(); }
    private static final class FluidBridge {
        private final Class<?> builderType;
        private final MethodHandle context, getType, setType, getId;
        private FluidBridge(Class<?> builderType, MethodHandle context, MethodHandle getType, MethodHandle setType, MethodHandle getId) {
            this.builderType = builderType; this.context = context; this.getType = getType; this.setType = setType; this.getId = getId;
        }
        private static FluidBridge find() {
            try {
                ClassLoader loader = ContainedWaterShaderCompat.class.getClassLoader();
                Class<?> builder = Class.forName("com.gtnewhorizons.angelica.rendering.celeritas.AngelicaChunkBuildContext", false, loader);
                Class<?> context = Class.forName("com.gtnewhorizons.angelica.rendering.celeritas.iris.BlockRenderContext", false, loader);
                MethodHandles.Lookup lookup = MethodHandles.publicLookup();
                return new FluidBridge(builder,
                    lookup.unreflect(builder.getMethod("getBlockRenderContext")).asType(MethodType.methodType(Object.class, Object.class)),
                    lookup.unreflectGetter(context.getField("renderType")).asType(MethodType.methodType(short.class, Object.class)),
                    lookup.unreflectSetter(context.getField("renderType")).asType(MethodType.methodType(void.class, Object.class, short.class)),
                    lookup.unreflectGetter(context.getField("blockId")).asType(MethodType.methodType(short.class, Object.class)));
            } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException olderAngelica) { return null; }
            catch (ReflectiveOperationException | LinkageError failure) { warn(failure); return null; }
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
