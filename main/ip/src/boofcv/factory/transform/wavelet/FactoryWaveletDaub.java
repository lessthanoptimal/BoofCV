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

package boofcv.factory.transform.wavelet;

import boofcv.alg.transform.wavelet.UtilWavelet;
import boofcv.core.image.border.BorderIndex1D;
import boofcv.core.image.border.BorderIndex1D_Reflect;
import boofcv.core.image.border.BorderIndex1D_Wrap;
import boofcv.core.image.border.BorderType;
import boofcv.struct.wavelet.*;
import org.ejml.data.DenseMatrix64F;
import org.ejml.factory.LinearSolverFactory;
import org.ejml.interfaces.linsol.LinearSolver;


/**
 * <p>
 * Creates different variety of Daubechie (Daub) wavelets. For Daub-J and Daub J/K wavelets the index
 * number refers to the number of coefficients. These wavelets are often used in image compression.
 * </p>
 *
 * <p>
 * Citations:<br>
 * James S. Walker, "A Primer on WAVELETS and Their Scientific Applications," 2nd Ed. 2008
 * </p>
 * @author Peter Abeles
 */
public class FactoryWaveletDaub {

	/**
	 * <p>
	 * DaubJ wavelets have the following properties:<br>
	 * <ul>
	 * <li>Conserve the signal's energy</li>
	 * <li>If the signal is approximately polynomial of degree J/2-1 or less within the support then fluctuations are approximately zero.</li>
	 * <li>The sum of the scaling numbers is sqrt(2)</li>
	 * <li>The sum of the wavelet numbers is 0</li>
	 * </ul>
	 * </p>
	 *
	 * @param J The wavelet's degree.
	 * @return Description of the DaubJ wavelet.
	 */
	public static WaveletDescription<WlCoef_F32> daubJ_F32( int J ) {
		if( J != 4 ) {
			throw new IllegalArgumentException("Only 4 is currently supported");
		}

		WlCoef_F32 coef = new WlCoef_F32();

		coef.offsetScaling = 0;
		coef.offsetWavelet = 0;

		coef.scaling = new float[4];
		coef.wavelet = new float[4];

		double sqrt3 = Math.sqrt(3);
		double div = 4.0*Math.sqrt(2);
		coef.scaling[0] = (float)((1+sqrt3)/div);
		coef.scaling[1] = (float)((3+sqrt3)/div);
		coef.scaling[2] = (float)((3-sqrt3)/div);
		coef.scaling[3] = (float)((1-sqrt3)/div);

		coef.wavelet[0] = coef.scaling[3];
		coef.wavelet[1] = -coef.scaling[2];
		coef.wavelet[2] = coef.scaling[1];
		coef.wavelet[3] = -coef.scaling[0];

		WlBorderCoefStandard<WlCoef_F32> inverse = new WlBorderCoefStandard<>(coef);

		return new WaveletDescription<>(new BorderIndex1D_Wrap(), coef, inverse);
	}

