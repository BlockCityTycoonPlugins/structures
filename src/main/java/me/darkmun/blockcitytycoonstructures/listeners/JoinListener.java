package me.darkmun.blockcitytycoonstructures.listeners;

import me.darkmun.blockcitytycoonstructures.BlockCityTycoonStructures;
import me.darkmun.blockcitytycoonstructures.CustomConfig;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Set;

public class JoinListener implements Listener {
    public static final int MAP_BORDER_POSITIVE_X = 85;
    public static final int MAP_BORDER_POSITIVE_Z = 653;
    public static final int MAP_BORDER_NEGATIVE_X = -362;
    public static final int MAP_BORDER_NEGATIVE_Z = -183;
    FileConfiguration config = BlockCityTycoonStructures.getPlugin().getConfig();
    CustomConfig playerUpgradesConfig = BlockCityTycoonStructures.getPlayerUpgradesConfig();
    Set<String> businesses = config.getKeys(false);
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        for (Entity painting : Bukkit.getWorld("world").getEntities()) {
            Bukkit.getLogger().info("entity: " + painting.getType());
            if (painting instanceof Painting) {
                /*Bukkit.getLogger().info("painting");
                int paintingChunkX = (int) (painting.getLocation().getX()/CHUNK_WIDTH);
                int paintingChunkZ = (int) (painting.getLocation().getZ()/CHUNK_WIDTH);
                Bukkit.getLogger().info("paintingChunkX: " + paintingChunkX + " paintingChunkZ" + paintingChunkZ);
                Bukkit.getLogger().info("painting name: " + painting.getName());
                if (paintingChunkX == copyChunkX && paintingChunkZ == copyChunkZ) {
                    double paintingX = painting.getLocation().getX();
                    double paintingY = painting.getLocation().getY();
                    double paintingZ = painting.getLocation().getZ();
                    Painting newPainting = clonePaintingEntityToAnotherLocation((Painting) painting, new Location(painting.getWorld(), paintingX + CHUNK_WIDTH * chunkXDelta, paintingY, paintingZ + CHUNK_WIDTH * chunkZDelta));
                }*/
            }
        }
        //BlockCityTycoonStructures.getPlugin().spawnAllPaintings(event.getPlayer().getWorld());
        /*Player player = event.getPlayer();
        String plUUID = player.getUniqueId().toString();
        Set<String> businessChunkUpgradeChunks;
        String businessValue;
        int pasteChunkX;
        int pasteChunkZ;
        for (Painting painting : player.getWorld().getEntitiesByClass(Painting.class)) {
            double paintingX = painting.getLocation().getX();
            double paintingY = painting.getLocation().getY();
            double paintingZ = painting.getLocation().getZ();
            if (MAP_BORDER_NEGATIVE_X <= paintingX && paintingX <= MAP_BORDER_POSITIVE_X
                    && MAP_BORDER_NEGATIVE_Z <= paintingZ && paintingZ <= MAP_BORDER_POSITIVE_Z) {

                    BlockCityTycoonStructures.getEntityHider().hideEntity(player, painting);
                }
            }*/
            /*int paintingChunkX = (int) (painting.getLocation().getX()/CHUNK_WIDTH);
            int paintingChunkZ = (int) (painting.getLocation().getZ()/CHUNK_WIDTH);
            if (paintingChunkX == copyChunkX && paintingChunkZ == copyChunkZ) {
                double paintingX = painting.getLocation().getX();
                double paintingY = painting.getLocation().getY();
                double paintingZ = painting.getLocation().getZ();
                Painting newPainting = clonePaintingEntityToAnotherLocation(painting, new Location(painting.getWorld(), paintingX + CHUNK_WIDTH * chunkXDelta, paintingY, paintingZ + CHUNK_WIDTH * chunkZDelta));

            }*/
        /*}
        for (String business : businesses) {
            if (playerUpgradesConfig.getConfig().contains(plUUID + "." + business)) {
                businessValue = playerUpgradesConfig.getConfig().getString(plUUID + "." + business);
                //if (chunkDataConfig.getConfig().contains(business + "." + businessValue)) {
                businessChunkUpgradeChunks = config.getConfigurationSection(business + "." + businessValue).getKeys(false);
                for (String businessChunkUpgradeChunk : businessChunkUpgradeChunks) {
                    pasteChunkX = config.getInt(business + "." + businessValue + "." + businessChunkUpgradeChunk + ".paste-to-chunk-x");
                    pasteChunkZ = config.getInt(business + "." + businessValue + "." + businessChunkUpgradeChunk + ".paste-to-chunk-z");
                    if ((ChunkX == pasteChunkX) && (ChunkZ == pasteChunkZ)) {

                    }
                }
            }
        }*/
    }
}
