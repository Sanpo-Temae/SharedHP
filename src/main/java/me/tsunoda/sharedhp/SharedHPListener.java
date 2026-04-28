package me.tsunoda.sharedhp;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffectType;

public final class SharedHPListener implements Listener {
    private final SharedHPPlugin plugin;
    private final SharedHPManager manager;

    public SharedHPListener(SharedHPPlugin plugin, SharedHPManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!manager.isActive() || manager.isSyncing()) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!manager.isParticipant(player)) {
            return;
        }

        double finalDamage = event.getFinalDamage();
        if (finalDamage <= 0.0) {
            return;
        }

        boolean lethal = manager.applyDamage(player, finalDamage);
        Entity knockbackSource = getKnockbackSource(event);
        manager.playSharedDamageFeedback(player, knockbackSource);

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!manager.isActive()) {
                return;
            }
            if (lethal || manager.getSharedHealth() <= 0.0) {
                manager.killAllParticipants();
                return;
            }
            manager.syncHealthToParticipants();
            manager.updateBossBar();
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRegainHealth(EntityRegainHealthEvent event) {
        if (!manager.isActive() || manager.isSyncing()) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!manager.isParticipant(player)) {
            return;
        }

        double originalAmount = event.getAmount();
        if (originalAmount <= 0.0) {
            return;
        }

        event.setCancelled(true);
        manager.applyHealing(originalAmount);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (!manager.isActive() || manager.isSyncing()) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!manager.isParticipant(player)) {
            return;
        }
        if (!PotionEffectType.ABSORPTION.equals(event.getModifiedType())) {
            return;
        }

        event.setCancelled(true);
        manager.clearAbsorption(player);
    }

    @EventHandler
    public void onItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        if (!manager.isActive() || !manager.isParticipant(player)) {
            return;
        }
        Material consumedType = event.getItem().getType();
        if (consumedType != Material.GOLDEN_APPLE && consumedType != Material.ENCHANTED_GOLDEN_APPLE) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> manager.clearAbsorption(player));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        manager.handleParticipantJoin(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        manager.handleParticipantQuit(event.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!manager.isActive() || !manager.isParticipant(player)) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> manager.handleParticipantJoin(player));
    }

    private Entity getKnockbackSource(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent damageByEntityEvent) {
            return damageByEntityEvent.getDamager();
        }
        return null;
    }
}
