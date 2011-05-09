/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.detect.corner;

import gecv.alg.filter.derivative.GradientSobel;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageSInt16;
import gecv.struct.image.ImageUInt8;

import static org.junit.Assert.assertTrue;

/**
 * Tests basic properties of a corner detector
 *
 * @author Peter Abeles
 */
public abstract class GenericCornerIntensityTests {

	protected int width = 20;
	protected int height = 21;

	protected ImageUInt8 imageI = new ImageUInt8(width,height);
	protected ImageFloat32 imageF = new ImageFloat32(width,height);

	protected ImageSInt16 derivX_I16 = new ImageSInt16(width,height);
	protected ImageSInt16 derivY_I16 = new ImageSInt16(width,height);

	protected ImageFloat32 derivX_F32 = new ImageFloat32(width,height);
	protected ImageFloat32 derivY_F32 = new ImageFloat32(width,height);

	public abstract ImageFloat32 computeIntensity();

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

	private float getResponse() {
		GradientSobel.process(imageF,derivX_F32,derivY_F32);
		GradientSobel.process(imageI,derivX_I16,derivY_I16);

		ImageFloat32 a = computeIntensity();

		return a.get(width/2,height/2);
	}

	private float getResponseAway() {
		GradientSobel.process(imageF,derivX_F32,derivY_F32);
		GradientSobel.process(imageI,derivX_I16,derivY_I16);

		ImageFloat32 a = computeIntensity();

		return a.get(2,2);
	}

	public ImageUInt8 createUniformI8() {
		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				imageI.set(j,i,10);
			}
		}

		return imageI;
	}

	public ImageFloat32 createUniformF32() {
		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				imageF.set(j,i,10);
			}
		}

		return imageF;
	}

	public ImageUInt8 createCornerI8() {

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

	public ImageFloat32 createCornerF32() {
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
