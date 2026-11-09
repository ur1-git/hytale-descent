package dev.descentinferno.commerce;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.io.IOException;

final class MarketPage extends InteractiveCustomUIPage<MarketPage.Input> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final int PAGE_SIZE = 6;
    private final Markets markets;
    private final Markets.Post post;
    private final String market;
    private int page;
    private String feedback = "";

    MarketPage(PlayerRef player, Markets markets, Markets.Post post, String market) {
        super(player, CustomPageLifetime.CanDismiss, Input.CODEC);
        this.markets = markets;
        this.post = post;
        this.market = market;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder ui, UIEventBuilder events, Store<EntityStore> store) {
        ui.append("Pages/DescentInfernoMarket.ui");
        ui.set("#MarketName.Text", "Market: " + market);
        ui.set("#Feedback.Text", feedback);
        ui.set("#Proceeds.Text", "Coins to claim: " + markets.proceeds(market, playerRef.getUuid()));

        var listings = markets.listings(market);
        int lastPage = Math.max(0, (listings.size() - 1) / PAGE_SIZE);
        page = Math.clamp(page, 0, lastPage);
        ui.set("#PageNumber.Text", (page + 1) + " / " + (lastPage + 1));
        for (int i = 0; i < PAGE_SIZE && page * PAGE_SIZE + i < listings.size(); i++) {
            var listing = listings.get(page * PAGE_SIZE + i);
            String row = "#Listings[" + i + "]";
            boolean own = listing.seller().equals(playerRef.getUuid());
            ui.append("#Listings", "Pages/DescentInfernoMarketRow.ui");
            ui.set(row + " #Item.Text", listing.stack().getDisplayName());
            ui.set(row + " #Price.Text", "x" + listing.stack().getQuantity() + " / " + listing.price() + " coins");
            ui.set(row + " #Trade.Text", own ? "Cancel" : "Buy");
            bind(events, row + " #Trade", new EventData().append("Action", own ? "cancel" : "buy")
                    .append("Listing", listing.id()));
        }

        bind(events, "#List", new EventData().append("Action", "list")
                .append("@Quantity", "#Quantity.Value").append("@Price", "#Price.Value"));
        bind(events, "#Claim", new EventData().append("Action", "claim"));
        bind(events, "#Previous", new EventData().append("Action", "previous"));
        bind(events, "#Next", new EventData().append("Action", "next"));
        bind(events, "#Refresh", new EventData().append("Action", "refresh"));
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, Input input) {
        var position = store.getComponent(ref, TransformComponent.getComponentType()).getPosition();
        if (position.distanceSquared(post.x() + 0.5, post.y() + 0.5, post.z() + 0.5) > 36
                || !Commerce.isTradingPost(store.getExternalData().getWorld(), post)
                || !market.equals(markets.marketAt(post))) {
            close();
            return;
        }

        var inventory = InventoryComponent.getCombined(store, ref, InventoryComponent.HOTBAR_STORAGE_BACKPACK);
        var player = playerRef.getUuid();
        try {
            switch (input.action) {
                case "list" -> {
                    var hotbar = store.getComponent(ref, InventoryComponent.Hotbar.getComponentType());
                    short slot = hotbar.getActiveSlot();
                    if (slot < 0 || slot >= hotbar.getInventory().getCapacity() || ItemStack.isEmpty(hotbar.getActiveItem())) {
                        feedback = "Hold the stack you want to sell first.";
                    } else {
                        feedback = markets.list(post, market, player, inventory, slot,
                                Integer.parseInt(input.quantity.strip()), Integer.parseInt(input.price.strip()));
                    }
                }
                case "buy", "cancel" -> feedback = markets.buyOrCancel(post, market, player, input.listing,
                        inventory, "cancel".equals(input.action));
                case "claim" -> feedback = markets.claim(post, market, player, inventory);
                case "previous" -> page--;
                case "next" -> page++;
                case "refresh" -> feedback = "";
                default -> feedback = "Unknown market action.";
            }
        } catch (NumberFormatException e) {
            feedback = "Enter a whole quantity and total price.";
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("Could not save Descent Inferno market transaction");
            feedback = "Could not save the market. Try again.";
        }
        rebuild();
    }

    private static void bind(UIEventBuilder events, String selector, EventData data) {
        events.addEventBinding(CustomUIEventBindingType.Activating, selector, data, false);
    }

    static final class Input {
        static final BuilderCodec<Input> CODEC = BuilderCodec.builder(Input.class, Input::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (input, value) -> input.action = value, input -> input.action).add()
                .append(new KeyedCodec<>("Listing", Codec.STRING), (input, value) -> input.listing = value, input -> input.listing).add()
                .append(new KeyedCodec<>("@Quantity", Codec.STRING), (input, value) -> input.quantity = value, input -> input.quantity).add()
                .append(new KeyedCodec<>("@Price", Codec.STRING), (input, value) -> input.price = value, input -> input.price).add()
                .build();

        private String action = "";
        private String listing = "";
        private String quantity = "";
        private String price = "";
    }
}
