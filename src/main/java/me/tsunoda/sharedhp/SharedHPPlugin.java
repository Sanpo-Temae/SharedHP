package me.tsunoda.sharedhp;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class SharedHPPlugin extends JavaPlugin {
    private SharedHPManager manager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        manager = new SharedHPManager(this);
        manager.startAbsorptionCleanupTask();

        SharedHPCommand sharedHPCommand = new SharedHPCommand(manager);
        PluginCommand command = getCommand("sharedhp");
        if (command != null) {
            command.setExecutor(sharedHPCommand);
            command.setTabCompleter(sharedHPCommand);
        } else {
            getLogger().warning("sharedhp command is not defined in plugin.yml");
        }

        getServer().getPluginManager().registerEvents(new SharedHPListener(this, manager), this);
        getLogger().info("SharedHP enabled");
    }

    @Override
    public void onDisable() {
        if (manager != null) {
            manager.shutdown();
        }
        getLogger().info("SharedHP disabled");
    }
}
