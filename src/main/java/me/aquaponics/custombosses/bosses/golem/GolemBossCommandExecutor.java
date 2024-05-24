package me.aquaponics.custombosses.bosses.golem;

import me.aquaponics.custombosses.CustomBosses;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class GolemBossCommandExecutor implements CommandExecutor {
    private final CustomBosses plugin;
    public GolemBossCommandExecutor(CustomBosses plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player p) {
            ItemStack egg = new ItemStack(Material.IRON_GOLEM_SPAWN_EGG);
            ItemMeta eggMeta = egg.getItemMeta();
            eggMeta.setDisplayName(ChatColor.WHITE + "" + ChatColor.BOLD + "Golem Boss");
            NamespacedKey key = new NamespacedKey(plugin, "golemboss");
            eggMeta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 1);
            egg.setItemMeta(eggMeta);
            p.getInventory().addItem(egg);
        }
        return true;
    }
}
