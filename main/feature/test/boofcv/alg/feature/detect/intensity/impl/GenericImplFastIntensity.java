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

import boofcv.alg.feature.detect.intensity.DetectorFastNaive;
import boofcv.alg.feature.detect.intensity.FastCornerIntensity;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.misc.DiscretizedCircle;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_I16;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public abstract class GenericImplFastIntensity {
	Random rand = new Random(234);

	int minContinuous;
	int detectDifference ;

	FastCornerIntensity<GrayU8> alg;

	public GenericImplFastIntensity(FastCornerIntensity<GrayU8> alg, int minContinuous, int detectDifference ) {
		this.alg = alg;
		this.minContinuous = minContinuous;
		this.detectDifference = detectDifference;
	}

	@Test
	public void compareToNaiveDetection() {

		GrayU8 input = new GrayU8(40,50);
		GImageMiscOps.fillUniform(input, rand, 0, 50);
		GrayF32 intensity = new GrayF32(input.width,input.height);

		DetectorFastNaive validator = new DetectorFastNaive(3,minContinuous,detectDifference);
		validator.process(input);

		alg.process(input,intensity);

		assertEquals(validator.getCandidates().size,alg.getCandidates().size);

		for( int i = 0; i < validator.getCandidates().size(); i++ ) {
			Point2D_I16 v = validator.getCandidates().get(i);
			Point2D_I16 a = alg.getCandidates().get(i);

			assertEquals(v.x,a.x);
			assertEquals(v.y,a.y);
		}
	}

	@Test
	public void checkIntensity() {
		GrayU8 input = new GrayU8(40,50);
		GrayF32 intensity = new GrayF32(input.width,input.height);

		int []offsets = DiscretizedCircle.imageOffsets(3, input.stride);
		createCircle(4,5,offsets,minContinuous,detectDifference+1,input);
		createCircle(12,20,offsets,minContinuous,detectDifference+10,input);

		alg.process(input,intensity);

		assertTrue(intensity.get(4,5) < intensity.get(12,20));
	}

	private void createCircle( int x , int y , int offsets[] , int n , int b ,
							   GrayU8 image ) {

		int index = image.startIndex + y*image.stride + x;

		for( int i = 0; i < n; i++ ) {
			image.data[index+offsets[i]] = (byte)b;
		}
	}
}
