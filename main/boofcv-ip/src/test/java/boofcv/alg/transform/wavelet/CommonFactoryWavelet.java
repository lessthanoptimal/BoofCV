/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.transform.wavelet;

import boofcv.BoofTesting;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.transform.wavelet.impl.ImplWaveletTransformNaive;
import boofcv.struct.border.BorderIndex1D;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS32;
import boofcv.struct.wavelet.WaveletDescription;
import boofcv.struct.wavelet.WlCoef_F32;
import boofcv.struct.wavelet.WlCoef_I32;
import boofcv.testing.BoofStandardJUnit;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.MatrixFeatures_DDRM;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Common testing functions for wavelet factories
 *
 * @author Peter Abeles
 */
public class CommonFactoryWavelet extends BoofStandardJUnit {

	int width = 20;
	int height = 30;

	/**
	 * See if the provided wavelets can be used to transform the image and change it back without error
	 *
	 * @param waveletDesc The wavelet being tested
	 */
	public void checkEncodeDecode_F32( WaveletDescription<WlCoef_F32> waveletDesc ) {

		// test both even and odd images
		for( int makeOdd = 0; makeOdd <= 1; makeOdd++ ) {
			GrayF32 orig = new GrayF32(width-makeOdd,height-makeOdd);
			GrayF32 tran = new GrayF32(width,height);
			GrayF32 rev = new GrayF32(width-makeOdd,height-makeOdd);

			ImageMiscOps.fillUniform(orig,rand,0,50);

			BorderIndex1D border = waveletDesc.getBorder();

			// First test again naive transform operations, which are the standard implementation
			ImplWaveletTransformNaive.horizontal(border,waveletDesc.forward,orig,tran);
			ImplWaveletTransformNaive.horizontalInverse(border,waveletDesc.inverse,tran,rev);

			BoofTesting.assertEquals(orig,rev,1e-4f);

			ImplWaveletTransformNaive.vertical(border,waveletDesc.forward,orig,tran);
			ImplWaveletTransformNaive.verticalInverse(border,waveletDesc.inverse,tran,rev);

			BoofTesting.assertEquals(orig,rev,1e-4f);

			// quick sanity check to make sure that WaveletTransformOps
			// also correctly does a transform with these wavelets
			// more of a robustness test of WaveletTransformOps than anything else
			WaveletTransformOps.transform1(waveletDesc,orig,tran,null);
			WaveletTransformOps.inverse1(waveletDesc,tran,rev,null,0,255);

//			BoofTesting.printDiff(orig,rev);
			BoofTesting.assertEquals(orig,rev,1e-4f);
		}
	}

	/**
	 * See if the provided wavelets can be used to transform the image and change it back without error
	 *
	 * @param waveletDesc The wavelet being tested
	 */
	public void checkEncodeDecode_I32(WaveletDescription<WlCoef_I32> waveletDesc ) {

		// test both even and odd images
		for( int makeOdd = 0; makeOdd <= 1; makeOdd++ ) {
			GrayS32 orig = new GrayS32(width-makeOdd,height-makeOdd);
			GrayS32 tran = new GrayS32(width,height);
			GrayS32 rev = new GrayS32(width-makeOdd,height-makeOdd);

			ImageMiscOps.fillUniform(orig,rand,-50,50);

			BorderIndex1D border = waveletDesc.getBorder();

			ImplWaveletTransformNaive.horizontal(border,waveletDesc.forward,orig,tran);
			ImplWaveletTransformNaive.horizontalInverse(border,waveletDesc.inverse,tran,rev);

			BoofTesting.assertEquals(orig,rev,0);

			// quick sanity check to make sure that WaveletTransformOps
			// also correctly does a transform with these wavelets
			// more of a robustness test of WaveletTransformOps than anything else
			WaveletTransformOps.transform1(waveletDesc,orig,tran,null);
			WaveletTransformOps.inverse1(waveletDesc,tran,rev,null,Integer.MIN_VALUE,Integer.MAX_VALUE);

			BoofTesting.assertEquals(orig,rev,0);
		}
	}

