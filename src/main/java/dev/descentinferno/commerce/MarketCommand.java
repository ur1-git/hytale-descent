package dev.descentinferno.commerce;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;

import java.io.IOException;
import java.util.Locale;

final class MarketCommand extends AbstractCommandCollection {
    MarketCommand(Markets markets) {
        super("descentinfernomarket", "Create markets and assign trading posts");
        addAliases("descentmarket");
        requirePermission("descentinferno.market.admin");
        addSubCommand(new Create(markets));
        addSubCommand(new Bind(markets));
        addSubCommand(new ListMarkets(markets));
        addSubCommand(new Delete(markets));
    }

    private abstract static class AdminAction extends AbstractPlayerCommand {
        private final RequiredArg<String> name = withRequiredArg("market", "Market ID", ArgTypes.STRING);
        final Markets markets;

        AdminAction(String command, String description, Markets markets) {
            super(command, description);
            requirePermission("descentinferno.market.admin");
            this.markets = markets;
        }

        AdminAction(String description, Markets markets) {
            super(description);
            requirePermission("descentinferno.market.admin");
            this.markets = markets;
        }

        @Override
        protected final void execute(CommandContext context, Store<EntityStore> store, Ref<EntityStore> ref,
                                     PlayerRef player, World world) {
            String market = name.get(context).toLowerCase(Locale.ROOT);
            if (!market.matches("[a-z0-9][a-z0-9_-]{0,31}")) {
                context.sendMessage(Message.raw("Use up to 32 letters, digits, hyphens or underscores."));
                return;
            }
            try {
                context.sendMessage(Message.raw(update(context, store, ref, world, market)));
            } catch (IOException e) {
                LOGGER.atWarning().withCause(e).log("Could not save Descent Inferno market");
                context.sendMessage(Message.raw("The market could not be saved."));
            }
        }

        abstract String update(CommandContext context, Store<EntityStore> store, Ref<EntityStore> ref,
                               World world, String market) throws IOException;
    }

    private static final class Create extends AdminAction {
        Create(Markets markets) {
            super("create", "Create an independent market", markets);
        }

        @Override
        String update(CommandContext context, Store<EntityStore> store, Ref<EntityStore> ref,
                      World world, String market) throws IOException {
            return markets.create(market);
        }
    }

    private static final class ListMarkets extends CommandBase {
        private final Markets markets;

        ListMarkets(Markets markets) {
            super("list", "List markets and their trading post counts");
            requirePermission("descentinferno.market.admin");
            this.markets = markets;
        }

        @Override
        protected void executeSync(CommandContext context) {
            var counts = markets.postCounts();
            if (counts.isEmpty()) {
                context.sendMessage(Message.raw("No markets have been created."));
                return;
            }
            context.sendMessage(Message.raw("Markets (" + counts.size() + ")"));
            counts.forEach((market, count) -> context.sendMessage(Message.raw(market + " (" + count + " posts)")));
        }
    }

    private static final class Delete extends AdminAction {
        Delete(Markets markets) {
            super("delete", "Delete a market with no listings or unclaimed coins", markets);
        }

        @Override
        String update(CommandContext context, Store<EntityStore> store, Ref<EntityStore> ref,
                      World world, String market) throws IOException {
            return markets.delete(market);
        }
    }

    private static final class Bind extends AdminAction {
        Bind(Markets markets) {
            super("bind", "Assign the trading post you are looking at", markets);
            addUsageVariant(new BindCoordinates(markets));
        }

        @Override
        String update(CommandContext context, Store<EntityStore> store, Ref<EntityStore> ref,
                      World world, String market) throws IOException {
            var target = TargetUtil.getTargetBlockOrigin(ref, 6, store);
            if (target == null) {
                return "Aim at a Descent Inferno Trading Post within six blocks.";
            }
            var post = new Markets.Post(world.getWorldConfig().getUuid(), target.x, target.y, target.z);
            if (!Commerce.isTradingPost(world, post)) {
                return "Aim at a Descent Inferno Trading Post within six blocks.";
            }
            return markets.bind(post, market);
        }
    }

    private static final class BindCoordinates extends AdminAction {
        private final RequiredArg<Integer> x = withRequiredArg("x", "Trading post X", ArgTypes.INTEGER);
        private final RequiredArg<Integer> y = withRequiredArg("y", "Trading post Y", ArgTypes.INTEGER);
        private final RequiredArg<Integer> z = withRequiredArg("z", "Trading post Z", ArgTypes.INTEGER);

        BindCoordinates(Markets markets) {
            super("Assign a nearby trading post by coordinates", markets);
        }

        @Override
        String update(CommandContext context, Store<EntityStore> store, Ref<EntityStore> ref,
                      World world, String market) throws IOException {
            var post = new Markets.Post(world.getWorldConfig().getUuid(), x.get(context), y.get(context), z.get(context));
            var position = store.getComponent(ref, TransformComponent.getComponentType()).getPosition();
            if (position.distanceSquared(post.x() + 0.5, post.y() + 0.5, post.z() + 0.5) > 36
                    || !Commerce.isTradingPost(world, post)) {
                return "Use the coordinates of a Descent Inferno Trading Post within six blocks.";
            }
            return markets.bind(post, market);
        }
    }
}
