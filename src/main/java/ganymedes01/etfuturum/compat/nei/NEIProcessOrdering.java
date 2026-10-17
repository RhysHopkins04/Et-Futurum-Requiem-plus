package ganymedes01.etfuturum.compat.nei;

import java.util.Map;

/** Prefer actual workstation uses to the generic wooden-block fuel page. */
public final class NEIProcessOrdering {
    private NEIProcessOrdering() {}
    public static void defaults(Map<String,Integer> order) {
        for(String name:new String[]{"CampfireRecipeHandler","SmithingRecipeHandler","CandleCakeRecipeHandler","StonecutterRecipeHandler","LoomRecipeHandler","GrindstoneRecipeHandler","CartographyRecipeHandler"}) {
            String id="etfuturum."+(name.startsWith("Campfire")?"campfire":name.startsWith("Smithing")?"smithing":name.startsWith("Stonecutter")?"stonecutting":name.startsWith("Loom")?"loom":name.startsWith("Grindstone")?"grinding":name.startsWith("Cartography")?"cartography":"candlecake");
            if(!order.containsKey(id) && !order.containsKey(NEIProcessOrdering.class.getPackage().getName()+"."+name))
                order.put(id,-10); // Explicit pack/user ordering always wins.
        }
    }
    @SuppressWarnings("unchecked")
    public static void installDefaults() {
        try {
            Object order=Class.forName("codechicken.nei.NEIClientConfig").getField("handlerOrdering").get(null);
            if(order instanceof Map)defaults((Map<String,Integer>)order);
        } catch (ReflectiveOperationException legacyNEI) {
            // Legacy NEI has no sortable handler map; registration order is used instead.
        }
    }
}
