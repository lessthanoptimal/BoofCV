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
import boofcv.struct.pyramid.PyramidFloat;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestHornSchunckPyramid {

	int width = 10;
	int height = 13;
	Random rand = new Random(234);


	@Test
	public void horn_schunck_optical_flow() {
		Random rand = new Random(234);

		int width = 10;
		int height = 12;

		ImageFloat32 I1 = new ImageFloat32(10,12);
		ImageFloat32 I2 = new ImageFloat32(10,12);

		for( int i = 0; i < I1.height; i++ ) {
			for( int j = 0; j < I1.width; j++ ) {
				I1.set(j,i,rand.nextFloat()*255);
				I2.set(j,i,rand.nextFloat()*255);
			}
		}

		ImageFloat32 dx = new ImageFloat32(10,12);
		ImageFloat32 dy = new ImageFloat32(10,12);

		gradient(I2.data,dx.data,dy.data,I2.width,I2.height);
//		ImageGradient<ImageFloat32, ImageFloat32> gradient = FactoryDerivative.two(ImageFloat32.class, ImageFloat32.class);
//		gradient.process(I2,dx,dy);

		InterpolatePixelS<ImageFloat32> interpolate = FactoryInterpolation.bilinearPixelS(ImageFloat32.class);
		HornSchunckPyramid alg = new HornSchunckPyramid(7,1.9f,5,10,1e-8f,0.7, 0.8,10,interpolate);
		alg.initFlowX.reshape(width,height);
		alg.initFlowY.reshape(width,height);
		alg.flowX.reshape(width,height);
		alg.flowY.reshape(width,height);
		alg.warpDeriv2X.reshape(width,height);
		alg.warpDeriv2Y.reshape(width,height);
		alg.warpImage2.reshape(width,height);
		alg.processLayer(I1,I2,dx,dy);

		alg.flowX.print("%4.2f");
		alg.flowY.print("%4.2f");
	}

	public void gradient(
			final float []input, // input image
			float []dx,          // computed x derivative
			float []dy,          // computed y derivative
			final int nx,       // image width
			final int ny        // image height
	)
	{
		// compute gradient in the central body of the image
		for(int i = 1; i < ny-1; i++)
		{
			for(int j = 1; j < nx-1; j++)
			{
				final int k = i * nx + j;
				dx[k] = 0.5f*(input[k+1] - input[k-1]);
				dy[k] = 0.5f*(input[k+nx] - input[k-nx]);
			}
		}

		// compute gradient in the first and last rows
		for(int j = 1; j < nx-1; j++)
		{
			dx[j] = 0.5f*(input[j+1] - input[j-1]);
			dy[j] = 0.5f*(input[j+nx] - input[j]);

			final int k = (ny - 1) * nx + j;

			dx[k] = 0.5f*(input[k+1] - input[k-1]);
			dy[k] = 0.5f*(input[k] - input[k-nx]);
		}

		// compute gradient in the first and last columns
		for(int i = 1; i < ny-1; i++)
		{
			final int p = i * nx;
			dx[p] = 0.5f*(input[p+1] - input[p]);
			dy[p] = 0.5f*(input[p+nx] - input[p-nx]);

			final int k = (i+1) * nx - 1;

			dx[k] = 0.5f*(input[k] - input[k-1]);
			dy[k] = 0.5f*(input[k+nx] - input[k-nx]);
		}

		// compute the gradient in the corners
		dx[0] = 0.5f*(input[1] - input[0]);
		dy[0] = 0.5f*(input[nx] - input[0]);

		dx[nx-1] = 0.5f*(input[nx-1] - input[nx-2]);
		dy[nx-1] = 0.5f*(input[2*nx-1] - input[nx-1]);

		dx[(ny-1)*nx] = 0.5f*(input[(ny-1)*nx + 1] - input[(ny-1)*nx]);
		dy[(ny-1)*nx] = 0.5f*(input[(ny-1)*nx] - input[(ny-2)*nx]);

		dx[ny*nx-1] = 0.5f*(input[ny*nx-1] - input[ny*nx-1-1]);
		dy[ny*nx-1] = 0.5f*(input[ny*nx-1] - input[(ny-1)*nx-1]);

	}

	@Test
	public void process() {
		int width = 30;
		int height = 40;

		ImageFloat32 original1 = new ImageFloat32(width,height);
		ImageFloat32 original2 = new ImageFloat32(width,height);

		ImageMiscOps.fillRectangle(original1,40,10,0,10,height);
		ImageMiscOps.fillRectangle(original2,40,15,0,10,height);

		PyramidFloat<ImageFloat32> pyr1 = UtilDenseOpticalFlow.standardPyramid(width,height,0.7,0,5,12,ImageFloat32.class);
		PyramidFloat<ImageFloat32> pyr2 = UtilDenseOpticalFlow.standardPyramid(width,height,0.7,0,5,12,ImageFloat32.class);

		pyr1.process(original1);
		pyr2.process(original2);

		ImageGradient<ImageFloat32, ImageFloat32> gradient = FactoryDerivative.two(ImageFloat32.class, ImageFloat32.class);

		ImageFloat32 derivX[] = PyramidOps.declareOutput(pyr2,ImageFloat32.class);
		ImageFloat32 derivY[] = PyramidOps.declareOutput(pyr2,ImageFloat32.class);


		PyramidOps.gradient(pyr2,gradient,derivX,derivY);

		InterpolatePixelS<ImageFloat32> interpolate = FactoryInterpolation.bilinearPixelS(ImageFloat32.class);
		HornSchunckPyramid alg = new HornSchunckPyramid(20,1.9f, 1, 100,1e-8f,0.7, 0.8,10,interpolate);
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
		HornSchunckPyramid alg = new HornSchunckPyramid(0.01f,1.9f, 1, 50,1e-8f,0.7, 0.8,10,interpolate);

		ImageFloat32 input = new ImageFloat32(5,7);
		ImageFloat32 output = new ImageFloat32(8,14);

		ImageMiscOps.fillUniform(input,rand,0,10);

		alg.interpolateFlowScale(input, output);

		// there should be no zero values.  This is a very crude test
		for( int i = 0; i < output.data.length; i++ ) {
			assertTrue( output.data[i] != 0 );
		}
	}

	@Test
	public void processLayer() {
		ImageFloat32 image1 = new ImageFloat32(width,height);
		ImageFloat32 image2 = new ImageFloat32(width,height);
		ImageFloat32 deriv2X = new ImageFloat32(width,height);
		ImageFloat32 deriv2Y = new ImageFloat32(width,height);

		ImageMiscOps.fillRectangle(image1,40,4,0,3,height);
		ImageMiscOps.fillRectangle(image2,40,5,0,3,height);

		GImageDerivativeOps.two(image2, deriv2X, deriv2Y, BorderType.EXTENDED);

		// have the smoothness constraint be weak
		InterpolatePixelS<ImageFloat32> interpolate = FactoryInterpolation.bilinearPixelS(ImageFloat32.class);
		HornSchunckPyramid alg = new HornSchunckPyramid(0.01f,1.9f, 1, 50,1e-8f,0.7, 0.8,10,interpolate);

		alg.initFlowX.reshape(width,height);
		alg.initFlowY.reshape(width,height);
		alg.flowX.reshape(width,height);
		alg.flowY.reshape(width,height);
		alg.warpDeriv2X.reshape(width,height);
		alg.warpDeriv2Y.reshape(width,height);
		alg.warpImage2.reshape(width,height);

		alg.processLayer(image1,image2,deriv2X,deriv2Y);

		for( int y = 0; y < height; y++ ) {
			for( int x = 0; x < width; x++ ) {
				assertEquals(1,alg.getFlowX().get(x,y),0.05f);
				assertEquals(0,alg.getFlowY().get(x,y),0.05f);
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

		assertEquals((1.0f/6.0f)*left + (1.0f/12.0f)*right,HornSchunckPyramid.A(x,y,flow),1e-4);
	}

	@Test
	public void safe() {
		ImageFloat32 flow = new ImageFloat32(width,height);

		ImageMiscOps.fillUniform(flow,rand,0,10);

		assertEquals(flow.get(0,0),HornSchunckPyramid.safe(-1,0,flow),1e-4f);
		assertEquals(flow.get(0,0),HornSchunckPyramid.safe(0,-1,flow),1e-4f);
		assertEquals(flow.get(5,0),HornSchunckPyramid.safe(5,-1,flow),1e-4f);
		assertEquals(flow.get(5,height-1),HornSchunckPyramid.safe(5,height,flow),1e-4f);
		assertEquals(flow.get(0,5),HornSchunckPyramid.safe(-1,5,flow),1e-4f);
		assertEquals(flow.get(width-1,5),HornSchunckPyramid.safe(width,5,flow),1e-4f);
	}

}
