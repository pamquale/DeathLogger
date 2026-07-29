package com.glisco.deathlog.storage;

import com.glisco.deathlog.DeathInfo;
import com.glisco.deathlog.death_info.SpecialPropertyProvider;
import com.glisco.deathlog.death_info.properties.CoordinatesProperty;
import com.glisco.deathlog.death_info.properties.InventoryProperty;
import com.glisco.deathlog.death_info.properties.LocationProperty;
import com.glisco.deathlog.death_info.properties.ScoreProperty;
import com.glisco.deathlog.death_info.properties.StringProperty;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Dedicated-server storage: one file per player under {@code <gamedir>/deaths}.
 */
public class ServerDeathLogStorage extends BaseDeathLogStorage {

    private final Map<UUID, List<DeathInfo>> deathInfos = new HashMap<>();
    private final Path deathLogDir;

    public ServerDeathLogStorage(RegistryAccess registries, Path deathLogDir) {
        super(registries);
        this.deathLogDir = deathLogDir;

        try {
            Files.createDirectories(deathLogDir);
        } catch (IOException e) {
            raiseError("Failed to create directory");
            LOGGER.error("Failed to create DeathLogger storage directory, further disk operations have been disabled", e);
            return;
        }

        try (Stream<Path> paths = Files.list(deathLogDir)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".dat"))
                    .forEach(this::loadProfile);
        } catch (IOException e) {
            raiseError("Unknown problem");
            LOGGER.error("Failed to load DeathLogger database, further disk operations have been disabled", e);
        }
    }

    /**
     * A file whose name is not a UUID is skipped rather than treated as fatal. The
     * old build disabled all disk operations for the session over a single stray
     * file in the directory.
     */
    private void loadProfile(Path path) {
        if (isErrored()) return;

        var name = path.getFileName().toString();
        var baseName = name.substring(0, name.length() - ".dat".length());

        final UUID uuid;
        try {
            uuid = UUID.fromString(baseName);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Ignoring '{}' in the DeathLogger directory: the file name is not a player UUID", name);
            return;
        }

        this.deathInfos.put(uuid, load(path));
    }

    private Path fileFor(UUID profile) {
        return this.deathLogDir.resolve(profile + ".dat");
    }

    @Override
    public List<DeathInfo> getDeathInfoList(UUID profile) {
        return this.deathInfos.computeIfAbsent(profile, uuid -> new ArrayList<>());
    }

    @Override
    public void delete(DeathInfo info, UUID profile) {
        var infoList = this.deathInfos.get(profile);
        if (infoList == null || !infoList.remove(info)) return;

        save(fileFor(profile), infoList);
    }

    @Override
    public void store(Component deathMessage, Player player) {
        final var deathInfo = new DeathInfo();

        deathInfo.setProperty(DeathInfo.INVENTORY_KEY, new InventoryProperty(player.getInventory()));
        deathInfo.setProperty(DeathInfo.COORDINATES_KEY, new CoordinatesProperty(player.blockPosition()));
        deathInfo.setProperty(DeathInfo.DIMENSION_KEY, new StringProperty(
                "deathlogger.deathinfoproperty.dimension",
                player.level().dimension().identifier().toString()
        ));
        deathInfo.setProperty(DeathInfo.LOCATION_KEY, new LocationProperty("Server", true));
        deathInfo.setProperty(DeathInfo.SCORE_KEY, new ScoreProperty(
                player.getScore(), player.experienceLevel, player.experienceProgress, player.totalExperience
        ));
        deathInfo.setProperty(DeathInfo.DEATH_MESSAGE_KEY, new StringProperty(
                "deathlogger.deathinfoproperty.death_message", deathMessage.getString()
        ));
        deathInfo.setProperty(DeathInfo.TIME_OF_DEATH_KEY, new StringProperty(
                "deathlogger.deathinfoproperty.time_of_death", new Date().toString()
        ));

        SpecialPropertyProvider.apply(deathInfo, player);
        DeathInfoCreatedCallback.invoke(deathInfo);

        var infoList = getDeathInfoList(player.getUUID());
        infoList.add(deathInfo);
        save(fileFor(player.getUUID()), infoList);
    }

    @Override
    public void restore(int index, UUID profile) {
        // Restoring is driven by the command and packet handlers, which call
        // DeathInfo#restore directly against the target player.
    }
}
