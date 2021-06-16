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
package de.bluecolored.bluemap.core.mca.extensions;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class GlassPaneConnectExtension extends ConnectSameOrFullBlockExtension {

	private static final HashSet<String> AFFECTED_BLOCK_IDS = new HashSet<>(Arrays.asList(
			"minecraft:glass_pane",
			"minecraft:white_stained_glass_pane",
			"minecraft:orange_stained_glass_pane",
			"minecraft:magenta_stained_glass_pane",
			"minecraft:light_blue_white_stained_glass_pane",
			"minecraft:yellow_stained_glass_pane",
			"minecraft:lime_stained_glass_pane",
			"minecraft:pink_stained_glass_pane",
			"minecraft:gray_stained_glass_pane",
			"minecraft:light_gray_stained_glass_pane",
			"minecraft:cyan_stained_glass_pane",
			"minecraft:purple_stained_glass_pane",
			"minecraft:blue_stained_glass_pane",
			"minecraft:green_stained_glass_pane",
			"minecraft:red_stained_glass_pane",
			"minecraft:black_stained_glass_pane",
			"minecraft:iron_bars"
		));
	
	@Override
	public Set<String> getAffectedBlockIds() {
		return AFFECTED_BLOCK_IDS;
	}

}
