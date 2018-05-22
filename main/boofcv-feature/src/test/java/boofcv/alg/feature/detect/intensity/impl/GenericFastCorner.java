/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.feature.detect.intensity.FastCornerDetector;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.misc.DiscretizedCircle;
import boofcv.struct.QueueCorner;
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
public abstract class GenericFastCorner {
	Random rand = new Random(234);

	int minContinuous;
	int detectDifference ;

	FastCornerDetector<GrayU8> alg;

	public GenericFastCorner(FastCornerDetector<GrayU8> alg, int minContinuous, int detectDifference ) {
		this.alg = alg;
		this.minContinuous = minContinuous;
		this.detectDifference = detectDifference;
	}

	@Test
	public void perfectCircle() {
		perfectCircle(true);
		perfectCircle(false);

	}

	public void perfectCircle( boolean high ) {
		int w=12;
		int h=14;
		GrayU8 input = new GrayU8(w,h);
		GrayF32 intensity = new GrayF32(input.width,input.height);

		int[] offsets = DiscretizedCircle.imageOffsets(3, input.stride);

		for (int i = 0; i < 16; i++) {
//			System.out.println("i = "+i);
			GImageMiscOps.fill(input,99);

			int center = input.getIndex(w/2,h/2);
			for (int j = 0; j < minContinuous; j++) {
				int index = center+offsets[(i+j)%16];
				input.data[index] = (byte)(high?255:0);
			}
			input.data[center] = 100;

//			input.print();
			alg.process(input,intensity);

			QueueCorner corners = high?alg.getCornersHigh():alg.getCornersLow();
			assertEquals(1,corners.size);
			Point2D_I16 found = corners.get(0);
			assertEquals(w/2,found.x);
			assertEquals(h/2,found.y);
		}

	}

	@Test
	public void compareToNaiveDetection() {

		GrayU8 input = new GrayU8(40,50);
		GImageMiscOps.fillUniform(input, rand, 0, 50);
		GrayF32 intensity = new GrayF32(input.width,input.height);

		DetectorFastNaive validator = new DetectorFastNaive(3,minContinuous,detectDifference);
		validator.process(input);

		alg.process(input,intensity);

		assertEquals(validator.getCandidatesLow().size,alg.getCornersLow().size);
		assertEquals(validator.getCandidatesHigh().size,alg.getCornersHigh().size);

		for( int i = 0; i < validator.getCandidatesLow().size(); i++ ) {
			Point2D_I16 v = validator.getCandidatesLow().get(i);
			Point2D_I16 a = alg.getCornersLow().get(i);

			assertEquals(v.x,a.x);
			assertEquals(v.y,a.y);
		}
		for( int i = 0; i < validator.getCandidatesHigh().size(); i++ ) {
			Point2D_I16 v = validator.getCandidatesHigh().get(i);
			Point2D_I16 a = alg.getCornersHigh().get(i);

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
