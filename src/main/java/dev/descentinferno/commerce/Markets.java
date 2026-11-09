package dev.descentinferno.commerce;

import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonString;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.TreeMap;
import java.util.UUID;

final class Markets {
    static final String COIN = "DescentInferno_Coin";
    static final String POST = "DescentInferno_Trading_Post";
    private final Path file;
    private Properties data = new Properties();

    Markets(Path directory) throws IOException {
        Files.createDirectories(directory);
        file = directory.resolve("markets.properties");
        if (Files.exists(file)) {
            try (var input = Files.newInputStream(file)) {
                data.load(input);
            }
        }
    }

    synchronized String create(String market) throws IOException {
        if (data.containsKey("market." + market)) {
            return "That market already exists.";
        }
        var updated = copy();
        updated.setProperty("market." + market, "true");
        save(updated);
        return "Created market " + market + ".";
    }

    synchronized String bind(Post post, String market) throws IOException {
        if (!data.containsKey("market." + market)) {
            return "Create that market first.";
        }
        String previous = marketAt(post);
        String coordinates = post.x() + ", " + post.y() + ", " + post.z();
        if (market.equals(previous)) {
            return "Post at " + coordinates + " already opens market " + market + ".";
        }
        if (previous != null && postCounts().getOrDefault(previous, 0) == 1 && hasOutstandingTrades(previous)) {
            return "This is the last post for " + previous + ". Clear its listings and claim its coins, or bind another post first.";
        }
        var updated = copy();
        updated.setProperty(post.key(), market);
        save(updated);
        return "Post at " + coordinates + " now opens market " + market
                + (previous == null ? "." : " (previously " + previous + ").");
    }

    synchronized Map<String, Integer> postCounts() {
        var counts = new TreeMap<String, Integer>();
        for (String key : data.stringPropertyNames()) {
            if (key.startsWith("market.")) {
                counts.putIfAbsent(key.substring("market.".length()), 0);
            } else if (key.startsWith("post.")) {
                String market = data.getProperty(key);
                if (data.containsKey("market." + market)) {
                    counts.merge(market, 1, Integer::sum);
                }
            }
        }
        return counts;
    }

    synchronized String delete(String market) throws IOException {
        if (!data.containsKey("market." + market)) {
            return "Market " + market + " does not exist.";
        }
        if (hasOutstandingTrades(market)) {
            return "Clear the market's listings and claim all its coins before deleting it.";
        }
        var updated = copy();
        updated.remove("market." + market);
        updated.keySet().removeIf(key -> {
            String name = (String) key;
            return name.startsWith("credit." + market + ".")
                    || (name.startsWith("post.") && market.equals(updated.getProperty(name)));
        });
        save(updated);
        return "Deleted market " + market + " and cleared its trading post bindings.";
    }

    private boolean hasOutstandingTrades(String market) {
        String creditPrefix = "credit." + market + ".";
        for (String key : data.stringPropertyNames()) {
            String value = data.getProperty(key);
            if (key.startsWith(creditPrefix) && Long.parseLong(value) > 0) {
                return true;
            }
            if (key.startsWith("listing.") && market.equals(BsonDocument.parse(value).getString("market").getValue())) {
                return true;
            }
        }
        return false;
    }

    synchronized String marketAt(Post post) {
        return data.getProperty(post.key());
    }

    synchronized List<Listing> listings(String market) {
        return data.stringPropertyNames().stream()
                .filter(key -> key.startsWith("listing."))
                .map(key -> readListing(key.substring("listing.".length())))
                .filter(listing -> listing.market().equals(market))
                .sorted(Comparator.comparing(Listing::id))
                .toList();
    }

    synchronized long proceeds(String market, UUID seller) {
        return Long.parseLong(data.getProperty(creditKey(market, seller), "0"));
    }

    synchronized String list(Post post, String market, UUID seller, ItemContainer inventory, short slot,
                             int quantity, int price) throws IOException {
        if (!market.equals(marketAt(post))) {
            return "This post's market changed. Open it again.";
        }
        ItemStack held = inventory.getItemStack(slot);
        if (ItemStack.isEmpty(held) || COIN.equals(held.getItemId()) || quantity < 1
                || quantity > held.getQuantity() || price < 1) {
            return "Hold a sale item and enter a valid quantity and total price.";
        }
        String id = UUID.randomUUID().toString();
        var stack = held.withQuantity(quantity);
        var encoded = new BsonDocument("market", new BsonString(market))
                .append("seller", new BsonString(seller.toString()))
                .append("stack", ItemStack.CODEC.encode(stack, new ExtraInfo()))
                .append("price", new BsonInt32(price));
        var updated = copy();
        updated.setProperty("listing." + id, encoded.toJson());

        var planned = snapshot(inventory);
        planned.setItemStackForSlot(slot, quantity == held.getQuantity() ? ItemStack.EMPTY
                : held.withQuantity(held.getQuantity() - quantity));
        commit(updated, inventory, planned);
        return "Listing posted.";
    }

