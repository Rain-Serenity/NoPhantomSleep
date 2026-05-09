package com.rserene.chosen.server.nophantomsleep;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /nps 命令执行器
 *
 * 处理以下子命令：
 *   /nps add <玩家名>    - 将玩家添加到排除列表
 *   /nps remove <玩家名> - 将玩家从排除列表移除
 *   /nps reload          - 重新加载所有配置文件
 *
 * 所有命令均需 nophantomsleep.admin 权限，且受冷却时间限制。
 */
public class CommandManager implements CommandExecutor {

    /** 持有主插件实例引用，用于操作配置和冷却 */
    private final NoPhantomSleep plugin;

    /**
     * 构造命令执行器
     *
     * @param plugin 主插件实例
     */
    public CommandManager(NoPhantomSleep plugin) {
        this.plugin = plugin;
    }

    /**
     * 命令入口方法
     *
     * 当玩家或控制台执行 /nps 时自动调用此方法。
     *
     * @param sender  命令发送者（可以是玩家或控制台）
     * @param command 被执行的命令对象
     * @param label   命令的别名（/nps 或 /nophantomsleep）
     * @param args    命令参数数组
     * @return true 表示命令处理成功
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // ----- 检查参数数量 -----
        // 如果没有任何参数，显示帮助信息
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        // ----- 处理子命令 -----
        switch (args[0].toLowerCase()) {
            case "add":
                // /nps add <玩家名> - 添加排除玩家
                return handleAdd(sender, args);
            case "remove":
                // /nps remove <玩家名> - 移除排除玩家
                return handleRemove(sender, args);
            case "reload":
                // /nps reload - 重载配置文件
                return handleReload(sender);
            default:
                // 未知子命令，显示帮助
                sendHelp(sender);
                return true;
        }
    }

    // ==================== 子命令处理 ====================

    /**
     * 处理 /nps add <玩家名> 子命令
     *
     * 将指定玩家添加到排除列表。添加后该玩家上床时不会触发插件效果。
     *
     * @param sender 命令发送者
     * @param args   命令参数（args[1] 应为玩家名）
     * @return true
     */
    private boolean handleAdd(CommandSender sender, String[] args) {
        // 检查是否提供了玩家名
        if (args.length < 2) {
            sender.sendMessage("§c用法: /nps add <玩家名>");
            return true;
        }

        String playerName = args[1];

        // 检查冷却时间（仅对玩家生效，控制台不受限制）
        // 如果玩家拥有绕过冷却的权限，则跳过冷却检查
        if (sender instanceof Player) {
            Player p = (Player) sender;
            if (!p.hasPermission(plugin.getCooldownBypassPermission()) && plugin.isOnCooldown(p.getUniqueId())) {
                long remaining = plugin.getRemainingCooldownSeconds(p.getUniqueId());
                sender.sendMessage("§c命令冷却中，请等待 " + formatCooldownTime(remaining) + " 后再试！");
                return true;
            }
        }

        // 尝试添加玩家到排除列表
        boolean added = plugin.addExcludedPlayer(playerName);

        if (added) {
            // 添加成功
            sender.sendMessage("§a已将玩家 " + playerName + " 添加到排除列表！");
            sender.sendMessage("§7该玩家上床时将不会触发插件效果。");
        } else {
            // 玩家已在排除列表中
            sender.sendMessage("§e玩家 " + playerName + " 已在排除列表中！");
        }

        // 设置冷却时间（仅对玩家生效，拥有绕过权限的玩家不触发冷却）
        if (sender instanceof Player) {
            Player p = (Player) sender;
            if (!p.hasPermission(plugin.getCooldownBypassPermission())) {
                plugin.setCooldown(p.getUniqueId());
            }
        }

        return true;
    }

    /**
     * 处理 /nps remove <玩家名> 子命令
     *
     * 将指定玩家从排除列表中移除。移除后该玩家上床时会正常触发插件效果。
     *
     * @param sender 命令发送者
     * @param args   命令参数（args[1] 应为玩家名）
     * @return true
     */
    private boolean handleRemove(CommandSender sender, String[] args) {
        // 检查是否提供了玩家名
        if (args.length < 2) {
            sender.sendMessage("§c用法: /nps remove <玩家名>");
            return true;
        }

        String playerName = args[1];

        // 检查冷却时间（仅对玩家生效，控制台不受限制）
        // 如果玩家拥有绕过冷却的权限，则跳过冷却检查
        if (sender instanceof Player) {
            Player p = (Player) sender;
            if (!p.hasPermission(plugin.getCooldownBypassPermission()) && plugin.isOnCooldown(p.getUniqueId())) {
                long remaining = plugin.getRemainingCooldownSeconds(p.getUniqueId());
                sender.sendMessage("§c命令冷却中，请等待 " + formatCooldownTime(remaining) + " 后再试！");
                return true;
            }
        }

        // 尝试从排除列表移除玩家
        boolean removed = plugin.removeExcludedPlayer(playerName);

        if (removed) {
            // 移除成功
            sender.sendMessage("§a已将玩家 " + playerName + " 从排除列表中移除！");
            sender.sendMessage("§7该玩家上床时将正常触发插件效果。");
        } else {
            // 玩家不在排除列表中
            sender.sendMessage("§e玩家 " + playerName + " 不在排除列表中！");
        }

        // 设置冷却时间（仅对玩家生效，拥有绕过权限的玩家不触发冷却）
        if (sender instanceof Player) {
            Player p = (Player) sender;
            if (!p.hasPermission(plugin.getCooldownBypassPermission())) {
                plugin.setCooldown(p.getUniqueId());
            }
        }

        return true;
    }

    /**
     * 处理 /nps reload 子命令
     *
     * 重新加载 config.yml 和 player.yml 配置文件，
     * 并清空所有冷却记录。
     *
     * @param sender 命令发送者
     * @return true
     */
    private boolean handleReload(CommandSender sender) {
        // 调用主类的重载方法
        plugin.reloadAllConfigs();
        // 发送成功消息
        sender.sendMessage("§aNoPhantomSleep 配置文件已重新加载！");
        sender.sendMessage("§7插件状态: " + (plugin.isPluginEnabled() ? "已启用" : "已禁用"));
        sender.sendMessage("§7排除列表: " + (plugin.isExcludeListEnabled() ? "已启用" : "已禁用"));
        sender.sendMessage("§7冷却时间: " + plugin.getCommandCooldownMinutes() + " 分钟");
        return true;
    }

    /**
     * 发送命令帮助信息
     *
     * @param sender 命令发送者
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6===== NoPhantomSleep 管理命令帮助 =====");
        sender.sendMessage("§e/nps add <玩家名> §7- 将玩家添加到排除列表");
        sender.sendMessage("§e/nps remove <玩家名> §7- 将玩家从排除列表移除");
        sender.sendMessage("§e/nps reload §7- 重新加载配置文件");
    }

    /**
     * 将秒数格式化为 "X分钟Y秒" 的可读字符串
     *
     * 例如：
     *   65 秒  -> "1分钟5秒"
     *   30 秒  -> "30秒"
     *   120 秒 -> "2分钟0秒"
     *
     * @param totalSeconds 总秒数
     * @return 格式化后的时间字符串
     */
    private String formatCooldownTime(long totalSeconds) {
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        if (minutes > 0) {
            return minutes + "分钟" + seconds + "秒";
        } else {
            return seconds + "秒";
        }
    }
}