package me.aquaponics.custombosses.bosses.golem;

import me.aquaponics.custombosses.CustomBosses;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class IronGolemBoss extends IronGolem {
    private final Plugin plugin = CustomBosses.getPlugin(CustomBosses.class);
    private final Level world;
    private int stallingTicks = 0;
    private int timesAttackedWithoutAttackingBack = 0;
    private final Component normalName = Component.literal("Golem Boss").withStyle(style -> style.withColor(TextColor.fromRgb(0xFFFFFF)).withBold(true));
    private final Component enrangedName = Component.literal("Golem Boss").withStyle(style -> style.withColor(TextColor.fromRgb(0xFF0000)).withBold(true));

    public IronGolemBoss(EntityType<? extends IronGolem> entitytypes, Level world) {
        super(entitytypes, world);
        this.world = world;
        this.setCustomName(normalName);
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(250);
        this.setHealth(250f);
        this.setCustomNameVisible(true);
    }

    @Override
    protected void registerGoals() {
        // MeleeAttackGoal params: mob (that will perform the attack), speedModifier while moving, followingTargetEvenIfNotSeen
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        // LookAtPlayerGoal params: mob, target, lookDistance (maximum), chance (optional)
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0F));
        // RandomStrollGoal params: mob, speedModifider
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 0.6D));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this)); // Retaliate when hurt
        // NearestAttackableTargetGoal params: mob, target, mustSee, mustReach (optional, default false), targetPredicate (optional, to filter target entities)
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) { // Called when another entity attacks the golem
        //TODO make bonemerang register as damage from player
        if (source.getEntity() instanceof Player nmsPlayer) {
            timesAttackedWithoutAttackingBack++;
            org.bukkit.entity.Player bukkitPlayer = (CraftPlayer) nmsPlayer.getBukkitEntity();
            if (Math.random() < 0.25) {
                bukkitPlayer.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 80, 1));
            }
            if (timesAttackedWithoutAttackingBack >= 4) {
                int amplifier = timesAttackedWithoutAttackingBack - 3;
                this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, amplifier));
                this.setCustomName(enrangedName);
            } else {
                this.setCustomName(normalName);
            }
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean doHurtTarget(Entity entity) { // Called when golem attacks another entity
        if (entity instanceof Player nmsPlayer) {
            org.bukkit.entity.Player player = (org.bukkit.entity.Player) nmsPlayer.getBukkitEntity();
            timesAttackedWithoutAttackingBack = 0;
            this.setCustomName(normalName);

            if (Math.random() < 0.25) {
                world.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, 1));
                player.sendMessage(ChatColor.AQUA + "" + ChatColor.ITALIC + "Golem dealt a crushing blow!");
            }
        }
        return super.doHurtTarget(entity);
    }

    @Override
    public void tick() { // Called each tick
        super.tick();
        org.bukkit.entity.Player bukkitPlayer = null;
        if (!plugin.isEnabled()) return;

        if (this.getTarget() instanceof Player nmsPlayer) {
            bukkitPlayer = (CraftPlayer) nmsPlayer.getBukkitEntity();
        }

        Location bossLoc = this.getBukkitEntity().getLocation();
        if (Math.random() < 0.008) {
            performSeismicWaveAttack(bossLoc);
        }

        // Prevent player from cheesing the boss by towering up out of range
        if (bukkitPlayer != null && (bukkitPlayer.getLocation().getBlockY() - bossLoc.getBlockY()) >= 3) {
            stallingTicks++;
        } else {
            stallingTicks = 0;
        }
        if (stallingTicks > 60) {
            bukkitPlayer.damage(6, this.getBukkitEntity());
            Location playerLoc = bukkitPlayer.getLocation();
            Vector direction = playerLoc.toVector().subtract(bossLoc.toVector()).normalize();
            Vector knockback = direction.setY(0.5);
            bukkitPlayer.setVelocity(knockback);
            stallingTicks = 0;
        }

    }

    private void performSeismicWaveAttack(Location bossLoc) {
        org.bukkit.World bukkitWorld = this.world.getWorld();
        org.bukkit.entity.Player target = getNearestPlayer(bossLoc);
        if (target == null) {
            return;
        }
        Vector targetDirection = target.getLocation().toVector().subtract(bossLoc.toVector()).normalize();

        // Make seismic wave effect, a rolling earthquake towards the player (like Valorant breach ult)
        new BukkitRunnable() {
            int steps = 1;
            final int maxSteps = 20;
            final List<Location> lastBlockLocs = new ArrayList<>();
            @Override
            public void run() {
                // Change previously raised blocks to their original position
                Iterator<Location> it = lastBlockLocs.iterator();
                while (it.hasNext()) { // Use iterator instead of for-each to remove the item
                    Location loc = it.next();
                    Location oneBelowLastBlockLoc = loc.clone().subtract(0, 1, 0);
                    oneBelowLastBlockLoc.getBlock().setType(loc.getBlock().getType());
                    loc.getBlock().setType(Material.AIR);
                    it.remove();
                }
                if (steps > maxSteps) {
                    this.cancel();
                    return;
                }

                // Move blocks in the wave's path up by one block, like an earthquake
                Location currentLoc = bossLoc.clone().add(targetDirection.clone().multiply(steps*0.5));
                Vector perpendicular = new Vector(-targetDirection.getZ(), 0, targetDirection.getX()).normalize();
                for (int i = -1; i <= 1; i++) { // Wave has width of 3
                    Location offsetLoc = currentLoc.clone().add(perpendicular.clone().multiply(i));
                    Block currentBlock = offsetLoc.getBlock();
                    while (currentBlock.getType().isAir() &&
                            Math.abs(offsetLoc.getY() - bossLoc.getY()) < 4) {
                        offsetLoc = offsetLoc.subtract(0, 1, 0);
                        currentBlock = offsetLoc.getBlock();
                    }
                    Location oneAboveCurrentLoc = offsetLoc.clone().add(0, 1, 0);
                    oneAboveCurrentLoc.getBlock().setType(currentBlock.getType());
                    currentBlock.setType(Material.AIR);
                    lastBlockLocs.add(oneAboveCurrentLoc);
                    bukkitWorld.spawnParticle(Particle.BLOCK_CRACK, offsetLoc, 1000, currentBlock.getBlockData());
                    for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
                        if (player.getLocation().distance(offsetLoc) < 1.5) {
                            player.damage(2, IronGolemBoss.this.getBukkitEntity());
                            player.setVelocity(targetDirection.clone().multiply(0.5));
                        }
                    }
                }
                steps++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // Find the player nearest to param location
    private org.bukkit.entity.Player getNearestPlayer(Location location) {
        double closestDistance = Double.MAX_VALUE;
        org.bukkit.entity.Player nearest = null;

        for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
            double distance = player.getLocation().distance(location);
            if (distance < closestDistance && player.getGameMode() == GameMode.SURVIVAL) {
                closestDistance = distance;
                nearest = player;
            }
        }
        return nearest;
    }
}
