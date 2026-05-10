package com.rserene.chosen.server.nophantomsleep;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * /nps 命令的自动补全器
 *
 * 提供以下补全：
 *   - 子命令补全：add, remove, reload
 *   - 玩家名补全：在 add/remove 命令后补全在线玩家名称
 */
public class CommandTabCompleter implements TabCompleter {

    /** 持有主插件实例引用，用于读取配置和权限 */
    private final NoPhantomSleep plugin;

    /** 所有可用的子命令列表 */
    private static final List<String> SUB_COMMANDS = Arrays.asList("add", "remove", "reload");

    /**
     * 构造命令补全器
     *
     * @param plugin 主插件实例
     */
    public CommandTabCompleter(NoPhantomSleep plugin) {
        this.plugin = plugin;
    }

    /**
     * 标签补全入口方法
     *
     * 当玩家按下 Tab 键时自动调用此方法，返回可能的补全选项列表。
     *
     * @param sender  命令发送者
     * @param command 被执行的命令对象
     * @param alias   命令的别名
     * @param args    当前已输入的参数数组
     * @return 可能的补全选项列表，null 表示使用默认补全
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // ----- 参数数量为 1 时：补全子命令 -----
        // 例如：/nps [Tab] -> 显示 add, remove, reload
        if (args.length == 1) {
            return filterByPrefix(SUB_COMMANDS, args[0]);
        }

        // ----- 参数数量为 2 时：根据子命令补全玩家名 -----
        // 例如：/nps add [Tab] -> 显示在线玩家列表
        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            // 只有 add 和 remove 命令需要玩家名补全
            if (subCommand.equals("add") || subCommand.equals("remove")) {
                return getPlayerNames(args[1]);
            }
        }

        // ----- 参数数量 >= 3 时：不提供补全 -----
        // 例如：/nps add PlayerName [Tab] -> 无补全
        return null;
    }

    /**
     * 根据前缀过滤字符串列表
     *
     * @param candidates 候选字符串列表
     * @param prefix     输入的前缀（不区分大小写）
     * @return 匹配的字符串列表（按字母排序）
     */
    private List<String> filterByPrefix(List<String> candidates, String prefix) {
        List<String> result = new ArrayList<>();
        String lowerPrefix = prefix.toLowerCase();
        for (String candidate : candidates) {
            if (candidate.toLowerCase().startsWith(lowerPrefix)) {
                result.add(candidate);
            }
        }
        // 按字母顺序排序，方便查找
        Collections.sort(result);
        return result;
    }

    /**
     * 获取匹配前缀的在线玩家名称列表
     *
     * @param prefix 输入的玩家名前缀（不区分大小写）
     * @return 匹配的玩家名列表（按字母排序）
     */
    private List<String> getPlayerNames(String prefix) {
        List<String> result = new ArrayList<>();
        String lowerPrefix = prefix.toLowerCase();

        // 遍历所有在线玩家
        for (Player player : Bukkit.getOnlinePlayers()) {
            String playerName = player.getName();
            // 忽略大小写匹配前缀
            if (playerName.toLowerCase().startsWith(lowerPrefix)) {
                result.add(playerName);
            }
        }

        // 按字母顺序排序，方便查找
        Collections.sort(result);
        return result;
    }
}