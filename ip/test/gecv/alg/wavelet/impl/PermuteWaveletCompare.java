/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.wavelet.impl;

import gecv.alg.misc.ImageTestingOps;
import gecv.core.image.border.BorderIndex1D_Wrap;
import gecv.struct.image.ImageFloat32;
import gecv.struct.wavelet.WaveletCoefficient_F32;

import java.util.Random;


/**
 * todo comment
 *
 * @author Peter Abeles
 */
// todo add sub-image test
public abstract class PermuteWaveletCompare {
	Random rand = new Random(234);

	int width = 20;
	int height = 30;

	public void runTests( boolean swapSizes ) {

		// normal image sizes for a single level
		runTest(20,30,20,30,swapSizes);
		runTest(19,29,20,30,swapSizes);

		// now try expanded borders
		runTest(20,30,22,32,swapSizes);
		runTest(20,30,24,34,swapSizes);
		runTest(20,30,24,30,swapSizes);
		runTest(19,29,22,32,swapSizes);
		runTest(19,29,24,34,swapSizes);
		runTest(19,29,24,30,swapSizes);
	}

	private void runTest( int widthIn , int heightIn , int widthOut , int heightOut , boolean swapSize ) {

		if( swapSize ) {
			int t = widthIn;
			widthIn = widthOut;
			widthOut = t;
			t = heightIn;
			heightIn = heightOut;
			heightOut = t;
		}

		ImageFloat32 input = new ImageFloat32(widthIn,heightIn);
		ImageFloat32 found = new ImageFloat32(widthOut,heightOut);
		ImageFloat32 expected = new ImageFloat32(widthOut,heightOut);

		ImageTestingOps.randomize(input,rand,0,50);

		// test different descriptions lengths and offsets
		for( int o = 0; o <= 2; o++ ) {
			for( int l = 2+o; l <= 5; l++ ) {
//					System.out.println("shrink "+shrink+" o = "+o+" l = "+l);
				ImageTestingOps.fill(found,0);
				ImageTestingOps.fill(expected,0);
				WaveletCoefficient_F32 desc = createDesc(-o,l);

				applyValidation(desc,input,expected);
				applyTransform(desc,input,found);

				compareResults(desc, input , expected , found);
			}
		}
	}

	public abstract void applyValidation( WaveletCoefficient_F32 desc , ImageFloat32 input , ImageFloat32 output );


	public abstract void applyTransform( WaveletCoefficient_F32 desc , ImageFloat32 input , ImageFloat32 output );

	public abstract void compareResults(WaveletCoefficient_F32 desc, ImageFloat32 input , ImageFloat32 expected, ImageFloat32 found );

	private WaveletCoefficient_F32 createDesc(int offset, int length) {
		WaveletCoefficient_F32 ret = new WaveletCoefficient_F32();
		ret.border = new BorderIndex1D_Wrap();
		ret.offsetScaling = offset;
		ret.offsetWavelet = offset;
		ret.scaling = new float[length];
		ret.wavelet = new float[length];

		for( int i = 0; i < length; i++ ) {
			ret.scaling[i] = (float)rand.nextGaussian();
			ret.wavelet[i] = (float)rand.nextGaussian();
		}

		return ret;
	}
}
