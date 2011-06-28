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

package gecv.alg.wavelet;

import gecv.core.image.border.BorderIndex1D_Wrap;
import gecv.struct.wavelet.*;


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

		return new WaveletDescription<WlCoef_F32>(new BorderIndex1D_Wrap(),
		coef,new WlBorderCoefStandard<WlCoef_F32>(coef));
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
	 * <p>
	 * NOTE a different set of coefficients is required when computing the inverse transform.
	 * See {@Link #invertBiorthogonalJ}.
	 * </p>
	 *
	 * @param J The wavelet's degree. K = J-2.
	 * @return Description of the Daub J/K wavelet.
	 */
	public static WaveletDescription<WlCoef_F32> biorthogonal_F32( int J ) {
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

		WlBorderCoef<WlCoef_F32> inverse = invertBiorthogonalJ(forward);

		return new WaveletDescription<WlCoef_F32>(new BorderIndex1D_Wrap(),forward,inverse);

	}

	private static WlBorderCoef<WlCoef_F32> invertBiorthogonalJ( WlCoef_F32 coef ) {

		WlCoef_F32 inner = computeInnerInverseBiorthogonal(coef);

		return new WlBorderCoefStandard<WlCoef_F32>(inner);
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
	 * Integer version of {@link #biorthogonal_F32}.  Use {@link #invertBiorthogonalJ}
	 * when computing the inverse transform.
	 *
	 * @param J The wavelet's degree. K = J-2.
	 * @return Description of the Daub J/K wavelet.
	 */
	public static WaveletDescription<WlCoef_I32> biorthogonal_I32( int J ) {
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

		WlBorderCoef<WlCoef_I32> inverse = invertBiorthogonalJ(forward);

		return new WaveletDescription<WlCoef_I32>(new BorderIndex1D_Wrap(),forward,inverse);
	}

	/**
	 * Computes the inverse of a biorthogonal wavelet.
	 *
	 * @param coef
	 * @return
	 */
	private static WlBorderCoef<WlCoef_I32> invertBiorthogonalJ( WlCoef_I32 coef ) {

		WlCoef_I32 inner = computeInnerBiorthogonal(coef);

		return new WlBorderCoefStandard<WlCoef_I32>(inner);
	}

	private static WlCoef_I32 computeInnerBiorthogonal(WlCoef_I32 coef) {
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
}
