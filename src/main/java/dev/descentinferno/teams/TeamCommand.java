package dev.descentinferno.teams;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

final class TeamCommand extends AbstractCommandCollection {
    TeamCommand(Teams teams) {
        super("descentinfernoteam", "View and assign Descent Inferno teams");
        requireNoPermission();
        addSubCommand(new AbstractPlayerCommand("status", "Show your team") {
            { requireNoPermission(); }

            @Override
            protected void execute(CommandContext context, Store<EntityStore> store, Ref<EntityStore> ref,
                                   PlayerRef player, World world) {
                String team = teams.team(player.getUuid());
                context.sendMessage(Message.raw(team == null ? "You have no team." : "Your team is " + team + "."));
            }
        });
        addSubCommand(new Assign(teams, false));
        addSubCommand(new Assign(teams, true));
    }

    private static final class Assign extends AbstractPlayerCommand {
        private final RequiredArg<String> teamArg = withRequiredArg("team", "Team ID or none", ArgTypes.STRING);
        private final RequiredArg<PlayerRef> target;
        private final Teams teams;

        Assign(Teams teams, boolean npc) {
            super(npc ? "npc" : "player", npc ? "Assign the nearest NPC within six blocks" : "Assign a player");
            this.teams = teams;
            target = npc ? null : withRequiredArg("player", "Player to assign", ArgTypes.PLAYER_REF);
            requirePermission("descentinferno.team.assign");
        }

        @Override
        protected void execute(CommandContext context, Store<EntityStore> store, Ref<EntityStore> ref,
                               PlayerRef player, World world) {
            String name = teamArg.get(context).toLowerCase(Locale.ROOT);
            if (!name.matches("[a-z0-9][a-z0-9_-]{0,31}")) {
                context.sendMessage(Message.raw("Use up to 32 letters, digits, hyphens or underscores."));
                return;
            }
            UUID id = target == null ? nearestNpc(store, ref) : target.get(context).getUuid();
            if (id == null) {
                context.sendMessage(Message.raw("There is no NPC within six blocks."));
                return;
            }
            try {
                teams.assign(id, name);
                context.sendMessage(Message.raw("Team set to " + name + "."));
            } catch (IOException e) {
                LOGGER.atWarning().withCause(e).log("Could not save Descent Inferno teams");
                context.sendMessage(Message.raw("Team assignment could not be saved."));
            }
        }

        private UUID nearestNpc(Store<EntityStore> store, Ref<EntityStore> player) {
            var origin = store.getComponent(player, TransformComponent.getComponentType()).getPosition();
            UUID[] nearest = {null};
            double[] distance = {36};
            store.forEachChunk(Query.and(NPCEntity.getComponentType(), TransformComponent.getComponentType(),
                    UUIDComponent.getComponentType()), (chunk, commands) -> {
                for (int i = 0; i < chunk.size(); i++) {
                    var position = chunk.getComponent(i, TransformComponent.getComponentType()).getPosition();
                    double candidate = origin.distanceSquared(position);
                    if (candidate <= distance[0]) {
                        distance[0] = candidate;
                        nearest[0] = chunk.getComponent(i, UUIDComponent.getComponentType()).getUuid();
                    }
                }
            });
            return nearest[0];
        }
    }
}
