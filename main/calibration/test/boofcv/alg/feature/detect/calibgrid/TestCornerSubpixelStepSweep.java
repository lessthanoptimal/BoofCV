/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.feature.detect.calibgrid;

import boofcv.alg.filter.derivative.GradientSobel;
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.alg.misc.ImageTestingOps;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.ImageSInt16;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.point.Point2D_F64;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestCornerSubpixelStepSweep {

	int width = 6;
	int height = 7;

	@Test
	public void stuff() {
		int cornerX = 3;
		int cornerY = 3;
		ImageUInt8 orig = new ImageUInt8(width,height);
		ImageTestingOps.fillRectangle(orig, 100, 0, 0, width, height);
		ImageTestingOps.fillRectangle(orig, 0, cornerX, cornerY, width, height);

		InterpolatePixel<ImageUInt8> interp = FactoryInterpolation.bilinearPixel(orig);
				
		CornerSubpixelStepSweep<ImageUInt8> alg = new CornerSubpixelStepSweep<ImageUInt8>();

		alg.process(interp);

		Point2D_F64 pt = alg.getCornerPoint();

//		System.out.println("found = "+alg.getX()+"  "+alg.getY());
		assertEquals(10, pt.getX(), 1e-8);
		assertEquals(10, pt.getY(),1e-8);
	}
}
