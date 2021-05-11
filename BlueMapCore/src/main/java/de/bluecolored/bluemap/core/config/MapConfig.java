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
package de.bluecolored.bluemap.core.config;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;
import com.nixxcode.jvmbrotli.common.BrotliLoader;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.map.MapSettings;
import de.bluecolored.bluemap.core.map.hires.RenderSettings;
import de.bluecolored.bluemap.core.util.ConfigUtils;
import de.bluecolored.bluemap.core.util.Compression;
import de.bluecolored.bluemap.core.util.CompressionType;
import ninja.leaping.configurate.ConfigurationNode;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

public class MapConfig implements MapSettings {
	private static final Pattern VALID_ID_PATTERN = Pattern.compile("[a-zA-Z0-9_]+");
	
	private String id;
	private String name;
	private String world;
	
	private Vector2i startPos;
	private int skyColor;
	private float ambientLight;
	
	private boolean renderCaves;
	
	private Vector3i min, max;
	private boolean renderEdges;
	
	private Compression compression;
	
	private boolean ignoreMissingLightData;
	
	private int hiresTileSize;
	
	private int lowresPointsPerHiresTile;
	private int lowresPointsPerLowresTile;
	
	public MapConfig(ConfigurationNode node) throws IOException {
		
		//id
		this.id = node.getNode("id").getString("");
		if (id.isEmpty()) throw new IOException("Invalid configuration: Node maps[?].id is not defined");
		if (!VALID_ID_PATTERN.matcher(id).matches()) throw new IOException("Invalid configuration: Node maps[?].id '" + id + "' has invalid characters in it");
		
		//name
		this.name = node.getNode("name").getString(id);
		
		//world
		this.world = node.getNode("world").getString("");
		if (world.isEmpty()) throw new IOException("Invalid configuration: Node maps[?].world is not defined");
		
		//startPos
		if (!node.getNode("startPos").isVirtual()) this.startPos = ConfigUtils.readVector2i(node.getNode("startPos"));
		
		//skyColor
		if (!node.getNode("skyColor").isVirtual()) this.skyColor = ConfigUtils.readColorInt(node.getNode("skyColor"));
		else this.skyColor = 0x7dabff;
		
		//ambientLight
		this.ambientLight = node.getNode("ambientLight").getFloat(0f);
		
		//renderCaves
		this.renderCaves = node.getNode("renderCaves").getBoolean(false);

		//bounds
		int minX = node.getNode("minX").getInt(MapSettings.super.getMin().getX());
		int maxX = node.getNode("maxX").getInt(MapSettings.super.getMax().getX());
		int minZ = node.getNode("minZ").getInt(MapSettings.super.getMin().getZ());
		int maxZ = node.getNode("maxZ").getInt(MapSettings.super.getMax().getZ());
		int minY = node.getNode("minY").getInt(MapSettings.super.getMin().getY());
		int maxY = node.getNode("maxY").getInt(MapSettings.super.getMax().getY());
		this.min = new Vector3i(minX, minY, minZ);
		this.max = new Vector3i(maxX, maxY, maxZ);
		
		//renderEdges
		this.renderEdges = node.getNode("renderEdges").getBoolean(true);

		//compression and compressionLevel
		this.compression = loadCompressionSettings(node);

		//ignoreMissingLightData
		this.ignoreMissingLightData = node.getNode("ignoreMissingLightData").getBoolean(false);
		
		//tile-settings
		this.hiresTileSize = node.getNode("hires", "tileSize").getInt(32);
		this.lowresPointsPerHiresTile = node.getNode("lowres", "pointsPerHiresTile").getInt(4);
		this.lowresPointsPerLowresTile = node.getNode("lowres", "pointsPerLowresTile").getInt(50);
		
		//check valid tile configuration values
		double blocksPerPoint = (double) this.hiresTileSize / (double) this.lowresPointsPerHiresTile;
		if (blocksPerPoint != Math.floor(blocksPerPoint)) throw new IOException("Invalid configuration: Invalid map resolution settings of map " + id + ": hires.tileSize / lowres.pointsPerTile has to be an integer result");
		
	}

	private Compression loadCompressionSettings(ConfigurationNode node) throws IOException {
		String compressionTypeId = node.getNode("compressionType").getString("gzip");
		CompressionType compressionType = CompressionType.GZIP;
		try {
			compressionType = CompressionType.forId(compressionTypeId);
		} catch (NoSuchElementException ex) {
			throw new IOException("Invalid configuration: Unknown CompressionType '" + compressionTypeId + "'!");
		}

		//backwards-compatibility for 'useCompression' setting
		if (node.getNode("compression").isVirtual()){
			boolean useCompression = node.getNode("useCompression").getBoolean(true);
			compressionType = useCompression ? CompressionType.GZIP : CompressionType.PLAIN;
		}

		int compressionLevel = node.getNode("compressionLevel").getInt(-1);

		//check brotli availabillity
		if (compressionType == CompressionType.BROTLI && !BrotliLoader.isBrotliAvailable()){
			Logger.global.logWarning("Library for brotli-compression is unavailable! Falling back to gzip for map '" + this.id + "'!");
			compressionType = CompressionType.GZIP;
			compressionLevel = -1; //also use default compression-level if we have to fall-back to gzip
		}

		return new Compression(compressionType, compressionLevel == -1 ? compressionType.getDefaultCompressionLevel() : compressionLevel);
	}

	public String getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	public String getWorldPath() {
		return world;
	}
	
	public Vector2i getStartPos() {
		return startPos;
	}
	
	public int getSkyColor() {
		return skyColor;
	}
	
	public float getAmbientLight() {
		return ambientLight;
	}

	public boolean isRenderCaves() {
		return renderCaves;
	}
	
	public boolean isIgnoreMissingLightData() {
		return ignoreMissingLightData;
	}

	@Override
	public int getHiresTileSize() {
		return hiresTileSize;
	}

	@Override
	public int getLowresPointsPerHiresTile() {
		return lowresPointsPerHiresTile;
	}

	@Override
	public int getLowresPointsPerLowresTile() {
		return lowresPointsPerLowresTile;
	}

	@Override
	public boolean isExcludeFacesWithoutSunlight() {
		return !isRenderCaves();
	}
	
	@Override
	public Vector3i getMin() {
		return min;
	}
	
	@Override
	public Vector3i getMax() {
		return max;
	}
	
	@Override
	public boolean isRenderEdges() {
		return renderEdges;
	}
	
	@Override
	public Compression getCompression() {
		return compression;
	}
	
}
