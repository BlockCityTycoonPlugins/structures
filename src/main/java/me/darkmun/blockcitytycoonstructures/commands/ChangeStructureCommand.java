package me.darkmun.blockcitytycoonstructures.commands;

import com.comphenix.packetwrapper.WrapperPlayServerMapChunk;
import me.darkmun.blockcitytycoonstructures.BlockCityTycoonStructures;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.Set;

public class ChangeStructureCommand implements CommandExecutor {

    private final BlockCityTycoonStructures plugin;

    public ChangeStructureCommand(BlockCityTycoonStructures instance) {
        plugin = instance;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender.hasPermission("bctstructures.chunks")) {
            if (args.length == 3) {
                Player pl = Bukkit.getPlayerExact(args[0]);
                if (pl != null) {
                    if (plugin.getConfig().contains(args[1] + "." + args[2])) {
                        int ChunkX;
                        int ChunkZ;

                        Set<String> chunks = plugin.getConfig().getConfigurationSection(args[1] + "." + args[2]).getKeys(false);

                        WrapperPlayServerMapChunk wrapperChunk;

                        plugin.getPlayerUpgradesConfig().getConfig().set(pl.getUniqueId().toString() + "." + args[1], args[2]);
                        plugin.getPlayerUpgradesConfig().saveConfig();
                        for (String chunk : chunks) {
                            ChunkX = plugin.getConfig().getInt(args[1] + "." + args[2] + "." + chunk + ".paste-to-chunk-x");
                            ChunkZ = plugin.getConfig().getInt(args[1] + "." + args[2] + "." + chunk + ".paste-to-chunk-z");

                            //packetChunk = manager.createPacket(PacketType.Play.Server.MAP_CHUNK);
                            wrapperChunk = new WrapperPlayServerMapChunk();
                            wrapperChunk.setChunkX(ChunkX);
                            wrapperChunk.setChunkZ(ChunkZ);
                            wrapperChunk.sendPacket(pl);
                        }
                        return true;
                    } else {
                        sender.sendMessage(ChatColor.RED + "В конфиге нет таких значений");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Игрока с таким никнеймом сейчас нет на сервере");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "У команды должно быть три аругмента\n/chunkchange <player> <business> <upgrade-num>");
            }
        } else {
            sender.sendMessage(ChatColor.RED + "У вас недостаточно прав на использование этой команды");
        }
        return false;
    }

}
