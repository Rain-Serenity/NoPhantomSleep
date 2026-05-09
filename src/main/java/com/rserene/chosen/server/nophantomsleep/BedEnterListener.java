package com.rserene.chosen.server.nophantomsleep;

import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;

/**
 * 玩家上床事件监听器
 *
 * 当玩家成功躺到床上时，重置其休息统计以防止幻翼生成。
 * 同时会检查插件配置开关和排除玩家列表。
 */
public class BedEnterListener implements Listener {

    /** 持有主插件实例引用，用于读取配置和检查排除列表 */
    private final NoPhantomSleep plugin;

    /**
     * 构造监听器
     *
     * @param plugin 主插件实例
     */
    public BedEnterListener(NoPhantomSleep plugin) {
        this.plugin = plugin;
    }

    /**
     * 玩家上床事件处理方法
     *
     * 执行流程：
     * 1. 检查插件是否启用（config.yml -> enabled）
     * 2. 检查玩家是否成功躺到床上（排除被阻挡等情况）
     * 3. 如果启用了排除列表，检查该玩家是否在排除列表中
     * 4. 通过所有检查后，重置休息统计并发送提示
     *
     * @param event 玩家上床事件
     */
    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        // ----- 第一步：检查插件是否启用 -----
        // 如果 config.yml 中 enabled 为 false，直接跳过不处理
        if (!plugin.isPluginEnabled()) {
            return;
        }

        // ----- 第二步：检查玩家是否成功躺在床上 -----
        // 排除因为白天、周围有怪物等原因上床失败的情况
        if (event.getBedEnterResult() != PlayerBedEnterEvent.BedEnterResult.OK) {
            return;
        }

        Player player = event.getPlayer();
        String playerName = player.getName();

        // ----- 第三步：检查排除玩家列表 -----
        // 如果启用了排除列表功能，且该玩家在排除列表中，则跳过处理
        if (plugin.isExcludeListEnabled() && plugin.isPlayerExcluded(playerName)) {
            // 告知被排除的玩家为什么没有效果
            player.sendMessage("§7你已处于排除列表中，插件不会影响你。");
            return;
        }

        // ----- 第四步：执行插件核心功能 -----
        // 将玩家的"距离上次休息时间"统计数据清零，这等同于睡了一整觉
        player.setStatistic(Statistic.TIME_SINCE_REST, 0);

// 发送提示消息
        player.sendMessage("§a你躺在了床上！你身上的疲惫消散了，今晚幻翼不会来找你了。");
    }
}