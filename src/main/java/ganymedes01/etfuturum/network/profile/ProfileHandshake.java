package ganymedes01.etfuturum.network.profile;

import io.netty.util.AttributeKey;

public final class ProfileHandshake {
    public static final AttributeKey<Boolean> REJECTED = new AttributeKey<>("etfuturum:profile-rejected");
    private ProfileHandshake() {}
}
