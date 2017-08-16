/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.sfm.d3.direct;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.Planar;
import georegression.geometry.ConvertRotation3D_F32;
import georegression.misc.GrlConstants;
import georegression.struct.se.Se3_F32;
import georegression.struct.so.Rodrigues_F32;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestVisOdomDirectColorDepth {

	Random rand = new Random(234);

	Class<GrayF32> imageType = GrayF32.class;

	int width = 320;
	int height = 240;
	int numBands = 2;
	float fx = 120;
	float fy = 100;
	float cx = width/2;
	float cy = height/2;

	/**
	 * Generate low level synthetic data that should simulate a translation along one axis.  Then check to see if
	 * has the expected behavior at a high level
	 */
	@Test
	public void singleStepArtificialTranslation() {
		// it wants to declares the color of each pixel, the gradient says it increases to the right
		// so it will move in the negative x direction
		Se3_F32 a = computeMotion(10,20,6,0);
		assertTrue(a.T.x < -0.02);
		assertEquals( 0 , a.T.y , 1e-2f);
		assertEquals( 0 , a.T.z , 1e-2f);
		assertTrue(rotationMag(a) < GrlConstants.TEST_SQ_F64);

		// reverse the direction
		Se3_F32 b = computeMotion(20,10,6,0);
		assertTrue(b.T.x > 0.02);
		assertEquals( 0 , b.T.y , 1e-2f);
		assertEquals( 0 , b.T.z , 1e-2f);
		assertTrue(rotationMag(b) < GrlConstants.TEST_SQ_F64);

		assertEquals( a.T.x , -b.T.x , 1e-4f);

		// make it move along the y-axis
		Se3_F32 c = computeMotion(10,20,0,6);
		assertEquals( 0 , c.T.x , 1e-2f);
		assertTrue(c.T.y < -0.02);
		assertEquals( 0 , c.T.z , 1e-2f);
		assertTrue(rotationMag(c) < GrlConstants.TEST_SQ_F64);
		assertEquals( a.T.x , c.T.y , 0.01);

		// increase the magnitude of the motion by making the gradient smaller
		Se3_F32 d = computeMotion(10,20,3,0);
		assertTrue( 1.5f*Math.abs(a.T.x) < Math.abs(d.T.x) );
	}

	public Se3_F32 computeMotion( float colorBefore , float colorAfter , float dx , float dy ) {
		VisOdomDirectColorDepth<GrayF32,GrayF32> alg = new VisOdomDirectColorDepth<>(numBands,imageType,imageType);
		alg.setCameraParameters(fx,fy,cx,cy,width,height);

		Planar<GrayF32> input = new Planar<>(GrayF32.class,width,height,numBands);
		GImageMiscOps.fill(input,colorAfter);
		alg.initMotion(input);
		GImageMiscOps.fill(alg.derivX,dx);
		GImageMiscOps.fill(alg.derivY,dy);
		// need to add noise to avoid pathological stuff
		GImageMiscOps.addUniform(alg.derivX, rand, 0f,0.1f);
		GImageMiscOps.addUniform(alg.derivY, rand, 0f,0.1f);

		// generate some synthetic data.  This will be composed of random points in front of the camera
		for (int i = 0; i < 100; i++) {
			VisOdomDirectColorDepth.Pixel p = alg.keypixels.grow();

			for (int band = 0; band < numBands; band++) {
				p.bands[band] = colorBefore;
			}
			p.x = rand.nextInt(width);
			p.y = rand.nextInt(height);

			float nx = (p.x-cx)/fx;
			float ny = (p.y-cy)/fy;
			// z needs to fixed value for it to generate a purely translational motion given fixed  gradient and
			// and fixed delta in color
			float z = 2;
			p.p3.x = nx*z;
			p.p3.y = ny*z;
			p.p3.z = z;
		}

		// estimate the motion
		alg.constructLinearSystem(input, new Se3_F32());
		assertTrue(alg.solveSystem());

		assertEquals(Math.abs(colorAfter-colorBefore), alg.getErrorOptical(), 1e-4f);
		assertTrue(alg.getInboundsPixels() > 95 ); // counting error can cause a drop

		return alg.motionTwist;
	}

	public float rotationMag(Se3_F32 motion ) {
		Rodrigues_F32 rod = ConvertRotation3D_F32.matrixToRodrigues(motion.R,null);
		return rod.theta;
	}
}
