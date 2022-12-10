package me.darkmun.blockcitytycoonstructures.serializers;

import net.minecraft.server.v1_12_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftItemFrame;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPainting;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Painting;

import java.io.*;

public class NbtTagCompoundSerializer {

    public static <T extends Entity> NBTTagCompound getNBTCompoundData(T entity, Class<T> clazz) {
        NBTTagCompound compound = new NBTTagCompound();
        if (clazz.equals(Painting.class)) {
            Bukkit.getLogger().info("Painting!!!!!!!!!!!");
            net.minecraft.server.v1_12_R1.EntityPainting nmsEntity = ((CraftPainting) entity).getHandle();
            nmsEntity.save(compound);
            nmsEntity.b(compound);
        } else if (clazz.equals(ItemFrame.class)) {
            Bukkit.getLogger().info("ItemFrame!!!!!!!!");
            net.minecraft.server.v1_12_R1.EntityItemFrame nmsEntity = ((CraftItemFrame) entity).getHandle();
            nmsEntity.save(compound);
            nmsEntity.b(compound);
        } else {
            Bukkit.getLogger().info("Other!!!!!!!!!!");
            net.minecraft.server.v1_12_R1.Entity nmsEntity = ((CraftEntity) entity).getHandle();
            nmsEntity.save(compound);
        }
        return compound;
    }

    /*public static NBTTagCompound getNBTCompoundData(ItemFrame entity) {
        net.minecraft.server.v1_12_R1.EntityItemFrame nmsEntity = ((CraftItemFrame) entity).getHandle(); //Converting our Entity to NMS
        NBTTagCompound compound = new NBTTagCompound();
        nmsEntity.save(compound); //Taking our entity and calling the obfuscated a_ method which will fill out our NBTCompound Object for us.
        nmsEntity.b(compound);
        return compound;
    }*/

    public static <T extends Entity> void serialize(DataOutput destination, T painting, Class<T> clazz) throws IOException {
        NBTTagCompound compound = getNBTCompoundData(painting, clazz);
        destination.writeUTF(compound.toString());
    }

    public static NBTTagCompound deserialize(DataInput destination) throws IOException, MojangsonParseException {
        return MojangsonParser.parse(destination.readUTF());
    }

}
