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

import georegression.geometry.UtilPolygons2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import org.ddogleg.struct.DogArray;

/**
 * Describes the locator pattern for an {@link AztecCode}
 *
 * @author Peter Abeles
 */
public class AztecPyramid {
	/**
	 * layers or rings in the pyramid. outermost is first. Corners are aligned so that they match up.
	 */
	public final DogArray<Layer> layers = new DogArray<>(Layer::new, Layer::reset);

	public void resize( int numLayers ) {
		layers.resetResize(numLayers);
	}

	/**
	 * Rotates the inner ring so that its corners indexes match up with the corners in the outer ring.
	 */
	public void alignCorners() {
		if (layers.size <= 1)
			return;

		double bestError = Double.MAX_VALUE;
		int best = -1;

		Polygon2D_F64 a = layers.get(0).square;
		Polygon2D_F64 b = layers.get(1).square;

		for (int i = 0; i < 4; i++) {
			double distance = a.get(0).distance2(b.get(i));
			if (distance < bestError) {
				bestError = distance;
				best = i;
			}
		}

		for (int rotateIdx = 0; rotateIdx < best; rotateIdx++) {
			UtilPolygons2D_F64.shiftDown(b);
		}
	}

	public void setTo( AztecPyramid src ) {
		this.layers.resetResize(src.layers.size);
		for (int i = 0; i < src.layers.size; i++) {
			layers.get(i).setTo(src.layers.get(i));
		}
	}

	public void reset() {
		layers.reset();
	}

	/** Description of a layer in the pyramid */
	public static class Layer {
		/** Detected square inside the image */
		public final Polygon2D_F64 square = new Polygon2D_F64(4);
		/** Center of square found using the intersection of the two lines created from opposing corners */
		public final Point2D_F64 center = new Point2D_F64();
		/** Local threshold computed from polygon's eedge */
		public double threshold;

		public void setTo( Layer src ) {
			this.square.setTo(src.square);
			this.center.setTo(src.center);
			this.threshold = src.threshold;
		}

		public void reset() {
			square.zero();
			threshold = -1;
		}
	}
}
