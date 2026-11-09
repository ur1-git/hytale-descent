package dev.descentinferno;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.asset.type.gameplay.respawn.RespawnController;
import com.hypixel.hytale.server.core.asset.type.gameplay.respawn.WorldSpawnPoint;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.concurrent.CompletableFuture;

final class CityRespawn implements RespawnController {
    private final CityCheckpoints checkpoints;

    CityRespawn(CityCheckpoints checkpoints) {
        this.checkpoints = checkpoints;
    }

    @Override
    public CompletableFuture<Void> respawnPlayer(World world, Ref<EntityStore> ref,
                                                ComponentAccessor<EntityStore> accessor) {
        PlayerRef player = accessor.getComponent(ref, PlayerRef.getComponentType());
        var destination = player == null ? null
                : checkpoints.destination(world.getWorldConfig().getUuid(), player.getUuid());
        if (destination == null) {
            return WorldSpawnPoint.INSTANCE.respawnPlayer(world, ref, accessor);
        }

        return CompletableFuture.runAsync(() -> {
            if (ref.isValid()) {
                ref.getStore().addComponent(ref, Teleport.getComponentType(),
                        Teleport.createForPlayer(destination, new Rotation3f()));
            }
        }, world);
    }
}