	/**
	 * <p>
	 * Daub J/K biorthogonal wavelets have the following properties:<br>
	 * <ul>
	 * <li>DO NOT conserve the signal's energy</li>
	 * <li>If the signal is approximately polynomial of degree (J-1)/2-1 within the support then fluctuations are approximately zero.</li>
	 * <li>The sum of the scaling numbers is 1</li>
	 * <li>The sum of the wavelet numbers is 0</li>
	 * </ul>
	 * </p>
	 *
	 * @param J The wavelet's degree. K = J-2.
	 * @param borderType How image borders are handled.
	 * @return Description of the Daub J/K wavelet.
	 */
	public static WaveletDescription<WlCoef_F32> biorthogonal_F32( int J ,
																   BorderType borderType ) {
		if( J != 5 ) {
			throw new IllegalArgumentException("Only 5 is currently supported");
		}

		WlCoef_F32 forward = new WlCoef_F32();

		forward.offsetScaling = -2;
		forward.offsetWavelet = 0;

		forward.scaling = new float[5];
		forward.wavelet = new float[3];

		forward.scaling[0] = (float)(-1.0/8.0);
		forward.scaling[1] = (float)(2.0/8.0);
		forward.scaling[2] = (float)(6.0/8.0);
		forward.scaling[3] = (float)(2.0/8.0);
		forward.scaling[4] = (float)(-1.0/8.0);

		forward.wavelet[0] = -1.0f/2.0f;
		forward.wavelet[1] = 1;
		forward.wavelet[2] = -1.0f/2.0f;

		BorderIndex1D border;
		WlBorderCoef<WlCoef_F32> inverse;

		if( borderType == BorderType.REFLECT ) {
			WlCoef_F32 inner = computeInnerInverseBiorthogonal(forward);
			border = new BorderIndex1D_Reflect();
			inverse = computeBorderCoefficients(border,forward,inner);
		} else if( borderType == BorderType.WRAP ) {
			WlCoef_F32 inner = computeInnerInverseBiorthogonal(forward);
			inverse = new WlBorderCoefStandard<>(inner);
			border = new BorderIndex1D_Wrap();
		} else {
			throw new IllegalArgumentException("Unsupported border type: "+borderType);
		}
		return new WaveletDescription<>(border, forward, inverse);

	}

	private static WlCoef_F32 computeInnerInverseBiorthogonal(WlCoef_F32 coef) {
		WlCoef_F32 ret = new WlCoef_F32();

		// center at zero
		ret.offsetScaling = -coef.wavelet.length/2;
		// center at one
		ret.offsetWavelet = 1-coef.scaling.length/2;

		ret.scaling = new float[coef.wavelet.length];
		ret.wavelet = new float[coef.scaling.length];

		for( int i = 0; i < ret.scaling.length; i++ ) {
			if( i % 2 == 0 )
				ret.scaling[i] = -coef.wavelet[i];
			else
				ret.scaling[i] = coef.wavelet[i];
		}
		for( int i = 0; i < ret.wavelet.length; i++ ) {
			if( i % 2 == 1 )
				ret.wavelet[i] = -coef.scaling[i];
			else
				ret.wavelet[i] = coef.scaling[i];
		}

		return ret;
	}

	/**
	 * Computes inverse coefficients 
	 *
	 * @param border
	 * @param forward Forward coefficients.
	 * @param inverse Inverse used in the inner portion of the data stream.
	 * @return
	 */
	private static WlBorderCoef<WlCoef_F32> computeBorderCoefficients( BorderIndex1D border ,
																	   WlCoef_F32 forward ,
																	   WlCoef_F32 inverse ) {
		int N = Math.max(forward.getScalingLength(),forward.getWaveletLength());
		N += N%2;
		N *= 2;
		border.setLength(N);

		// Because the wavelet transform is a linear invertible system the inverse coefficients
		// can be found by creating a matrix and inverting the matrix.  Boundary conditions are then
		// extracted from this inverted matrix.
		DenseMatrix64F A = new DenseMatrix64F(N,N);
		for( int i = 0; i < N; i += 2 ) {
			
			for( int j = 0; j < forward.scaling.length; j++ ) {
				int index = border.getIndex(j+i+forward.offsetScaling);
				A.add(i,index,forward.scaling[j]);
			}

			for( int j = 0; j < forward.wavelet.length; j++ ) {
				int index = border.getIndex(j+i+forward.offsetWavelet);
				A.add(i+1,index,forward.wavelet[j]);
			}
		}

		LinearSolver<DenseMatrix64F> solver = LinearSolverFactory.linear(N);
		if( !solver.setA(A) || solver.quality() < 1e-5) {
			throw new IllegalArgumentException("Can't invert matrix");
		}

		DenseMatrix64F A_inv = new DenseMatrix64F(N,N);
		solver.invert(A_inv);

		int numBorder = UtilWavelet.borderForwardLower(inverse)/2;

		WlBorderCoefFixed<WlCoef_F32> ret = new WlBorderCoefFixed<>(numBorder, numBorder + 1);
		ret.setInnerCoef(inverse);

		// add the lower coefficients first
		for( int i = 0; i < ret.getLowerLength(); i++) {
			computeLowerCoef(inverse, A_inv, ret, i*2);
		}

		// add upper coefficients
		for( int i = 0; i < ret.getUpperLength(); i++) {
			computeUpperCoef(inverse, N, A_inv, ret, i*2);
		}

		return ret;
	}

