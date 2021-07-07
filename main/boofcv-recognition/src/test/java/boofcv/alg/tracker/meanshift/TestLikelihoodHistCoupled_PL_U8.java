/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.shapes.RectangleLength2D_I32;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestLikelihoodHistCoupled_PL_U8 extends BoofStandardJUnit {

	@Test void numBins() {
		LikelihoodHistCoupled_PL_U8 alg = new LikelihoodHistCoupled_PL_U8(255,30);

		Planar<GrayU8> image = new Planar<>(GrayU8.class,30,40,3);

		// make sure the upper limit is handled correctly
		setColor(image,5,6,255,255,255);
		alg.setImage(image);
		alg.createModel(new RectangleLength2D_I32(5,6,1,1));

		assertEquals(30 * 30 * 30, alg.hist.length);
		assertEquals(1f,alg.hist[alg.hist.length-1],1e-4);
	}


	@Test void singleColor() {
		LikelihoodHistCoupled_PL_U8 alg = new LikelihoodHistCoupled_PL_U8(255,11);

		Planar<GrayU8> image = new Planar<>(GrayU8.class,30,40,3);

		RectangleLength2D_I32 r = new RectangleLength2D_I32(3,4,12,8);
		setColor(image,r,100,105,12);

		alg.setImage(image);
		alg.createModel(r);

		assertEquals(1.0f,alg.compute(3,4),1e-4);
		assertEquals(1.0f,alg.compute(14,11),1e-4);
		assertEquals(0,alg.compute(10,30),1e-4);

	}

	@Test void multipleColors() {
		LikelihoodHistCoupled_PL_U8 alg = new LikelihoodHistCoupled_PL_U8(255,11);

		Planar<GrayU8> image = new Planar<>(GrayU8.class,30,40,3);

		RectangleLength2D_I32 r0 = new RectangleLength2D_I32(3,4,8,8);
		RectangleLength2D_I32 r1 = new RectangleLength2D_I32(11,4,4,8);
		setColor(image,r0,100,105,12);
		setColor(image,r1,50,200,50);


		RectangleLength2D_I32 region = new RectangleLength2D_I32(3,4,12,8);
		alg.setImage(image);
		alg.createModel(region);


		float v0 = alg.compute(3,4);
		float v1 = alg.compute(11,4);

		assertEquals(1.0f,v0+v1,1e-4);
		assertTrue(v0>v1);
	}

	public static void setColor(Planar<GrayU8> image , RectangleLength2D_I32 rect , int r , int g , int b ) {

		for( int y = 0; y < rect.height; y++ ) {
			for( int x = 0; x < rect.width; x++ ) {
				setColor(image,x+rect.x0,y+rect.y0,r,g,b);
			}
		}

	}

	public static void setColor(Planar<GrayU8> image , int x , int y , int r , int g , int b ) {
		image.getBand(0).set(x,y,r);
		image.getBand(1).set(x,y,g);
		image.getBand(2).set(x,y,b);
	}
}
