package me.darkmun.blockcitytycoonstructures.listeners;

import me.darkmun.blockcitytycoonstructures.BlockCityTycoonStructures;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener implements Listener {

    private BlockCityTycoonStructures plugin;
    public JoinListener(BlockCityTycoonStructures instance) { plugin = instance; }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {

        /*Player pl = e.getPlayer();

        Set<String> businesses = plugin.getConfig().getKeys(false);

        if (!pl.hasPlayedBefore()) {
            String playerID = pl.getUniqueId().toString();

            plugin.getPlayerUpgradesConfig().getConfig().createSection(playerID);
            for (String business : businesses) {
                plugin.getPlayerUpgradesConfig().getConfig().set(playerID + "." + business, "0");
            }
            plugin.getPlayerUpgradesConfig().saveConfig();
        }*/
    }
}
