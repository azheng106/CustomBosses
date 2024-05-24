package me.aquaponics.custombosses;

import me.aquaponics.custombosses.bosses.golem.GolemBossCommandExecutor;
import me.aquaponics.custombosses.listeners.SpawnEggListener;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class CustomBosses extends JavaPlugin implements Listener {
    @Override
    public void onEnable() {
        CustomBosses plugin = this;
        getCommand("golemboss").setExecutor(new GolemBossCommandExecutor(plugin));
        getServer().getPluginManager().registerEvents(new SpawnEggListener(this), this);
    }
}
