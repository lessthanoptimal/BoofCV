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

import gecv.core.image.GeneralizedImageOps;
import gecv.core.image.border.BorderIndex1D_Wrap;
import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageFloat32;
import gecv.struct.wavelet.WaveletCoefficient;
import gecv.struct.wavelet.WaveletCoefficient_F32;
import gecv.struct.wavelet.WaveletCoefficient_I32;
import gecv.testing.GecvTesting;

import java.util.Random;


/**
 * Generalized wavelet test which allows one algorithm to be compared against another one across
 * a wide variety of image shapes, wavelet sizes, and sub-images.
 *
 * @author Peter Abeles
 */
public abstract class PermuteWaveletCompare {
	Random rand = new Random(234);

	Class<?> inputType;
	Class<?> outputType;

	protected PermuteWaveletCompare(Class<?> inputType, Class<?> outputType) {
		this.inputType = inputType;
		this.outputType = outputType;
	}

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

		ImageBase input = GecvTesting.createImage(inputType,widthIn,heightIn);
		ImageBase found = GecvTesting.createImage(outputType,widthOut,heightOut);
		ImageBase expected = GecvTesting.createImage(outputType,widthOut,heightOut);

		GeneralizedImageOps.randomize(input, rand, 0 , 50);

		// test different descriptions lengths and offsets
		for( int o = 0; o <= 2; o++ ) {
			for( int l = 2+o; l <= 5; l++ ) {
//					System.out.println("shrink "+shrink+" o = "+o+" l = "+l);
				GeneralizedImageOps.fill(found,0);
				GeneralizedImageOps.fill(expected,0);
				// create a random wavelet.  does not have to be a real once
				// since it just is checking that two functions produce the same output
				WaveletCoefficient desc = createDesc(-o,l);
				applyValidation(desc,input,expected);

				// make sure it works on sub-images
				GecvTesting.checkSubImage(this,"innerTest",false,input,found, expected, desc);
			}
		}
	}

	public void innerTest(ImageBase input, ImageBase found, ImageBase expected,
						   WaveletCoefficient desc) {
		applyTransform(desc,input,found);
		compareResults(desc, input , expected , found);
	}

	public abstract void applyValidation( WaveletCoefficient desc , ImageBase input , ImageBase output );

	public abstract void applyTransform( WaveletCoefficient desc , ImageBase input , ImageBase output );

	public abstract void compareResults( WaveletCoefficient desc, ImageBase input , ImageBase expected, ImageBase found );

	private WaveletCoefficient createDesc(int offset, int length) {
		if( inputType == ImageFloat32.class ) {
			return createDesc_F32(offset,length);
		} else {
			return createDesc_I32(offset,length);
		}
	}

	private WaveletCoefficient_F32 createDesc_F32(int offset, int length) {
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

	private WaveletCoefficient_I32 createDesc_I32(int offset, int length) {
		WaveletCoefficient_I32 ret = new WaveletCoefficient_I32();
		ret.border = new BorderIndex1D_Wrap();
		ret.offsetScaling = offset;
		ret.offsetWavelet = offset;
		ret.scaling = new int[length];
		ret.wavelet = new int[length];
		ret.denominatorScaling = 2;
		ret.denominatorWavelet = 3;

		for( int i = 0; i < length; i++ ) {
			ret.scaling[i] = rand.nextInt(6)-3;
			ret.wavelet[i] = rand.nextInt(6)-3;
		}

		return ret;
	}
}
