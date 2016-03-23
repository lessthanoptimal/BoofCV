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

package boofcv.alg.feature.describe.impl;

import boofcv.alg.feature.describe.DescribePointPixelRegion;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.feature.TupleDesc_F32;
import boofcv.struct.image.GrayF32;

import java.util.Arrays;

/**
 * Implementation of {@link DescribePointPixelRegion}.
 *
 * @author Peter Abeles
 */
public class ImplDescribePointPixelRegion_F32 extends DescribePointPixelRegion<GrayF32,TupleDesc_F32> {

	public ImplDescribePointPixelRegion_F32(int regionWidth, int regionHeight) {
		super(regionWidth, regionHeight);
	}

	@Override
	public void process(int c_x, int c_y, TupleDesc_F32 desc) {
		// if it is entirely inside the image then faster code can be run
		if(BoofMiscOps.checkInside(image,c_x,c_y,radiusWidth,radiusHeight)) {
			int centerIndex = image.startIndex + c_y*image.stride + c_x;
			for( int i = 0; i < offset.length; i++ ) {
				desc.value[i] = image.data[centerIndex + offset[i]];
			}
		} else {
			// all pixels outside the image will be zero
			Arrays.fill(desc.value, 0);

			// only read pixels inside the image
			int x0 = c_x-radiusWidth;
			int x1 = c_x+radiusWidth;
			int y0 = c_y-radiusHeight;
			int y1 = c_y+radiusHeight;

			if( x0 < 0 ) x0 = 0;
			if( y0 < 0 ) y0 = 0;
			if( x1 >= image.width ) x1 = image.width-1;
			if( y1 >= image.height ) y1 = image.height-1;

			for( int y = y0; y <= y1; y++) {
				int indexImage = image.startIndex + y*image.stride + x0;
				int indexDesc = (y - (c_y-radiusHeight))*regionWidth + (x0-(c_x-radiusWidth));
				for( int x = x0; x <= x1; x++ ) {
					desc.value[indexDesc++] = image.data[indexImage++];
				}
			}
		}
	}

	@Override
	public Class<TupleDesc_F32> getDescriptorType() {
		return TupleDesc_F32.class;
	}
}
