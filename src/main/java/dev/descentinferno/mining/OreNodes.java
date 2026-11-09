package dev.descentinferno.mining;

import dev.descentinferno.DescentInfernoPlugin;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SetBlockSettings;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockOperations;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.joml.Vector3ic;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

final class OreNodes {
    private static final int COOLDOWN_SECONDS = 15;
    private static final int SWAP_FLAGS = SetBlockSettings.NO_DROP_ITEMS | SetBlockSettings.NO_FIRE_ON_BREAK;

    private final DescentInfernoPlugin plugin;
    private final ConcurrentHashMap<NodePosition, Object> cooldowns = new ConcurrentHashMap<>();

    OreNodes(DescentInfernoPlugin plugin) {
        this.plugin = plugin;
    }

    void harvest(World world, Vector3ic position, PlayerRef player, ItemContainer inventory, OreTiers.Node ore) {
        var node = new NodePosition(world, position.x(), position.y(), position.z());
        var chunks = world.getChunkStore();
        var section = chunks.getChunkSectionReferenceAtBlock(node.x(), node.y(), node.z());
        var blocks = section == null ? null : section.getStore().getComponent(section, BlockSection.getComponentType());
        var types = BlockType.getAssetMap();
        int ready = types.getIndex(ore.ready());
        int depleted = types.getIndex(ore.depleted());
        if (blocks == null || depleted < 0 || blocks.get(node.x(), node.y(), node.z()) != ready) {
            return;
        }

        var reward = new ItemStack(ore.reward(), 1);
        if (!inventory.canAddItemStack(reward)) {
            player.sendMessage(Message.raw("Make room for the ore before mining this node."));
            return;
        }

        int rotation = blocks.getRotationIndex(node.x(), node.y(), node.z());
        if (!swap(node, section, depleted, rotation)) {
            return;
        }
        var transaction = inventory.addItemStack(reward);
        if (!ItemStack.isEmpty(transaction.getRemainder())) {
            swap(node, section, ready, rotation);
            player.sendMessage(Message.raw("Make room for the ore before mining this node."));
            return;
        }
        startCooldown(node, rotation, ore);
    }

    private void startCooldown(NodePosition node, int rotation, OreTiers.Node ore) {
        // A new harvest replaces the token so an older timer can't restore this node early.
        Object token = new Object();
        cooldowns.put(node, token);
        World world = node.world();
        CompletableFuture.runAsync(() -> {}, CompletableFuture.delayedExecutor(COOLDOWN_SECONDS, TimeUnit.SECONDS))
                .thenComposeAsync(ignored -> world.getChunkStore()
                        .getChunkSectionReferenceAtBlockAsync(node.x(), node.y(), node.z()), world)
                .thenAcceptAsync(section -> {
                    if (plugin.isEnabled() && cooldowns.remove(node, token)) {
                        restore(node, section, rotation, ore);
                    }
                }, world)
                .exceptionally(error -> {
                    cooldowns.remove(node, token);
                    plugin.getLogger().atWarning().withCause(error).log("Could not restore a Descent Inferno ore node");
                    return null;
                });
    }

    private void restore(NodePosition node, Ref<ChunkStore> section, int rotation, OreTiers.Node ore) {
        if (section == null || !section.isValid()) {
            return;
        }
        var blocks = section.getStore().getComponent(section, BlockSection.getComponentType());
        var types = BlockType.getAssetMap();
        if (blocks != null && blocks.get(node.x(), node.y(), node.z()) == types.getIndex(ore.depleted())) {
            swap(node, section, types.getIndex(ore.ready()), rotation);
        }
    }

    private static boolean swap(NodePosition node, Ref<ChunkStore> section, int blockId, int rotation) {
        var type = BlockType.getAssetMap().getAsset(blockId);
        return BlockOperations.setBlock(node.world().getChunkStore(), section, node.x(), node.y(), node.z(),
                blockId, type, rotation, 0, SWAP_FLAGS);
    }

    private record NodePosition(World world, int x, int y, int z) {
    }
}