	/**
	 * Computes the dot product of two wavelets separated by different offsets. If
	 * the offset is zero and they have an orthogonal/biorothogonal relationship then
	 * the dot product should be one. Otherwise it will be zero.
	 */
	public static void checkBiorthogonal_F32( WaveletDescription<WlCoef_F32> desc )
	{
		WlCoef_F32 forward = desc.getForward();
		BorderIndex1D border = desc.getBorder();

		int N = Math.max(forward.getScalingLength(),forward.getWaveletLength());
		N += N%2;
		N *= 2;
		border.setLength(N);

		DMatrixRMaj A = new DMatrixRMaj(N,N);
		DMatrixRMaj B = new DMatrixRMaj(N,N);

		// using the provided wrapping rule to construct a matrix with the coefficients
		for( int i = 0; i < N; i += 2 ) {
			for( int j = 0; j < forward.scaling.length; j++ ) {
				int index = border.getIndex(i+j+forward.offsetScaling);
				A.add(i,index,forward.scaling[j]);
			}
			for( int j = 0; j < forward.wavelet.length; j++ ) {
				int index = border.getIndex(i+j+forward.offsetWavelet);
				A.add(i+1,index,forward.wavelet[j]);
			}
		}

		// the inverse coefficients should create a matrix that is the inverse of the forward coefficients
		final int lowerBorder = desc.getInverse().getLowerLength()*2;
		final int upperBorder = N-desc.getInverse().getUpperLength()*2;

		for( int i = 0; i < N; i += 2 ) {
			WlCoef_F32 inverse;
			
			if( i < lowerBorder ) {
				inverse = desc.getInverse().getBorderCoefficients(i);
			} else if( i >= upperBorder ) {
				inverse = desc.getInverse().getBorderCoefficients(i-N);
			} else {
				inverse = desc.getInverse().getInnerCoefficients();
			}

			for( int j = 0; j < inverse.scaling.length; j++ ) {
				int index = border.getIndex(i+j+inverse.offsetScaling);
				B.add(index,i,inverse.scaling[j]);
			}
			for( int j = 0; j < inverse.wavelet.length; j++ ) {
				int index = border.getIndex(i+j+inverse.offsetWavelet);
				B.add(index,i+1,inverse.wavelet[j]);
			}
		}

		DMatrixRMaj C = new DMatrixRMaj(N,N);

		CommonOps_DDRM.mult(A,B,C);

//		A.print();
//		B.print();
//		C.print();

		assertTrue(MatrixFeatures_DDRM.isIdentity(C,1e-4));
	}

	public static void checkBiorthogonal_I32( WaveletDescription<WlCoef_I32> desc )
	{
		WlCoef_I32 forward = desc.getForward();
		BorderIndex1D border = desc.getBorder();

		int N = Math.max(forward.getScalingLength(),forward.getWaveletLength());
		N += N%2;
		N *= 2;
		border.setLength(N);

		DMatrixRMaj A = new DMatrixRMaj(N,N);
		DMatrixRMaj B = new DMatrixRMaj(N,N);

		// using the wrapping rule construct a matrix with the coefficients
		for( int i = 0; i < N; i += 2 ) {
			for( int j = 0; j < forward.scaling.length; j++ ) {
				int index = border.getIndex(i+j+forward.offsetScaling);
				A.add(i,index,(double)forward.scaling[j]/forward.denominatorScaling);
			}
			for( int j = 0; j < forward.wavelet.length; j++ ) {
				int index = border.getIndex(i+j+forward.offsetWavelet);
				A.add(i+1,index,(double)forward.wavelet[j]/forward.denominatorWavelet);
			}
		}

		// the inverse coefficients should be a matrix which is the inverse of the forward coefficients
		final int lowerBorder = desc.getInverse().getLowerLength()*2;
		final int upperBorder = N-desc.getInverse().getUpperLength()*2;

		for( int i = 0; i < N; i += 2 ) {
			WlCoef_I32 inverse;

			if( i < lowerBorder ) {
				inverse = desc.getInverse().getBorderCoefficients(i);
			} else if( i >= upperBorder ) {
				inverse = desc.getInverse().getBorderCoefficients(i-N);
			} else {
				inverse = desc.getInverse().getInnerCoefficients();
			}

			for( int j = 0; j < inverse.scaling.length; j++ ) {
				int index = border.getIndex(i+j+inverse.offsetScaling);
				B.add(index,i,(double)inverse.scaling[j]/inverse.denominatorScaling);
			}
			for( int j = 0; j < inverse.wavelet.length; j++ ) {
				int index = border.getIndex(i+j+inverse.offsetWavelet);
				B.add(index,i+1,(double)inverse.wavelet[j]/inverse.denominatorWavelet);
			}
		}

		DMatrixRMaj C = new DMatrixRMaj(N,N);

		CommonOps_DDRM.mult(A,B,C);

		assertTrue(MatrixFeatures_DDRM.isIdentity(C,1e-4));
	}

	public static void checkPolySumToZero(float support[], int polyOrder, int offset ) {
		for( int o = 1; o <= polyOrder; o++ ) {
			double total = 0;
			for( int j = 0; j < support.length; j++ ) {
				double coef = Math.pow(j+offset,o);
				total += coef*support[j];
			}
			assertEquals(0,total,1e-4,"Failed poly test at order "+o);
		}
	}

	public static void checkPolySumToZero(int support[], int polyOrder, int offset ) {
		for( int o = 1; o <= polyOrder; o++ ) {
			double total = 0;
			for( int j = 0; j < support.length; j++ ) {
				double coef = Math.pow(j+offset,o);
				total += coef*support[j];
			}
			assertEquals(0,total,1e-4,"Failed poly test at order "+o);
		}
	}
}
