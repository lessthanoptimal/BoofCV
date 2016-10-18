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

package boofcv.alg.transform.wavelet.impl;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.BorderIndex1D;
import boofcv.core.image.border.BorderIndex1D_Reflect;
import boofcv.core.image.border.BorderIndex1D_Wrap;
import boofcv.core.image.border.BorderType;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import boofcv.struct.wavelet.*;
import boofcv.testing.BoofTesting;

import java.util.Random;


/**
 * Generalized wavelet test which allows one algorithm to be compared against another one across
 * a wide variety of image shapes, wavelet sizes, and sub-images.
 *
 * @author Peter Abeles
 */
public abstract class PermuteWaveletCompare {
	Random rand = new Random(234);

	Class inputType;
	Class outputType;
	

	protected PermuteWaveletCompare(Class inputType, Class outputType) {
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

		ImageGray input = GeneralizedImageOps.createSingleBand(inputType, widthIn, heightIn);
		ImageGray found = GeneralizedImageOps.createSingleBand(outputType, widthOut, heightOut);
		ImageGray expected = GeneralizedImageOps.createSingleBand(outputType, widthOut, heightOut);

		GImageMiscOps.fillUniform(input, rand, 0, 50);

//		System.out.println("In [ "+widthIn+" , "+heightIn+" ]  Out [ "+widthOut+" , "+heightOut+" ]");

		// test different descriptions lengths and offsets, and borders
		for( BorderType type : BorderType.values() ) {
			for( int o = 0; o <= 2; o++ ) {
				for( int l = 2+o; l <= 5; l++ ) {
//					System.out.println("type "+type+" o = "+o+" l = "+l);
					GImageMiscOps.fill(found, 0);
					GImageMiscOps.fill(expected,0);
					// create a random wavelet.  does not have to be a real once
					// since it just is checking that two functions produce the same output
					WaveletDescription<?> desc = createDesc(-o,l,type);
					applyValidation(desc,input,expected);

					// make sure it works on sub-images
					BoofTesting.checkSubImage(this,"innerTest",false,input,found, expected, desc);
				}
			}
		}
	}

	public void innerTest(ImageGray input, ImageGray found, ImageGray expected,
						  WaveletDescription<?> desc) {
		applyTransform(desc,input,found);
//		BoofTesting.printDiff(found,expected);
		compareResults(desc, input , expected , found);
	}

	public abstract void applyValidation(WaveletDescription<?> desc , ImageGray input , ImageGray output );

	public abstract void applyTransform(WaveletDescription<?> desc , ImageGray input , ImageGray output );

	public abstract void compareResults(WaveletDescription<?> desc, ImageGray input , ImageGray expected, ImageGray found );

	private WaveletDescription<?> createDesc(int offset, int length, BorderType type ) {
		if( inputType == GrayF32.class ) {
			return createDesc_F32(offset,length,type);
		} else {
			return createDesc_I32(offset,length,type);
		}
	}

	private WaveletDescription<WlCoef_F32> createDesc_F32(int offset, int length, BorderType type ) {
		WlCoef_F32 forward = createRandomCoef_F32(offset, length);

		WlBorderCoef<WlCoef_F32> inverse;
		BorderIndex1D border;

		if( type == BorderType.WRAP ) {
			inverse = new WlBorderCoefStandard<>(forward);
			border = new BorderIndex1D_Wrap();
		} else {
			inverse = createFixedCoef_F32(forward);
			border = new BorderIndex1D_Reflect();
		}

		return new WaveletDescription<>(border, forward, inverse);
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

	private WlBorderCoef<WlCoef_F32> createFixedCoef_F32(WlCoef_F32 forward) {

		int l = Math.max(forward.getScalingLength()+forward.offsetScaling,forward.getWaveletLength()+forward.offsetWavelet);

		int numLower = -Math.max(forward.offsetScaling,forward.offsetWavelet);
		int numUpper = Math.max(0,l-2);

		numLower = (numLower + numLower%2)/2;
		numUpper = (numUpper + numUpper%2)/2;


		WlBorderCoefFixed<WlCoef_F32> ret = new WlBorderCoefFixed<>(numLower, numUpper);
		ret.setInnerCoef(forward);

		for( int i = 0; i < numLower; i++ ) {
			ret.setLower(i,createRandomCoef_F32(forward.offsetScaling, forward.getScalingLength()));
		}
		for( int j = 0; j < numUpper; j++ ) {
			ret.setUpper(j,createRandomCoef_F32(forward.offsetScaling, forward.getScalingLength()));
		}

		return ret;
	}

	private WaveletDescription<WlCoef_I32> createDesc_I32(int offset, int length, BorderType type ) {
		WlCoef_I32 forward = createRandomCoef_I32(offset, length);

		BorderIndex1D border;
		WlBorderCoef<WlCoef_I32> inverse;

		if( type == BorderType.WRAP ) {
			inverse = new WlBorderCoefStandard<>(forward);
			border = new BorderIndex1D_Wrap();
		} else {
			inverse = createFixedCoef_I32(forward);
			border = new BorderIndex1D_Reflect();
		}

		return new WaveletDescription<>(border, forward, inverse);
	}

	private WlCoef_I32 createRandomCoef_I32(int offset, int length) {
		WlCoef_I32 forward = new WlCoef_I32();
		forward.offsetScaling = offset;
		forward.offsetWavelet = offset;
		forward.scaling = new int[length];
		forward.wavelet = new int[length];
		forward.denominatorScaling = 2;
		forward.denominatorWavelet = 3;

		for( int i = 0; i < length; i++ ) {
			forward.scaling[i] = rand.nextInt(8)-3;
			forward.wavelet[i] = rand.nextInt(8)-3;
			// it would never be zero in practice
			if( forward.scaling[i] == 0 )
				forward.scaling[i] = 1;
			if( forward.wavelet[i] == 0 )
				forward.wavelet[i] = 1;
		}
		return forward;
	}

	private WlBorderCoef<WlCoef_I32> createFixedCoef_I32(WlCoef_I32 forward) {

		int l = Math.max(forward.getScalingLength()+forward.offsetScaling,forward.getWaveletLength()+forward.offsetWavelet);

		int numLower = -Math.max(forward.offsetScaling,forward.offsetWavelet);
		int numUpper = Math.max(0,l-2);

		numLower = (numLower + numLower%2)/2;
		numUpper = (numUpper + numUpper%2)/2;


		WlBorderCoefFixed<WlCoef_I32> ret = new WlBorderCoefFixed<>(numLower, numUpper);
		ret.setInnerCoef(forward);

		for( int i = 0; i < numLower; i++ ) {
			ret.setLower(i,createRandomCoef_I32(forward.offsetScaling, forward.getScalingLength()));
		}
		for( int j = 0; j < numUpper; j++ ) {
			ret.setUpper(j,createRandomCoef_I32(forward.offsetScaling, forward.getScalingLength()));
		}

		return ret;
	}
}