	private static void computeLowerCoef(WlCoef_F32 inverse, DenseMatrix64F a_inv, WlBorderCoefFixed ret, int col) {
		int lengthWavelet = inverse.wavelet.length + inverse.offsetWavelet + col;
		int lengthScaling = inverse.scaling.length + inverse.offsetScaling + col;
		lengthWavelet = Math.min(lengthWavelet,inverse.wavelet.length);
		lengthScaling = Math.min(lengthScaling,inverse.scaling.length);

		float []coefScaling = new float[lengthScaling];
		float []coefWavelet = new float[lengthWavelet];

		for( int j = 0; j < lengthScaling; j++ ) {
			coefScaling[j] = (float) a_inv.get(j,col);
		}
		for( int j = 0; j < lengthWavelet; j++ ) {
			coefWavelet[j] = (float) a_inv.get(j,col+1);
		}
		ret.lowerCoef[col] = new WlCoef_F32(coefScaling,0,coefWavelet,0);
	}

	private static void computeUpperCoef(WlCoef_F32 inverse, int n, DenseMatrix64F a_inv, WlBorderCoefFixed ret, int col) {
		int indexEnd = n - col - 2;
		int lengthWavelet = indexEnd+inverse.offsetWavelet+inverse.wavelet.length;
		int lengthScaling = indexEnd+inverse.offsetScaling+inverse.scaling.length;
		lengthWavelet = lengthWavelet > n ? inverse.wavelet.length - (lengthWavelet-n) : inverse.wavelet.length;
		lengthScaling = lengthScaling > n ? inverse.scaling.length - (lengthScaling-n) : inverse.scaling.length;

		float []coefScaling = new float[lengthScaling];
		float []coefWavelet = new float[lengthWavelet];

		for( int j = 0; j < lengthScaling; j++ ) {
			coefScaling[j] = (float) a_inv.get(indexEnd+j+inverse.offsetScaling, n -2-col);
		}
		for( int j = 0; j < lengthWavelet; j++ ) {
			coefWavelet[j] = (float) a_inv.get(indexEnd+j+inverse.offsetWavelet, n -2-col+1);
		}
		ret.upperCoef[col/2] = new WlCoef_F32(coefScaling,inverse.offsetScaling,coefWavelet,inverse.offsetWavelet);
	}

	/**
	 * Integer version of {@link #biorthogonal_F32}.
	 *
	 * @param J The wavelet's degree. K = J-2.
	 * @param borderType How image borders are handled.
	 * @return Description of the Daub J/K wavelet.
	 */
	public static WaveletDescription<WlCoef_I32> biorthogonal_I32( int J ,
																   BorderType borderType ) {
		if( J != 5 ) {
			throw new IllegalArgumentException("Only 5 is currently supported");
		}

		WlCoef_I32 forward = new WlCoef_I32();

		forward.offsetScaling = -2;
		forward.offsetWavelet = 0;

		forward.scaling = new int[5];
		forward.wavelet = new int[3];

		forward.denominatorScaling = 8;
		forward.scaling[0] = -1;
		forward.scaling[1] = 2;
		forward.scaling[2] = 6;
		forward.scaling[3] = 2;
		forward.scaling[4] = -1;

		forward.denominatorWavelet = 2;
		forward.wavelet[0] = -1;
		forward.wavelet[1] = 2;
		forward.wavelet[2] = -1;

		BorderIndex1D border;
		WlBorderCoef<WlCoef_I32> inverse;

		if( borderType == BorderType.WRAP ) {
			WlCoef_I32 inner = computeInnerBiorthogonalInverse(forward);
			inverse = new WlBorderCoefStandard<>(inner);
			border = new BorderIndex1D_Wrap();
		} else if( borderType == BorderType.REFLECT ) {
			WlCoef_I32 inner = computeInnerBiorthogonalInverse(forward);
			inverse = convertToInt((WlBorderCoefFixed<WlCoef_F32>)biorthogonal_F32(J,borderType).getInverse(),inner);
			border = new BorderIndex1D_Reflect();
		} else {
			throw new IllegalArgumentException("Unsupported border type: "+borderType);
		}
		return new WaveletDescription<>(border, forward, inverse);

	}

