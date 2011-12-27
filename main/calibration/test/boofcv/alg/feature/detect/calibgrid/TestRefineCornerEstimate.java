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
import boofcv.alg.misc.ImageTestingOps;
import boofcv.struct.image.ImageSInt16;
import boofcv.struct.image.ImageUInt8;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestRefineCornerEstimate {

	int width = 50;
	int height = 60;

	@Test
	public void stuff() {
		ImageUInt8 orig = new ImageUInt8(width,height);
		ImageSInt16 derivX = new ImageSInt16(width,height);
		ImageSInt16 derivY = new ImageSInt16(width,height);

		ImageTestingOps.fillRectangle(orig, 100, 10, 10, 40, 50);
		ImageTestingOps.addGaussian(orig,new Random(),5,0,255);

		GradientSobel.process(orig,derivX,derivY,null);
				
		RefineCornerEstimate<ImageSInt16> alg = new RefineCornerEstimate<ImageSInt16>();
		alg.setInputs(derivX,derivY);

		assertTrue(alg.process(0, 0, 20, 20));
		System.out.println("found = "+alg.getX()+"  "+alg.getY());
		assertEquals(10, alg.getX(), 1e-8);
		assertEquals(10,alg.getY(),1e-8);

		assertTrue(alg.process(width-15,height-15,width,height));
		assertEquals(40,alg.getX(),1e-8);
		assertEquals(50,alg.getY(),1e-8);
	}
}
