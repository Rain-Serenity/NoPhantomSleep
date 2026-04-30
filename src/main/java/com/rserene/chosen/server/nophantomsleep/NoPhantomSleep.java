package com.rserene.chosen.server.nophantomsleep;

import org.bukkit.plugin.java.JavaPlugin;

public class NoPhantomSleep extends JavaPlugin {
    @Override
    public void onEnable() {
        // 实例化并注册刚刚创建的监听器类
        getServer().getPluginManager().registerEvents(new BedEnterListener(), this);

        getLogger().info("NoPhantomSleep 插件已启用！");
    }

    @Override
    public void onDisable() {
        getLogger().info("NoPhantomSleep 插件已卸载！");
    }
}