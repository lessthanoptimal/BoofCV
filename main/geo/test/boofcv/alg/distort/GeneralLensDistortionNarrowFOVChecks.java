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

package boofcv.alg.distort;

import boofcv.struct.distort.Point2Transform2_F32;
import boofcv.struct.distort.Point2Transform2_F64;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Common checks for LensDistortionPinhole
 *
 * @author Peter Abeles
 */
public abstract class GeneralLensDistortionNarrowFOVChecks {

	protected float tol_F32 = 1e-3f;
	protected double tol_F64 = 1e-5;

	public abstract LensDistortionNarrowFOV create();

	@Test
	public void forwardsBackwards_F32() {
		LensDistortionNarrowFOV alg = create();

		for (int i = 0; i < 4; i++) {
			boolean inputPixel = i%2 == 0;
			boolean outputPixel = i/2 == 0;

			Point2Transform2_F32 distort = alg.distort_F32(inputPixel,outputPixel);
			Point2Transform2_F32 undistort = alg.undistort_F32(outputPixel,inputPixel);

			float inputX,inputY,scale;
			if( inputPixel ) {
				inputX = 21.3f;
				inputY = 45.1f;
				scale = 10.0f;
			} else {
				inputX=0.05f;
				inputY=-0.1f;
				scale = 0.1f;
			}

			Point2D_F32 middle = new Point2D_F32();
			Point2D_F32 found = new Point2D_F32();

			distort.compute(inputX,inputY,middle);
			undistort.compute(middle.x,middle.y,found);

			assertEquals(inputX, found.x, scale * tol_F32);
			assertEquals(inputY, found.y, scale * tol_F32);
		}
	}

	@Test
	public void forwardsBackwards_F64() {
		LensDistortionNarrowFOV alg = create();

		for (int i = 0; i < 4; i++) {
			boolean inputPixel = i % 2 == 0;
			boolean outputPixel = i / 2 == 0;

			Point2Transform2_F64 distort = alg.distort_F64(inputPixel, outputPixel);
			Point2Transform2_F64 undistort = alg.undistort_F64(outputPixel, inputPixel);

			double inputX, inputY, scale;
			if (inputPixel) {
				inputX = 21.3;
				inputY = 45.1;
				scale = 10.0;
			} else {
				inputX = 0.05;
				inputY = -0.1;
				scale = 0.1;
			}

			Point2D_F64 middle = new Point2D_F64();
			Point2D_F64 found = new Point2D_F64();

			distort.compute(inputX, inputY, middle);
			undistort.compute(middle.x, middle.y, found);

			assertEquals(inputX, found.x, scale * tol_F64);
			assertEquals(inputY, found.y, scale * tol_F64);
		}
	}
}
