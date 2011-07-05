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

import gecv.alg.wavelet.WaveletBorderType;
import gecv.core.image.GeneralizedImageOps;
import gecv.core.image.border.BorderIndex1D;
import gecv.core.image.border.BorderIndex1D_Reflect;
import gecv.core.image.border.BorderIndex1D_Wrap;
import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageFloat32;
import gecv.struct.wavelet.*;
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

		System.out.println("In [ "+widthIn+" , "+heightIn+" ]  Out [ "+widthOut+" , "+heightOut+" ]");

		// test different descriptions lengths and offsets, and borders
		for( WaveletBorderType type : WaveletBorderType.values() ) {
//		WaveletBorderType type = WaveletBorderType.WRAP; {
			for( int o = 0; o <= 2; o++ ) {
				for( int l = 2+o; l <= 5; l++ ) {
					System.out.println("type "+type+" o = "+o+" l = "+l);
					GeneralizedImageOps.fill(found,0);
					GeneralizedImageOps.fill(expected,0);
					// create a random wavelet.  does not have to be a real once
					// since it just is checking that two functions produce the same output
					WaveletDescription<?> desc = createDesc(-o,l,type);
					applyValidation(desc,input,expected);

					// make sure it works on sub-images
//					innerTest(input,found,expected,desc);
					GecvTesting.checkSubImage(this,"innerTest",false,input,found, expected, desc);
				}
			}
		}
	}

	public void innerTest(ImageBase input, ImageBase found, ImageBase expected,
						  WaveletDescription<?> desc) {
		applyTransform(desc,input,found);
//		GecvTesting.printDiff(found,expected);
		compareResults(desc, input , expected , found);
	}

	public abstract void applyValidation( WaveletDescription<?> desc , ImageBase input , ImageBase output );

	public abstract void applyTransform( WaveletDescription<?> desc , ImageBase input , ImageBase output );

	public abstract void compareResults( WaveletDescription<?> desc, ImageBase input , ImageBase expected, ImageBase found );

	private WaveletDescription<?> createDesc(int offset, int length, WaveletBorderType type ) {
		if( inputType == ImageFloat32.class ) {
			return createDesc_F32(offset,length,type);
		} else {
			return createDesc_I32(offset,length,type);
		}
	}

	private WaveletDescription<WlCoef_F32> createDesc_F32(int offset, int length, WaveletBorderType type ) {
		WlCoef_F32 forward = createRandomCoef_F32(offset, length);

		WlBorderCoef<WlCoef_F32> inverse;
		BorderIndex1D border;

		if( type == WaveletBorderType.WRAP ) {
			inverse = new WlBorderCoefStandard<WlCoef_F32>(forward);
			border = new BorderIndex1D_Wrap();
		} else {
			inverse = createWrappingCoef_F32(forward);
			border = new BorderIndex1D_Reflect();
		}

		return new WaveletDescription<WlCoef_F32>(border,forward,inverse);
	}

	private WlCoef_F32 createRandomCoef_F32(int offset, int length) {
		WlCoef_F32 forward = new WlCoef_F32();
		forward.offsetScaling = offset;
		forward.offsetWavelet = offset;
		forward.scaling = new float[length];
		forward.wavelet = new float[length];

		for( int i = 0; i < length; i++ ) {
			forward.scaling[i] = (float)rand.nextGaussian();
			forward.wavelet[i] = (float)rand.nextGaussian();
		}
		return forward;
	}

	private WlBorderCoef<WlCoef_F32> createWrappingCoef_F32(WlCoef_F32 forward) {

		int l = Math.max(forward.getScalingLength()+forward.offsetScaling,forward.getWaveletLength()+forward.offsetWavelet);

		int numLower = -Math.max(forward.offsetScaling,forward.offsetWavelet);
		int numUpper = Math.max(0,l-2);

		numLower = (numLower + numLower%2)/2;
		numUpper = (numUpper + numUpper%2)/2;


		WlBorderCoefFixed<WlCoef_F32> ret = new WlBorderCoefFixed<WlCoef_F32>(numLower,numUpper);
		ret.setInnerCoef(forward);

		for( int i = 0; i < numLower; i++ ) {
			ret.setLower(i,createRandomCoef_F32(forward.offsetScaling, forward.getScalingLength()));
		}
		for( int j = 0; j < numUpper; j++ ) {
			ret.setUpper(j,createRandomCoef_F32(forward.offsetScaling, forward.getScalingLength()));
		}

		return ret;
	}

	private WaveletDescription<WlCoef_I32> createDesc_I32(int offset, int length, WaveletBorderType type ) {
		WlCoef_I32 forward = new WlCoef_I32();
		forward.offsetScaling = offset;
		forward.offsetWavelet = offset;
		forward.scaling = new int[length];
		forward.wavelet = new int[length];
		forward.denominatorScaling = 2;
		forward.denominatorWavelet = 3;

		for( int i = 0; i < length; i++ ) {
			forward.scaling[i] = rand.nextInt(6)-3;
			forward.wavelet[i] = rand.nextInt(6)-3;
			// it would never be zero in practice
			if( forward.scaling[i] == 0 )
				forward.scaling[i] = 1;
			if( forward.wavelet[i] == 0 )
				forward.wavelet[i] = 1;
		}

		BorderIndex1D border = new BorderIndex1D_Wrap();

		// todo add support for reflect
		WlBorderCoef<WlCoef_I32> inverse = new WlBorderCoefStandard<WlCoef_I32>(forward);

		return new WaveletDescription<WlCoef_I32>(border,forward,inverse);
	}
}
