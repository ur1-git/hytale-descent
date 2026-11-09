package dev.descentinferno;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;

import java.util.Arrays;
import java.util.Set;

final class ArmorDrops extends DeathSystems.OnDeathSystem {
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(Player.getComponentType(), InventoryComponent.Armor.getComponentType(),
                TransformComponent.getComponentType());
    }

    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Set.of(new SystemDependency<>(Order.AFTER, DeathSystems.DropPlayerDeathItems.class),
                new SystemDependency<>(Order.BEFORE, DeathSystems.PlayerDeathScreen.class));
    }

    @Override
    public void onComponentAdded(Ref<EntityStore> ref, DeathComponent death, Store<EntityStore> store,
                                 CommandBuffer<EntityStore> commands) {
        var world = store.getExternalData().getWorld();
        var player = store.getComponent(ref, Player.getComponentType());
        if (!(world.getDeathConfig().getRespawnController() instanceof CityRespawn)
                || player.getGameMode() == GameMode.Creative) {
            return;
        }

        var armor = store.getComponent(ref, InventoryComponent.Armor.getComponentType()).getInventory();
        var dropped = Arrays.stream(armor.clear().getItems())
                .filter(stack -> !ItemStack.isEmpty(stack))
                .toList();
        if (dropped.isEmpty()) {
            return;
        }

        var transform = store.getComponent(ref, TransformComponent.getComponentType());
        var position = new Vector3d(transform.getPosition()).add(0, 1, 0);
        commands.addEntities(ItemComponent.generateItemDrops(store, dropped, position, transform.getRotation()),
                AddReason.SPAWN);
        death.setItemsLostOnDeath(dropped);
    }
}
