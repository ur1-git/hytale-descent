package dev.descentinferno.mining;

import com.hypixel.hytale.server.core.inventory.ItemStack;

import java.util.Map;

final class OreTiers {
    private static final String PREFIX = "DescentInferno_Ore_";
    private static final Map<String, Integer> TOOLS = Map.of(
            "Crude", 0, "Wood", 0, "Scrap", 0, "Copper", 1, "Iron", 2,
            "Thorium", 3, "Cobalt", 4, "Adamantite", 5, "Mithril", 6, "Onyxium", 7);
    private static final Map<String, Integer> ORES = Map.of(
            "Copper", 1, "Iron", 2, "Silver", 2, "Gold", 3, "Thorium", 3,
            "Cobalt", 4, "Adamantite", 5, "Mithril", 6, "Onyxium", 7, "Prisma", 8);

    private OreTiers() {
    }

    static Node node(String block) {
        if ("DescentInferno_Iron_Node".equals(block)) {
            return new Node(block, "DescentInferno_Iron_Node_Depleted", "Ore_Iron", 2);
        }
        if (!block.startsWith(PREFIX)) {
            return null;
        }
        int separator = block.indexOf('_', PREFIX.length());
        if (separator < 0) {
            return null;
        }
        String metal = block.substring(PREFIX.length(), separator);
        String host = block.substring(separator + 1);
        boolean hasNode = "Stone".equals(host) ? !"Iron".equals(metal)
                : "Basalt".equals(host) && !"Prisma".equals(metal);
        if (!hasNode) {
            return null;
        }
        Integer tier = ORES.get(metal);
        return tier == null ? null : new Node(block, "DescentInferno_Depleted_" + host,
                "Ore_" + metal, tier);
    }

    record Node(String ready, String depleted, String reward, int tier) {
        boolean canMine(ItemStack held) {
            if (ItemStack.isEmpty(held) || !held.getItemId().startsWith("Tool_Pickaxe_")) {
                return false;
            }
            int toolTier = TOOLS.getOrDefault(held.getItemId().substring("Tool_Pickaxe_".length()), -1);
            return toolTier >= 0 && tier <= toolTier + 1;
        }
    }
}
