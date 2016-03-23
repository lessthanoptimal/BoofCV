/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.describe;

import boofcv.alg.feature.describe.brief.BinaryCompareDefinition_I32;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.feature.TupleDesc_B;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_I32;

/**
 * <p>
 * For each bit in the descriptor it samples two points inside an image patch and compares their values.  A value of
 * 1 or 0 is assigned depending on their relative values.  This type of descriptor is referred to as a random tree [1],
 * random fern, and is used in BRIEF.
 * </p>
 *
 * <p>
 * [1] Y. Amit and D. Geman. Shape Quantization and Recognition with Randomized Trees. Neural Computation,
 * 9(7):1545–1588, 1997.
 * </p>
 *
 * @author Peter Abeles
 */
public abstract class DescribePointBinaryCompare<T extends ImageGray> {
	// describes where points are sampled and how they are compared
	protected BinaryCompareDefinition_I32 definition;

	// Input image
	protected T image;

	// precomputed offsets of sample points inside the image.
	// splitting it into two arrays avoids an extract array lookup, boosting performance by about 30%
	private  int offsets[] = new int[0]; // just a temporary place holder
	protected int offsetsA[] = new int[0];
	protected int offsetsB[] = new int[0];

	public DescribePointBinaryCompare(BinaryCompareDefinition_I32 definition) {
		this.definition = definition;

		offsets = new int[ definition.samplePoints.length ];
		offsetsA = new int[ definition.compare.length ];
		offsetsB = new int[ definition.compare.length ];
	}

	/**
	 * Specifies the image from which feature descriptions are to be created.
	 *
	 * @param image Image being examined.
	 */
	public void setImage(T image) {
		this.image = image;

		// precompute offsets for faster computing later on
		for( int i = 0; i < definition.samplePoints.length ; i++ ) {
			Point2D_I32 a = definition.samplePoints[i];

			offsets[i] = image.stride*a.y + a.x;
		}

		for( int i = 0; i < definition.compare.length ; i++ ) {
			Point2D_I32 p = definition.compare[i];
			offsetsA[i] = offsets[p.x];
			offsetsB[i] = offsets[p.y];
		}
	}

	/**
	 * Computes the descriptor at the specified point.  If the region go outside of the image then a description
	 * will not be made.
	 *
	 * @param c_x Center of region being described.
	 * @param c_y Center of region being described.
	 * @param feature Where the descriptor is written to.
	 */
	public void process( int c_x , int c_y , TupleDesc_B feature ) {
		if( BoofMiscOps.checkInside(image,c_x, c_y, definition.radius) ) {
			processInside(c_x,c_y,feature);
		} else {
			processBorder(c_x,c_y,feature);
		}
	}

	/**
	 * Called if the descriptor region is contained entirely inside the image
	 */
	public abstract void processInside( int c_x , int c_y , TupleDesc_B feature );

	/**
	 * Called if the descriptor region goes outside the image border
	 */
	public abstract void processBorder( int c_x , int c_y , TupleDesc_B feature );

	public BinaryCompareDefinition_I32 getDefinition() {
		return definition;
	}
}
