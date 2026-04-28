package me.tsunoda.sharedhp;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class SharedHPCommand implements CommandExecutor, TabCompleter {
    private static final String ADMIN_PERMISSION = "sharedhp.admin";
    private static final String VIEW_PERMISSION = "sharedhp.view";
    private final SharedHPManager manager;

    public SharedHPCommand(SharedHPManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!hasAnySharedHPPermission(sender)) {
                sendNoPermission(sender, VIEW_PERMISSION);
                return true;
            }
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        String requiredPermission = getRequiredPermission(subCommand, args);
        if (!hasPermission(sender, requiredPermission)) {
            sendNoPermission(sender, requiredPermission);
            return true;
        }

        switch (subCommand) {
            case "add" -> handleAdd(sender, args);
            case "damage" -> handleDamage(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "list" -> handleList(sender);
            case "start" -> handleStart(sender, args);
            case "stop" -> handleStop(sender);
            case "reset" -> handleReset(sender);
            case "set" -> handleSet(sender, args);
            case "heal" -> handleHeal(sender, args);
            case "status" -> handleStatus(sender);
            default -> sendHelp(sender);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!hasAnySharedHPPermission(sender)) {
            return List.of();
        }

        if (args.length == 1) {
            List<String> commands = new ArrayList<>(List.of("damage", "list", "status"));
            if (hasPermission(sender, ADMIN_PERMISSION)) {
                commands.addAll(List.of("add", "remove", "start", "stop", "reset", "set", "heal"));
            }
            return filter(commands, args[0]);
        }

        if (args.length == 2) {
            String subCommand = args[0].toLowerCase(Locale.ROOT);
            if (subCommand.equals("damage") && hasPermission(sender, ADMIN_PERMISSION)) {
                return filter(List.of("reset"), args[1]);
            }
            if (subCommand.equals("add") && hasPermission(sender, ADMIN_PERMISSION)) {
                return filter(Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .toList(), args[1]);
            }
            if (subCommand.equals("remove") && hasPermission(sender, ADMIN_PERMISSION)) {
                return filter(manager.getParticipants().stream()
                        .map(manager::getDisplayName)
                        .toList(), args[1]);
            }
            if (subCommand.equals("start") && hasPermission(sender, ADMIN_PERMISSION)) {
                return filter(List.of("force"), args[1]);
            }
            if (subCommand.equals("set") && hasPermission(sender, ADMIN_PERMISSION)) {
                return filter(List.of("20", "10", "5", "1"), args[1]);
            }
            if (subCommand.equals("heal") && hasPermission(sender, ADMIN_PERMISSION)) {
                return filter(List.of("25", "50", "75", "100"), args[1]);
            }
        }

        return List.of();
    }

    private void handleAdd(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("使い方: /sharedhp add <player>");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage("オンライン中のプレイヤーを指定してください: " + args[1]);
            return;
        }

        if (manager.hasParticipant(target.getUniqueId())) {
            sender.sendMessage(target.getName() + " はすでに対象者です。");
            return;
        }

        manager.addParticipant(target);
        manager.logAction(sender, "added participant " + target.getName() + " (" + target.getUniqueId() + ")");
        sender.sendMessage(target.getName() + " を共有HP対象者に追加しました。");
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("使い方: /sharedhp remove <player>");
            return;
        }

        UUID uuid = manager.findParticipantByName(args[1]);
        if (uuid == null) {
            sender.sendMessage("対象者が見つかりません: " + args[1]);
            return;
        }

        String displayName = manager.getDisplayName(uuid);
        manager.removeParticipant(uuid);
        manager.logAction(sender, "removed participant " + displayName + " (" + uuid + ")");
        sender.sendMessage(displayName + " を共有HP対象者から外しました。");
    }

    private void handleDamage(CommandSender sender, String[] args) {
        if (args.length >= 2 && args[1].equalsIgnoreCase("reset")) {
            manager.resetDamageTotals();
            manager.logAction(sender, "reset damage ranking");
            sender.sendMessage("被ダメージランキングをリセットしました。");
            return;
        }

        manager.broadcastDamageRanking(sender);
    }

    private void handleList(CommandSender sender) {
        if (manager.getParticipantCount() == 0) {
            sender.sendMessage("共有HP対象者はまだ登録されていません。");
            return;
        }

        sender.sendMessage("共有HP対象者:");
        for (UUID uuid : manager.getParticipants()) {
            Player player = Bukkit.getPlayer(uuid);
            String status = player != null && player.isOnline() ? "オンライン" : "オフライン";
            sender.sendMessage("- " + manager.getDisplayName(uuid) + " (" + status + ")");
        }
    }

    private void handleStart(CommandSender sender, String[] args) {
        boolean force = args.length >= 2 && args[1].equalsIgnoreCase("force");

        if (manager.isActive()) {
            sender.sendMessage("共有HPシステムはすでに開始されています。HPを戻したい場合は /sharedhp reset を使ってください。");
            return;
        }

        if (manager.getParticipantCount() == 0) {
            sender.sendMessage("共有HP対象者が登録されていないため開始できません。/sharedhp add <player> で追加してください。");
            return;
        }

        if (manager.getOnlineParticipants().isEmpty()) {
            sender.sendMessage("オンラインの対象者が1人もいないため開始できません。");
            return;
        }

        manager.start();
        manager.logAction(sender, "started SharedHP with " + manager.getOnlineParticipants().size() + " online participant(s)"
                + (force ? " using force compatibility flag" : ""));
        sender.sendMessage(force
                ? "共有HPシステムを開始しました。"
                : "共有HPシステムを開始しました。");
    }

    private void handleStop(CommandSender sender) {
        manager.stop();
        manager.logAction(sender, "stopped SharedHP");
        sender.sendMessage("共有HPシステムを停止しました。");
    }

    private void handleReset(CommandSender sender) {
        manager.resetSharedHealth();
        manager.logAction(sender, "reset shared health and damage ranking");
        sender.sendMessage("共有HPを " + formatHealth(manager.getMaxSharedHealth()) + " にリセットしました。");
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("使い方: /sharedhp set <value>");
            return;
        }

        double value;
        try {
            value = Double.parseDouble(args[1]);
        } catch (NumberFormatException exception) {
            sender.sendMessage("数値を指定してください: " + args[1]);
            return;
        }

        manager.setSharedHealth(value);
        manager.logAction(sender, "set shared health to " + formatHealth(manager.getSharedHealth()));
        sender.sendMessage("共有HPを " + formatHealth(manager.getSharedHealth()) + " に設定しました。");
    }

    private void handleHeal(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("使い方: /sharedhp heal <percent>");
            sender.sendMessage("例: /sharedhp heal 25 で回復効率を25%にします。");
            return;
        }

        String rawValue = args[1].endsWith("%") ? args[1].substring(0, args[1].length() - 1) : args[1];
        double percent;
        try {
            percent = Double.parseDouble(rawValue);
        } catch (NumberFormatException exception) {
            sender.sendMessage("0〜100のパーセント数値を指定してください: " + args[1]);
            return;
        }

        if (!Double.isFinite(percent) || percent < 0.0 || percent > 100.0) {
            sender.sendMessage("回復効率は0〜100の範囲で指定してください。例: /sharedhp heal 25");
            return;
        }

        if (percent > 0.0 && percent < 1.0) {
            sender.sendMessage("回復効率はパーセントで指定します。25%なら /sharedhp heal 25 です。");
            return;
        }

        manager.setHealMultiplier(percent / 100.0);
        manager.logAction(sender, "set heal multiplier to " + formatMultiplier(manager.getHealMultiplier())
                + " (" + formatPercent(manager.getHealMultiplier() * 100.0) + "%)");
        sender.sendMessage("回復効率を " + formatPercent(manager.getHealMultiplier() * 100.0) + "% に設定しました。");
        if (manager.isActive()) {
            sender.sendMessage("この設定は次の回復から反映されます。");
        }
    }

    private void handleStatus(CommandSender sender) {
        List<String> onlineNames = manager.getOnlineParticipants().stream()
                .map(Player::getName)
                .toList();

        sender.sendMessage("SharedHP status");
        sender.sendMessage("- active: " + manager.isActive());
        sender.sendMessage("- sharedHealth: " + formatHealth(manager.getSharedHealth())
                + " / " + formatHealth(manager.getMaxSharedHealth()));
        sender.sendMessage("- 登録人数: " + manager.getParticipantCount());
        sender.sendMessage("- オンライン対象者: "
                + (onlineNames.isEmpty() ? "なし" : String.join(", ", onlineNames)));
        sender.sendMessage("- ボスバー表示: " + manager.isBossBarShown());
        sender.sendMessage("- 回復倍率: " + formatMultiplier(manager.getHealMultiplier())
                + " (" + formatPercent(manager.getHealMultiplier() * 100.0) + "%)");
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("SharedHP commands:");
        sender.sendMessage("/sharedhp status");
        sender.sendMessage("/sharedhp add <player>");
        sender.sendMessage("/sharedhp remove <player>");
        sender.sendMessage("/sharedhp list");
        sender.sendMessage("/sharedhp start [force]");
        sender.sendMessage("/sharedhp stop");
        sender.sendMessage("/sharedhp reset");
        sender.sendMessage("/sharedhp set <value>");
        sender.sendMessage("/sharedhp heal <percent>");
        sender.sendMessage("/sharedhp damage [reset]");
    }

    private String getRequiredPermission(String subCommand, String[] args) {
        if (subCommand.equals("damage") && !(args.length >= 2 && args[1].equalsIgnoreCase("reset"))) {
            return VIEW_PERMISSION;
        }
        if (subCommand.equals("list") || subCommand.equals("status")) {
            return VIEW_PERMISSION;
        }
        if (!List.of("add", "damage", "remove", "start", "stop", "reset", "set", "heal").contains(subCommand)) {
            return VIEW_PERMISSION;
        }
        return ADMIN_PERMISSION;
    }

    private boolean hasAnySharedHPPermission(CommandSender sender) {
        return hasPermission(sender, VIEW_PERMISSION) || hasPermission(sender, ADMIN_PERMISSION);
    }

    private boolean hasPermission(CommandSender sender, String permission) {
        if (!(sender instanceof Player player)) {
            return true;
        }
        return player.hasPermission(permission)
                || (permission.equals(VIEW_PERMISSION) && player.hasPermission(ADMIN_PERMISSION));
    }

    private void sendNoPermission(CommandSender sender, String permission) {
        sender.sendMessage("権限がありません。必要権限: " + permission);
    }

    private String formatHealth(double value) {
        return String.format(Locale.JAPAN, "%.1f", value);
    }

    private String formatMultiplier(double value) {
        return String.format(Locale.JAPAN, "%.2f", value);
    }

    private String formatPercent(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.000001) {
            return String.format(Locale.JAPAN, "%.0f", value);
        }
        return String.format(Locale.JAPAN, "%.1f", value);
    }

    private List<String> filter(List<String> values, String prefix) {
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(lowerPrefix)) {
                result.add(value);
            }
        }
        return result;
    }
}
