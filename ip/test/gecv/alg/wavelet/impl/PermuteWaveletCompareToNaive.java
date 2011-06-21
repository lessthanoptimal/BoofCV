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
import gecv.struct.wavelet.WaveletDesc_F32;

import java.util.Random;


/**
 * @author Peter Abeles
 */
public abstract class PermuteWaveletCompareToNaive {
	Random rand = new Random(234);

	int width = 20;
	int height = 30;

	public void runTests( boolean shrinkInput ) {
				// test even and odd width images
		for( int shrink = 0; shrink <= 1; shrink++ ) {
			ImageFloat32 input,found,expected;
			if( shrinkInput ) {
				input = new ImageFloat32(width-shrink,height-shrink);
				found = new ImageFloat32(width,height);
				expected = new ImageFloat32(width,height);
			} else {
				input = new ImageFloat32(width,height);
				found = new ImageFloat32(width-shrink,height-shrink);
				expected = new ImageFloat32(width-shrink,height-shrink);
			}

			ImageTestingOps.randomize(input,rand,0,50);

			// test different descriptions lengths and offsets
			for( int o = 0; o <= 2; o++ ) {
				for( int l = 2+o; l <= 5; l++ ) {
//					System.out.println("shrink "+shrink+" o = "+o+" l = "+l);
					ImageTestingOps.fill(found,0);
					ImageTestingOps.fill(expected,0);
					WaveletDesc_F32 desc = createDesc(-o,l);

					applyValidation(desc,input,expected);
					applyTransform(desc,input,found);

					compareResults(desc, expected , found, shrink != 0);
				}
			}
		}
	}
	public abstract void applyValidation( WaveletDesc_F32 desc , ImageFloat32 input , ImageFloat32 output );


	public abstract void applyTransform( WaveletDesc_F32 desc , ImageFloat32 input , ImageFloat32 output );

	public abstract void compareResults(WaveletDesc_F32 desc, ImageFloat32 expected, ImageFloat32 found, boolean shrunk);

	private WaveletDesc_F32 createDesc(int offset, int length) {
		WaveletDesc_F32 ret = new WaveletDesc_F32();
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
