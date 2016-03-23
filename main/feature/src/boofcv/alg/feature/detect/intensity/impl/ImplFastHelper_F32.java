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

package boofcv.alg.feature.detect.intensity.impl;

import boofcv.struct.image.GrayF32;

/**
 * @author Peter Abeles
 */
public class ImplFastHelper_F32 implements FastHelper<GrayF32> {
	private GrayF32 image;

	// how similar do the pixel in the circle need to be to the center pixel
	private float pixelTol;

	// lower and upper threshold
	private float lower,upper;
	// value of the center pixel
	private float centerValue;

	// pixel index offsets for circle
	private int offsets[];

	public ImplFastHelper_F32(int pixelTol) {
		this.pixelTol = pixelTol;
	}

	@Override
	public void setImage(GrayF32 image , int offsets[] ) {
		this.image = image;
		this.offsets = offsets;
	}

	@Override
	public void setThresholds(int index) {
		centerValue = image.data[index];
		lower = centerValue - pixelTol;
		upper = centerValue + pixelTol;
	}

	@Override
	public float scoreLower( int index ) {
		int total = 0;
		int count = 0;
		for( int i = 0; i < offsets.length; i++ ) {
			float v = image.data[index+offsets[i]];
			if( v < lower ) {
				total += v;
				count++;
			}
		}

		if( count == 0 )
			return 0;

		return centerValue*count - total;
	}

	@Override
	public float scoreUpper( int index ) {
		int total = 0;
		int count = 0;
		for( int i = 0; i < offsets.length; i++ ) {
			float v = image.data[index+offsets[i]];
			if( v > upper ) {
				total += v;
				count++;
			}
		}

		if( count == 0 )
			return 0;

		return total - centerValue*count;
	}

	@Override
	public boolean checkPixelLower( int index )
	{
		return (image.data[index] ) < lower;
	}

	@Override
	public boolean checkPixelUpper( int index )
	{
		return (image.data[index] ) > upper;
	}
}
