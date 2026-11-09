package dev.descentinferno.commerce;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.descentinferno.DescentInfernoPlugin;

import java.io.IOException;

public final class Commerce {
    private Commerce() {
    }

    public static void register(DescentInfernoPlugin plugin) throws IOException {
        Markets markets = new Markets(plugin.getDataDirectory());
        plugin.getCommandRegistry().registerCommand(new MarketCommand(markets));
        var codec = BuilderCodec.builder(PostPageSupplier.class, () -> new PostPageSupplier(markets)).build();
        plugin.getCodecRegistry(OpenCustomUIInteraction.PAGE_CODEC).register("DescentInfernoMarket", PostPageSupplier.class, codec);
    }

    static boolean isTradingPost(World world, Markets.Post post) {
        if (!world.getWorldConfig().getUuid().equals(post.world())) {
            return false;
        }
        var section = world.getChunkStore().getChunkSectionReferenceAtBlock(post.x(), post.y(), post.z());
        if (section == null || !section.isValid()) {
            return false;
        }
        var blocks = section.getStore().getComponent(section, BlockSection.getComponentType());
        return blocks != null && blocks.get(post.x(), post.y(), post.z()) == BlockType.getAssetMap().getIndex(Markets.POST);
    }

    private record PostPageSupplier(Markets markets) implements OpenCustomUIInteraction.CustomPageSupplier {
        @Override
        public CustomUIPage tryCreate(Ref<EntityStore> ref, ComponentAccessor<EntityStore> accessor, PlayerRef player,
                                      InteractionContext interaction) {
            var target = interaction.getTargetBlock();
            if (target == null) {
                return null;
            }
            var world = accessor.getExternalData().getWorld();
            var post = new Markets.Post(world.getWorldConfig().getUuid(), target.x, target.y, target.z);
            String market = markets.marketAt(post);
            if (market == null) {
                player.sendMessage(Message.raw("This trading post has no market assigned."));
                return null;
            }
            return new MarketPage(player, markets, post, market);
        }
    }
}
