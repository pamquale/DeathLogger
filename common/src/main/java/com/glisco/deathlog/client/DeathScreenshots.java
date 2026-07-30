package com.glisco.deathlog.client;

import com.glisco.deathlog.DeathLog;
import com.glisco.deathlog.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.util.Util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Grabs and stores the screenshot taken at the moment of death.
 */
public final class DeathScreenshots {

    private DeathScreenshots() {}

    public static Path directory() {
        return Services.PLATFORM.getGameDirectory().resolve(DeathLog.MOD_ID).resolve("screenshots");
    }

    public static Path resolve(String fileName) {
        return directory().resolve(fileName);
    }

    /**
     * Captures the current frame and writes it under a generated name, which is
     * returned immediately - the write itself happens on the callback. The caller
     * stores the name on the DeathInfo.
     */
    public static String capture() {
        var client = Minecraft.getInstance();
        if (client.getMainRenderTarget() == null) return null;

        var fileName = UUID.randomUUID() + ".png";
        var target = resolve(fileName);

        // The callback lands on the render thread; encoding a PNG there is what
        // made the game hitch at the moment of death. Hand it to the IO pool.
        Screenshot.takeScreenshot(client.getMainRenderTarget(), image -> Util.ioPool().execute(() -> {
            try (image) {
                Files.createDirectories(target.getParent());
                image.writeToFile(target);
            } catch (IOException e) {
                DeathLog.LOGGER.error("Failed to write the death screenshot", e);
            }
        }));

        return fileName;
    }
}
