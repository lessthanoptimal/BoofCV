/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.alg.filter.blur.impl;

import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayI;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.Planar;
import org.ddogleg.sorting.QuickSelect;
import org.ddogleg.struct.GrowQueue;
import org.ddogleg.struct.GrowQueue_F32;
import org.ddogleg.struct.GrowQueue_I32;
import org.jetbrains.annotations.Nullable;

/**
 * <p>
 * Median filter which uses quick select to find the local median value.  It is naive because the sort operation is started
 * from scratch for each pixel, discarding any information learned previously.
 * </p>
 *
 * @author Peter Abeles
 */
public class ImplMedianSortNaive {

	/**
	 * Performs a median filter.
	 *
	 * @param input Raw input image.
	 * @param output Filtered image.
	 * @param radiusX Size of the filter region. x-axis
	 * @param radiusY Size of the filter region. Y-axis
	 * @param workspace Array used for storage.  If null a new array is declared internally.
	 */
	public static void process( GrayI input, GrayI output, int radiusX, int radiusY, @Nullable GrowQueue_I32 workspace ) {

		int w = 2*radiusX + 1;
		int h = 2*radiusY + 1;

		int[] workArray = BoofMiscOps.checkDeclare(workspace, w*h, false);

		for (int y = 0; y < input.height; y++) {
			int minI = y - radiusY;
			int maxI = y + radiusY + 1;

			// bound the y-axis inside the image
			if (minI < 0) minI = 0;
			if (maxI > input.height) maxI = input.height;

			for (int x = 0; x < input.width; x++) {
				int minJ = x - radiusX;
				int maxJ = x + radiusX + 1;

				// bound the x-axis to be inside the image
				if (minJ < 0) minJ = 0;
				if (maxJ > input.width) maxJ = input.width;

				int index = 0;

				for (int i = minI; i < maxI; i++) {
					for (int j = minJ; j < maxJ; j++) {
						workArray[index++] = input.get(j, i);
					}
				}

				// use quick select to avoid sorting the whole list
				int median = QuickSelect.select(workArray, index/2, index);
				output.set(x, y, median);
			}
		}
	}

	/**
	 * Performs a median filter.
	 *
	 * @param input Raw input image.
	 * @param output Filtered image.
	 * @param radiusX Size of the filter region. x-axis
	 * @param radiusY Size of the filter region. Y-axis
	 * @param workspace Array used for storage.  If null a new array is declared internally.
	 */
	public static void process( GrayF32 input, GrayF32 output, int radiusX, int radiusY,
								@Nullable GrowQueue_F32 workspace ) {

		int w = 2*radiusX + 1;
		int h = 2*radiusY + 1;

		float[] workArray = BoofMiscOps.checkDeclare(workspace, w*h, false);

		for (int y = 0; y < input.height; y++) {
			int minI = y - radiusY;
			int maxI = y + radiusY + 1;

			// bound the y-axis inside the image
			if (minI < 0) minI = 0;
			if (maxI > input.height) maxI = input.height;

			for (int x = 0; x < input.width; x++) {
				int minJ = x - radiusX;
				int maxJ = x + radiusX + 1;

				// bound the x-axis to be inside the image
				if (minJ < 0) minJ = 0;
				if (maxJ > input.width) maxJ = input.width;

				int index = 0;

				for (int i = minI; i < maxI; i++) {
					for (int j = minJ; j < maxJ; j++) {
						workArray[index++] = input.get(j, i);
					}
				}

				// use quick select to avoid sorting the whole list
				float median = QuickSelect.select(workArray, index/2, index);
				output.set(x, y, median);
			}
		}
	}

	public static void process( ImageGray input, ImageGray output, int radiusX, int radiusY,
								@Nullable GrowQueue workspace ) {
		if (input.getDataType().isInteger()) {
			process((GrayI)input, (GrayI)output, radiusX, radiusY, (GrowQueue_I32)workspace);
		} else {
			process((GrayF32)input, (GrayF32)output, radiusX, radiusY, (GrowQueue_F32)workspace);
		}
	}

	public static void process( Planar input, Planar output, int radiusX, int radiusY,
								@Nullable GrowQueue workspace ) {
		for (int i = 0; i < input.getNumBands(); i++) {
			process(input.getBand(i), output.getBand(i), radiusX, radiusY, workspace);
		}
	}
}
