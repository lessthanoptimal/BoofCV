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

import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageGray;

/**
 * Describes a rectangular region using its raw pixel intensities. Score between two regions of this type is typically
 * computed using Sum of Absolute Differences (SAD).  If the descriptor goes outside of the image
 * bounds those pixels will be set to zero in the descriptor.
 *
 * @author Peter Abeles
 */
public abstract class DescribePointPixelRegion<T extends ImageGray, D extends TupleDesc>
		extends DescribePointRectangleRegion<T>
{


	protected DescribePointPixelRegion(int regionWidth, int regionHeight) {
		super(regionWidth, regionHeight);
	}

	/**
	 * Extracts the descriptor from the specified point.
	 *
	 * @param c_x Center of region descriptor.
	 * @param c_y Center of region descriptor.
	 * @param desc Where the descriptor is written to
	 */
	public abstract void process( int c_x , int c_y , D desc );

	/**
	 * The type of region descriptor generated
	 *
	 * @return Returns the descriptor type.
	 */
	public abstract Class<D> getDescriptorType();
}
