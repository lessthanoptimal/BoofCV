/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.intensity;

import boofcv.alg.filter.blur.GBlurImageOps;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.GrayF32;
import org.ejml.UtilEjml;

/**
 * @author Peter Abeles
 */
public class XCornerAbeles2019Intensity {

	int radiusA = 3;
	int radiusD = 2;

	GrayF32 blurred = new GrayF32(1,1);

	public void process(GrayF32 input, GrayF32 intensity) {
		intensity.reshape(input.width,input.height);
		ImageMiscOps.fillBorder(intensity,0, radiusA);

		blurred.reshape(input.width,input.height);
		GBlurImageOps.gaussian(input,blurred,-1,1,null);

		for (int y = radiusA; y < input.height - radiusA; y++) {
			int outputIdx = intensity.startIndex + y*intensity.stride + radiusA;
			for (int x = radiusA; x < input.width - radiusA; x++) {
				intensity.data[outputIdx++] = score(x,y);
			}
		}
	}

	private float score( int x , int y ) {
		float a = blurred.unsafe_get(x,y - radiusA);
		float b = blurred.unsafe_get(x,y + radiusA);
		float c = blurred.unsafe_get(x - radiusA,y);
		float d = blurred.unsafe_get(x + radiusA,y);

		float e = blurred.unsafe_get(x - radiusD,y - radiusD);
		float f = blurred.unsafe_get(x + radiusD,y + radiusD);
		float g = blurred.unsafe_get(x - radiusD,y + radiusD);
		float h = blurred.unsafe_get(x + radiusD,y - radiusD);

		float mean = (a+b+c+d)/4f;
		float div = mean + UtilEjml.F_EPS;

		a = (a-mean)/div;
		b = (b-mean)/div;
		c = (c-mean)/div;
		d = (d-mean)/div;

		float inten0 = a*b + c*d;

		mean = (e+f+g+h)/4f;
		div = mean + UtilEjml.F_EPS;

		e = (e-mean)/div;
		f = (f-mean)/div;
		g = (g-mean)/div;
		h = (h-mean)/div;

		float inten1 = e*f + g*h;

		return Math.max(inten0,inten1);
	}
}
