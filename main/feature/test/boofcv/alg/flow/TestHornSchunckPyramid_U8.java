/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.transform.pyramid.PyramidOps;
import boofcv.core.image.border.BorderType;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt16;
import boofcv.struct.image.ImageUInt8;
import boofcv.struct.pyramid.PyramidFloat;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestHornSchunckPyramid_U8 {

	int width = 10;
	int height = 13;
	Random rand = new Random(234);

	@Test
	public void process() {
		int width = 30;
		int height = 40;

		ImageUInt8 original1 = new ImageUInt8(width,height);
		ImageUInt8 original2 = new ImageUInt8(width,height);

		ImageMiscOps.fillRectangle(original1,40,10,0,10,height);
		ImageMiscOps.fillRectangle(original2,40,15,0,10,height);

		PyramidFloat<ImageUInt8> pyr1 = UtilDenseOpticalFlow.standardPyramid(width,height,0.7,0,5,12,ImageUInt8.class);
		PyramidFloat<ImageUInt8> pyr2 = UtilDenseOpticalFlow.standardPyramid(width,height,0.7,0,5,12,ImageUInt8.class);

		pyr1.process(original1);
		pyr2.process(original2);

		ImageGradient<ImageUInt8, ImageSInt16> gradient = FactoryDerivative.two(ImageUInt8.class, ImageSInt16.class);

		ImageSInt16 derivX[] = PyramidOps.declareOutput(pyr2,ImageSInt16.class);
		ImageSInt16 derivY[] = PyramidOps.declareOutput(pyr2,ImageSInt16.class);


		PyramidOps.gradient(pyr2,gradient,derivX,derivY);

		InterpolatePixelS<ImageFloat32> interpolate = FactoryInterpolation.bilinearPixelS(ImageFloat32.class);
		HornSchunckPyramid_U8 alg = new HornSchunckPyramid_U8(20,1.9f,100,1e-8f,interpolate);
		alg.process(pyr1,pyr2,derivX,derivY);

		alg.getFlowX().print();

		for( int y = 0; y < height; y++ ) {
			for( int x = 0; x < width; x++ ) {
				assertEquals(5,alg.getFlowX().get(x,y),0.25f);
				assertEquals(0,alg.getFlowY().get(x,y),0.25f);
			}
		}

	}

	@Test
	public void interpolateFlow() {
		InterpolatePixelS<ImageFloat32> interpolate = FactoryInterpolation.bilinearPixelS(ImageFloat32.class);
		HornSchunckPyramid_U8 alg = new HornSchunckPyramid_U8(0.01f,1.9f,50,1e-8f,interpolate);

		ImageFloat32 input = new ImageFloat32(5,7);
		ImageFloat32 output = new ImageFloat32(8,14);

		ImageMiscOps.fillUniform(input,rand,0,10);

		alg.interpolateFlow(input,output);

		// there should be no zero values.  This is a very crude test
		for( int i = 0; i < output.data.length; i++ ) {
			assertTrue( output.data[i] != 0 );
		}
	}

	@Test
	public void processLayer() {
		ImageUInt8 image1 = new ImageUInt8(width,height);
		ImageUInt8 image2 = new ImageUInt8(width,height);
		ImageSInt16 deriv2X = new ImageSInt16(width,height);
		ImageSInt16 deriv2Y = new ImageSInt16(width,height);

		ImageMiscOps.fillRectangle(image1,40,4,0,3,height);
		ImageMiscOps.fillRectangle(image2,40,5,0,3,height);

		GImageDerivativeOps.two(image2, deriv2X, deriv2Y, BorderType.EXTENDED);

		// have the smoothness constraint be weak
		HornSchunckPyramid_U8 alg = new HornSchunckPyramid_U8(0.01f,1.9f,50,1e-8f,null);

		alg.initFlowX.reshape(width,height);
		alg.initFlowY.reshape(width,height);
		alg.flowX.reshape(width,height);
		alg.flowY.reshape(width,height);

		alg.processLayer(image1,image2,deriv2X,deriv2Y);

		for( int y = 0; y < height; y++ ) {
			for( int x = 0; x < width; x++ ) {
				assertEquals(1,alg.getFlowX().get(x,y),0.25f);
				assertEquals(0,alg.getFlowY().get(x,y),0.25f);
			}
		}
	}

	@Test
	public void A() {
		ImageFloat32 flow = new ImageFloat32(width,height);

		ImageMiscOps.fillUniform(flow,rand,0,10);

		int x = 5,y=6;

		float left = flow.get(x-1,y) + flow.get(x+1,y) + flow.get(x,y-1) + flow.get(x,y+1);
		float right = flow.get(x-1,y-1) + flow.get(x+1,y-1) + flow.get(x-1,y+1) + flow.get(x+1,y+1);

		assertEquals((1.0f/6.0f)*left + (1.0f/12.0f)*right,HornSchunckPyramid_U8.A(x,y,flow),1e-4);
	}

	@Test
	public void safe() {
		ImageFloat32 flow = new ImageFloat32(width,height);

		ImageMiscOps.fillUniform(flow,rand,0,10);

		assertEquals(flow.get(0,0),HornSchunckPyramid_U8.safe(-1,0,flow),1e-4f);
		assertEquals(flow.get(0,0),HornSchunckPyramid_U8.safe(0,-1,flow),1e-4f);
		assertEquals(flow.get(5,0),HornSchunckPyramid_U8.safe(5,-1,flow),1e-4f);
		assertEquals(flow.get(5,height-1),HornSchunckPyramid_U8.safe(5,height,flow),1e-4f);
		assertEquals(flow.get(0,5),HornSchunckPyramid_U8.safe(-1,5,flow),1e-4f);
		assertEquals(flow.get(width-1,5),HornSchunckPyramid_U8.safe(width,5,flow),1e-4f);
	}

}
