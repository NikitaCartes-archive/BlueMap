/*
 * This file is part of BlueMap, licensed under the MIT License (MIT).
 *
 * Copyright (c) Blue (Lukas Rieger) <https://bluecolored.de>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.bluecolored.bluemap.core.mca;

import com.flowpowered.math.vector.Vector3i;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.mca.mapping.BiomeMapper;
import de.bluecolored.bluemap.core.world.Biome;
import de.bluecolored.bluemap.core.world.BlockState;
import de.bluecolored.bluemap.core.world.LightData;
import net.querz.nbt.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class ChunkAnvil116 extends MCAChunk {
	private BiomeMapper biomeIdMapper;

	private boolean isGenerated;
	private boolean hasLight;
	private Map<Integer, Section> sections;
	private int sectionMin, sectionMax;
	private int[] biomes;
	
	@SuppressWarnings("unchecked")
	public ChunkAnvil116(CompoundTag chunkTag, boolean ignoreMissingLightData, BiomeMapper biomeIdMapper) {
		super(chunkTag);
		
		this.biomeIdMapper = biomeIdMapper;
		
		CompoundTag levelData = chunkTag.getCompoundTag("Level");
		
		String status = levelData.getString("Status");
		this.isGenerated = status.equals("full");
		this.hasLight = isGenerated;
		
		if (!isGenerated && ignoreMissingLightData) {
			isGenerated = !status.equals("empty");
		}

		this.sections = new HashMap<>(); // Is using a has-map the fastest/best way for an int->Object mapping?
		this.sectionMin = Integer.MAX_VALUE;
		this.sectionMax = Integer.MIN_VALUE;
		if (levelData.containsKey("Sections")) {
			for (CompoundTag sectionTag : ((ListTag<CompoundTag>) levelData.getListTag("Sections"))) {
				if (sectionTag.getListTag("Palette") == null) continue; // ignore empty sections

				Section section = new Section(sectionTag);
				int y = section.getSectionY();

				if (sectionMin > y) sectionMin = y;
				if (sectionMax < y) sectionMax = y;

				sections.put(y, section);
			}
		}
		
		Tag<?> tag = levelData.get("Biomes"); //tag can be byte-array or int-array
		if (tag instanceof ByteArrayTag) {
			byte[] bs = ((ByteArrayTag) tag).getValue();
			this.biomes = new int[bs.length];
			
			for (int i = 0; i < bs.length; i++) {
				biomes[i] = bs[i] & 0xFF;
			}
		}
		else if (tag instanceof IntArrayTag) {
			this.biomes = ((IntArrayTag) tag).getValue();
		}
		
		if (biomes == null) {
			this.biomes = new int[0];
		}
	}

	@Override
	public boolean isGenerated() {
		return isGenerated;
	}

	@Override
	public BlockState getBlockState(Vector3i pos) {
		int sectionY = pos.getY() >> 4;
		
		Section section = this.sections.get(sectionY);
		if (section == null) return BlockState.AIR;
		
		return section.getBlockState(pos);
	}

	@Override
	public LightData getLightData(Vector3i pos) {
		if (!hasLight) return LightData.SKY;
		
		int sectionY = pos.getY() >> 4;

		Section section = this.sections.get(sectionY);
		if (section == null) return (sectionY < sectionMin) ? LightData.ZERO : LightData.SKY;
		
		return section.getLightData(pos);
	}

	@Override
	public Biome getBiome(int x, int y, int z) {
		if (biomes.length < 16) return Biome.DEFAULT;

		x = (x & 0xF) / 4; // Math.floorMod(pos.getX(), 16)
		z = (z & 0xF) / 4;
		y = y / 4;
		int biomeIntIndex = y * 16 + z * 4 + x; // TODO: fix this for 1.17+ worlds with negative y?

		// shift y up/down if not in range
		if (biomeIntIndex >= biomes.length) biomeIntIndex -= (((biomeIntIndex - biomes.length) >> 4) + 1) * 16;
		if (biomeIntIndex < 0) biomeIntIndex -= (biomeIntIndex >> 4) * 16;
		
		return biomeIdMapper.get(biomes[biomeIntIndex]);
	}

	@Override
	public int getMinY(int x, int z) {
		return sectionMin * 16;
	}

	@Override
	public int getMaxY(int x, int z) {
		return sectionMax * 16 + 15;
	}

	private static class Section {
		private static final String AIR_ID = "minecraft:air";
		
		private int sectionY;
		private byte[] blockLight;
		private byte[] skyLight;
		private long[] blocks;
		private BlockState[] palette;

		private int bitsPerBlock;
		
		@SuppressWarnings("unchecked")
		public Section(CompoundTag sectionData) {
			this.sectionY = sectionData.get("Y", NumberTag.class).asInt();
			this.blockLight = sectionData.getByteArray("BlockLight");
			this.skyLight = sectionData.getByteArray("SkyLight");
			this.blocks = sectionData.getLongArray("BlockStates");

			if (blocks.length < 256 && blocks.length > 0) blocks = Arrays.copyOf(blocks, 256);
			if (blockLight.length < 2048 && blockLight.length > 0) blockLight = Arrays.copyOf(blockLight, 2048);
			if (skyLight.length < 2048 && skyLight.length > 0) skyLight = Arrays.copyOf(skyLight, 2048);
			
			//read block palette
			ListTag<CompoundTag> paletteTag = (ListTag<CompoundTag>) sectionData.getListTag("Palette");
			if (paletteTag != null) {
				this.palette = new BlockState[paletteTag.size()];
				for (int i = 0; i < this.palette.length; i++) {
					CompoundTag stateTag = paletteTag.get(i);
					
					String id = stateTag.getString("Name"); //shortcut to save time and memory
					if (id.equals(AIR_ID)) {
						palette[i] = BlockState.AIR;
						continue;
					}
					
					Map<String, String> properties = new HashMap<>();
					
					if (stateTag.containsKey("Properties")) {
						CompoundTag propertiesTag = stateTag.getCompoundTag("Properties");
						for (Entry<String, Tag<?>> property : propertiesTag) {
							properties.put(property.getKey().toLowerCase(), ((StringTag) property.getValue()).getValue().toLowerCase());
						}
					}
					
					palette[i] = new BlockState(id, properties);
				}
			} else {
				this.palette = new BlockState[0];
			}
			
			this.bitsPerBlock = this.blocks.length >> 6; // available longs * 64 (bits per long) / 4096 (blocks per section) (floored result)
		}
		
		public int getSectionY() {
			return sectionY;
		}
		
		public BlockState getBlockState(Vector3i pos) {
			if (blocks.length == 0) return BlockState.AIR;
			
			int x = pos.getX() & 0xF; // Math.floorMod(pos.getX(), 16)
			int y = pos.getY() & 0xF;
			int z = pos.getZ() & 0xF;
			int blockIndex = y * 256 + z * 16 + x;

			long value = MCAMath.getValueFromLongArray(blocks, blockIndex, bitsPerBlock);
			if (value >= palette.length) {
				Logger.global.noFloodWarning("palettewarning", "Got palette value " + value + " but palette has size of " + palette.length + "! (Future occasions of this error will not be logged)");
				return BlockState.MISSING;
			}
			
			return palette[(int) value];
		}
		
		public LightData getLightData(Vector3i pos) {
			if (blockLight.length == 0 && skyLight.length == 0) return LightData.ZERO;
			
			int x = pos.getX() & 0xF; // Math.floorMod(pos.getX(), 16)
			int y = pos.getY() & 0xF;
			int z = pos.getZ() & 0xF;
			int blockByteIndex = y * 256 + z * 16 + x;
			int blockHalfByteIndex = blockByteIndex >> 1; // blockByteIndex / 2 
			boolean largeHalf = (blockByteIndex & 0x1) != 0; // (blockByteIndex % 2) == 0

			int blockLight = this.blockLight.length > 0 ? MCAMath.getByteHalf(this.blockLight[blockHalfByteIndex], largeHalf) : 0;
			int skyLight = this.skyLight.length > 0 ? MCAMath.getByteHalf(this.skyLight[blockHalfByteIndex], largeHalf) : 0;
			
			return new LightData(skyLight, blockLight);
		}
	}
	
}
