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
package de.bluecolored.bluemap.core.map.hires.blockmodel;

import com.flowpowered.math.matrix.Matrix3f;
import com.flowpowered.math.vector.Vector2f;
import com.flowpowered.math.vector.Vector3f;
import com.flowpowered.math.vector.Vector4f;
import de.bluecolored.bluemap.core.MinecraftVersion;
import de.bluecolored.bluemap.core.map.hires.RenderSettings;
import de.bluecolored.bluemap.core.model.ExtendedFace;
import de.bluecolored.bluemap.core.model.ExtendedModel;
import de.bluecolored.bluemap.core.resourcepack.BlockColorCalculator;
import de.bluecolored.bluemap.core.resourcepack.BlockModelResource;
import de.bluecolored.bluemap.core.resourcepack.Texture;
import de.bluecolored.bluemap.core.resourcepack.TransformedBlockModelResource;
import de.bluecolored.bluemap.core.util.Direction;
import de.bluecolored.bluemap.core.world.Block;
import de.bluecolored.bluemap.core.world.BlockState;

import java.util.Arrays;
import java.util.HashSet;

/**
 * A model builder for all liquid blocks
 */
public class LiquidModelBuilder {
	
	private static final HashSet<String> DEFAULT_WATERLOGGED_BLOCK_IDS = new HashSet<>(Arrays.asList(
			"minecraft:seagrass",
			"minecraft:tall_seagrass",
			"minecraft:kelp",
			"minecraft:kelp_plant",
			"minecraft:bubble_column"
	));
	
	private final BlockState liquidBlockState;
	private final Block block;
	private final RenderSettings renderSettings;
	private final BlockColorCalculator colorCalculator;

	private final boolean useWaterColorMap;
	
	public LiquidModelBuilder(Block block, BlockState liquidBlockState, MinecraftVersion minecraftVersion, RenderSettings renderSettings, BlockColorCalculator colorCalculator) {
		this.block = block;
		this.renderSettings = renderSettings;
		this.liquidBlockState = liquidBlockState;
		this.colorCalculator = colorCalculator;

		this.useWaterColorMap = minecraftVersion.isAtLeast(new MinecraftVersion(1, 13));
	}

	public BlockStateModel build(TransformedBlockModelResource bmr) {
		return build(bmr.getModel());
	}
	
	public BlockStateModel build(BlockModelResource bmr) {
		if (this.renderSettings.isExcludeFacesWithoutSunlight() && block.getSunLightLevel() == 0) return new BlockStateModel();
		
		int level = getLiquidLevel(block.getBlockState());
		float[] heights = new float[]{16f, 16f, 16f, 16f};
		float coloralpha = 0.2f;
		
		if (level < 8 && !(level == 0 && isLiquid(block.getRelativeBlock(0, 1, 0)))){
			heights = new float[]{
					getLiquidCornerHeight(-1, 0, -1),
					getLiquidCornerHeight(-1, 0, 0),
					getLiquidCornerHeight(0, 0, -1),
					getLiquidCornerHeight(0, 0, 0)
				};

			coloralpha = 0.8f;
		}
		
		BlockStateModel model = new BlockStateModel();
		Texture texture = bmr.getTexture("still");
		
		Vector3f[] c = new Vector3f[]{
			new Vector3f( 0, 0, 0 ),
			new Vector3f( 0, 0, 16 ),
			new Vector3f( 16, 0, 0 ),
			new Vector3f( 16, 0, 16 ),
			new Vector3f( 0, heights[0], 0 ),
			new Vector3f( 0, heights[1], 16 ),
			new Vector3f( 16, heights[2], 0 ),
			new Vector3f( 16, heights[3], 16 ),
		};

		int textureId = texture.getId();
		Vector3f tintcolor = Vector3f.ONE;
		if (useWaterColorMap && liquidBlockState.getFullId().equals("minecraft:water")) {
			tintcolor = colorCalculator.getWaterAverageColor(block);
		}
		
		createElementFace(model, Direction.DOWN, c[0], c[2], c[3], c[1], tintcolor, textureId);
		createElementFace(model, Direction.UP, c[5], c[7], c[6], c[4], tintcolor, textureId);
		createElementFace(model, Direction.NORTH, c[2], c[0], c[4], c[6], tintcolor, textureId);
		createElementFace(model, Direction.SOUTH, c[1], c[3], c[7], c[5], tintcolor, textureId);
		createElementFace(model, Direction.WEST, c[0], c[1], c[5], c[4], tintcolor, textureId);
		createElementFace(model, Direction.EAST, c[3], c[2], c[6], c[7], tintcolor, textureId);
	
		//scale down
		model.transform(Matrix3f.createScaling(1f / 16f));

		//calculate mapcolor
		Vector4f mapcolor = texture.getColor();
		mapcolor = mapcolor.mul(tintcolor.toVector4(coloralpha));
		model.setMapColor(mapcolor);
		
		return model;
	}
	
