package dev.descentinferno.mining;

import dev.descentinferno.DescentInfernoPlugin;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class MiningProtection {
    public static void register(DescentInfernoPlugin plugin) {
        plugin.getEntityStoreRegistry().registerSystem(new DamageOre());
        plugin.getEntityStoreRegistry().registerSystem(new BreakBlocks(new OreNodes(plugin)));
        plugin.getEntityStoreRegistry().registerSystem(new PlaceBlocks());
    }

    private MiningProtection() {
    }

    private static boolean isRestricted(Player player, Store<EntityStore> store) {
        return player.getGameMode() == GameMode.Adventure
                && "DescentInfernoMiningTest".equals(store.getExternalData().getWorld().getGameplayConfig().getId());
    }

    static final class BreakBlocks extends EntityEventSystem<EntityStore, BreakBlockEvent> {
        private final OreNodes oreNodes;

        BreakBlocks(OreNodes oreNodes) {
            super(BreakBlockEvent.class);
            this.oreNodes = oreNodes;
        }

        @Override
        public Query<EntityStore> getQuery() {
            return Player.getComponentType();
        }

        @Override
        public void handle(int index, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store,
                           CommandBuffer<EntityStore> commands, BreakBlockEvent event) {
            Player player = chunk.getComponent(index, Player.getComponentType());
            if (event.isCancelled() || !isRestricted(player, store)) {
                return;
            }
            event.setCancelled(true);
            var ore = OreTiers.node(event.getBlockType().getId());
            if (ore != null && ore.canMine(event.getItemInHand())) {
                var playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
                if (playerRef != null) {
                    var inventory = InventoryComponent.getCombined(commands, chunk, index,
                            InventoryComponent.STORAGE_HOTBAR_BACKPACK);
                    oreNodes.harvest(store.getExternalData().getWorld(), event.getTargetBlock(), playerRef, inventory, ore);
                }
            }
        }
    }

    static final class DamageOre extends EntityEventSystem<EntityStore, DamageBlockEvent> {
        DamageOre() {
            super(DamageBlockEvent.class);
        }

        @Override
        public Query<EntityStore> getQuery() {
            return Player.getComponentType();
        }

        @Override
        public void handle(int index, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store,
                           CommandBuffer<EntityStore> commands, DamageBlockEvent event) {
            Player player = chunk.getComponent(index, Player.getComponentType());
            if (!isRestricted(player, store)) {
                return;
            }
            var ore = OreTiers.node(event.getBlockType().getId());
            if (ore == null || !ore.canMine(event.getItemInHand())) {
                event.setCancelled(true);
            }
        }
    }

    static final class PlaceBlocks extends EntityEventSystem<EntityStore, PlaceBlockEvent> {
        PlaceBlocks() {
            super(PlaceBlockEvent.class);
        }

        @Override
        public Query<EntityStore> getQuery() {
            return Player.getComponentType();
        }

        @Override
        public void handle(int index, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store,
                           CommandBuffer<EntityStore> commands, PlaceBlockEvent event) {
            Player player = chunk.getComponent(index, Player.getComponentType());
            if (isRestricted(player, store)) {
                event.setCancelled(true);
            }
        }
    }
}
