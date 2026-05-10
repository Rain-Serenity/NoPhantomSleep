package com.rserene.chosen.server.nophantomsleep;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * NoPhantomSleep 插件主类
 *
 * 功能：
 * - 加载和管理 config.yml 主配置文件
 * - 加载和管理 player.yml 玩家排除列表文件
 * - 注册 BedEnterListener 事件监听器
 * - 注册 CommandManager 命令执行器
 * - 管理 /nps 命令的冷却时间
 */
public class NoPhantomSleep extends JavaPlugin {

    // ==================== 配置文件相关 ====================

    /** 玩家排除列表对应的 YAML 文件对象 */
    private File playerFile;

    /** 玩家排除列表对应的 FileConfiguration 对象（用于读写操作） */
    private FileConfiguration playerConfig;

    // ==================== 冷却时间相关 ====================

    /** 记录每个玩家上次使用 /nps 命令的时间戳（UUID -> 毫秒时间戳） */
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    // ==================== 插件启动/关闭 ====================

    @Override
    public void onEnable() {
        // ----- 第一步：加载/保存默认配置文件 -----
        // 将 jar 包内的 config.yml 释放到插件数据文件夹（如果不存在）
        saveDefaultConfig();

        // ----- 第二步：加载/创建玩家排除列表文件 -----
        loadPlayerFile();

        // ----- 第三步：注册事件监听器 -----
        // 将 BedEnterListener 注册到服务器事件总线，传入当前插件实例以便读取配置
        getServer().getPluginManager().registerEvents(new BedEnterListener(this), this);

        // ----- 第四步：注册命令执行器和补全器 -----
        // 将 /nps 命令绑定到 CommandManager 进行处理
        Objects.requireNonNull(getCommand("nps")).setExecutor(new CommandManager(this));
        // 将 /nps 命令绑定到 CommandTabCompleter 进行自动补全
        getCommand("nps").setTabCompleter(new CommandTabCompleter(this));

        // ----- 第五步：打印启动信息 -----
        getLogger().info("NoPhantomSleep 插件已启用！");
        getLogger().info("插件状态: " + (isPluginEnabled() ? "已启用" : "已禁用"));
        getLogger().info("排除列表状态: " + (isExcludeListEnabled() ? "已启用" : "已禁用"));
    }

    @Override
    public void onDisable() {
        getLogger().info("NoPhantomSleep 插件已卸载！");
    }

    // ==================== 玩家排除列表文件管理 ====================

    /**
     * 加载或创建 player.yml 文件
     *
     * 如果 player.yml 不存在则自动创建一个空的，并初始化 players 列表为空列表；
     * 如果已存在则直接加载到内存中。
     */
    private void loadPlayerFile() {
        // 获取插件数据文件夹下的 player.yml 文件对象
        playerFile = new File(getDataFolder(), "player.yml");

        // 如果文件不存在，手动创建空的 player.yml 文件
        // 注意：不使用 saveResource()，因为 player.yml 是运行时数据文件，不是 jar 内的模板资源
        if (!playerFile.exists()) {
            try {
                // 创建插件数据文件夹（如果不存在）
                getDataFolder().mkdirs();
                // 创建空的 player.yml 文件
                playerFile.createNewFile();
            } catch (IOException e) {
                getLogger().warning("无法创建 player.yml 文件: " + e.getMessage());
            }
        }

        // 将文件加载为 FileConfiguration 对象，便于后续读写操作
        playerConfig = YamlConfiguration.loadConfiguration(playerFile);

        // 如果文件中还没有 "players" 节点，则初始化为空列表并保存
        if (!playerConfig.contains("players")) {
            playerConfig.set("players", new ArrayList<String>());
            savePlayerFile();
        }
    }

    /**
     * 保存 player.yml 文件到磁盘
     *
     * 每次对排除列表进行增删操作后都应调用此方法，确保数据持久化。
     */
    public void savePlayerFile() {
        try {
            playerConfig.save(playerFile);
        } catch (IOException e) {
            getLogger().warning("无法保存 player.yml 文件: " + e.getMessage());
        }
    }

    /**
     * 重新加载 player.yml 文件
     *
     * 当管理员通过 /nps reload 命令手动触发重载时调用，
     * 会丢弃内存中的修改并从磁盘重新读取。
     */
    public void reloadPlayerFile() {
        playerConfig = YamlConfiguration.loadConfiguration(playerFile);
    }

    // ==================== 排除列表操作 ====================

    /**
     * 获取排除玩家列表
     *
     * @return 排除的玩家名列表（String 集合）
     */
    public List<String> getExcludedPlayers() {
        return playerConfig.getStringList("players");
    }

    /**
     * 将指定玩家添加到排除列表
     *
     * @param playerName 要添加的玩家名称
     * @return true 如果添加成功（之前不在列表中），false 如果已在列表中
     */
    public boolean addExcludedPlayer(String playerName) {
        List<String> players = getExcludedPlayers();
        // 忽略大小写检查是否已存在
        for (String name : players) {
            if (name.equalsIgnoreCase(playerName)) {
                return false;
            }
        }
        // 添加到列表并保存
        players.add(playerName);
        playerConfig.set("players", players);
        savePlayerFile();
        return true;
    }

