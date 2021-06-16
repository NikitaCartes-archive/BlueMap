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

import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.mca.mapping.BlockIdMapper;
import de.bluecolored.bluemap.core.world.BlockState;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.loader.ConfigurationLoader;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class BlockIdConfig implements BlockIdMapper {

	private final ConfigurationLoader<? extends ConfigurationNode> autopoulationConfigLoader;
	private final Map<BlockNumeralIDMeta, BlockState> numeralMappings;
	private final Map<BlockIDMeta, BlockState> idMappings;

	private final ReentrantReadWriteLock lock;

	public BlockIdConfig(ConfigurationNode node) {
		this(node, null);
	}
	
	public BlockIdConfig(ConfigurationNode node, ConfigurationLoader<? extends ConfigurationNode> autopoulationConfigLoader) {
		this.autopoulationConfigLoader = autopoulationConfigLoader;
		
		this.numeralMappings = new ConcurrentHashMap<>(200, 0.5f, 8);
		this.idMappings = new ConcurrentHashMap<>(200, 0.5f, 8);

		this.lock = new ReentrantReadWriteLock();
		
		for (Entry<Object, ? extends ConfigurationNode> e : node.childrenMap().entrySet()){
			String key = e.getKey().toString();
			String value = e.getValue().getString();

			try {
				int splitIndex = key.lastIndexOf(':');
				
				if (splitIndex <= 0 || splitIndex >= key.length() - 1) {
					Logger.global.logWarning("Loading BlockIdConfig: Failed to parse blockid:meta from key '" + key + "'");
					continue;
				}

				String blockId = key.substring(0, splitIndex);
				int blockNumeralId;
				try {
					blockNumeralId = Integer.parseInt(blockId);
				} catch (NumberFormatException ex) {
					blockNumeralId = -1;
				}
				int blockMeta = Integer.parseInt(key.substring(splitIndex + 1));
				BlockState state = BlockState.fromString(value);
				
				if (blockNumeralId >= 0) {
					BlockNumeralIDMeta idmeta = new BlockNumeralIDMeta(blockNumeralId, blockMeta);
					if (blockNumeralId == 0) state = BlockState.AIR; //use the static field to increase render speed (== comparison)
					numeralMappings.put(idmeta, state);
				} else {
					BlockIDMeta idmeta = new BlockIDMeta(blockId, blockMeta);
					idMappings.put(idmeta, state);
				}
			} catch (NumberFormatException ex) {
				Logger.global.logWarning("Loading BlockIdConfig: Failed to parse blockid:meta from key '" + key + "'");
			} catch (IllegalArgumentException ex) {
				Logger.global.logWarning("Loading BlockIdConfig: Failed to parse BlockState from value '" + value + "'");
			}
		}	
	}
	
	@Override
	public BlockState get(int numeralId, int meta) {
		if (numeralId == 0) return BlockState.AIR;

		BlockNumeralIDMeta numidmeta;
		BlockState state;

		try { lock.readLock().lock();

			numidmeta = new BlockNumeralIDMeta(numeralId, meta);
			state = numeralMappings.get(numidmeta);

		} finally { lock.readLock().unlock(); }

		if (state == null) {

			try { lock.writeLock().lock();

				state = numeralMappings.getOrDefault(new BlockNumeralIDMeta(numeralId, 0), BlockState.MISSING); //meta-fallback
				numeralMappings.put(numidmeta, state);

				if (autopoulationConfigLoader != null) {
					try {
						ConfigurationNode node = autopoulationConfigLoader.load();
						node.node(numeralId + ":" + meta).set(state.toString());
						autopoulationConfigLoader.save(node);
					} catch (IOException ex) {
						Logger.global.noFloodError("blockidconf-autopopulate-ioex", "Failed to auto-populate BlockIdConfig!", ex);
					}
				}

			} finally { lock.writeLock().unlock(); }

		}

		return state;

	}

	@Override
	public BlockState get(String id, int numeralId, int meta) {
		if (numeralId == 0) return BlockState.AIR;

		BlockNumeralIDMeta numidmeta;
		BlockState state;
		BlockIDMeta idmeta = null;

		try { lock.readLock().lock();

			numidmeta = new BlockNumeralIDMeta(numeralId, meta);
			state = numeralMappings.get(numidmeta);

			if (state == null) {
				idmeta = new BlockIDMeta(id, meta);
				state = idMappings.get(idmeta);
			}

		} finally { lock.readLock().unlock(); }

		if (state == null) {

			try { lock.writeLock().lock();

				state = idMappings.get(new BlockIDMeta(id, 0));
				if (state == null) {
					state = numeralMappings.get(new BlockNumeralIDMeta(numeralId, 0));
					if (state == null) state = new BlockState(id);
				}

				idMappings.put(idmeta, state);

				if (autopoulationConfigLoader != null) {
					try {
						ConfigurationNode node = autopoulationConfigLoader.load();
						node.node(id + ":" + meta).set(state.toString());
						autopoulationConfigLoader.save(node);
					} catch (IOException ex) {
						Logger.global.noFloodError("blockidconf-autopopulate-ioex", "Failed to auto-populate BlockIdConfig!", ex);
					}
				}

			} finally { lock.writeLock().unlock(); }

		}

		return state;
	}
	
	static class BlockNumeralIDMeta {
		private final int id;
		private final int meta;
		
		public BlockNumeralIDMeta(int id, int meta) {
			this.id = id;
			this.meta = meta;
		}
		
		public int getId() {
			return id;
		}
		
		public int getMeta() {
			return meta;
		}
		
		@Override
		public int hashCode() {
			return id * 16 + meta;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof BlockNumeralIDMeta) {
				BlockNumeralIDMeta other = (BlockNumeralIDMeta) obj;
				return other.id == id && other.meta == meta;
			}
			
			return false;
		}
	}
	
	static class BlockIDMeta {
		private final String id;
		private final int meta;
		
		public BlockIDMeta(String id, int meta) {
			this.id = id;
			this.meta = meta;
		}
		
		public String getId() {
			return id;
		}
		
		public int getMeta() {
			return meta;
		}
		
		@Override
		public int hashCode() {
			return id.hashCode() * 16 + meta;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof BlockIDMeta) {
				BlockIDMeta other = (BlockIDMeta) obj;
				return other.id.equals(id) && other.meta == meta;
			}
			
			return false;
		}
	}
	
}
