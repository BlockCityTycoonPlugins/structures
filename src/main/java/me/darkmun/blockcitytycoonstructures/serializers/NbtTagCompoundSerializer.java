package me.darkmun.blockcitytycoonstructures.serializers;

import net.minecraft.server.v1_12_R1.*;
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
            net.minecraft.server.v1_12_R1.EntityPainting nmsEntity = ((CraftPainting) entity).getHandle();
            nmsEntity.save(compound);
            nmsEntity.b(compound);
        } else if (clazz.equals(ItemFrame.class)) {
            net.minecraft.server.v1_12_R1.EntityItemFrame nmsEntity = ((CraftItemFrame) entity).getHandle();
            nmsEntity.save(compound);
            nmsEntity.b(compound);
        } else {
            net.minecraft.server.v1_12_R1.Entity nmsEntity = ((CraftEntity) entity).getHandle();
            nmsEntity.save(compound);
        }
        return compound;
    }

    public static <T extends Entity> void serialize(DataOutput destination, T painting, Class<T> clazz) throws IOException {
        NBTTagCompound compound = getNBTCompoundData(painting, clazz);
        destination.writeUTF(compound.toString());
    }

    public static NBTTagCompound deserialize(DataInput destination) throws IOException, MojangsonParseException {
        return MojangsonParser.parse(destination.readUTF());
    }

}
