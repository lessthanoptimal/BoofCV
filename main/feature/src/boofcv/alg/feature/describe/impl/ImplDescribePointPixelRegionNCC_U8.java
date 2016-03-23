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

import boofcv.alg.feature.describe.DescribePointPixelRegionNCC;
import boofcv.struct.feature.NccFeature;
import boofcv.struct.image.GrayU8;

/**
 * Implementation of {@link boofcv.alg.feature.describe.DescribePointPixelRegionNCC}.
 *
 * @author Peter Abeles
 */
public class ImplDescribePointPixelRegionNCC_U8 extends DescribePointPixelRegionNCC<GrayU8> {

	public ImplDescribePointPixelRegionNCC_U8(int regionWidth, int regionHeight) {
		super(regionWidth, regionHeight);
	}

	@Override
	public void process(int c_x, int c_y, NccFeature desc) {
		double mean = 0;
		int centerIndex = image.startIndex + c_y*image.stride + c_x;
		for( int i = 0; i < offset.length; i++ ) {
			mean += desc.value[i] = image.data[centerIndex + offset[i]] & 0xFF;
		}
		mean /= offset.length;
		double variance = 0;
		for( int i = 0; i < desc.value.length; i++ ) {
			double d = desc.value[i] -= mean;
			variance += d*d;
		}
		variance /= offset.length;

		desc.mean = mean;
		desc.sigma = Math.sqrt(variance);
	}
}
