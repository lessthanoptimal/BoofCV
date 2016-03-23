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

package boofcv.alg.flow;

import boofcv.alg.filter.derivative.DerivativeType;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.border.BorderType;
import boofcv.factory.flow.ConfigHornSchunckPyramid;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.GrayF32;
import boofcv.struct.pyramid.PyramidFloat;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestHornSchunckPyramid {

	int width = 10;
	int height = 13;
	Random rand = new Random(234);

	@Test
	public void process() {
		int width = 30;
		int height = 40;

		GrayF32 original1 = new GrayF32(width,height);
		GrayF32 original2 = new GrayF32(width,height);

		ImageMiscOps.fillRectangle(original1, 40, 10, 0, 10, height);
		ImageMiscOps.fillRectangle(original2, 40, 15, 0, 10, height);

		PyramidFloat<GrayF32> pyr1 = UtilDenseOpticalFlow.standardPyramid(width,height,0.7,0,5,12,GrayF32.class);
		PyramidFloat<GrayF32> pyr2 = UtilDenseOpticalFlow.standardPyramid(width,height,0.7,0,5,12,GrayF32.class);

		pyr1.process(original1);
		pyr2.process(original2);

		InterpolatePixelS<GrayF32> interpolate = FactoryInterpolation.bilinearPixelS(
				GrayF32.class, BorderType.EXTENDED);
		HornSchunckPyramid alg = new HornSchunckPyramid(new ConfigHornSchunckPyramid(20f,100),interpolate);
		alg.process(pyr1,pyr2);

		for( int y = 0; y < height; y++ ) {
			for( int x = 0; x < width; x++ ) {
				assertEquals(5,alg.getFlowX().get(x,y),0.25f);
				assertEquals(0,alg.getFlowY().get(x,y),0.25f);
			}
		}

	}

	@Test
	public void processLayer() {
		GrayF32 image1 = new GrayF32(width,height);
		GrayF32 image2 = new GrayF32(width,height);
		GrayF32 deriv2X = new GrayF32(width,height);
		GrayF32 deriv2Y = new GrayF32(width,height);

		ImageMiscOps.fillRectangle(image1,40,4,0,3,height);
		ImageMiscOps.fillRectangle(image2,40,5,0,3,height);

		GImageDerivativeOps.gradient(DerivativeType.TWO_0,image2, deriv2X, deriv2Y, BorderType.EXTENDED);

		// have the smoothness constraint be weak
		InterpolatePixelS<GrayF32> interpolate = FactoryInterpolation.bilinearPixelS(
				GrayF32.class, BorderType.EXTENDED);
		HornSchunckPyramid alg = new HornSchunckPyramid(new ConfigHornSchunckPyramid(100,200),interpolate);

		alg.initFlowX.reshape(width,height);
		alg.initFlowY.reshape(width,height);
		alg.flowX.reshape(width,height);
		alg.flowY.reshape(width,height);
		alg.warpDeriv2X.reshape(width,height);
		alg.warpDeriv2Y.reshape(width,height);
		alg.warpImage2.reshape(width,height);

		alg.processLayer(image1,image2,deriv2X,deriv2Y);

//		alg.getFlowX().print("%4f");

		for( int y = 0; y < height; y++ ) {
			for( int x = 0; x < width; x++ ) {
				assertEquals(x+" "+y,1,alg.getFlowX().get(x,y),0.1f);
				assertEquals(0,alg.getFlowY().get(x,y),0.1f);
			}
		}
	}

	@Test
	public void A() {
		GrayF32 flow = new GrayF32(width,height);

		ImageMiscOps.fillUniform(flow,rand,0,10);

		int x = 5,y=6;

		float left = flow.get(x-1,y) + flow.get(x+1,y) + flow.get(x,y-1) + flow.get(x,y+1);
		float right = flow.get(x-1,y-1) + flow.get(x+1,y-1) + flow.get(x-1,y+1) + flow.get(x+1,y+1);

		assertEquals((1.0f/6.0f)*left + (1.0f/12.0f)*right,HornSchunckPyramid.A(x,y,flow),1e-4);
	}

	@Test
	public void safe() {
		GrayF32 flow = new GrayF32(width,height);

		ImageMiscOps.fillUniform(flow,rand,0,10);

		assertEquals(flow.get(0,0),HornSchunckPyramid.safe(-1,0,flow),1e-4f);
		assertEquals(flow.get(0,0),HornSchunckPyramid.safe(0,-1,flow),1e-4f);
		assertEquals(flow.get(5,0),HornSchunckPyramid.safe(5,-1,flow),1e-4f);
		assertEquals(flow.get(5,height-1),HornSchunckPyramid.safe(5,height,flow),1e-4f);
		assertEquals(flow.get(0,5),HornSchunckPyramid.safe(-1,5,flow),1e-4f);
		assertEquals(flow.get(width-1,5),HornSchunckPyramid.safe(width,5,flow),1e-4f);
	}

}
