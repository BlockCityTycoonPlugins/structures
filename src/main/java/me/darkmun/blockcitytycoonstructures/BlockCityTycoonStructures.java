package me.darkmun.blockcitytycoonstructures;

import com.comphenix.packetwrapper.WrapperPlayServerMapChunk;
import com.comphenix.packetwrapper.WrapperPlayServerUnloadChunk;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.nbt.NbtBase;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import com.comphenix.protocol.wrappers.nbt.NbtList;
import com.comphenix.protocol.wrappers.nbt.io.NbtBinarySerializer;
import com.comphenix.protocol.wrappers.nbt.io.NbtTextSerializer;
import example.EntityHider;
import io.netty.buffer.Unpooled;
import me.darkmun.blockcitytycoonstructures.commands.ChangeStructureCommand;
import me.darkmun.blockcitytycoonstructures.listeners.JoinListener;
import net.minecraft.server.v1_12_R1.Chunk;
import net.minecraft.server.v1_12_R1.EntityPainting;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import net.minecraft.server.v1_12_R1.PacketPlayOutMapChunk;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_12_R1.CraftServer;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPainting;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

public final class BlockCityTycoonStructures extends JavaPlugin {

    public static final int CHUNK_WIDTH = 16;
    private static CustomConfig playerUpgradesConfig;
    private static CustomConfig chunkDataConfig;
    private Database database;
    private static BlockCityTycoonStructures plugin;
    private static EntityHider entityHider;

