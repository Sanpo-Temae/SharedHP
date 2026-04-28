package me.tsunoda.sharedhp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.command.CommandSender;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public final class SharedHPManager {
    private static final String DEFAULT_BOSSBAR_TITLE_FORMAT = "共有HP: %.1f / %.1f | 回復効率%.0f%%";
    private static final String LEGACY_BOSSBAR_TITLE_FORMAT = "共有HP: %.1f / %.1f | 回復効率25%%";
    private static final double DEFAULT_KNOCKBACK_STRENGTH = 0.5;
    private static final double MIN_KNOCKBACK_STRENGTH = 0.1;
    private static final double MAX_KNOCKBACK_STRENGTH = 2.0;
    private static final double MIN_KNOCKBACK_DIRECTION_LENGTH_SQUARED = 0.000001;
    private static final double SYNTHETIC_KNOCKBACK_VERTICAL_BOOST = 0.32;

    private final SharedHPPlugin plugin;
    private final Set<UUID> participants = new LinkedHashSet<>();
    private final Map<UUID, Double> damageTotals = new HashMap<>();

    private double maxSharedHealth;
    private double healMultiplier;
    private String bossBarTitleFormat;
    private double sharedHealth;
    private boolean active;
    private boolean syncing;
    private BossBar bossBar;
    private BukkitTask absorptionCleanupTask;

    public SharedHPManager(SharedHPPlugin plugin) {
        this.plugin = plugin;
        reloadSettings();
        this.sharedHealth = maxSharedHealth;
        this.bossBar = Bukkit.createBossBar(formatBossBarTitle(), BarColor.RED, BarStyle.SEGMENTED_10);
        this.bossBar.setVisible(false);
    }

    public void reloadSettings() {
        try {
            plugin.reloadConfig();
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to reload config.yml. Using available defaults.", exception);
        }

        FileConfiguration config = plugin.getConfig();

        participants.clear();
        for (String rawUuid : config.getStringList("participants")) {
            try {
                participants.add(UUID.fromString(rawUuid));
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Invalid participant UUID in config.yml: " + rawUuid);
            }
        }

        maxSharedHealth = Math.max(1.0, config.getDouble("max-shared-health", 20.0));
        healMultiplier = Math.max(0.0, config.getDouble("heal-multiplier", 0.25));
        bossBarTitleFormat = config.getString(
                "bossbar-title-format",
                DEFAULT_BOSSBAR_TITLE_FORMAT
        );
    }

    public void startAbsorptionCleanupTask() {
        if (absorptionCleanupTask != null) {
            return;
        }

        absorptionCleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!active) {
                return;
            }
            for (Player player : getOnlineParticipants()) {
                clearAbsorption(player);
            }
        }, 1L, 5L);
    }

    public void shutdown() {
        active = false;
        if (absorptionCleanupTask != null) {
            absorptionCleanupTask.cancel();
            absorptionCleanupTask = null;
        }
        hideBossBar();
    }

    public boolean addParticipant(Player player) {
        boolean added = participants.add(player.getUniqueId());
        if (added) {
            saveParticipants();
            if (active) {
                bossBar.addPlayer(player);
                syncHealthToParticipants();
                updateBossBar();
            }
        }
        return added;
    }

    public boolean removeParticipant(UUID uuid) {
        boolean removed = participants.remove(uuid);
        if (removed) {
            damageTotals.remove(uuid);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && bossBar != null) {
                bossBar.removePlayer(player);
            }
            saveParticipants();
            updateBossBar();
        }
        return removed;
    }

    public void saveParticipants() {
        List<String> values = participants.stream()
                .map(UUID::toString)
                .toList();
        plugin.getConfig().set("participants", values);
        plugin.saveConfig();
    }

    public void start() {
        active = true;
        sharedHealth = maxSharedHealth;
        resetDamageTotals();
        refreshBossBarPlayers();
        updateBossBar();
        syncHealthToParticipants();
    }

    public void stop() {
        active = false;
        hideBossBar();
    }

    public void resetSharedHealth() {
        sharedHealth = maxSharedHealth;
        resetDamageTotals();
        if (active) {
            syncHealthToParticipants();
            updateBossBar();
        }
    }

    public void setSharedHealth(double value) {
        sharedHealth = clampSharedHealth(value);
        if (!active) {
            updateBossBar();
            return;
        }

        if (sharedHealth <= 0.0) {
            updateBossBar();
            killAllParticipants();
            return;
        }

        syncHealthToParticipants();
        updateBossBar();
    }

    public void setHealMultiplier(double value) {
        healMultiplier = Math.max(0.0, value);
        FileConfiguration config = plugin.getConfig();
        config.set("heal-multiplier", healMultiplier);

        String configuredTitle = config.getString("bossbar-title-format", DEFAULT_BOSSBAR_TITLE_FORMAT);
        if (LEGACY_BOSSBAR_TITLE_FORMAT.equals(configuredTitle)) {
            bossBarTitleFormat = DEFAULT_BOSSBAR_TITLE_FORMAT;
            config.set("bossbar-title-format", DEFAULT_BOSSBAR_TITLE_FORMAT);
        }

        plugin.saveConfig();
        updateBossBar();
    }

    public boolean applyDamage(Player damagedPlayer, double finalDamage) {
        if (finalDamage <= 0.0) {
            return false;
        }

        addDamageTotal(damagedPlayer, finalDamage);
        sharedHealth = clampSharedHealth(sharedHealth - finalDamage);
        updateBossBar();
        return sharedHealth <= 0.0;
    }

    public void resetDamageTotals() {
        damageTotals.clear();
        for (UUID uuid : participants) {
            damageTotals.put(uuid, 0.0);
        }
    }

    public void broadcastDamageRanking(org.bukkit.command.CommandSender requester) {
        List<String> lines = buildDamageRankingLines();
        Set<UUID> delivered = new LinkedHashSet<>();
        for (Player player : getOnlineParticipants()) {
            for (String line : lines) {
                player.sendMessage(line);
            }
            delivered.add(player.getUniqueId());
        }

        if (!(requester instanceof Player player) || !delivered.contains(player.getUniqueId())) {
            for (String line : lines) {
                requester.sendMessage(line);
            }
        }
    }

    public void playSharedDamageFeedback(Player damagedPlayer, Entity source) {
        Vector sourceDirection = source == null ? null : getKnockbackSourceDirection(damagedPlayer, source);
        double strength = source == null ? 0.0 : getKnockbackStrength(source);
        for (Player player : getOnlineParticipants()) {
            if (player.isDead() || player.getUniqueId().equals(damagedPlayer.getUniqueId())) {
                continue;
            }
            playSyntheticHurtFeedback(player, sourceDirection, strength);
        }
    }

    public void applyHealing(double amount) {
        if (amount <= 0.0) {
            return;
        }

        sharedHealth = clampSharedHealth(sharedHealth + (amount * healMultiplier));
        syncHealthToParticipants();
        updateBossBar();
    }

    public void syncHealthToParticipants() {
        syncing = true;
        try {
            for (Player player : getOnlineParticipants()) {
                if (player.isDead()) {
                    continue;
                }
                clearAbsorption(player);
                double health = Math.min(sharedHealth, getMaxHealth(player));
                health = Math.max(0.0, health);
                player.setHealth(health);
            }
        } finally {
            syncing = false;
        }
    }

    public void killAllParticipants() {
        syncing = true;
        try {
            for (Player player : getOnlineParticipants()) {
                clearAbsorption(player);
                if (!player.isDead()) {
                    player.setHealth(0.0);
                }
            }
        } finally {
            syncing = false;
        }
        updateBossBar();
    }

    public void clearAbsorption(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        player.setAbsorptionAmount(0.0);
        player.removePotionEffect(PotionEffectType.ABSORPTION);
    }

    public void handleParticipantJoin(Player player) {
        if (!isParticipant(player) || !active) {
            return;
        }
        bossBar.addPlayer(player);
        clearAbsorption(player);
        if (sharedHealth > 0.0) {
            syncHealthToParticipants();
        }
        updateBossBar();
    }

    public void handleParticipantQuit(Player player) {
        if (bossBar != null) {
            bossBar.removePlayer(player);
        }
    }

    public void refreshBossBarPlayers() {
        if (bossBar == null) {
            return;
        }

        for (Player player : new ArrayList<>(bossBar.getPlayers())) {
            if (!player.isOnline() || !isParticipant(player) || !active) {
                bossBar.removePlayer(player);
            }
        }

        if (!active) {
            bossBar.setVisible(false);
            return;
        }

        for (Player player : getOnlineParticipants()) {
            bossBar.addPlayer(player);
        }
        bossBar.setVisible(true);
    }

    public void updateBossBar() {
        if (bossBar == null) {
            return;
        }

        bossBar.setTitle(formatBossBarTitle());
        bossBar.setProgress(clampProgress(sharedHealth / maxSharedHealth));
        refreshBossBarPlayers();
    }

    public void hideBossBar() {
        if (bossBar == null) {
            return;
        }
        bossBar.removeAll();
        bossBar.setVisible(false);
    }

    public UUID findParticipantByName(String name) {
        Player onlinePlayer = Bukkit.getPlayerExact(name);
        if (onlinePlayer != null && participants.contains(onlinePlayer.getUniqueId())) {
            return onlinePlayer.getUniqueId();
        }

        for (UUID uuid : participants) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            String knownName = offlinePlayer.getName();
            if (knownName != null && knownName.equalsIgnoreCase(name)) {
                return uuid;
            }
        }

        return null;
    }

    public String getDisplayName(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            return player.getName();
        }

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        String name = offlinePlayer.getName();
        return name != null ? name : uuid.toString();
    }

    public List<Player> getOnlineParticipants() {
        List<Player> players = new ArrayList<>();
        for (UUID uuid : participants) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                players.add(player);
            }
        }
        return players;
    }

    public List<String> getOfflineParticipantDisplayNames() {
        List<String> names = new ArrayList<>();
        for (UUID uuid : participants) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                names.add(getDisplayName(uuid));
            }
        }
        return names;
    }

    public List<UUID> getParticipants() {
        return Collections.unmodifiableList(new ArrayList<>(participants));
    }

    public boolean isParticipant(Player player) {
        return player != null && participants.contains(player.getUniqueId());
    }

    public boolean hasParticipant(UUID uuid) {
        return participants.contains(uuid);
    }

    public double clampSharedHealth(double value) {
        return Math.max(0.0, Math.min(maxSharedHealth, value));
    }

    public boolean isActive() {
        return active;
    }

    public boolean isSyncing() {
        return syncing;
    }

    public double getSharedHealth() {
        return sharedHealth;
    }

    public double getMaxSharedHealth() {
        return maxSharedHealth;
    }

    public double getHealMultiplier() {
        return healMultiplier;
    }

    public int getParticipantCount() {
        return participants.size();
    }

    public boolean isBossBarShown() {
        return bossBar != null && bossBar.isVisible() && !bossBar.getPlayers().isEmpty();
    }

    public void logAction(CommandSender sender, String action) {
        plugin.getLogger().info(sender.getName() + " " + action);
    }

    private double getMaxHealth(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
        if (attribute == null) {
            return maxSharedHealth;
        }
        return Math.max(1.0, attribute.getValue());
    }

    private void addDamageTotal(Player damagedPlayer, double finalDamage) {
        if (damagedPlayer == null) {
            return;
        }
        damageTotals.merge(damagedPlayer.getUniqueId(), finalDamage, Double::sum);
    }

    private List<String> buildDamageRankingLines() {
        List<UUID> sortedParticipants = new ArrayList<>(participants);
        sortedParticipants.sort(Comparator
                .comparingDouble((UUID uuid) -> damageTotals.getOrDefault(uuid, 0.0))
                .reversed()
                .thenComparing(this::getDisplayName, String.CASE_INSENSITIVE_ORDER));

        List<String> lines = new ArrayList<>();
        lines.add("共有HP 被ダメージランキング");
        if (sortedParticipants.isEmpty()) {
            lines.add("対象者はまだ登録されていません。");
            return lines;
        }

        int rank = 1;
        for (UUID uuid : sortedParticipants) {
            lines.add(rank + ". " + getDisplayName(uuid)
                    + " - " + String.format(Locale.JAPAN, "%.1f", damageTotals.getOrDefault(uuid, 0.0)));
            rank++;
        }
        return lines;
    }

    private Vector getKnockbackSourceDirection(Player damagedPlayer, Entity source) {
        Vector direction = source.getLocation().toVector().subtract(damagedPlayer.getLocation().toVector());
        direction.setY(0.0);

        if (direction.lengthSquared() <= MIN_KNOCKBACK_DIRECTION_LENGTH_SQUARED
                && source instanceof Projectile projectile) {
            direction = projectile.getVelocity().multiply(-1.0);
            direction.setY(0.0);
        }

        if (direction.lengthSquared() <= MIN_KNOCKBACK_DIRECTION_LENGTH_SQUARED) {
            return null;
        }

        return direction.normalize();
    }

    private double getKnockbackStrength(Entity source) {
        double strength = DEFAULT_KNOCKBACK_STRENGTH;
        if (source instanceof LivingEntity livingEntity) {
            AttributeInstance attackKnockback = livingEntity.getAttribute(Attribute.ATTACK_KNOCKBACK);
            if (attackKnockback != null) {
                strength += Math.max(0.0, attackKnockback.getValue());
            }
        }

        return Math.max(MIN_KNOCKBACK_STRENGTH, Math.min(MAX_KNOCKBACK_STRENGTH, strength));
    }

    private void playSyntheticHurtFeedback(Player player, Vector sourceDirection, double strength) {
        float hurtYaw = sourceDirection == null ? player.getLocation().getYaw() : getHurtYaw(sourceDirection);
        player.playHurtAnimation(hurtYaw);
        player.sendHurtAnimation(hurtYaw);

        Sound hurtSound = player.getHurtSound();
        if (hurtSound != null) {
            player.getWorld().playSound(player.getLocation(), hurtSound, SoundCategory.PLAYERS, 1.0f, 1.0f);
        }

        if (sourceDirection == null) {
            return;
        }

        Vector awayFromSource = sourceDirection.clone().multiply(-1.0);
        Vector velocity = player.getVelocity().clone();
        velocity.setX(velocity.getX() + awayFromSource.getX() * strength);
        velocity.setY(Math.max(velocity.getY(), SYNTHETIC_KNOCKBACK_VERTICAL_BOOST));
        velocity.setZ(velocity.getZ() + awayFromSource.getZ() * strength);
        player.setVelocity(velocity);
    }

    private float getHurtYaw(Vector sourceDirection) {
        Vector awayFromSource = sourceDirection.clone().multiply(-1.0);
        return (float) Math.toDegrees(Math.atan2(-awayFromSource.getX(), awayFromSource.getZ()));
    }

    private String formatBossBarTitle() {
        try {
            return String.format(Locale.JAPAN, bossBarTitleFormat, sharedHealth, maxSharedHealth, healMultiplier * 100.0);
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.WARNING, "Invalid bossbar-title-format in config.yml", exception);
            return String.format(Locale.JAPAN, DEFAULT_BOSSBAR_TITLE_FORMAT,
                    sharedHealth, maxSharedHealth, healMultiplier * 100.0);
        }
    }

    private double clampProgress(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
