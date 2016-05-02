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

package boofcv.alg.filter.stat;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GConvertImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.struct.convolve.Kernel1D;
import boofcv.struct.image.GrayF;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayF64;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestImageLocalNormalization {

	Random rand = new Random(345);

	Class types[] = {GrayF32.class, GrayF64.class};

	int width = 40;
	int height = 30;

	double delta = 1e-4;
	int radius = 3;

	@Test
	public void zeroMeanStdOne_kernel() {
		double maxPixelValue = 5;

		for( Class type : types ) {
			int bits = type == GrayF32.class ? 32 : 64;
			Kernel1D kernel = FactoryKernelGaussian.gaussian(1,true,bits,-1,radius);

			GrayF input = (GrayF)GeneralizedImageOps.createSingleBand(type,width,height);
			GrayF found = (GrayF)GeneralizedImageOps.createSingleBand(type,width,height);

			GImageMiscOps.fillUniform(input,rand,0,maxPixelValue);

			ImageLocalNormalization alg = new ImageLocalNormalization(type);

			alg.zeroMeanStdOne(kernel,input,maxPixelValue,delta,found);

			compareToExpected(input,kernel,found);
		}
	}

	@Test
	public void zeroMeanStdOne() {
		fail("Implement");
	}

	private void compareToExpected( GrayF origInput , Kernel1D origKernel, GrayF found  ) {
		GrayF64 input = new GrayF64(width,height);
		GConvertImage.convert(origInput,input);

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				double f = GeneralizedImageOps.get(found,x,y);
				double expected = compute(x,y,input,origKernel);
				assertEquals(x+" "+y,expected,f,1e-4);
			}
		}
	}

	private double compute( int cx , int cy , GrayF64 input , Kernel1D kernel ) {
		int width = radius*2+1;
		double vals[] = new double[width*width];
		double weights[] = new double[width*width];
		int numVals = 0;
		double totalW = 0;
		for (int ry = -radius; ry <= radius ; ry++) {
			for (int rx = -radius; rx <= radius ; rx++) {
				int x = cx+rx;
				int y = cy+ry;

				if( !input.isInBounds(x,y) )
					continue;

				double w = kernel.getDouble(radius+rx)*kernel.getDouble(radius+ry);
				double v = input.get(x,y);

				weights[numVals] = w;
				vals[numVals++] = v;
				totalW += w;
			}
		}

		double mean = 0;
		for (int i = 0; i < numVals; i++) {
			mean += weights[i]*vals[i];
		}
		mean /= totalW;

		double variance = 0;
		for (int i = 0; i < numVals; i++) {
			double delta = vals[i]-mean;
			variance += weights[i]*delta*delta;
		}
		variance /= totalW;

		return mean/(Math.sqrt(variance)+delta);
	}

}