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

package boofcv.alg.tracker.meanshift;

import boofcv.alg.color.ColorHsv;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;
import georegression.struct.shapes.RectangleLength2D_I32;
import org.junit.Test;

import static boofcv.alg.tracker.meanshift.TestLikelihoodHistCoupled_PL_U8.setColor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestLikelihoodHueSatHistCoupled_PL_U8 {

	@Test
	public void numBins() {
		LikelihoodHueSatHistCoupled_PL_U8 alg = new LikelihoodHueSatHistCoupled_PL_U8(255,30);

		Planar<GrayU8> image = new Planar<>(GrayU8.class,30,40,3);

		// make sure the upper limit is handled correctly
		setColor(image,5,6,255,255,255);
		alg.setImage(image);
		alg.createModel(new RectangleLength2D_I32(5,6,1,1));

		assertEquals(30 , alg.numHistogramBins);
		assertEquals(30 * 30 , alg.bins.length);

		// it comes out to a slightly larger size on purpose
		assertEquals(2*Math.PI,alg.sizeH*30,0.01);
		assertEquals(1.0,alg.sizeS*30,0.01);
	}

	@Test
	public void convertToHueSat() {
		LikelihoodHueSatHistCoupled_PL_U8 alg = new LikelihoodHueSatHistCoupled_PL_U8(255,30);

		Planar<GrayU8> image = new Planar<>(GrayU8.class,30,40,3);
		setColor(image,5,6,120,50,255);
		alg.setImage(image);
		alg.createModel(new RectangleLength2D_I32(5,6,1,1));

		float hsv[] = new float[3];
		ColorHsv.rgbToHsv(120,50,255,hsv);

		int indexH = (int)(hsv[0]/alg.sizeH);
		int indexS = (int)(hsv[1]/alg.sizeS);

		int index = indexH*30 + indexS;
		assertEquals(1.0,alg.bins[index],1e-4);
	}

	@Test
	public void singleColor() {
		LikelihoodHueSatHistCoupled_PL_U8 alg = new LikelihoodHueSatHistCoupled_PL_U8(255,5);

		Planar<GrayU8> image = new Planar<>(GrayU8.class,30,40,3);

		RectangleLength2D_I32 r = new RectangleLength2D_I32(3,4,12,8);
		setColor(image,r,100,105,12);

		alg.setImage(image);
		alg.createModel(r);

		assertEquals(1.0f,alg.compute(3, 4),1e-4);
		assertEquals(1.0f,alg.compute(14, 11),1e-4);
		assertEquals(0,alg.compute(10, 30),1e-4);
	}

	@Test
	public void multipleColors() {
		LikelihoodHueSatHistCoupled_PL_U8 alg = new LikelihoodHueSatHistCoupled_PL_U8(255,5);

		Planar<GrayU8> image = new Planar<>(GrayU8.class,30,40,3);

		RectangleLength2D_I32 r0 = new RectangleLength2D_I32(3,4,8,8);
		RectangleLength2D_I32 r1 = new RectangleLength2D_I32(11,4,4,8);
		setColor(image,r0,100,105,12);
		setColor(image,r1,50,200,50);


		RectangleLength2D_I32 region = new RectangleLength2D_I32(3,4,12,8);
		alg.setImage(image);
		alg.createModel(region);


		float v0 = alg.compute(3, 4);
		float v1 = alg.compute(11, 4);

		assertEquals(1.0f,v0+v1,1e-4);
		assertTrue(v0>v1);
	}

}
