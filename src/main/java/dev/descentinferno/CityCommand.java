package dev.descentinferno;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.io.IOException;
import java.util.Locale;

final class CityCommand extends AbstractCommandCollection {
    CityCommand(CityCheckpoints checkpoints) {
        super("descentinfernocity", "Manage Descent Inferno city checkpoints");
        requireNoPermission();
        addSubCommand(new SaveCity("create", "Create a city checkpoint at your feet", checkpoints, true));
        addSubCommand(new SaveCity("checkin", "Set your respawn at a nearby city checkpoint", checkpoints, false));
        addSubCommand(new AbstractPlayerCommand("status", "Show your current city checkpoint") {
            {
                requireNoPermission();
            }

            @Override
            protected void execute(CommandContext context, Store<EntityStore> store, Ref<EntityStore> ref,
                                   PlayerRef player, World world) {
                if (!(world.getDeathConfig().getRespawnController() instanceof CityRespawn)) {
                    context.sendMessage(Message.raw("Descent Inferno city respawning is disabled in this world."));
                    return;
                }
                String city = checkpoints.selectedCity(world.getWorldConfig().getUuid(), player.getUuid());
                context.sendMessage(Message.raw(city == null ? "You have no city checkpoint in this world."
                        : "Your city checkpoint is " + city + "."));
            }
        });
    }

    private static final class SaveCity extends AbstractPlayerCommand {
        private final RequiredArg<String> cityArg = withRequiredArg("city", "City checkpoint ID", ArgTypes.STRING);
        private final CityCheckpoints checkpoints;
        private final boolean create;

        SaveCity(String name, String description, CityCheckpoints checkpoints, boolean create) {
            super(name, description);
            this.checkpoints = checkpoints;
            this.create = create;
            if (create) {
                requirePermission("descentinferno.city.create");
            } else {
                requireNoPermission();
            }
        }

        @Override
        protected void execute(CommandContext context, Store<EntityStore> store, Ref<EntityStore> ref,
                               PlayerRef player, World world) {
            if (!(world.getDeathConfig().getRespawnController() instanceof CityRespawn)) {
                context.sendMessage(Message.raw("Descent Inferno city respawning is disabled in this world."));
                return;
            }
            if (store.getComponent(ref, DeathComponent.getComponentType()) != null) {
                context.sendMessage(Message.raw("You must be alive to use a city checkpoint."));
                return;
            }
            String city = cityArg.get(context).toLowerCase(Locale.ROOT);
            if (!city.matches("[a-z0-9][a-z0-9_-]{0,31}")) {
                context.sendMessage(Message.raw("Use 1–32 letters, digits, hyphens, or underscores for the city ID."));
                return;
            }

            var position = store.getComponent(ref, TransformComponent.getComponentType()).getPosition();
            var worldId = world.getWorldConfig().getUuid();
            try {
                String result = create ? checkpoints.create(worldId, city, position)
                        : checkpoints.checkIn(worldId, player.getUuid(), city, position);
                context.sendMessage(Message.raw(result));
            } catch (IOException e) {
                LOGGER.atWarning().withCause(e).log("Could not save Descent Inferno city checkpoints");
                context.sendMessage(Message.raw("The checkpoint could not be saved. Try again or contact an admin."));
            }
        }
    }
}
