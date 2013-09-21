/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.transform.fft;

import boofcv.struct.image.ImageFloat32;

/**
 * Direct implementation of a Fourier transform.  Horribly inefficient.
 *
 * @author Peter Abeles
 */
public class FourierTransformNaive {

	public static float PI2 = (float)(2.0*Math.PI);

	public static void forward( float inputR[] , float outputR[] , float outputI[] , int start , int length ) {
		for( int k = 0; k < length; k++ ) {

			float real = 0;
			float img = 0;

			for( int l = 0; l < length; l++ ) {
				float theta = -PI2*l*k/(float)length;

				real += inputR[start+l]*Math.cos(theta);
				img += inputR[start+l]*Math.sin(theta);

			}

			outputR[k] = real/length;
			outputI[k] = img/length;
		}
	}

	public static void inverse( float inputR[] , float inputI[] , float outputR[] , int start , int length ) {
		for( int k = 0; k < length; k++ ) {

			float real = 0;

			for( int l = 0; l < length; l++ ) {
				float theta = PI2*l*k/(float)length;

				float ra = (float)Math.cos(theta);
				float ia = (float)Math.sin(theta);

				float rb = inputR[start+l];
				float ib = inputI[start+l];

				real += ra*rb - ia*ib;
				// assume imaginary component is zero
			}

			outputR[k] = real;
		}
	}

	public static void forward( float inputR[] , float inputI[], float outputR[] , float outputI[] , int start , int length ) {
		for( int k = 0; k < length; k++ ) {

			float real = 0;
			float img = 0;

			for( int l = 0; l < length; l++ ) {
				float theta = -PI2*l*k/(float)length;

				float ra = (float)Math.cos(theta);
				float ia = (float)Math.sin(theta);

				float rb = inputR[start+l];
				float ib = inputI[start+l];

				real += ra*rb - ia*ib;
				img += ia*rb + ra*ib;
			}

			outputR[k] = real/length;
			outputI[k] = img/length;
		}
	}

	public static void inverse( float inputR[] , float inputI[], float outputR[] , float outputI[] , int start , int length ) {
		for( int k = 0; k < length; k++ ) {

			float real = 0;
			float img = 0;

			for( int l = 0; l < length; l++ ) {
				float theta = PI2*l*k/(float)length;

				float ra = (float)Math.cos(theta);
				float ia = (float)Math.sin(theta);

				float rb = inputR[start+l];
				float ib = inputI[start+l];

				real += ra*rb - ia*ib;
				img += ia*rb + ra*ib;
			}

			outputR[k] = real;
			outputI[k] = img;
		}
	}
	
	public static void forward( ImageFloat32 inputR , ImageFloat32 outputR , ImageFloat32 outputI ) {

		ImageFloat32 tempR = new ImageFloat32(inputR.width,inputR.height);
		ImageFloat32 tempI = new ImageFloat32(outputI.width,outputI.height);

		for( int y = 0; y < inputR.height; y++ ) {
			int index = inputR.startIndex + inputR.stride*y;
			forward( inputR.data , tempR.data , tempI.data , index , inputR.width);
		}

		// todo transpose the images

		// again

		// transpose back

	}

	public static void inverse( ImageFloat32 inputR , ImageFloat32 inputI, ImageFloat32 outputR ) {

	}

	public static void transpose( ImageFloat32 a ) {

	}
}
