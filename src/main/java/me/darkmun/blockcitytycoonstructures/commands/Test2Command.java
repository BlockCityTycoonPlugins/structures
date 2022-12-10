package me.darkmun.blockcitytycoonstructures.commands;

//import com.comphenix.packetwrapper.WrapperPlayServerSpawnEntityPainting;
import com.comphenix.packetwrapper.WrapperPlayServerSpawnEntityPainting;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.EnumWrappers;
import me.darkmun.blockcitytycoonstructures.listeners.ChunkSendingListener;
import net.minecraft.server.v1_12_R1.Entity;
import net.minecraft.server.v1_12_R1.EntityPainting;
import net.minecraft.server.v1_12_R1.EnumDirection;
import net.minecraft.server.v1_12_R1.PacketPlayOutSpawnEntityPainting;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;

public class Test2Command implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (sender instanceof Player) {
            Player pl = (Player) sender;
            Bukkit.getLogger().info("Painting Count: " + ChunkSendingListener.getEntityCount());
            for (Painting painting : pl.getWorld().getEntitiesByClass(Painting.class)) {
                Bukkit.getLogger().info("Entity ID: " + painting.getEntityId());
                Bukkit.getLogger().info("X: " + painting.getLocation().getX());
                Bukkit.getLogger().info("Y: " + painting.getLocation().getY());
                Bukkit.getLogger().info("Z: " + painting.getLocation().getZ());
            }
            /*try {
                Field entityCount = Entity.class.getDeclaredField("entityCount");
                entityCount.setAccessible(true);
                int entityCountInt = (int) entityCount.get(null);
                Bukkit.getLogger().info("Entity count1: " + entityCountInt);
                WrapperPlayServerSpawnEntityPainting painting = new WrapperPlayServerSpawnEntityPainting();
                painting.setEntityID(entityCountInt);
                painting.getHandle().getStrings().write(0, "Kebab");
                painting.setDirection(EnumWrappers.Direction.SOUTH);
                painting.setLocation(new BlockPosition(-83, 45, 25));
                painting.sendPacket(pl);
                entityCount.set(null, entityCountInt + 1);
                entityCount.setAccessible(false);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }*/
            if (args.length != 0) {
                //EntityPainting painting = new EntityPainting(((CraftWorld)pl.getWorld()).getHandle(), new net.minecraft.server.v1_12_R1.BlockPosition(-83, 45, 25), EnumDirection.SOUTH);
                /*try {
                    Field entityCount = Entity.class.getDeclaredField("entityCount");
                    entityCount.setAccessible(true);
                    Bukkit.getLogger().info("Entity count2: " + (int) entityCount.get(null));
                    entityCount.setAccessible(false);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    e.printStackTrace();
                }*/
                //painting.art = EntityPainting.EnumArt.ALBAN;
                //Bukkit.getLogger().info("New painting id (before creating packet): " + painting.getId());
                //PacketPlayOutSpawnEntityPainting packet = new PacketPlayOutSpawnEntityPainting(painting);
                //Bukkit.getLogger().info("New painting id (after creating packet): " + painting.getId());
                //((CraftPlayer)pl).getHandle().playerConnection.sendPacket(packet);
                //Bukkit.getLogger().info("New painting id (after sending packet): " + painting.getId());
                WrapperPlayServerSpawnEntityPainting painting1 = new WrapperPlayServerSpawnEntityPainting();
                painting1.setEntityID(10005);
                painting1.getHandle().getStrings().write(0, "Kebab");
                painting1.setDirection(EnumWrappers.Direction.SOUTH);
                painting1.setLocation(new BlockPosition(-82, 45, 25));
                painting1.sendPacket(pl);
            }
        }

        return true;
    }
}