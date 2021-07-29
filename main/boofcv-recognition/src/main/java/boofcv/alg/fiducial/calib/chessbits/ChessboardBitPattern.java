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

package boofcv.alg.fiducial.calib.chessbits;

import boofcv.struct.geo.PointIndex2D_F64;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_B;

/**
 * Storage for a found chessboard pattern.
 *
 * @author Peter Abeles
 */
public class ChessboardBitPattern {
	/** Which marker it came from. -1 if unknown */
	public int marker;

	/** Number of square columns in the pattern. */
	public int cols;

	/** Number of square rows in the pattern. */
	public int rows;

	/** Found calibration corners it was able to observe */
	public final DogArray<PointIndex2D_F64> corners = new DogArray<>(PointIndex2D_F64::new, (c)->c.setTo(-1,-1,-1));

	/** Indicates if a corner was decoded next to an encoding. Very unlikely to be a false positive. */
	public final DogArray_B encoded = new DogArray_B();

	/** Number of squares it was able to decode in the found chessboard */
	public int decodedSquares;

	public void reset() {
		marker = -1;
		cols = -1;
		rows = -1;
		corners.reset();
		encoded.reset();
		decodedSquares = -1;
	}
}
