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
import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestBroxWarpingSpacial {

	int width = 10;
	int height = 13;
	Random rand = new Random(234);

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

		InterpolatePixelS<ImageFloat32> interpolate = FactoryInterpolation.bilinearPixelS(ImageFloat32.class);
		BroxWarpingSpacial<ImageFloat32> alg = new BroxWarpingSpacial<ImageFloat32>(new ConfigBroxWarping(),interpolate);
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
		fail("Implement");
	}

	@Test
	public void computePsiSmooth() {
		fail("Implement");
	}

	@Test
	public void computePsiDataPsiGradient() {
		fail("Implement");
	}

	@Test
	public void computeDivUVD_safe() {
		fail("Implement");
	}

	@Test
	public void s() {
		fail("Implement");
	}

}
