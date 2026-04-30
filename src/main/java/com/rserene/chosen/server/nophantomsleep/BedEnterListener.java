package com.rserene.chosen.server.nophantomsleep;

import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;

public class BedEnterListener implements Listener {

    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        // 检查玩家是否成功躺在床上（排除因为白天、周围有怪物等原因上床失败的情况）
        if (event.getBedEnterResult() == PlayerBedEnterEvent.BedEnterResult.OK) {
            Player player = event.getPlayer();

            // 将玩家的“距离上次休息时间”统计数据清零，这等同于睡了一整觉
            player.setStatistic(Statistic.TIME_SINCE_REST, 0);

            // 发送提示消息
            player.sendMessage("§a你躺在了床上！你身上的疲惫消散了，今晚幻翼不会来找你了。");
        }
    }
}