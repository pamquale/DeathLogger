package com.glisco.deathlog.client;

import com.glisco.deathlog.DeathInfo;
import com.glisco.deathlog.death_info.SpecialPropertyProvider;
import com.glisco.deathlog.death_info.properties.CoordinatesProperty;
import com.glisco.deathlog.death_info.properties.InventoryProperty;
import com.glisco.deathlog.death_info.properties.LocationProperty;
import com.glisco.deathlog.death_info.properties.ScoreProperty;
import com.glisco.deathlog.death_info.properties.ScreenshotProperty;
import com.glisco.deathlog.death_info.properties.StringProperty;
import com.glisco.deathlog.network.DeathLogPacket;
import com.glisco.deathlog.platform.Services;
import com.glisco.deathlog.storage.BaseDeathLogStorage;
import com.glisco.deathlog.storage.DeathInfoCreatedCallback;
import com.glisco.deathlog.storage.DirectDeathLogStorage;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * The client's own log, one file per world or server under
 * {@code <gamedir>/deathlogger}. Which file that is gets decided by the caller,
 * because only the loader module knows how to name it.
 */
public class ClientDeathLogStorage extends BaseDeathLogStorage implements DirectDeathLogStorage {

    private final List<DeathInfo> deathInfos;
    private final Path deathLogFile;
    private final String locationName;
    private final boolean multiplayer;

    public ClientDeathLogStorage(RegistryAccess registries, Path deathLogFile, String locationName, boolean multiplayer) {
        super(registries);
        this.deathLogFile = deathLogFile;
        this.locationName = locationName;
        this.multiplayer = multiplayer;
        this.deathInfos = load(deathLogFile);
    }

    @Override
    public List<DeathInfo> getDeathInfoList(UUID profile) {
        return this.deathInfos;
    }

    @Override
    public void delete(DeathInfo info, UUID profile) {
        if (!this.deathInfos.remove(info)) return;
        save(this.deathLogFile, this.deathInfos);
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
        deathInfo.setProperty(DeathInfo.LOCATION_KEY, new LocationProperty(this.locationName, this.multiplayer));
        deathInfo.setProperty(DeathInfo.SCORE_KEY, new ScoreProperty(
                player.getScore(), player.experienceLevel, player.experienceProgress, player.totalExperience
        ));
        deathInfo.setProperty(DeathInfo.DEATH_MESSAGE_KEY, new StringProperty(
                "deathlogger.deathinfoproperty.death_message", deathMessage.getString()
        ));
        deathInfo.setProperty(DeathInfo.TIME_OF_DEATH_KEY, new StringProperty(
                "deathlogger.deathinfoproperty.time_of_death", new Date().toString()
        ));

        if (DeathLogConfig.get().screenshotsEnabled()) {
            var fileName = DeathScreenshots.capture();
            if (fileName != null) deathInfo.setProperty(DeathInfo.SCREENSHOT_KEY, new ScreenshotProperty(fileName));
        }

        SpecialPropertyProvider.apply(deathInfo, player);
        DeathInfoCreatedCallback.invoke(deathInfo);

        this.deathInfos.add(deathInfo);
        save(this.deathLogFile, this.deathInfos);
    }

    @Override
    public void restore(int index, UUID profile) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;

        Services.NETWORK.sendToServer(new DeathLogPacket.RestoreRequest(player.getUUID(), index));
    }
}