    /**
     * 将指定玩家从排除列表移除
     *
     * @param playerName 要移除的玩家名称
     * @return true 如果移除成功（之前在列表中），false 如果不在列表中
     */
    public boolean removeExcludedPlayer(String playerName) {
        List<String> players = getExcludedPlayers();
        // 忽略大小写查找并移除
        boolean removed = players.removeIf(name -> name.equalsIgnoreCase(playerName));
        if (removed) {
            playerConfig.set("players", players);
            savePlayerFile();
        }
        return removed;
    }

    /**
     * 检查指定玩家是否在排除列表中
     *
     * @param playerName 要检查的玩家名称
     * @return true 如果该玩家在排除列表中
     */
    public boolean isPlayerExcluded(String playerName) {
        return getExcludedPlayers().stream()
                .anyMatch(name -> name.equalsIgnoreCase(playerName));
    }

    // ==================== 配置读取（带缓存） ====================

    /**
     * 检查插件是否启用
     *
     * 读取 config.yml 中的 enabled 字段。
     *
     * @return true 表示插件正常工作，false 表示插件被配置关闭
     */
    public boolean isPluginEnabled() {
        return getConfig().getBoolean("enabled", true);
    }

    /**
     * 检查是否启用了排除玩家列表功能
     *
     * 读取 config.yml 中的 exclude-player-list-enabled 字段。
     * 当此功能关闭时，所有玩家都会受到插件影响（忽略排除列表）。
     *
     * @return true 表示排除列表功能已开启
     */
    public boolean isExcludeListEnabled() {
        return getConfig().getBoolean("exclude-player-list-enabled", false);
    }

    /**
     * 获取 /nps 命令冷却时间（单位：分钟）
     *
     * 读取 config.yml 中的 command-cooldown-minutes 字段。
     *
     * @return 冷却分钟数，最小为 0
     */
    public int getCommandCooldownMinutes() {
        return Math.max(0, getConfig().getInt("command-cooldown-minutes", 30));
    }

    /**
     * 获取可绕过冷却的权限节点名称
     *
     * 读取 config.yml 中的 cooldown-bypass-permission 字段。
     * 拥有此权限的玩家使用 /nps 命令时不受冷却时间限制。
     *
     * @return 权限节点名称字符串
     */
    public String getCooldownBypassPermission() {
        return getConfig().getString("cooldown-bypass-permission", "nophantomsleep.bypasscooldown");
    }

    // ==================== 冷却时间管理 ====================

    /**
     * 检查指定玩家是否处于冷却中
     *
     * @param playerUuid 玩家的 UUID
     * @return true 表示该玩家还在冷却中，不能使用 /nps 命令
     */
    public boolean isOnCooldown(UUID playerUuid) {
        // 获取该玩家的上次使用时间戳
        Long lastUsed = cooldowns.get(playerUuid);
        if (lastUsed == null) {
            // 从未使用过，不在冷却中
            return false;
        }
        // 计算冷却时间对应的毫秒数
        long cooldownMillis = getCommandCooldownMinutes() * 60L * 1000L;
        // 获取当前时间
        long now = System.currentTimeMillis();
        // 如果当前时间 - 上次时间 >= 冷却时间，则冷却已过
        return (now - lastUsed) < cooldownMillis;
    }

    /**
     * 获取指定玩家剩余的冷却时间（单位：秒）
     *
     * @param playerUuid 玩家的 UUID
     * @return 剩余冷却秒数（向下取整），如果不在冷却中则返回 0
     */
    public long getRemainingCooldownSeconds(UUID playerUuid) {
        Long lastUsed = cooldowns.get(playerUuid);
        if (lastUsed == null) {
            return 0;
        }
        long cooldownMillis = getCommandCooldownMinutes() * 60L * 1000L;
        long now = System.currentTimeMillis();
        long elapsed = now - lastUsed;
        long remaining = cooldownMillis - elapsed;
        return Math.max(0, remaining / 1000);
    }

    /**
     * 设置指定玩家的冷却时间（从当前时间开始计算）
     *
     * @param playerUuid 玩家的 UUID
     */
    public void setCooldown(UUID playerUuid) {
        cooldowns.put(playerUuid, System.currentTimeMillis());
    }

    // ==================== 重载配置 ====================

    /**
     * 重新加载所有配置文件（config.yml 和 player.yml）
     *
     * 调用此方法会：
     * 1. 重新加载 config.yml（丢弃内存中的修改）
     * 2. 重新加载 player.yml（丢弃内存中的修改）
     * 3. 清空所有冷却记录
     */
    public void reloadAllConfigs() {
        // 重新加载 config.yml
        reloadConfig();
        // 重新加载 player.yml
        reloadPlayerFile();
        // 清空冷却记录，防止重载后冷却时间残留
        cooldowns.clear();
        getLogger().info("所有配置文件已重新加载！");
    }
}