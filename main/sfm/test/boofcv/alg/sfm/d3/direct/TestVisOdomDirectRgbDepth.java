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

package boofcv.alg.sfm.d3.direct;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.se.Se3_F32;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestVisOdomDirectRgbDepth {

	Random rand = new Random(234);

	ImageType<GrayF32> imageType = ImageType.single(GrayF32.class);

	int width = 320;
	int height = 240;
	float fx = 120;
	float fy = 100;
	float cx = width/2;
	float cy = height/2;

	/**
	 * Generate synthetic data that should simulate a translation along one axis.
	 */
	@Test
	public void injectedMotion() {
		// the color before and after + the image gradient needs to be carefully chosen
		float colorBefore = 20;
		float colorAfter = 10;

		VisOdomDirectRgbDepth<GrayF32,GrayF32> alg = new VisOdomDirectRgbDepth<>(imageType,imageType);
		alg.setCameraParameters(fx,fy,cx,cy,width,height);

		GrayF32 input = new GrayF32(width,height);
		ImageMiscOps.fill(input,colorAfter);
		alg.initMotion(input);
		ImageMiscOps.fill(alg.derivX,100);
		ImageMiscOps.fill(alg.derivY,1f);

		// generate some synthetic data.  This will be composed of random points in front of the camera
		for (int i = 0; i < 100; i++) {
			VisOdomDirectRgbDepth.Pixel p = alg.keypixels.grow();

			p.bands[0] = colorBefore;
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
		alg.constructLinearSystem(width,height, new Se3_F32());
		assertTrue(alg.solveSystem());

		alg.motionTwist.print();

		double[] euler = ConvertRotation3D_F64.matrixToEuler(alg.motionTwist.getR(), EulerType.XYZ, null);
		System.out.println(euler[0]+"  "+euler[1]+"  "+euler[2]);

		fail("Finish");
	}
}
