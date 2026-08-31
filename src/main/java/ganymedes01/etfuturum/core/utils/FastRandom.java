// To the extent possible under law, the original xoshiro authors and Java
// porter dedicated this implementation to the public domain (CC0).
package ganymedes01.etfuturum.core.utils;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

/** xoshiro256** Random implementation, previously consumed through HogUtils. */
public class FastRandom extends Random {
    private static final AtomicLong UNIQ = new AtomicLong(System.nanoTime());
    private static final long SPLITMIX_MAGIC = 0x9E3779B97F4A7C15L;
    private long s0, s1, s2, s3;

    private static long nextUniq() { return splitmix64_2(UNIQ.addAndGet(SPLITMIX_MAGIC)); }
    private static long splitmix64_1(long x) { return x + SPLITMIX_MAGIC; }
    private static long splitmix64_2(long z) {
        z = (z ^ (z >> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >> 31);
    }

    public FastRandom() { this(System.nanoTime() ^ nextUniq()); }
    public FastRandom(long seed) { super(seed); }
    public FastRandom(long s0, long s1, long s2, long s3) { setState(s0, s1, s2, s3); }

    @Override
    public void setSeed(long seed) {
        super.setSeed(seed);
        long x = splitmix64_1(seed); s0 = splitmix64_2(x);
        x = splitmix64_1(x); s1 = splitmix64_2(x);
        x = splitmix64_1(x); s2 = splitmix64_2(x);
        x = splitmix64_1(x); s3 = splitmix64_2(x);
    }

    public void setState(long s0, long s1, long s2, long s3) {
        if (s0 == 0 && s1 == 0 && s2 == 0 && s3 == 0) throw new IllegalArgumentException("xoshiro256** state cannot be all zeroes");
        this.s0 = s0; this.s1 = s1; this.s2 = s2; this.s3 = s3;
    }

    @Override protected int next(int bits) { return (int)(nextLong() & ((1L << bits) - 1)); }
    @Override public int nextInt() { return (int)nextLong(); }
    @Override public int nextInt(int bound) { return (int)nextLong(bound); }
    public long nextLong(long bound) {
        if (bound <= 0) throw new IllegalArgumentException("bound must be positive");
        return (nextLong() & Long.MAX_VALUE) % bound;
    }
    @Override public double nextDouble() { return (nextLong() >>> 11) * 0x1.0P-53; }
    @Override public float nextFloat() { return (nextLong() >>> 40) * 0x1.0P-24f; }
    @Override public boolean nextBoolean() { return (nextLong() & 1) != 0; }
    @Override public void nextBytes(byte[] bytes) { nextBytes(bytes, 0, bytes.length); }
    public void nextBytes(byte[] bytes, int offset, int length) {
        if (offset < 0 || offset > bytes.length || offset + length > bytes.length) throw new ArrayIndexOutOfBoundsException();
        int remaining = 8; long value = 0;
        for (int i = offset; i < offset + length; i++) {
            if (remaining >= 8) { value = nextLong(); remaining = 0; }
            bytes[i] = (byte)(value & 0xFF); value >>>= 8; remaining++;
        }
    }
    private static long rotl(long x, int k) { return (x << k) | (x >>> (64 - k)); }
    @Override public long nextLong() {
        long result = rotl(s1 * 5, 7) * 9;
        long t = s1 << 17;
        s2 ^= s0; s3 ^= s1; s1 ^= s2; s0 ^= s3; s2 ^= t; s3 = rotl(s3, 45);
        return result;
    }
}
