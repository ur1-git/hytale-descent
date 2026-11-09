package dev.descentinferno;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.asset.type.gameplay.respawn.RespawnController;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import dev.descentinferno.commerce.Commerce;
import dev.descentinferno.mining.MiningProtection;
import dev.descentinferno.teams.Teams;

import java.io.IOException;
import java.io.UncheckedIOException;

public final class DescentInfernoPlugin extends JavaPlugin {
    public DescentInfernoPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        CityCheckpoints checkpoints;
        try {
            checkpoints = new CityCheckpoints(getDataDirectory());
            Teams.register(this);
            Commerce.register(this);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot load Descent Inferno data", e);
        }
        var codec = BuilderCodec.builder(CityRespawn.class, () -> new CityRespawn(checkpoints)).build();
        getCodecRegistry(RespawnController.CODEC).register("DescentInfernoCity", CityRespawn.class, codec);
        getCommandRegistry().registerCommand(new CityCommand(checkpoints));
        getEntityStoreRegistry().registerSystem(new ArmorDrops());
        MiningProtection.register(this);
    }
}
