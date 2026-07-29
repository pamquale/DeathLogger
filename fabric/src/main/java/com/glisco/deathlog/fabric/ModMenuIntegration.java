package com.glisco.deathlog.fabric;

import com.glisco.deathlog.client.gui.DeathLogConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Puts the settings screen behind Mod Menu's config button. Mod Menu is a
 * compile-only dependency; the entrypoint is simply never called when it is
 * absent, so nothing here needs a presence check.
 */
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return DeathLogConfigScreen::new;
    }
}
