/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.aztec;

import georegression.struct.shapes.Polygon2D_F64;
import lombok.Getter;
import org.ddogleg.struct.DogArray;

/**
 * Information on a detected Aztec Code
 *
 * @author Peter Abeles
 */
public class AztecCode {

	/** Number of layers or rings outside the locator pattern that data is encoded on */
	public int dataLayers = 0;

	/** Which Structure does it have. Determins shape of locator pattern and maximum number of data layers. */
	public Structure structure = Structure.COMPACT;

	/**
	 * Locations of extern contours around the squares in a locator pattern. Starts from the innermost ring to
	 * the outermost. 2-rings for "compact" and 3-rings for "full-range"
	 */
	public final DogArray<Polygon2D_F64> locatorRings = new DogArray<>(() -> new Polygon2D_F64(4), Polygon2D_F64::zero);

	/** Number of squares (data bits) wide the marker is */
	public int getMarkerSquareCount() {
		return getLocatorSquareCount() + 6 + dataLayers*4;
	}

	/** Number of rings in the locator pattern */
	public int getLocatorRingCount() {
		return switch (structure) {
			case COMPACT -> 2;
			case FULL -> 3;
		};
	}

	/** Number of squares in locator pattern */
	public int getLocatorSquareCount() {
		return (getLocatorRingCount() - 1)*4 + 1;
	}

	public void reset() {
		dataLayers = 0;
		structure = Structure.COMPACT;
		locatorRings.reset();
	}

	public AztecCode setTo( AztecCode src ) {
		return this;
	}

	/** Which symbol structure is used */
	enum Structure {
		COMPACT(4),
		FULL(32);

		Structure(int maxDataLayers) {
			this.maxDataLayers = maxDataLayers;
		}

		/** Maximum number of data layers */
		@Getter int maxDataLayers;
	}
}
