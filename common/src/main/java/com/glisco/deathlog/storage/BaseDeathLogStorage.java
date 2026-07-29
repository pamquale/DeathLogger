package com.glisco.deathlog.storage;

import com.glisco.deathlog.DeathInfo;
import com.glisco.deathlog.DeathLog;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.Util;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared NBT read/write for the on-disk storages.
 * <p>
 * Carries an <em>errored</em> latch: any disk failure calls {@link #raiseError},
 * which permanently disables further disk operations for the session. That is
 * deliberate - it stops a broken database from being overwritten with an empty
 * one on the next death.
 */
public abstract class BaseDeathLogStorage implements DeathLogStorage {

    /**
     * Bumped from 3 when serialization moved off owo's endec onto vanilla Codecs.
     * Revision 3 files cannot be read; {@link #load} moves them aside instead of
     * refusing to work, since there is no converter.
     */
    private static final int FORMAT_REVISION = 4;

    public static final Logger LOGGER = DeathLog.LOGGER;

    private final RegistryAccess registries;
    private boolean errored = false;

    protected BaseDeathLogStorage(RegistryAccess registries) {
        this.registries = registries;
    }

    protected List<DeathInfo> load(Path file) {
        if (this.errored) {
            LOGGER.warn("Attempted to load DeathLogger database even though disk operations are disabled");
            return new ArrayList<>();
        }

        if (!Files.exists(file)) return new ArrayList<>();

        final CompoundTag root;
        try {
            root = NbtIo.read(file);
        } catch (IOException e) {
            raiseError("Disk access failed");
            LOGGER.error("Failed to load DeathLogger database, further disk operations have been disabled", e);
            return new ArrayList<>();
        }

        int revision = root.getIntOr("FormatRevision", 0);
        if (revision != FORMAT_REVISION) {
            retireIncompatibleFile(file, revision);
            return new ArrayList<>();
        }

        var ops = RegistryOps.create(NbtOps.INSTANCE, this.registries);
        return DeathInfo.CODEC.listOf()
                .parse(ops, root.getListOrEmpty("Deaths"))
                .resultOrPartial(error -> LOGGER.error("Failed to decode death info: {}", error))
                .map(ArrayList::new)
                .orElseGet(ArrayList::new);
    }

    protected void save(Path file, List<DeathInfo> listIn) {
        if (this.errored) {
            LOGGER.warn("Attempted to save DeathLogger database even though disk operations are disabled");
            return;
        }

        final var snapshot = List.copyOf(listIn);
        Util.ioPool().execute(() -> {
            var ops = RegistryOps.create(NbtOps.INSTANCE, this.registries);
            var encoded = DeathInfo.CODEC.listOf().encodeStart(ops, snapshot);

            var deaths = encoded.resultOrPartial(error -> LOGGER.error("Failed to encode death info: {}", error));
            if (deaths.isEmpty()) return;

            final var root = new CompoundTag();
            root.put("Deaths", deaths.get());
            root.putInt("FormatRevision", FORMAT_REVISION);

            try {
                Files.createDirectories(file.getParent());
                NbtIo.write(root, file);
            } catch (IOException e) {
                LOGGER.error("Failed to save DeathLogger database", e);
            }
        });
    }

    /**
     * Renames a database this build cannot read instead of deleting it, so the old
     * data survives on disk even though nothing reads it any more.
     */
    private void retireIncompatibleFile(Path file, int revision) {
        var backup = file.resolveSibling(file.getFileName() + ".r" + revision + ".bak");
        try {
            Files.move(file, backup, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.warn(
                    "DeathLogger database at {} uses format revision {} (expected {}); moved it to {} and started a fresh log",
                    file, revision, FORMAT_REVISION, backup.getFileName()
            );
        } catch (IOException e) {
            raiseError("Incompatible format");
            LOGGER.error("Failed to move incompatible DeathLogger database aside, further disk operations have been disabled", e);
        }
    }

    @Override
    public boolean isErrored() {
        return this.errored;
    }

    protected void raiseError(String error) {
        this.errored = true;
    }

    @Override
    public RegistryAccess registries() {
        return this.registries;
    }
}
