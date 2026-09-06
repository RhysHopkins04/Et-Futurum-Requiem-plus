package ganymedes01.etfuturum.core.utils;

/** Pass 35 state-only extension for vanilla 1.7 Command Blocks. */
public interface IModernCommandBlockState {
    int etfu$getModernFacing();
    boolean etfu$isConditional();
    void etfu$setModernState(int facing, boolean conditional);
}
