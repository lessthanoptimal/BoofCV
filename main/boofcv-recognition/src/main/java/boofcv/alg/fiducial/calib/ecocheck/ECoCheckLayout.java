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

package boofcv.alg.fiducial.calib.ecocheck;

import org.ddogleg.struct.DogArray_I32;

/**
 * Defines bit sampling pattern for different sized grids. Damage to markers tends to occur in local regions
 * and not as random bit errors. ECC can only fix errors in a per-word basis (8-bits), so it makes sense to cluster
 * all the points in a word as close to each other as possible. A simple formula is used to select the layout of
 * bits in the N by N grid that attempts to group bits in the same word together. A naive layout would cause a small
 * error in a local region to damage multiple words, crippling error correction.
 *
 * @author Peter Abeles
 */
public class ECoCheckLayout {
	/**
	 * Select points using a snake pattern. A cluster is found by moving in one direction until it hits an object
	 * or the flip counter hits its limit. It then shifts over one and turns around
	 *
	 * @param gridSize Size of data grid
	 * @param order (Output)
	 */
	public void selectSnake( int gridSize, DogArray_I32 order ) {
		int row = 0, col = 0;
		boolean up = true;
		boolean right = true;

		int rowLower = 0;
		int rowUpper = Math.min(gridSize, 4);

		order.reserve(gridSize*gridSize);
		order.reset();
		order.add(0);

		while (true) {
			int prevRow = row;
			int prevCol = col;
			row += up ? 1 : -1;

			if (row < rowLower || row >= rowUpper) {
				col += right ? 1 : -1;
				row = prevRow;

				if (col < 0 || col >= gridSize) {
					right = !right;
					col = prevCol;

					if (!up) {
						row = rowUpper;
						up = true;
					} else {
						row += 1;
						while (row < rowUpper) {
							row += 1;
							order.add(row*gridSize + col);
						}
					}
					rowLower = rowUpper;
					rowUpper = Math.min(gridSize, rowUpper + 4);
					if (rowLower >= gridSize)
						break;
				} else {
					up = !up;
				}
			}

			order.add(row*gridSize + col);
		}
	}
}
