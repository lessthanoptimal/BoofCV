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

import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.pyramid.PyramidFloat;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestBroxWarpingSpacial {

	int width = 10;
	int height = 13;
	Random rand = new Random(234);

	double epsilon = 0.001;

	InterpolatePixelS<ImageFloat32> interpolate = FactoryInterpolation.bilinearPixelS(ImageFloat32.class);

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

		BroxWarpingSpacial<ImageFloat32> alg = new BroxWarpingSpacial<ImageFloat32>(new ConfigBroxWarping(),interpolate);
		alg.process(pyr1,pyr2);

		for( int y = 0; y < height; y++ ) {
			for( int x = 10; x < 20; x++ ) {
				assertEquals(5,alg.getFlowX().get(x,y),1);
				assertEquals(0,alg.getFlowY().get(x,y),1);
			}
		}

	}

	@Test
	public void computePsiDataPsiGradient() {
		BroxWarpingSpacial<ImageFloat32> alg = new BroxWarpingSpacial<ImageFloat32>(new ConfigBroxWarping(),interpolate);
		alg.resizeForLayer(width, height);

		ImageFloat32 image1 = new ImageFloat32(width,height);
		ImageFloat32 image2 = new ImageFloat32(width,height);
		ImageFloat32 deriv1x = new ImageFloat32(width,height);
		ImageFloat32 deriv1y = new ImageFloat32(width,height);
		ImageFloat32 deriv2x = new ImageFloat32(width,height);
		ImageFloat32 deriv2y = new ImageFloat32(width,height);
		ImageFloat32 deriv2xx = new ImageFloat32(width,height);
		ImageFloat32 deriv2yy = new ImageFloat32(width,height);
		ImageFloat32 deriv2xy = new ImageFloat32(width,height);
		ImageFloat32 du = new ImageFloat32(width,height);
		ImageFloat32 dv = new ImageFloat32(width,height);

		ImageFloat32 psiData = new ImageFloat32(width,height);
		ImageFloat32 psiGradient = new ImageFloat32(width,height);

		ImageMiscOps.fillUniform(image1,rand,-1,1);
		ImageMiscOps.fillUniform(image2,rand,-1,1);
		ImageMiscOps.fillUniform(deriv1x,rand,-1,1);
		ImageMiscOps.fillUniform(deriv1y,rand,-1,1);
		ImageMiscOps.fillUniform(deriv2x,rand,-1,1);
		ImageMiscOps.fillUniform(deriv2y,rand,-1,1);
		ImageMiscOps.fillUniform(deriv2xx,rand,-1,1);
		ImageMiscOps.fillUniform(deriv2yy,rand,-1,1);
		ImageMiscOps.fillUniform(deriv2xy,rand,-1,1);
		ImageMiscOps.fillUniform(du,rand,-1,1);
		ImageMiscOps.fillUniform(dv,rand,-1,1);

		alg.computePsiDataPsiGradient(image1,image2,deriv1x,deriv1y,deriv2x,deriv2y,deriv2xx,deriv2yy,deriv2xy,du,dv,
				psiData,psiGradient);

		float expectedPsiData = computePsiData(5,6,image1,image2,deriv2x,deriv2y,du,dv);
		float expectedPsiGradient =
				computePsiGradient(5,6,deriv1x,deriv1y,deriv2x,deriv2y,deriv2xx,deriv2yy,deriv2xy,du,dv);

		assertEquals(expectedPsiData,psiData.get(5,6),1e-4);
		assertEquals(expectedPsiGradient,psiGradient.get(5,6),1e-4);

	}

	private float computePsiData( int x, int y ,
								  ImageFloat32 image1, ImageFloat32 image2,
								  ImageFloat32 deriv2x, ImageFloat32 deriv2y,
								  ImageFloat32 du, ImageFloat32 dv ) {

		float taylor2 = image2.get(x,y) + deriv2x.get(x,y)*du.get(x,y) + deriv2y.get(x,y)*dv.get(x,y);

		float d = taylor2 - image1.get(x,y);
		return (float)(1.0/(2.0*Math.sqrt(d*d+epsilon*epsilon)));      // in the paper it is 1/2 but not their code
	}

	private float computePsiGradient( int x, int y ,
									  ImageFloat32 deriv1x, ImageFloat32 deriv1y,
									  ImageFloat32 deriv2x, ImageFloat32 deriv2y,
									  ImageFloat32 deriv2xx, ImageFloat32 deriv2yy, ImageFloat32 deriv2xy,
									  ImageFloat32 du, ImageFloat32 dv ) {

		float taylor2x = deriv2x.get(x,y) + deriv2xx.get(x,y)*du.get(x,y) + deriv2xy.get(x,y)*dv.get(x,y);
		float taylor2y = deriv2y.get(x,y) + deriv2xy.get(x,y)*du.get(x,y) + deriv2yy.get(x,y)*dv.get(x,y);

		float dx = taylor2x - deriv1x.get(x,y);
		float dy = taylor2y - deriv1y.get(x,y);

		return (float)(1.0/(2.0*Math.sqrt(dx*dx + dy*dy + epsilon*epsilon)));   // in the paper it is 1/2 but not their code
	}

	@Test
	public void computeDivUVD_safe() {
		ImageFloat32 u = new ImageFloat32(width,height);
		ImageFloat32 v = new ImageFloat32(width,height);
		ImageFloat32 psi = new ImageFloat32(width,height);
		ImageFloat32 divU = new ImageFloat32(width,height);
		ImageFloat32 divV = new ImageFloat32(width,height);
		ImageFloat32 divD = new ImageFloat32(width,height);

		ImageMiscOps.fillUniform(u,rand,-1,1);
		ImageMiscOps.fillUniform(v,rand,-1,1);
		ImageMiscOps.fillUniform(psi,rand,-1,1);

		BroxWarpingSpacial<ImageFloat32> alg = new BroxWarpingSpacial<ImageFloat32>(new ConfigBroxWarping(),interpolate);
		alg.resizeForLayer(width,height);

		alg.computeDivUVD_safe(5,6,u,v,psi,divU,divV,divD);

		float expectedDivU = computeDivU(5,6,u,psi);
		float expectedDivV = computeDivU(5, 6, v, psi);
		float expectedDivD = computeDivD(5, 6, psi);

		assertEquals(expectedDivU,divU.get(5,6),1e-4);
		assertEquals(expectedDivV,divV.get(5,6),1e-4);
		assertEquals(expectedDivD,divD.get(5,6),1e-4);
	}

	private float computeDivU( int x , int y , ImageFloat32 u , ImageFloat32 psi )
	{
		float coef0 = 0.5f*(psi.get(x+1,y) + psi.get(x,y));
		float coef1 = 0.5f*(psi.get(x-1,y) + psi.get(x,y));
		float coef2 = 0.5f*(psi.get(x,y+1) + psi.get(x,y));
		float coef3 = 0.5f*(psi.get(x,y-1) + psi.get(x,y));

		float diff0 = u.get(x+1,y) - u.get(x,y);
		float diff1 = u.get(x-1,y) - u.get(x,y);
		float diff2 = u.get(x,y+1) - u.get(x,y);
		float diff3 = u.get(x,y-1) - u.get(x,y);

		return coef0*diff0 + coef1*diff1 + coef2*diff2 + coef3*diff3;
	}

	private float computeDivD( int x , int y , ImageFloat32 psi )
	{
		float coef0 = 0.5f*(psi.get(x+1,y) + psi.get(x,y));
		float coef1 = 0.5f*(psi.get(x-1,y) + psi.get(x,y));
		float coef2 = 0.5f*(psi.get(x,y+1) + psi.get(x,y));
		float coef3 = 0.5f*(psi.get(x,y-1) + psi.get(x,y));

		return coef0 + coef1 + coef2 + coef3;
	}

	@Test
	public void s() {
		BroxWarpingSpacial<ImageFloat32> alg = new BroxWarpingSpacial<ImageFloat32>(new ConfigBroxWarping(),interpolate);

		alg.resizeForLayer(10,13);
		ImageFloat32 a = alg.warpImage2;

		assertEquals(a.getIndex(0,0),alg.s(-1,0),1e-4f);
		assertEquals(a.getIndex(0,0),alg.s(0,-1),1e-4f);
		assertEquals(a.getIndex(5,0),alg.s(5,-1),1e-4f);
		assertEquals(a.getIndex(5,height-1),alg.s(5,height),1e-4f);
		assertEquals(a.getIndex(0,5),alg.s(-1,5),1e-4f);
		assertEquals(a.getIndex(width-1,5),alg.s(width,5),1e-4f);
	}
}
