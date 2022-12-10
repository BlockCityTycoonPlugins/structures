package me.darkmun.blockcitytycoonstructures;

import net.minecraft.server.v1_12_R1.*;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftItemFrame;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPainting;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Painting;

import java.io.*;

public class NbtTagCompoundSerializer {

    public static NBTTagCompound getNBTCompoundData(Painting entity) {
        net.minecraft.server.v1_12_R1.EntityPainting nmsEntity = ((CraftPainting) entity).getHandle(); //Converting our Entity to NMS
        NBTTagCompound compound = new NBTTagCompound();
        nmsEntity.save(compound); //Taking our entity and calling the obfuscated a_ method which will fill out our NBTCompound Object for us.
        nmsEntity.b(compound);
        return compound;
    }

    public static NBTTagCompound getNBTCompoundData(ItemFrame entity) {
        net.minecraft.server.v1_12_R1.EntityItemFrame nmsEntity = ((CraftItemFrame) entity).getHandle(); //Converting our Entity to NMS
        NBTTagCompound compound = new NBTTagCompound();
        nmsEntity.save(compound); //Taking our entity and calling the obfuscated a_ method which will fill out our NBTCompound Object for us.
        nmsEntity.b(compound);
        return compound;
    }

    public static void serialize(Painting painting, DataOutput destination) throws IOException {
        NBTTagCompound compound = getNBTCompoundData(painting);
        destination.writeUTF(compound.toString());

        //NBTCompressedStreamTools.a(compound, destination);
    }

    public static NBTTagCompound deserialize(DataInput destination, World world) throws IOException, MojangsonParseException {

        return MojangsonParser.parse(destination.readUTF());//NBTCompressedStreamTools.a(destination);
    }

}