	private static WlCoef_I32 computeInnerBiorthogonalInverse(WlCoef_I32 coef) {
		WlCoef_I32 ret = new WlCoef_I32();

		// center at zero
		ret.offsetScaling = -coef.wavelet.length/2;
		// center at one
		ret.offsetWavelet = 1-coef.scaling.length/2;

		ret.denominatorScaling = coef.denominatorWavelet;
		ret.denominatorWavelet = coef.denominatorScaling;

		ret.scaling = new int[coef.wavelet.length];
		ret.wavelet = new int[coef.scaling.length];

		for( int i = 0; i < ret.scaling.length; i++ ) {
			if( i % 2 == 0 )
				ret.scaling[i] = -coef.wavelet[i];
			else
				ret.scaling[i] = coef.wavelet[i];
		}
		for( int i = 0; i < ret.wavelet.length; i++ ) {
			if( i % 2 == 1 )
				ret.wavelet[i] = -coef.scaling[i];
			else
				ret.wavelet[i] = coef.scaling[i];
		}
		return ret;
	}

	// todo rename and move to a utility function?
	public static WlBorderCoefFixed<WlCoef_I32> convertToInt( WlBorderCoefFixed<WlCoef_F32> orig ,
															  WlCoef_I32 inner ) {
		WlBorderCoefFixed<WlCoef_I32> ret =
				new WlBorderCoefFixed<>(orig.getLowerLength(), orig.getUpperLength());

		for( int i = 0; i < orig.getLowerLength(); i++ ) {
			WlCoef_F32 o = orig.getLower(i);
			WlCoef_I32 r = new WlCoef_I32();
			ret.setLower(i,r);
			convertCoef_F32_to_I32(inner.denominatorScaling, inner.denominatorWavelet, o, r);
		}
		for( int i = 0; i < orig.getUpperLength(); i++ ) {
			WlCoef_F32 o = orig.getUpper(i);
			WlCoef_I32 r = new WlCoef_I32();
			ret.setUpper(i,r);
			convertCoef_F32_to_I32(inner.denominatorScaling, inner.denominatorWavelet, o, r);
		}

		ret.setInnerCoef(inner);

		return ret;
	}

	private static void convertCoef_F32_to_I32(int denominatorScaling, int denominatorWavelet, WlCoef_F32 o, WlCoef_I32 r) {
		r.denominatorScaling = denominatorScaling;
		r.denominatorWavelet = denominatorWavelet;
		r.scaling = new int[ o.scaling.length ];
		r.wavelet = new int[ o.wavelet.length ];
		r.offsetScaling = o.offsetScaling;
		r.offsetWavelet = o.offsetWavelet;

		for( int j = 0; j < o.scaling.length; j++ ) {
			r.scaling[j] = Math.round(o.scaling[j]*denominatorScaling);
		}
		for( int j = 0; j < o.wavelet.length; j++ ) {
			r.wavelet[j] = Math.round(o.wavelet[j]*denominatorWavelet);
		}
	}
}
