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

package boofcv.alg.feature.associate;

import georegression.struct.point.Point2D_F64;

/**
 * Computes the Euclidean distance squared between two points for association
 *
 * @author Peter Abeles
 */
public class AssociateImageDistanceEuclideanSq implements AssociateImageDistanceFunction {

	final Point2D_F64 src = new Point2D_F64();

	@Override
	public void setSource( int index, Point2D_F64 pixel ) {
		this.src.setTo(pixel);
	}

	@Override
	public double distance( int index, Point2D_F64 pixel ) {
		return src.distance2(pixel);
	}

	@Override
	public AssociateImageDistanceFunction copyConcurrent() {
		return new AssociateImageDistanceEuclideanSq();
	}

	@Override
	public double convertPixelsToDistance( double pixels ) {
		return pixels*pixels;
	}
}
