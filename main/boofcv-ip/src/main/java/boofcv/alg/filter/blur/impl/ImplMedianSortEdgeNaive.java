/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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
import org.ddogleg.sorting.QuickSelect;
import org.ddogleg.struct.DogArray_F32;
import org.ddogleg.struct.DogArray_I32;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Generated;

/**
 * <p>
 * Median filter which process only the image edges and uses quick select find the median.
 * </p>
 *
 * <p>
 * radius: size of the filter's box.<br>
 * storage:  Used to store local values. If null an array will be declared.
 * </p>
 *
 * <p>DO NOT MODIFY. Automatically generated code created by GenerateImplMedianSortEdgeNaive</p>
 *
 * @author Peter Abeles
 */
@Generated("boofcv.alg.filter.blur.impl.GenerateImplMedianSortEdgeNaive")
public class ImplMedianSortEdgeNaive {

	public static void process( GrayF32 input, GrayF32 output, int radiusX, int radiusY,
								@Nullable DogArray_F32 workspace ) {
		int w = 2*radiusX + 1;
		int h = 2*radiusY + 1;

		float[] workArray = BoofMiscOps.checkDeclare(workspace, w*h, false);

		for (int y = 0; y < radiusY; y++) {
			int minI = y - radiusY;
			int maxI = y + radiusY + 1;
			if (minI < 0) minI = 0;
			if (maxI > input.height) maxI = input.height;

			for (int x = 0; x < input.width; x++) {
				int minJ = x - radiusX;
				int maxJ = x + radiusX + 1;

				// bound it ot be inside the image
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

		for (int y = input.height - radiusY; y < input.height; y++) {
			int minI = y - radiusY;
			int maxI = y + radiusY + 1;
			if (minI < 0) minI = 0;
			if (maxI > input.height) maxI = input.height;

			for (int x = 0; x < input.width; x++) {
				int minJ = x - radiusX;
				int maxJ = x + radiusX + 1;

				// bound it ot be inside the image
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

		for (int y = radiusY; y < input.height - radiusY; y++) {
			int minI = y - radiusY;
			int maxI = y + radiusY + 1;
			for (int x = 0; x < radiusX; x++) {
				int minJ = x - radiusX;
				int maxJ = x + radiusX + 1;

				// bound it ot be inside the image
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

		for (int y = radiusY; y < input.height - radiusY; y++) {
			int minI = y - radiusY;
			int maxI = y + radiusY + 1;
			for (int x = input.width - radiusX; x < input.width; x++) {
				int minJ = x - radiusX;
				int maxJ = x + radiusX + 1;

				// bound it ot be inside the image
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

	public static void process( GrayI input, GrayI output, int radiusX, int radiusY,
								@Nullable DogArray_I32 workspace ) {
		int w = 2*radiusX + 1;
		int h = 2*radiusY + 1;

		int[] workArray = BoofMiscOps.checkDeclare(workspace, w*h, false);

		for (int y = 0; y < radiusY; y++) {
			int minI = y - radiusY;
			int maxI = y + radiusY + 1;
			if (minI < 0) minI = 0;
			if (maxI > input.height) maxI = input.height;

			for (int x = 0; x < input.width; x++) {
				int minJ = x - radiusX;
				int maxJ = x + radiusX + 1;

				// bound it ot be inside the image
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

		for (int y = input.height - radiusY; y < input.height; y++) {
			int minI = y - radiusY;
			int maxI = y + radiusY + 1;
			if (minI < 0) minI = 0;
			if (maxI > input.height) maxI = input.height;

			for (int x = 0; x < input.width; x++) {
				int minJ = x - radiusX;
				int maxJ = x + radiusX + 1;

				// bound it ot be inside the image
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

		for (int y = radiusY; y < input.height - radiusY; y++) {
			int minI = y - radiusY;
			int maxI = y + radiusY + 1;
			for (int x = 0; x < radiusX; x++) {
				int minJ = x - radiusX;
				int maxJ = x + radiusX + 1;

				// bound it ot be inside the image
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

		for (int y = radiusY; y < input.height - radiusY; y++) {
			int minI = y - radiusY;
			int maxI = y + radiusY + 1;
			for (int x = input.width - radiusX; x < input.width; x++) {
				int minJ = x - radiusX;
				int maxJ = x + radiusX + 1;

				// bound it ot be inside the image
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
}
