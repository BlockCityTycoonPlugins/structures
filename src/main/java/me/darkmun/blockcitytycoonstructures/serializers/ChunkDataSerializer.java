package me.darkmun.blockcitytycoonstructures.serializers;


import io.netty.buffer.ByteBuf;
import net.minecraft.server.v1_12_R1.*;
import org.bukkit.Bukkit;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ChunkDataSerializer {

    public final int CHUNK_HEIGHT = 256;
    public final int SECTION_HEIGHT = 16;
    public final int SECTION_WIDTH = 16;
    private static final int SEGMENT_BITS = 0x7F;
    private static final int CONTINUE_BIT = 0x80;

    public int readVarInt(ByteBuf data) {
        int value = 0;
        int position = 0;
        byte currentByte;

        while (true) {
            currentByte = data.readByte();
            value |= (currentByte & SEGMENT_BITS) << position;

            if ((currentByte & CONTINUE_BIT) == 0) break;

            position += 7;

            if (position >= 32) throw new RuntimeException("VarInt is too big");
        }

        return value;
    }

    /*private int GetGlobalPaletteIDFromState(IBlockData state) {
        // NOTE: This method will change in 1.13

        byte metadata = (byte) state.getBlock().toLegacyData(state);
        int id = Block.getId(state.getBlock());

        return id << 4 | metadata;
    }*/

    @SuppressWarnings("deprecation")
    private IBlockData GetStateFromGlobalPaletteID(int value) {
        // NOTE: This method will change in 1.13

        byte metadata = (byte) (value & 0xF);
        int id = value >> 4;

        return Block.getById(id).fromLegacyData(metadata);
    }

    @SuppressWarnings("deprecation")
    public void ReadChunkColumn(Chunk chunk, int mask, ByteBuf data) throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        for (int sectionY = 0; sectionY < (CHUNK_HEIGHT / SECTION_HEIGHT); sectionY++) {
            if ((mask & (1 << sectionY)) != 0) {  // Is the given bit set in the mask?

                ChunkSection section;
                if (Bukkit.getWorld("world").getEnvironment().getId() == org.bukkit.World.Environment.NORMAL.getId()) {
                    section = new ChunkSection(sectionY, true);
                }
                else {
                    section = new ChunkSection(sectionY, false);
                }
                writeDataToChunkSection(data, section);
                chunk.getSections()[sectionY] = section;
            }
        }

        byte[] biomes = chunk.getBiomeIndex();
        for (int z = 0; z < SECTION_WIDTH; z++) {
            for (int x = 0; x < SECTION_WIDTH; x++) {
                biomes[z << 4 | x] = data.readByte();
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void writeDataToChunkSection(ByteBuf data, ChunkSection section) throws NoSuchFieldException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        byte bitsPerBlock = data.readByte();

        Field dataPaletteBlockField = ChunkSection.class.getDeclaredField("blockIds");
        Method dataPaletteBlockBMethod = DataPaletteBlock.class.getDeclaredMethod("b", int.class);
        Field dataBitsField = DataPaletteBlock.class.getDeclaredField("b");
        Field dataBitsAField = DataBits.class.getDeclaredField("a");
        Field dataPaletteField = DataPaletteBlock.class.getDeclaredField("c");

        dataBitsField.setAccessible(true);
        dataBitsAField.setAccessible(true);
        dataPaletteBlockField.setAccessible(true);
        dataPaletteBlockBMethod.setAccessible(true);
        dataPaletteField.setAccessible(true);

        dataPaletteBlockBMethod.invoke(dataPaletteBlockField.get(section), bitsPerBlock);
        dataPaletteBlockBMethod.setAccessible(false);

        DataPalette dataPalette = (DataPalette) dataPaletteField.get(dataPaletteBlockField.get(section));
        int length = readVarInt(data);
        // Palette
        for (int id = 0; id < length; id++) {
            int stateId = readVarInt(data);
            IBlockData state = GetStateFromGlobalPaletteID(stateId);
            dataPalette.a(state);
        }
        dataPaletteField.setAccessible(false);

        int dataArrayLength = readVarInt(data);
        long[] dataArray = (long[]) dataBitsAField.get(dataBitsField.get(dataPaletteBlockField.get(section)));
        for (int i = 0; i < dataArrayLength; i ++) {
            dataArray[i] = data.readLong();
        }
        dataBitsAField.setAccessible(false);
        dataBitsField.setAccessible(false);
        dataPaletteBlockField.setAccessible(false);


        for (int y = 0; y < SECTION_HEIGHT; y++) {
            for (int z = 0; z < SECTION_WIDTH; z++) {
                for (int x = 0; x < SECTION_WIDTH; x += 2) {
                    // Note: x += 2 above; we read 2 values along x each time
                    byte value = data.readByte();

                    section.b(x, y, z, value & 0xF);
                    section.b(x + 1, y, z, (value >> 4) & 0xF);
                }
            }
        }

        if (Bukkit.getWorld("world").getEnvironment().getId() == org.bukkit.World.Environment.NORMAL.getId()) { // IE, current dimension is overworld / 0
            for (int y = 0; y < SECTION_HEIGHT; y++) {
                for (int z = 0; z < SECTION_WIDTH; z++) {
                    for (int x = 0; x < SECTION_WIDTH; x += 2) {
                        // Note: x += 2 above; we read 2 values along x each time
                        byte value = data.readByte();

                        section.a(x, y, z, value & 0xF);
                        section.a(x + 1, y, z, (value >> 4) & 0xF);
                    }
                }
            }
        }
    }
}