    synchronized String buyOrCancel(Post post, String market, UUID player, String id,
                                    ItemContainer inventory, boolean cancel) throws IOException {
        if (!market.equals(marketAt(post))) {
            return "This post's market changed. Open it again.";
        }
        Listing listing = readListing(id);
        if (listing == null || !listing.market().equals(market)) {
            return "That listing is no longer available.";
        }
        if (cancel != listing.seller().equals(player)) {
            return cancel ? "Only the seller can cancel this listing." : "You already own this listing.";
        }

        var planned = snapshot(inventory);
        if (!cancel && !ItemStack.isEmpty(planned.removeItemStack(
                new ItemStack(COIN, listing.price()), true, true).getRemainder())) {
            return "You don't have enough Descent Inferno Coins.";
        }
        if (!ItemStack.isEmpty(planned.addItemStack(listing.stack(), true, false, true).getRemainder())) {
            return "Make room in your inventory first.";
        }
        var updated = copy();
        updated.remove("listing." + id);
        if (!cancel) {
            long credit = Math.addExact(proceeds(market, listing.seller()), listing.price());
            updated.setProperty(creditKey(market, listing.seller()), Long.toString(credit));
        }
        commit(updated, inventory, planned);
        return cancel ? "Listing cancelled and items returned." : "Purchase complete.";
    }

    synchronized String claim(Post post, String market, UUID seller, ItemContainer inventory) throws IOException {
        if (!market.equals(marketAt(post))) {
            return "This post's market changed. Open it again.";
        }
        long available = proceeds(market, seller);
        if (available == 0) {
            return "You have no coins to claim here.";
        }
        int requested = (int) Math.min(1000, available);
        var planned = snapshot(inventory);
        var remainder = planned.addItemStack(new ItemStack(COIN, requested), false, false, true).getRemainder();
        int claimed = requested - (ItemStack.isEmpty(remainder) ? 0 : remainder.getQuantity());
        if (claimed == 0) {
            return "Make room for coins first.";
        }
        var updated = copy();
        updated.setProperty(creditKey(market, seller), Long.toString(available - claimed));
        commit(updated, inventory, planned);
        return "Claimed " + claimed + " coins.";
    }

    private Listing readListing(String id) {
        String value = data.getProperty("listing." + id);
        if (value == null) {
            return null;
        }
        var document = BsonDocument.parse(value);
        return new Listing(id, document.getString("market").getValue(),
                UUID.fromString(document.getString("seller").getValue()),
                ItemStack.CODEC.decode(document.getDocument("stack"), new ExtraInfo()),
                document.getInt32("price").getValue());
    }

    private static ItemContainer snapshot(ItemContainer inventory) {
        var planned = new SimpleItemContainer(inventory.getCapacity());
        for (short slot = 0; slot < inventory.getCapacity(); slot++) {
            ItemStack stack = inventory.getItemStack(slot);
            if (!ItemStack.isEmpty(stack)) {
                planned.setItemStackForSlot(slot, stack);
            }
        }
        return planned;
    }

    private void commit(Properties updated, ItemContainer inventory, ItemContainer planned) throws IOException {
        var changed = new ArrayList<SlotChange>();
        try {
            for (short slot = 0; slot < inventory.getCapacity(); slot++) {
                ItemStack before = inventory.getItemStack(slot);
                ItemStack after = planned.getItemStack(slot);
                if (!Objects.equals(before, after) && !(ItemStack.isEmpty(before) && ItemStack.isEmpty(after))) {
                    changed.add(new SlotChange(slot, before));
                    if (!inventory.setItemStackForSlot(slot, ItemStack.isEmpty(after) ? ItemStack.EMPTY : after,
                            false).succeeded()) {
                        throw new IOException("Could not apply market inventory change");
                    }
                }
            }
            save(updated);
        } catch (IOException | RuntimeException error) {
            for (var change : changed.reversed()) {
                ItemStack before = change.before();
                inventory.setItemStackForSlot(change.slot(), ItemStack.isEmpty(before) ? ItemStack.EMPTY : before, false);
            }
            throw error;
        }
    }

    private record SlotChange(short slot, ItemStack before) {
    }

    private Properties copy() {
        var updated = new Properties();
        updated.putAll(data);
        return updated;
    }

    private void save(Properties updated) throws IOException {
        Path temporary = Files.createTempFile(file.getParent(), "market-", ".tmp");
        try {
            try (var output = Files.newOutputStream(temporary)) {
                updated.store(output, "Descent Inferno markets");
            }
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            data = updated;
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static String creditKey(String market, UUID seller) {
        return "credit." + market + "." + seller;
    }

    record Post(UUID world, int x, int y, int z) {
        String key() {
            return "post." + world + "." + x + "." + y + "." + z;
        }
    }

    record Listing(String id, String market, UUID seller, ItemStack stack, int price) {
    }
}
