package me.aquaponics.custombosses.listeners;

import me.aquaponics.custombosses.CustomBosses;
import me.aquaponics.custombosses.bosses.golem.IronGolemBoss;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v1_20_R3.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class SpawnEggListener implements Listener {
    private CustomBosses plugin;
    public SpawnEggListener(CustomBosses plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSpawnEgg(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player p = e.getPlayer();
        ItemStack item = e.getItem();
        NamespacedKey golemKey = new NamespacedKey(plugin, "golemboss");
        if (item.getItemMeta().getPersistentDataContainer().has(golemKey, PersistentDataType.INTEGER)) {
            Level world = ((CraftWorld) p.getWorld()).getHandle();
            IronGolemBoss boss = new IronGolemBoss(EntityType.IRON_GOLEM, world);
            Location loc = e.getClickedBlock().getLocation();
            loc.getWorld().strikeLightningEffect(loc);
            boss.setPos(loc.getX(), loc.getY(), loc.getZ());
            world.addFreshEntity(boss);
            p.sendMessage(ChatColor.WHITE + "" + ChatColor.BOLD + "IRON GOLEM BOSS SPAWNED!");
            e.setCancelled(true);
            if (p.getGameMode() != GameMode.CREATIVE) {
                item.setAmount(item.getAmount() - 1);
            }

        }
    }
}
