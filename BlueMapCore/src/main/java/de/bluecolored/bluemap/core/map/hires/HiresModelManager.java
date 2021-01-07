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
package de.bluecolored.bluemap.core.render.hires;
package de.bluecolored.bluemap.core.map.hires;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.util.AABB;
import de.bluecolored.bluemap.core.util.Compression;
import de.bluecolored.bluemap.core.util.AtomicFileHelper;
import de.bluecolored.bluemap.core.util.FileUtils;
import de.bluecolored.bluemap.core.CompressionConfig;
import de.bluecolored.bluemap.core.world.Grid;
import de.bluecolored.bluemap.core.world.World;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class HiresModelManager {

	private final Path fileRoot;
	private final HiresModelRenderer renderer;
	private final Grid tileGrid;
	private final Compression compression;

	public HiresModelManager(Path fileRoot, ResourcePack resourcePack, RenderSettings renderSettings, Grid tileGrid) {
		this(fileRoot, new HiresModelRenderer(resourcePack, renderSettings), tileGrid, renderSettings.useGzipCompression());
	}

	public HiresModelManager(Path fileRoot, HiresModelRenderer renderer, Grid tileGrid, Compression compression) {
		this.fileRoot = fileRoot;
		this.renderer = renderer;

		this.tileGrid = tileGrid;
		
		this.compression = compression;
	}
	
	/**
	 * Renders the given world tile with the provided render-settings
	 */
	public HiresModel render(World world, Vector2i tile) {
		Vector2i tileMin = tileGrid.getCellMin(tile);
		Vector2i tileMax = tileGrid.getCellMax(tile);

		Vector3i modelMin = new Vector3i(tileMin.getX(), world.getMinY(), tileMin.getY());
		Vector3i modelMax = new Vector3i(tileMax.getX(), world.getMaxY(), tileMax.getY());

		HiresModel model = renderer.render(world, modelMin, modelMax);
		save(model, tile);
		return model;
	}
	
	private void save(final HiresModel model, Vector2i tile) {
		final String modelJson = model.toBufferGeometry().toJson();
		save(modelJson, tile);
	}
	
	private void save(String modelJson, Vector2i tile){
		File file = getFile(tile, compressionType.getFileExtension());
		
		try {
			OutputStream os = compressionType.getOutputStream(new BufferedOutputStream(AtomicFileHelper.createFilepartOutputStream(file)));
			OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8);
			try (
				PrintWriter pw = new PrintWriter(osw);
			){
				pw.print(modelJson);
			}
			
			//logger.logDebug("Saved hires model: " + model.getTile()); 
		} catch (IOException e){
			Logger.global.logError("Failed to save hires model: " + file, e);
		}
	}
	
	/**
	 * Returns all tiles that the provided chunks are intersecting
	 */
	@Deprecated
	public Collection<Vector2i> getTilesForChunks(Iterable<Vector2i> chunks){
		Set<Vector2i> tiles = new HashSet<>();
		for (Vector2i chunk : chunks) {
			Vector3i minBlockPos = new Vector3i(chunk.getX() * 16, 0, chunk.getY() * 16);
			
			//loop to cover the case that a tile is smaller then a chunk, should normally only add one tile (at 0, 0)
			for (int x = 0; x < 15; x += tileGrid.getGridSize().getX()) {
				for (int z = 0; z < 15; z += tileGrid.getGridSize().getY()) {
					tiles.add(posToTile(minBlockPos.add(x, 0, z)));
				}
			}
			
			tiles.add(posToTile(minBlockPos.add(0, 0, 15)));
			tiles.add(posToTile(minBlockPos.add(15, 0, 0)));
			tiles.add(posToTile(minBlockPos.add(15, 0, 15)));
		}
		
		return tiles;
	}
	
	/**
	 * Returns the tile-grid
	 */
	public Grid getTileGrid() {
		return tileGrid;
	}
	
	/**
	 * Converts a block-position to a map-tile-coordinate
	 */
	public Vector2i posToTile(Vector3i pos){
		return tileGrid.getCell(pos.toVector2(true));
	}

	/**
	 * Converts a block-position to a map-tile-coordinate
	 */
	public Vector2i posToTile(Vector3d pos){
		return tileGrid.getCell(new Vector2i(pos.getFloorX(), pos.getFloorZ()));
	}
	
	/**
	 * Returns the file for a tile
	 */
	public File getFile(Vector2i tilePos){
		return FileUtils.coordsToFile(fileRoot, tilePos, "json" + compression.getCompressionType().getFileExtension());
	}
	
}
