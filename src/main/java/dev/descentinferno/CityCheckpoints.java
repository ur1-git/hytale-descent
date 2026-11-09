package dev.descentinferno;

import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.UUID;

final class CityCheckpoints {
    private static final double CHECK_IN_DISTANCE = 12;
    private final Path file;
    private Properties data = new Properties();

    CityCheckpoints(Path directory) throws IOException {
        Files.createDirectories(directory);
        file = directory.resolve("checkpoints.properties");
        if (Files.exists(file)) {
            try (var input = Files.newInputStream(file)) {
                data.load(input);
            }
        }
    }

    synchronized String create(UUID world, String city, Vector3dc position) throws IOException {
        String key = cityKey(world, city);
        if (data.containsKey(key)) {
            return "That city already exists in this world.";
        }
        save(key, position.x() + " " + position.y() + " " + position.z());
        return "Created city checkpoint " + city + " at your feet.";
    }

    synchronized String checkIn(UUID world, UUID player, String city, Vector3dc position) throws IOException {
        Vector3d destination = cityPosition(world, city);
        if (destination == null) {
            return "Unknown city checkpoint in this world.";
        }
        if (destination.distanceSquared(position) > CHECK_IN_DISTANCE * CHECK_IN_DISTANCE) {
            return "Get within 12 blocks of the city checkpoint to check in.";
        }
        String key = playerKey(world, player);
        if (!city.equals(data.getProperty(key))) {
            save(key, city);
        }
        return "Your respawn checkpoint is now " + city + ".";
    }

    synchronized String selectedCity(UUID world, UUID player) {
        return data.getProperty(playerKey(world, player));
    }

    synchronized Vector3d destination(UUID world, UUID player) {
        String city = selectedCity(world, player);
        return city == null ? null : cityPosition(world, city);
    }

    private Vector3d cityPosition(UUID world, String city) {
        String value = data.getProperty(cityKey(world, city));
        if (value == null) {
            return null;
        }
        String[] coordinates = value.split(" ");
        if (coordinates.length != 3) {
            throw new IllegalStateException("Invalid coordinates for city " + city);
        }
        return new Vector3d(Double.parseDouble(coordinates[0]), Double.parseDouble(coordinates[1]),
                Double.parseDouble(coordinates[2]));
    }

    private void save(String key, String value) throws IOException {
        Properties updated = new Properties();
        updated.putAll(data);
        updated.setProperty(key, value);
        Path temporary = Files.createTempFile(file.getParent(), "checkpoints-", ".tmp");
        try {
            try (var output = Files.newOutputStream(temporary)) {
                updated.store(output, "Descent Inferno city checkpoints");
            }
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            data = updated;
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static String cityKey(UUID world, String city) {
        return "city." + world + "." + city;
    }

    private static String playerKey(UUID world, UUID player) {
        return "player." + world + "." + player;
    }
}
