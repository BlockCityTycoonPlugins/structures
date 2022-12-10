package me.darkmun.blockcitytycoonstructures;


import com.comphenix.protocol.wrappers.WrappedBlockData;
import io.netty.buffer.ByteBuf;
import io.netty.internal.tcnative.Buffer;
import net.minecraft.server.v1_12_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.material.MaterialData;

import java.awt.image.DataBufferByte;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

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

    public long readVarLong(ByteBuf data) {
        long value = 0;
        int position = 0;
        byte currentByte;

        while (true) {
            currentByte = data.readByte();
            value |= (long) (currentByte & SEGMENT_BITS) << position;

            if ((currentByte & CONTINUE_BIT) == 0) break;

            position += 7;

            if (position >= 64) throw new RuntimeException("VarLong is too big");
        }

        return value;
    }
    private int GetGlobalPaletteIDFromState(IBlockData state) {
        // NOTE: This method will change in 1.13

        byte metadata = (byte) state.getBlock().toLegacyData(state);
        int id = Block.getId(state.getBlock());

        return id << 4 | metadata;
    }

    private IBlockData GetStateFromGlobalPaletteID(int value) {
        // NOTE: This method will change in 1.13

        byte metadata = (byte) (value & 0xF);
        int id = value >> 4;

        return Block.getById(id).fromLegacyData(metadata);
    }

    /*public interface Palette {
        int idForState(MaterialData state);
        MaterialData stateForId(int id);
        byte getBitsPerBlock();
        void read(ByteBuf data);
        void write(ByteBuf data);
    }

    public class IndirectPalette implements Palette {
        Map<Integer, MaterialData> idToState;
        Map<MaterialData, Integer> stateToId;
        byte bitsPerBlock;

        public IndirectPalette(byte palBitsPerBlock) {
            bitsPerBlock = palBitsPerBlock;
        }

        public int idForState(MaterialData state) {
            return stateToId.get(state);
        }

        public MaterialData stateForId(int id) {
            return idToState.get(id);
        }

        public byte getBitsPerBlock() {
            return bitsPerBlock;
        }

        public void read(ByteBuf data) {
            idToState = new HashMap<>();
            stateToId = new HashMap<>();
            // Palette Length
            int length = readVarInt(data);
            Bukkit.getLogger().info("Palette length: " + length);
            // Palette
            for (int id = 0; id < length; id++) {
                int stateId = readVarInt(data);
                Bukkit.getLogger().info("State id: " + Integer.toBinaryString(stateId));
                MaterialData state = GetStateFromGlobalPaletteID(stateId);
                idToState.put(id, state);
                stateToId.put(state, id);
            }
        }

        public void write(ByteBuf data) {
            assert (idToState.size() == stateToId.size()); // both should be equivalent
            // Palette Length
            data.writeInt(idToState.size());
            // Palette
            for (int id = 0; id < idToState.size(); id++) {
                MaterialData state = idToState.get(id);
                int stateId = GetGlobalPaletteIDFromState(state);
                data.writeInt(stateId);
            }
        }
    }

    public class DirectPalette implements Palette {
        public int idForState(MaterialData state) {
            return GetGlobalPaletteIDFromState(state);
        }

        public MaterialData stateForId(int id) {
            return GetStateFromGlobalPaletteID(id);
        }

        public byte getBitsPerBlock() {
            return 13; // currently 13
        }

        public void read(ByteBuf data) {
            // Dummy Palette Length
            data.readInt();
        }

        public void write(ByteBuf data) {
            // Dummy Palette Length (ignored)
            data.writeInt(0);
        }
    }

    public Palette ChoosePalette(byte bitsPerBlock) {
        if (bitsPerBlock <= 4) {
            return new IndirectPalette((byte) 4);
        } else if (bitsPerBlock <= 8) {
            return new IndirectPalette(bitsPerBlock);
        } else {
            return new DirectPalette();
        }
    }*/

    public void ReadChunkColumn(Chunk chunk, int mask, ByteBuf data) throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        for (int sectionY = 0; sectionY < (CHUNK_HEIGHT / SECTION_HEIGHT); sectionY++) {
            if ((mask & (1 << sectionY)) != 0) {  // Is the given bit set in the mask?
                byte bitsPerBlock = data.readByte();

                ChunkSection section;
                if (Bukkit.getWorld("world").getEnvironment().getId() == org.bukkit.World.Environment.NORMAL.getId()) {
                    section = new ChunkSection(sectionY, true);
                }
                else {
                    section = new ChunkSection(sectionY, false);
                }

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
                //Bukkit.getLogger().info("Palette length: " + length);
                // Palette
                for (int id = 0; id < length; id++) {
                    int stateId = readVarInt(data);
                    //Bukkit.getLogger().info("State id: " + Integer.toBinaryString(stateId));
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




                //Поменять этот палит (а точнее заменить рид) на встроенный палит, сделать штуки с помощью рефлексии


                // A bitmask that contains bitsPerBlock set bits
                /*int individualValueMask = (1 << bitsPerBlock) - 1;


                Bukkit.getLogger().info("Data array length: " + dataArrayLength);
                Bukkit.getLogger().info("Section Y: " + sectionY);


                for (int y = 0; y < SECTION_HEIGHT; y++) {
                    for (int z = 0; z < SECTION_WIDTH; z++) {
                        for (int x = 0; x < SECTION_WIDTH; x++) {
                            int blockNumber = (((y * SECTION_HEIGHT) + z) * SECTION_WIDTH) + x;
                            int startLong = (blockNumber * bitsPerBlock) / 64;
                            int startOffset = (blockNumber * bitsPerBlock) % 64;
                            int endLong = ((blockNumber + 1) * bitsPerBlock - 1) / 64;

                            int id;
                            if (startLong == endLong) {
                                id = (int)(dataArray[startLong] >> startOffset);
                            } else {
                                int endOffset = 64 - startOffset;
                                id = (int)(dataArray[startLong] >> startOffset | dataArray[endLong] << endOffset);
                            }
                            id &= individualValueMask;

                            // data should always be valid for the palette
                            // If you're reading a power of 2 minus one (15, 31, 63, 127, etc...) that's out of bounds,
                            // you're probably reading light data instead
                            MaterialData state = palette.stateForId(id);
                            if (state == null) {
                                double log = Math.log(id) / Math.log(2);
                                long roundLog = Math.round(log);
                                int powerOfTwo = (int) Math.pow(2, roundLog);
                                id = powerOfTwo - 1 - id;
                                //id &= 3;
                                //state = new MaterialData(0, (byte) 0);
                                state = palette.stateForId(id);
                                Bukkit.getLogger().info("Fake");
                                Bukkit.getLogger().info("power of Two: " + powerOfTwo);
                                //if (x == 5 && 3 <= y && y <= 8) {
                                Bukkit.getLogger().info("Block number: " + blockNumber);
                                Bukkit.getLogger().info("PaletteID: " + id);
                                Bukkit.getLogger().info("x: " + x + " y: " + y + " z: " + z);
                                Bukkit.getLogger().info("State type id: " + state.getItemTypeId() + " State data: " + state.getData());
                                //}
                            }

                            section.setType(x, y, z, Block.getById(state.getItemTypeId()).fromLegacyData(state.getData()));
                            //(IBlockData) WrappedBlockData.createData(state.getItemType(), state.getItemTypeId())
                        }
                    }
                }*/

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

                // May replace an existing section or a null one
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
}