    @Override
    public void onEnable() {
        getConfig().options().copyDefaults();
        saveDefaultConfig();
        plugin = this;

        if (getConfig().getBoolean("enable")) {
            playerUpgradesConfig = new CustomConfig();
            chunkDataConfig = new CustomConfig();

            playerUpgradesConfig.setup("playersUpgrades");
            playerUpgradesConfig.getConfig().options().copyDefaults(true);

            try {
                database = new Database();
                database.initializeDatabase();

            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            getCommand("chunkchange").setExecutor(new ChangeStructureCommand(this));
            getServer().getPluginManager().registerEvents(new JoinListener(), this);

            Bukkit.getWorld("world").spawn(new Location(Bukkit.getWorld("world"), -84, 39, 25), Painting.class).setArt(Art.ALBAN);



            entityHider = new EntityHider(this, EntityHider.Policy.BLACKLIST);
            ProtocolManager manager = ProtocolLibrary.getProtocolManager();
            manager.addPacketListener(new PacketAdapter(this, PacketType.Play.Server.MAP_CHUNK) {
                final NbtTextSerializer serializer = new NbtTextSerializer();

                @Override
                public void onPacketSending(PacketEvent event) {
                    PacketContainer packet = event.getPacket();
                    WrapperPlayServerMapChunk wrapper = new WrapperPlayServerMapChunk(packet);

                    Player pl = event.getPlayer();
                    int ChunkX = packet.getIntegers().read(0);
                    int ChunkZ = packet.getIntegers().read(1);
                    /*for (Entity painting : Bukkit.getWorld("world").getEntities()) {
                        NbtFactory.fromNMSCompound().
                        NBTTagCompound asd = new NBTTagCompound();
                        asd.
                        ((Painting)painting).
                        Bukkit.getLogger().info("entity: " + painting.getType());
                    }*/
                    fillConfigWithChunkData(chunkDataConfig, wrapper, ChunkX, ChunkZ);

                    try {
                        changeChunkToPlayer(playerUpgradesConfig, chunkDataConfig, wrapper, ChunkX, ChunkZ, pl);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                public void fillConfigWithChunkData(CustomConfig chunkDataConfig, WrapperPlayServerMapChunk packet, int ChunkX, int ChunkZ) {
                    Set<String> businessesChunks = getConfig().getKeys(false);
                    businessesChunks.remove("enable");
                    Set<String> businessChunkUpgrades;
                    Set<String> businessChunkUpgradeChunks;
                    byte[] data;
                    int bitMask;
                    boolean groundUp;
                    List<NbtBase<?>> tileEntities;
                    List<String> configTileEntities = new ArrayList<>();

                    for (String businessChunk : businessesChunks) {
                        businessChunkUpgrades = getConfig().getConfigurationSection(businessChunk).getKeys(false);
                        for (String businessChunkUpgrade : businessChunkUpgrades) {
                            businessChunkUpgradeChunks = getConfig().getConfigurationSection(businessChunk + "." + businessChunkUpgrade).getKeys(false);
                            for (String businessChunkUpgradeChunk : businessChunkUpgradeChunks) {
                                int businessUpgradeChunkX = getConfig().getInt(businessChunk + "." + businessChunkUpgrade + "." + businessChunkUpgradeChunk + ".copy-from-chunk-x");
                                int businessUpgradeChunkZ = getConfig().getInt(businessChunk + "." + businessChunkUpgrade + "." + businessChunkUpgradeChunk + ".copy-from-chunk-z");
                                if ((businessUpgradeChunkX == ChunkX) && (businessUpgradeChunkZ == ChunkZ) /*&& !chunkDataConfig.getConfig().contains(businessChunk + "." + businessChunkUpgrade + "." + businessChunkUpgradeChunk)*/) {
                                    data = packet.getData();
                                    bitMask = packet.getBitmask();
                                    groundUp = packet.getGroundUpContinuous();
                                    tileEntities = packet.getTileEntities();

                                    if (!tileEntities.isEmpty()) {
                                        for (NbtBase<?> tileEntity : tileEntities) {
                                            configTileEntities.add(serializer.serialize(tileEntity));
                                        }
                                    }

                                    try {
                                        PreparedStatement statement = database.getConnection()
                                                .prepareStatement("INSERT INTO chunk_data (structure,upgrade,chunk,data,bit_mask,ground_up_continuous,tile_entities) " +
                                                        "SELECT * FROM (SELECT ? AS structure, ? AS upgrade, ? AS chunk, ? AS data, ? AS bit_mask, ? AS ground_up_continuous, ? AS tile_entities) AS temp " +
                                                        "WHERE NOT EXISTS ( " +
                                                        "    SELECT structure, upgrade, chunk FROM chunk_data WHERE structure = ? AND upgrade = ? AND chunk = ? " +
                                                        ") LIMIT 1");
                                        statement.setString(1, businessChunk);
                                        statement.setString(2, businessChunkUpgrade);
                                        statement.setString(3, businessChunkUpgradeChunk);
                                        statement.setBytes(4, data);
                                        statement.setInt(5, bitMask);
                                        statement.setBoolean(6, groundUp);

                                        //NbtList<?> nbtList = NbtFactory.ofList("tileEntities", tileEntities);
                                        NbtBinarySerializer nbtSerializer = new NbtBinarySerializer();
                                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                        DataOutputStream out = new DataOutputStream(baos);
                                        for (NbtBase<?> tileEntity : tileEntities) {
                                            nbtSerializer.serialize(tileEntity, out);
                                        }

                                        byte[] bytes = baos.toByteArray();

                                        statement.setBytes(7, bytes);
                                        statement.setString(8, businessChunk);
                                        statement.setString(9, businessChunkUpgrade);
                                        statement.setString(10, businessChunkUpgradeChunk);

                                        statement.executeUpdate();

                                        statement.close();
                                    } catch (SQLException ex) {
                                        ex.printStackTrace();
                                    }
                                    return;
                                }
                            }
                        }
                    }
                }

                public void changeChunkToPlayer(CustomConfig playerUpgradesConfig, CustomConfig chunkDataConfig, WrapperPlayServerMapChunk packet, int ChunkX, int ChunkZ, Player player) throws IOException {
                    Set<String> businesses = getConfig().getKeys(false);
                    Set<String> businessChunkUpgradeChunks;
                    String businessValue;
                    String plUUID = player.getUniqueId().toString();

                    byte[] data;
                    int bitMask;
                    boolean groundUp;

                    int pasteChunkX;
                    int pasteChunkZ;

                    for (String business : businesses) {
                        if (playerUpgradesConfig.getConfig().contains(plUUID + "." + business)) {
                            businessValue = playerUpgradesConfig.getConfig().getString(plUUID + "." + business);
                            //if (chunkDataConfig.getConfig().contains(business + "." + businessValue)) {
                                businessChunkUpgradeChunks = getConfig().getConfigurationSection(business + "." + businessValue).getKeys(false);
                                for (String businessChunkUpgradeChunk : businessChunkUpgradeChunks) {
                                    pasteChunkX = getConfig().getInt(business + "." + businessValue + "." + businessChunkUpgradeChunk + ".paste-to-chunk-x");
                                    pasteChunkZ = getConfig().getInt(business + "." + businessValue + "." + businessChunkUpgradeChunk + ".paste-to-chunk-z");
                                    if ((ChunkX == pasteChunkX) && (ChunkZ == pasteChunkZ)) {
                                        try {
                                            PreparedStatement statement = database.getConnection().prepareStatement("SELECT * FROM chunk_data WHERE structure = ? AND upgrade = ? AND chunk = ?");
                                            statement.setString(1, business);
                                            statement.setString(2, businessValue);
                                            statement.setString(3, businessChunkUpgradeChunk);

                                            ResultSet set = statement.executeQuery();
                                            if (set.next()) {
                                                data = set.getBytes("data");
                                                bitMask = set.getInt("bit_mask");
                                                groundUp = set.getBoolean("ground_up_continuous");
                                                byte[] tileEntitiesBytes = set.getBytes("tile_entities");


                                                List<NbtBase<?>> tileEntities = new ArrayList<>();
                                                NbtBinarySerializer nbtSerializer = new NbtBinarySerializer();
                                                //List<String> configTileEntities = new ArrayList<>();
                                                ByteArrayInputStream bais = new ByteArrayInputStream(tileEntitiesBytes);
                                                DataInputStream in = new DataInputStream(bais);
                                                while (in.available() > 0) {
                                                    tileEntities.add(nbtSerializer.deserialize(in));
                                                }
                                                /*NbtList<?> nbtList = NbtBinarySerializer.DEFAULT.deserializeList(in);
                                                if (nbtList.asCollection() instanceof List) {
                                                    tileEntities = new ArrayList<>(nbtList.asCollection());
                                                }
                                                else {

                                                }*/
                                                /*while (in.available() > 0) {
                                                    String element = in.readUTF();
                                                    configTileEntities.add(element);
                                                }

                                                for (String tileEntity : configTileEntities) {
                                                    tileEntities.add(serializer.deserialize(tileEntity));
                                                }*/

                                                int copyChunkX = getConfig().getInt(business + "." + businessValue + "." + businessChunkUpgradeChunk + ".copy-from-chunk-x");
                                                int copyChunkZ = getConfig().getInt(business + "." + businessValue + "." + businessChunkUpgradeChunk + ".copy-from-chunk-z");
                                                int chunkXDelta = pasteChunkX - copyChunkX;
                                                int chunkZDelta = pasteChunkZ - copyChunkZ;

                                                for (NbtBase<?> tileEntity : tileEntities) {
                                                    Map<String, NbtBase<?>> map = (Map<String, NbtBase<?>>) tileEntity.getValue();
                                                    int x = (int) map.get("x").getValue();
                                                    int z = (int) map.get("z").getValue();
                                                    map.put("x", NbtFactory.of("x", x + CHUNK_WIDTH * chunkXDelta));
                                                    map.put("z", NbtFactory.of("z", z + CHUNK_WIDTH * chunkZDelta));

                                                    Bukkit.getLogger().info("Map value info");
                                                    for (String key : map.keySet()) {
                                                        Bukkit.getLogger().info("\tKey: " + key);
                                                        Bukkit.getLogger().info("\tName: " + map.get(key).getName());
                                                        Bukkit.getLogger().info("\tValue: " + map.get(key).getValue().toString());
                                                        Bukkit.getLogger().info("\tType name: " + map.get(key).getType().name());
                                                        Bukkit.getLogger().info("\tValue type class name: " + map.get(key).getType().getValueType().getName());
                                                        Bukkit.getLogger().info("\tType raw id: " + map.get(key).getType().getRawID());
                                                    }

                                                    Bukkit.getLogger().info("Name: " + tileEntity.getName());
                                                    Bukkit.getLogger().info("Value: " + tileEntity.getValue().toString());
                                                    Bukkit.getLogger().info("Type name: " + tileEntity.getType().name());
                                                    Bukkit.getLogger().info("Value type class name: " + tileEntity.getType().getValueType().getName());
                                                    Bukkit.getLogger().info("Type raw id: " + tileEntity.getType().getRawID());
                                                }

                                                /*for (Painting painting : player.getWorld().getEntitiesByClass(Painting.class)) {
                                                    int paintingChunkX = (int) (painting.getLocation().getX()/CHUNK_WIDTH);
                                                    int paintingChunkZ = (int) (painting.getLocation().getZ()/CHUNK_WIDTH);
                                                    if (paintingChunkX == copyChunkX && paintingChunkZ == copyChunkZ) {
                                                        double paintingX = painting.getLocation().getX();
                                                        double paintingY = painting.getLocation().getY();
                                                        double paintingZ = painting.getLocation().getZ();
                                                        Painting newPainting = clonePaintingEntityToAnotherLocation(painting, new Location(painting.getWorld(), paintingX + CHUNK_WIDTH * chunkXDelta, paintingY, paintingZ + CHUNK_WIDTH * chunkZDelta));
                                                        for (Player pl : newPainting.getWorld().getPlayers()) {
                                                            if (pl != player) {
                                                                entityHider.hideEntity(pl, newPainting);
                                                            }
                                                        }
                                                    }
                                                }*/

                                                Bukkit.getLogger().info(String.format("Chunk X: %s Chunk Z: %s Primary Bit Mask: %s", ChunkX, ChunkZ, bitMask));
                                                Chunk chunk;
                                                if (groundUp) {
                                                    chunk = new Chunk(((CraftPlayer)player).getHandle().getWorld(), ChunkX, ChunkZ);
                                                } else {
                                                    chunk = ((CraftPlayer)player).getHandle().getWorld().getChunkAt(ChunkX, ChunkZ);
                                                }
                                                ChunkDataSerializer chunkDataSerializer = new ChunkDataSerializer();
                                                chunkDataSerializer.ReadChunkColumn(chunk, bitMask, Unpooled.wrappedBuffer(data));
                                                PacketPlayOutMapChunk packetMapChunk = new PacketPlayOutMapChunk(chunk, 65535);

                                                byte[] bytes;
                                                Field dataField;
                                                try {
                                                    dataField = packetMapChunk.getClass().getDeclaredField("d");
                                                    dataField.setAccessible(true);
                                                    bytes = (byte[])dataField.get(packetMapChunk);
                                                    dataField.setAccessible(false);

                                                    boolean[] bools = new boolean[data.length];
                                                    for (int i = 0; i < data.length; i++) {
                                                        bools[i] = data[i] == bytes[i];
                                                    }
                                                    Bukkit.getLogger().info("Real data length: " + packet.getData().length);
                                                    Bukkit.getLogger().info("Fake data length: " + data.length);
                                                    Bukkit.getLogger().info("Copied data length: " + bytes.length);
                                                    //Bukkit.getLogger().info(Arrays.toString(bools));

                                                    WrapperPlayServerUnloadChunk wrapperUnloadChunk = new WrapperPlayServerUnloadChunk();
                                                    wrapperUnloadChunk.setChunkX(ChunkX);
                                                    wrapperUnloadChunk.setChunkZ(ChunkZ);
                                                    wrapperUnloadChunk.sendPacket(player);

                                                    packet.setData(bytes);
                                                    //packet.setData(data);
                                                    packet.setBitmask(bitMask);
                                                    packet.setGroundUpContinuous(groundUp);
                                                    packet.setTileEntities(tileEntities);
                                                } catch (NoSuchFieldException | SecurityException | IllegalAccessException e){
                                                    e.printStackTrace();
                                                }
                                            }

                                        } catch (SQLException | NoSuchFieldException | InvocationTargetException |
                                                 IllegalAccessException | NoSuchMethodException ex) {
                                            ex.printStackTrace();
                                        }

                                        /*data = (byte []) chunkDataConfig.getConfig().get(business + "." + businessValue + "." + businessChunkUpgradeChunk + ".data");
                                        bitMask = chunkDataConfig.getConfig().getInt(business + "." + businessValue + "." + businessChunkUpgradeChunk + ".bit-mask");
                                        groundUp = chunkDataConfig.getConfig().getBoolean(business + "." + businessValue + "." + businessChunkUpgradeChunk + ".ground-up-continuous");

                                        List<String> configTileEntities = chunkDataConfig.getConfig().getStringList(business + "." + businessValue + "." + businessChunkUpgradeChunk + ".tile-entities");
                                        for (String tileEntity : configTileEntities) {
                                            tileEntities.add(serializer.deserialize(tileEntity));
                                        }*/


                                        return;
                                    }
                                }

                            //}
                        }

                    }
                }
            });

            getLogger().log(Level.INFO, "Plugin enabled.");
        }
        else {
            getLogger().log(Level.INFO, "Plugin not enabled.");
        }

    }

    @Override
    public void onDisable() {
        database.closeConnection();
        removeAllPaintings(Bukkit.getWorld("world"));
        getLogger().log(Level.INFO, "Plugin disabled.");
    }

    public void spawnAllPaintings(World world) {
        Set<String> businessesChunks = getConfig().getKeys(false);
        businessesChunks.remove("enable");
        Set<String> businessChunkUpgrades;
        String lastUpgrade;
        Set<String> businessChunkUpgradeChunks;
        for (String businessChunk : businessesChunks) {
            businessChunkUpgrades = getConfig().getConfigurationSection(businessChunk).getKeys(false);
            lastUpgrade = (String) businessChunkUpgrades.toArray()[businessChunkUpgrades.size() - 1];
            businessChunkUpgradeChunks = getConfig().getConfigurationSection(businessChunk + "." + lastUpgrade).getKeys(false);
            for (String businessChunkUpgradeChunk : businessChunkUpgradeChunks) {
                int copyChunkX = getConfig().getInt(businessChunk + "." + lastUpgrade + "." + businessChunkUpgradeChunk + ".copy-from-chunk-x");
                int copyChunkZ = getConfig().getInt(businessChunk + "." + lastUpgrade + "." + businessChunkUpgradeChunk + ".copy-from-chunk-z");
                int pasteChunkX = getConfig().getInt(businessChunk + "." + lastUpgrade + "." + businessChunkUpgradeChunk + ".paste-to-chunk-x");
                int pasteChunkZ = getConfig().getInt(businessChunk + "." + lastUpgrade + "." + businessChunkUpgradeChunk + ".paste-to-chunk-z");
                int chunkXDelta = pasteChunkX - copyChunkX;
                int chunkZDelta = pasteChunkZ - copyChunkZ;

                for (Entity painting : world.getEntities()) {
                    Bukkit.getLogger().info("entity: " + painting.getType());
                    if (painting instanceof Painting) {
                        Bukkit.getLogger().info("painting");
                        int paintingChunkX = (int) (painting.getLocation().getX()/CHUNK_WIDTH);
                        int paintingChunkZ = (int) (painting.getLocation().getZ()/CHUNK_WIDTH);
                        Bukkit.getLogger().info("paintingChunkX: " + paintingChunkX + " paintingChunkZ" + paintingChunkZ);
                        Bukkit.getLogger().info("painting name: " + painting.getName());
                        if (paintingChunkX == copyChunkX && paintingChunkZ == copyChunkZ) {
                            double paintingX = painting.getLocation().getX();
                            double paintingY = painting.getLocation().getY();
                            double paintingZ = painting.getLocation().getZ();
                            Painting newPainting = clonePaintingEntityToAnotherLocation((Painting) painting, new Location(painting.getWorld(), paintingX + CHUNK_WIDTH * chunkXDelta, paintingY, paintingZ + CHUNK_WIDTH * chunkZDelta));
                        }
                    }
                }
            }
        }
    }

    public void removeAllPaintings(World world) {
        Set<String> businessesChunks = getConfig().getKeys(false);
        businessesChunks.remove("enable");
        Set<String> businessChunkUpgrades;
        String lastUpgrade;
        Set<String> businessChunkUpgradeChunks;
        for (String businessChunk : businessesChunks) {
            businessChunkUpgrades = getConfig().getConfigurationSection(businessChunk).getKeys(false);
            lastUpgrade = (String) businessChunkUpgrades.toArray()[businessChunkUpgrades.size() - 1];
            businessChunkUpgradeChunks = getConfig().getConfigurationSection(businessChunk + "." + lastUpgrade).getKeys(false);
            for (String businessChunkUpgradeChunk : businessChunkUpgradeChunks) {
                //int copyChunkX = getConfig().getInt(businessChunk + "." + lastUpgrade + "." + businessChunkUpgradeChunk + ".copy-from-chunk-x");
                //int copyChunkZ = getConfig().getInt(businessChunk + "." + lastUpgrade + "." + businessChunkUpgradeChunk + ".copy-from-chunk-z");
                int pasteChunkX = getConfig().getInt(businessChunk + "." + lastUpgrade + "." + businessChunkUpgradeChunk + ".paste-to-chunk-x");
                int pasteChunkZ = getConfig().getInt(businessChunk + "." + lastUpgrade + "." + businessChunkUpgradeChunk + ".paste-to-chunk-z");
                //int chunkXDelta = pasteChunkX - copyChunkX;
                //int chunkZDelta = pasteChunkZ - copyChunkZ;

                for (Painting painting : world.getEntitiesByClass(Painting.class)) {
                    int paintingChunkX = (int) (painting.getLocation().getX()/CHUNK_WIDTH);
                    int paintingChunkZ = (int) (painting.getLocation().getZ()/CHUNK_WIDTH);
                    Bukkit.getLogger().info("paintingChunkX: " + paintingChunkX + " paintingChunkZ" + paintingChunkZ);
                    Bukkit.getLogger().info("painting name: " + painting.getName());
                    if (paintingChunkX == pasteChunkX && paintingChunkZ == pasteChunkZ) {
                        painting.remove();
                    }
                }
            }
        }
    }

    public Painting clonePaintingEntityToAnotherLocation(Painting painting, Location location) {
        Painting paintingClone = location.getWorld().spawn(location, Painting.class);
        paintingClone.setArt(painting.getArt());
        paintingClone.setFallDistance(painting.getFallDistance());
        paintingClone.setPortalCooldown(painting.getPortalCooldown());
        paintingClone.setFacingDirection(painting.getFacing());
        paintingClone.setVelocity(painting.getVelocity());
        Bukkit.getLogger().info("Painting clone name: " + paintingClone.getName());
        return paintingClone;
    }

    public static CustomConfig getPlayerUpgradesConfig() {
        return playerUpgradesConfig;
    }

    public static CustomConfig getChunkDataConfig() {
        return chunkDataConfig;
    }

    public static EntityHider getEntityHider() {
        return entityHider;
    }

    public static BlockCityTycoonStructures getPlugin() {
        return plugin;
    }
}
