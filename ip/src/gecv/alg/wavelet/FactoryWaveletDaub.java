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
	public static WaveletDesc_F32 standard_F32( int J ) {
		if( J != 4 ) {
			throw new IllegalArgumentException("Only 4 is currently supported");
		}

		WaveletDesc_F32 ret = new WaveletDesc_F32();

		ret.border = new BorderIndex1D_Wrap();
		ret.offsetScaling = 0;
		ret.offsetWavelet = 0;

		ret.scaling = new float[4];
		ret.wavelet = new float[4];

		double sqrt3 = Math.sqrt(3);
		double div = 4.0*Math.sqrt(2);
		ret.scaling[0] = (float)((1+sqrt3)/div);
		ret.scaling[1] = (float)((3+sqrt3)/div);
		ret.scaling[2] = (float)((3-sqrt3)/div);
		ret.scaling[3] = (float)((1-sqrt3)/div);

		ret.wavelet[0] = ret.scaling[3];
		ret.wavelet[1] = -ret.scaling[2];
		ret.wavelet[2] = ret.scaling[1];
		ret.wavelet[3] = -ret.scaling[0];

		return ret;
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
	 * See {@Link #biorthogonalInv_F32}.
	 * </p>
	 *
	 * @param J The wavelet's degree. K = J-2.
	 * @return Description of the Daub J/K wavelet.
	 */
	public static WaveletDesc_F32 biorthogonal_F32( int J ) {
		if( J != 5 ) {
			throw new IllegalArgumentException("Only 5 is currently supported");
		}

		WaveletDesc_F32 ret = new WaveletDesc_F32();

		ret.border = new BorderIndex1D_Wrap();
		ret.offsetScaling = -2;
		ret.offsetWavelet = 0;

		ret.scaling = new float[5];
		ret.wavelet = new float[3];

		ret.scaling[0] = (float)(-1.0/8.0);
		ret.scaling[1] = (float)(2.0/8.0);
		ret.scaling[2] = (float)(6.0/8.0);
		ret.scaling[3] = (float)(2.0/8.0);
		ret.scaling[4] = (float)(-1.0/8.0);

		ret.wavelet[0] = -1.0f/2.0f;
		ret.wavelet[1] = 1;
		ret.wavelet[2] = -1.0f/2.0f;

		return ret;
	}

	/**
	 * Integer version of {@link #biorthogonal_F32}.  Use {@link #biorthogonalInv_I32}
	 * when computing the inverse transform.
	 *
	 * @param J The wavelet's degree. K = J-2.
	 * @return Description of the Daub J/K wavelet.
	 */
	public static WaveletDesc_I32 biorthogonal_I32( int J ) {
		if( J != 5 ) {
			throw new IllegalArgumentException("Only 5 is currently supported");
		}

		WaveletDesc_I32 ret = new WaveletDesc_I32();

		ret.border = new BorderIndex1D_Wrap();
		ret.offsetScaling = -2;
		ret.offsetWavelet = 0;

		ret.scaling = new int[5];
		ret.wavelet = new int[3];

		ret.denominatorScaling = 8;
		ret.scaling[0] = -1;
		ret.scaling[1] = 2;
		ret.scaling[2] = 6;
		ret.scaling[3] = 2;
		ret.scaling[4] = -1;

		ret.denominatorWavelet = 2;
		ret.wavelet[0] = -1;
		ret.wavelet[1] = 2;
		ret.wavelet[2] = -1;

		return ret;
	}

	/**
	 * See {@link #biorthogonal_F32} for more information.  Used when
	 * computing the inverse transformation.
	 *
	 * @param J The wavelet's degree. K = J-2.
	 * @return Description of the Daub J/K wavelet.
	 */
	public static WaveletDesc_F32 biorthogonalInv_F32( int J ) {
		if( J != 5 ) {
			throw new IllegalArgumentException("Only 5 is currently supported");
		}

		WaveletDesc_F32 ret = new WaveletDesc_F32();

		ret.border = new BorderIndex1D_Wrap();
		ret.offsetScaling = -1;
		ret.offsetWavelet = -1;

		ret.scaling = new float[3];
		ret.wavelet = new float[5];

		ret.scaling[0] = 1.0f/2.0f;
		ret.scaling[1] = 1;
		ret.scaling[2] = 1.0f/2.0f;

		ret.wavelet[0] = (float)(-1.0/8.0);
		ret.wavelet[1] = (float)(-2.0/8.0);
		ret.wavelet[2] = (float)(6.0/8.0);
		ret.wavelet[3] = (float)(-2.0/8.0);
		ret.wavelet[4] = (float)(-1.0/8.0);
		
		return ret;
	}

	/**
	 * See {@link #biorthogonal_I32} for more information.  Used when
	 * computing the inverse transformation.
	 *
	 * @param J The wavelet's degree. K = J-2.
	 * @return Description of the Daub J/K wavelet.
	 */
	public static WaveletDesc_I32 biorthogonalInv_I32( int J ) {
		if( J != 5 ) {
			throw new IllegalArgumentException("Only 5 is currently supported");
		}

		WaveletDesc_I32 ret = new WaveletDesc_I32();

		ret.border = new BorderIndex1D_Wrap();
		ret.offsetScaling = -1;
		ret.offsetWavelet = -1;

		ret.scaling = new int[3];
		ret.wavelet = new int[5];

		ret.denominatorScaling = 2;
		ret.scaling[0] = 1;
		ret.scaling[1] = 2;
		ret.scaling[2] = 1;

		ret.denominatorWavelet = 8;
		ret.wavelet[0] = -1;
		ret.wavelet[1] = -2;
		ret.wavelet[2] = 6;
		ret.wavelet[3] = -2;
		ret.wavelet[4] = -1;

		return ret;
	}
}
