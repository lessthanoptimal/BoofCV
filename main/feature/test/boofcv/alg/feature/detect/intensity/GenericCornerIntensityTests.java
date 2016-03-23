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

package boofcv.alg.feature.detect.intensity;

import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;

import static org.junit.Assert.assertTrue;

/**
 * Tests basic properties of a corner detector
 *
 * @author Peter Abeles
 */
public abstract class GenericCornerIntensityTests {

	protected int width = 20;
	protected int height = 21;

	protected GrayU8 imageI = new GrayU8(width,height);
	protected GrayF32 imageF = new GrayF32(width,height);
	protected GrayF32 intensity = new GrayF32(width,height);


	public abstract void computeIntensity( GrayF32 intensity );

	public void performAllTests() {
		testLargerCorner();
	}

	/**
	 * Checks to see if an image with a corner has a larger response than a flat image.
	 */
	public void testLargerCorner() {
		createUniformI8();
		createUniformF32();
		float flatResponse = getResponse();

		createCornerI8();
		createCornerF32();
		float cornerResponse = getResponse();

		// a corner should cause a positive response
		assertTrue(cornerResponse>flatResponse);

		// a point far away from the corner should have a small response
		float awayResponse = getResponseAway();

		assertTrue(cornerResponse>awayResponse);
	}

	/**
	 * Computes image derivatives or other derived components from the input image needed for
	 * calculating corners.
	 */
	protected abstract void computeDerivatives();

	private float getResponse() {
		computeDerivatives();

		computeIntensity(intensity);

		// return the sum of the region around the corner
		// so that the intensity doesn't have to be right on the corner.
		float sum = 0;
		for( int i = -1; i <= 1; i++ ) {
			for( int j = -1; j <= 1; j++ ) {
				sum += intensity.get(width/2+j,height/2+j);
			}
		}
		return sum;
	}

	private float getResponseAway() {
		computeDerivatives();

		computeIntensity(intensity);

		return intensity.get(2,2);
	}

	public GrayU8 createUniformI8() {
		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				imageI.set(j,i,10);
			}
		}

		return imageI;
	}

	public GrayF32 createUniformF32() {
		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				imageF.set(j,i,10);
			}
		}

		return imageF;
	}

	public GrayU8 createCornerI8() {

		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				if( j > width/2 && i > height/2)
					imageI.set(j,i,30);
				else
					imageI.set(j,i,0);
			}
		}

		return imageI;
	}

	public GrayF32 createCornerF32() {
		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				if( j > width/2 && i > height/2)
					imageF.set(j,i,30);
				else
					imageF.set(j,i,0);
			}
		}

		return imageF;
	}


}
