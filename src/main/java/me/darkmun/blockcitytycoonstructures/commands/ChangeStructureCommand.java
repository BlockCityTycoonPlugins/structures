package me.darkmun.blockcitytycoonstructures.commands;

import com.comphenix.packetwrapper.WrapperPlayServerMapChunk;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import me.darkmun.blockcitytycoonstructures.BlockCityTycoonStructures;
import org.bukkit.Bukkit;
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
        ProtocolManager manager = ProtocolLibrary.getProtocolManager();

        if (sender instanceof Player && plugin.getConfig().contains(args[0] + "." + args[1])) {
            Bukkit.getConsoleSender().sendMessage("command are working");
            int ChunkX;
            int ChunkZ;

            Player pl = (Player) sender;
            Set<String> chunks = plugin.getConfig().getConfigurationSection(args[0] + "." + args[1]).getKeys(false);

            PacketContainer packetChunk;
            WrapperPlayServerMapChunk wrapperChunk;

            plugin.getPlayerUpgradesConfig().getConfig().set(pl.getUniqueId().toString() + "." + args[0], args[1]);
            plugin.getPlayerUpgradesConfig().saveConfig();
            for (String chunk : chunks) {
                ChunkX = plugin.getConfig().getInt(args[0] + "." + args[1] + "." + chunk + ".paste-to-chunk-x");
                ChunkZ = plugin.getConfig().getInt(args[0] + "." + args[1] + "." + chunk + ".paste-to-chunk-z");

                packetChunk = manager.createPacket(PacketType.Play.Server.MAP_CHUNK);
                wrapperChunk = new WrapperPlayServerMapChunk(packetChunk);
                wrapperChunk.setChunkX(ChunkX);
                wrapperChunk.setChunkZ(ChunkZ);
                wrapperChunk.sendPacket(pl);
            }



            /*wrapperChunk.setData((byte[]) plugin.getChunkDataConfig().getConfig().get("1.1.1.Data"));
            wrapperChunk.setBitmask(plugin.getChunkDataConfig().getConfig().getInt("1.1.1.BitMask"));
            wrapperChunk.setGroundUpContinuous(plugin.getChunkDataConfig().getConfig().getBoolean("GroundUpContinuous"));
            List<NbtBase<?>> tileEntities = new ArrayList<>();
            NbtTextSerializer serializer = new NbtTextSerializer();
            int i = 0;
            while(plugin.getChunkDataConfig().getConfig().contains("1.1.1.TileEntities" + i)) {
                Bukkit.getConsoleSender().sendMessage("1.1.1.TileEntities" + i);
                String str = plugin.getChunkDataConfig().getConfig().getString("1.1.1.TileEntities" + i);
                if (str != null && !str.equals("") && !str.equals(" ")) {
                    try {
                        tileEntities.add(serializer.deserialize(str));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                i++;
            }*/

            /*PacketContainer packet = manager.createPacket(PacketType.Play.Server.MULTI_BLOCK_CHANGE);
            WrapperPlayServerMultiBlockChange wrapper = new WrapperPlayServerMultiBlockChange(packet);

            //double x = Integer.parseInt(args[0]);
            //double y = Integer.parseInt(args[1]);
            //double z = Integer.parseInt(args[2]);
            Vector copyFrom1 = plugin.getConfig().getVector(args[0] + "." + args[1] + ".copyFrom1");
            Vector copyFrom2 = plugin.getConfig().getVector(args[0] + "." + args[1] + ".copyFrom2");
            Vector pasteTo1 = plugin.getConfig().getVector(args[0] + "." + args[1] + ".pasteToCoord");
            int ChunkX = plugin.getConfig().getInt(args[0] + "." + args[1] + ".pasteToChunkX");
            int ChunkZ = plugin.getConfig().getInt(args[0] + "." + args[1] + ".pasteToChunkZ");

            plugin.getPlayerUpgradesConfig().getConfig().set(pl.getUniqueId().toString() + "." + args[0], args[1]);
            plugin.getPlayerUpgradesConfig().saveConfig();

            int arraySize = (copyFrom2.getBlockX()-copyFrom1.getBlockX()+1)*(copyFrom2.getBlockY()-copyFrom1.getBlockY()+1)*(copyFrom2.getBlockZ()-copyFrom1.getBlockZ()+1);
            MultiBlockChangeInfo[] mbcArray = new MultiBlockChangeInfo[arraySize];
            Bukkit.getConsoleSender().sendMessage(String.valueOf(arraySize));

            Material copyBlockMaterial;
            int copyBlockData;

            int i = 0;
            for (int x1 = copyFrom1.getBlockX(), x2 = pasteTo1.getBlockX(); x1 <= copyFrom2.getBlockX(); x1++, x2++) {
                for (int y1 = copyFrom1.getBlockY(), y2 = pasteTo1.getBlockY(); y1 <= copyFrom2.getBlockY(); y1++, y2++) {
                    for (int z1 = copyFrom1.getBlockZ(), z2 = pasteTo1.getBlockZ(); z1 <= copyFrom2.getBlockZ(); z1++, z2++) {
                        copyBlockMaterial = pl.getWorld().getBlockAt(x1, y1, z1).getType();
                        copyBlockData = pl.getWorld().getBlockAt(x1, y1, z1).getData();
                        mbcArray[i] = new MultiBlockChangeInfo(new Location(pl.getWorld(), x2, y2, z2), WrappedBlockData.createData(copyBlockMaterial, (int)copyBlockData));
                        i++;
                    }
                }
            }

            wrapper.setChunk(new ChunkCoordIntPair(ChunkX,ChunkZ));
            wrapper.setRecords(mbcArray);

            wrapper.sendPacket(pl);*/
            return true;
        }
        return false;
    }

}
