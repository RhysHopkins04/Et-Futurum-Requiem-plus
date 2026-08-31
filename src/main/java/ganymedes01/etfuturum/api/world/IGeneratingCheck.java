package ganymedes01.etfuturum.api.world;

/** Marker mixed into ChunkProviderServer so EFR can distinguish population from later block changes. */
public interface IGeneratingCheck {
    boolean efr$isGenerating();
}
