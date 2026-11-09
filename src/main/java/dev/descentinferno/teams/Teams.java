package dev.descentinferno.teams;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.asset.type.attitude.Attitude;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.events.StartWorldEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.blackboard.Blackboard;
import com.hypixel.hytale.server.npc.blackboard.view.attitude.AttitudeView;
import dev.descentinferno.DescentInfernoPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.UUID;

public final class Teams {
    private final DescentInfernoPlugin plugin;
    private final Path file;
    private volatile Properties members;

    public static void register(DescentInfernoPlugin plugin) throws IOException {
        Teams teams = new Teams(plugin);
        plugin.getCommandRegistry().registerCommand(new TeamCommand(teams));
        plugin.getEntityStoreRegistry().registerSystem(teams.new FriendlyFire());
        plugin.getEventRegistry().registerGlobal(StartWorldEvent.class, event -> teams.registerAttitudes(event.getWorld()));
    }

    private Teams(DescentInfernoPlugin plugin) throws IOException {
        this.plugin = plugin;
        file = plugin.getDataDirectory().resolve("teams.properties");
        members = new Properties();
        if (Files.exists(file)) {
            try (var input = Files.newInputStream(file)) {
                members.load(input);
            }
        }
    }

    String team(UUID member) {
        return members.getProperty(member.toString());
    }

    synchronized void assign(UUID member, String team) throws IOException {
        Properties updated = new Properties();
        updated.putAll(members);
        if ("none".equals(team)) {
            updated.remove(member.toString());
        } else {
            updated.setProperty(member.toString(), team);
        }
        Path temporary = Files.createTempFile(file.getParent(), "teams-", ".tmp");
        try {
            try (var output = Files.newOutputStream(temporary)) {
                updated.store(output, "Descent Inferno team membership");
            }
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            members = updated;
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private String team(Ref<EntityStore> entity, ComponentAccessor<EntityStore> accessor) {
        var id = accessor.getComponent(entity, UUIDComponent.getComponentType());
        return id == null ? null : team(id.getUuid());
    }

    private void registerAttitudes(World world) {
        world.execute(() -> {
            var blackboard = world.getEntityStore().getStore().getResource(Blackboard.getResourceType());
            blackboard.getView(AttitudeView.class, 0L).registerProvider(-1, (source, role, target, accessor) -> {
                if (!plugin.isEnabled()) {
                    return null;
                }
                String sourceTeam = team(source, accessor);
                String targetTeam = team(target, accessor);
                return sourceTeam == null || targetTeam == null ? null
                        : sourceTeam.equals(targetTeam) ? Attitude.FRIENDLY : Attitude.HOSTILE;
            });
        });
    }

    private final class FriendlyFire extends DamageEventSystem {
        @Override
        public SystemGroup<EntityStore> getGroup() {
            return DamageModule.get().getFilterDamageGroup();
        }

        @Override
        public Query<EntityStore> getQuery() {
            return UUIDComponent.getComponentType();
        }

        @Override
        public void handle(int index, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store,
                           CommandBuffer<EntityStore> commands, Damage damage) {
            if (damage.getSource() instanceof Damage.EntitySource source && source.getRef().isValid()
                    && source.getRef() != chunk.getReferenceTo(index)) {
                String attacker = team(source.getRef(), commands);
                if (attacker != null && attacker.equals(team(chunk.getReferenceTo(index), commands))) {
                    damage.setCancelled(true);
                }
            }
        }
    }
}
