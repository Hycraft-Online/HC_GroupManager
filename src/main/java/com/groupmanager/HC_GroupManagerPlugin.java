package com.groupmanager;

import com.groupmanager.api.GroupManagerProvider;
import com.groupmanager.bridge.PartyGroupService;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;
import java.util.logging.Level;

/**
 * Shim plugin that implements the HaporeLab:GroupManager API
 * expected by ArenaPvP, delegating to HC_Party.
 */
public class HC_GroupManagerPlugin extends JavaPlugin {

    private PartyGroupService service;

    public HC_GroupManagerPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        super.setup();

        service = new PartyGroupService();
        GroupManagerProvider.set(service);

        getLogger().at(Level.INFO).log("HC_GroupManager (HaporeLab:GroupManager shim) loaded - GroupService registered");
    }

    @Override
    protected void shutdown() {
        super.shutdown();
        GroupManagerProvider.set(null);
        getLogger().at(Level.INFO).log("HC_GroupManager shutdown - GroupService unregistered");
    }
}
