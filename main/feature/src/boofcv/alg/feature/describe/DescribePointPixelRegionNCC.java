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

import boofcv.alg.descriptor.DescriptorDistance;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.feature.NccFeature;
import boofcv.struct.image.ImageGray;

/**
 * Describes a rectangular region using its raw pixel intensities which have been normalized for intensity.  This
 * allows the descriptor to be light invariant.  The entire region must be inside the image for a descriptor to be computed
 * because any outside values will change its intensity normalization.
 *
 * @see DescriptorDistance#ncc(boofcv.struct.feature.NccFeature, boofcv.struct.feature.NccFeature)
 *
 * @author Peter Abeles
 */
public abstract class DescribePointPixelRegionNCC<T extends ImageGray>
		extends DescribePointRectangleRegion<T>
{
	protected DescribePointPixelRegionNCC(int regionWidth, int regionHeight) {
		super(regionWidth, regionHeight);
	}

	/**
	 * The entire region must be inside the image because any outside pixels will change the statistics
	 */
	public boolean isInBounds( int c_x , int c_y ) {
		return BoofMiscOps.checkInside(image, c_x, c_y, radiusWidth, radiusHeight);
	}

	/**
	 * Extracts the descriptor at the specified location.
	 *
	 * @param c_x Center of the descriptor region.
	 * @param c_y Center of the descriptor region.
	 * @param desc Where the description is written to.
	 */
	public abstract void process( int c_x , int c_y , NccFeature desc );
}