	private float getLiquidCornerHeight(int x, int y, int z){
		for (int ix = x; ix <= x+1; ix++){
			for (int iz = z; iz<= z+1; iz++){
				if (isLiquid(block.getRelativeBlock(ix, y+1, iz))){
					return 16f;
				}
			}
		}
		
		float sumHeight = 0f;
		int count = 0;
		
		for (int ix = x; ix <= x+1; ix++){
			for (int iz = z; iz<= z+1; iz++){
				Block b = block.getRelativeBlock(ix, y, iz);
				if (isLiquid(b)){
					if (getLiquidLevel(b.getBlockState()) == 0) return 14f;
					
					sumHeight += getLiquidBaseHeight(b.getBlockState());
					count++;
				} 
				
				else if (!isLiquidBlockingBlock(b)){
					count++;
				}
			}
		}
		
		//should both never happen
		if (sumHeight == 0) return 3f;
		if (count == 0) return 3f;
		
		return sumHeight / count;
	}
	
	private boolean isLiquidBlockingBlock(Block block){
		if (block.getBlockState().equals(BlockState.AIR)) return false;
		return true;
	}

	private boolean isLiquid(Block block){
		return isLiquid(block.getBlockState());
	}
	
	private boolean isLiquid(BlockState blockState){
		if (blockState.getFullId().equals(liquidBlockState.getFullId())) return true;
		return LiquidModelBuilder.isWaterlogged(blockState);
	}
	
	private float getLiquidBaseHeight(BlockState block){
		int level = getLiquidLevel(block);
		float baseHeight = 14f - level * 1.9f;
		return baseHeight;
	}
	
	private int getLiquidLevel(BlockState block){
		if (block.getProperties().containsKey("level")) {
			return Integer.parseInt(block.getProperties().get("level"));
		}
		return 0;
	}
	
	private void createElementFace(ExtendedModel model, Direction faceDir, Vector3f c0, Vector3f c1, Vector3f c2, Vector3f c3, Vector3f color, int textureId) {
		
		//face culling
		Block bl = block.getRelativeBlock(faceDir);
		if (isLiquid(bl) || (faceDir != Direction.UP && bl.isCullingNeighborFaces())) return;
		
		//UV
		Vector4f uv = new Vector4f(0, 0, 16, 16).div(16);

		//create both triangles
		Vector2f[] uvs = new Vector2f[4];
		uvs[0] = new Vector2f(uv.getX(), uv.getW());
		uvs[1] = new Vector2f(uv.getZ(), uv.getW());
		uvs[2] = new Vector2f(uv.getZ(), uv.getY());
		uvs[3] = new Vector2f(uv.getX(), uv.getY());
		
		ExtendedFace f1 = new ExtendedFace(c0, c1, c2, uvs[0], uvs[1], uvs[2], textureId);
		ExtendedFace f2 = new ExtendedFace(c0, c2, c3, uvs[0], uvs[2], uvs[3], textureId);
		
		// move face in a tiny bit to avoid z-fighting with waterlogged blocks (doesn't work because it is rounded back when storing the model later)
		//f1.translate(faceDir.opposite().toVector().toFloat().mul(0.01));
		//f2.translate(faceDir.opposite().toVector().toFloat().mul(0.01));
		
		float blockLight = bl.getBlockLightLevel();
		float sunLight = bl.getSunLightLevel();
		
		if (faceDir == Direction.UP) {
			blockLight = block.getBlockLightLevel();
			sunLight = block.getSunLightLevel();
		}
		
		f1.setC1(color);
		f1.setC2(color);
		f1.setC3(color);
		f2.setC1(color);
		f2.setC2(color);
		f2.setC3(color);
		
		f1.setBl1(blockLight);
		f1.setBl2(blockLight);
		f1.setBl3(blockLight);
		f2.setBl1(blockLight);
		f2.setBl2(blockLight);
		f2.setBl3(blockLight);
		
		f1.setSl1(sunLight);
		f1.setSl2(sunLight);
		f1.setSl3(sunLight);
		f2.setSl1(sunLight);
		f2.setSl2(sunLight);
		f2.setSl3(sunLight);
		
		//add the face
		model.addFace(f1);
		model.addFace(f2);
	}
	
	public static boolean isWaterlogged(BlockState blockState) {
		if (DEFAULT_WATERLOGGED_BLOCK_IDS.contains(blockState.getFullId())) return true;
		return blockState.getProperties().getOrDefault("waterlogged", "false").equals("true");
	}
	
}
