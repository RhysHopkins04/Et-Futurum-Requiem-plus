package ganymedes01.etfuturum.movement.client;

import net.minecraft.entity.Entity;

public final class CrawlModelRender {
    private static final ThreadLocal<Entity> CRAWLING_PLAYER = new ThreadLocal<>();

    private CrawlModelRender() {}

    public static void render(Entity player, Runnable draw) {
        Entity previous = CRAWLING_PLAYER.get();
        CRAWLING_PLAYER.set(player);
        try {
            draw.run();
        } finally {
            if (previous == null) CRAWLING_PLAYER.remove();
            else CRAWLING_PLAYER.set(previous);
        }
    }

    public static boolean appliesTo(Entity player) {
        return player == CRAWLING_PLAYER.get() && player != null;
    }
}
