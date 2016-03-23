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

package boofcv.alg.transform.fft;

import boofcv.struct.image.GrayF32;

/**
 * Direct implementation of a Fourier transform.  Horribly inefficient.
 *
 * @author Peter Abeles
 */
public class FourierTransformNaive_F32 {

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

			outputR[start+k] = real/length;
			outputI[start+k] = img/length;
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

			outputR[start+k] = real;
		}
	}

	public static void transform( boolean forward  ,
								  float inputR[] , float inputI[],
								  float outputR[] , float outputI[] ,
								  int start , int length )
	{

		float coef = PI2/(float)length;
		if( forward )
			coef = -coef;

		for( int k = 0; k < length; k++ ) {

			float real = 0;
			float img = 0;

			for( int l = 0; l < length; l++ ) {
				float theta = coef*l*k;

				float ra = (float)Math.cos(theta);
				float ia = (float)Math.sin(theta);

				float rb = inputR[start+l];
				float ib = inputI[start+l];

				real += ra*rb - ia*ib;
				img += ia*rb + ra*ib;
			}

			if( forward ) {
				outputR[start+k] = real/length;
				outputI[start+k] = img/length;
			} else {
				outputR[start+k] = real;
				outputI[start+k] = img;
			}
		}
	}

	public static void forward(GrayF32 inputR , GrayF32 outputR , GrayF32 outputI ) {

		GrayF32 tempR = new GrayF32(inputR.width,inputR.height);
		GrayF32 tempI = new GrayF32(outputI.width,outputI.height);

		for( int y = 0; y < inputR.height; y++ ) {
			int index = inputR.startIndex + inputR.stride*y;
			forward( inputR.data , tempR.data , tempI.data , index , inputR.width);
		}

		float columnR0[] = new float[ inputR.height ];
		float columnI0[] = new float[ inputR.height ];

		float columnR1[] = new float[ inputR.height ];
		float columnI1[] = new float[ inputR.height ];

		for( int x = 0; x < inputR.width; x++ ) {
			// copy the column
			for( int y = 0; y < inputR.height; y++ ) {
				columnR0[y] = tempR.unsafe_get(x,y);
				columnI0[y] = tempI.unsafe_get(x,y);
			}

			transform(true,columnR0,columnI0,columnR1,columnI1,0,inputR.height);

			// copy the results back
			for( int y = 0; y < inputR.height; y++ ) {
				outputR.unsafe_set(x,y,columnR1[y]);
				outputI.unsafe_set(x,y,columnI1[y]);
			}
		}
	}

	public static void inverse(GrayF32 inputR , GrayF32 inputI, GrayF32 outputR ) {
		GrayF32 tempR = new GrayF32(inputR.width,inputR.height);
		GrayF32 tempI = new GrayF32(inputI.width,inputI.height);

		for( int y = 0; y < inputR.height; y++ ) {
			int index = inputR.startIndex + inputR.stride*y;
			transform(false,inputR.data , inputI.data, tempR.data , tempI.data,index,inputR.width);
		}

		float columnR0[] = new float[ inputR.height ];
		float columnI0[] = new float[ inputR.height ];

		float columnR1[] = new float[ inputR.height ];

		for( int x = 0; x < inputR.width; x++ ) {
			// copy the column
			for( int y = 0; y < inputR.height; y++ ) {
				columnR0[y] = tempR.unsafe_get(x,y);
				columnI0[y] = tempI.unsafe_get(x,y);
			}

			inverse(columnR0,columnI0,columnR1,0,inputR.height);

			// copy the results back
			for( int y = 0; y < inputR.height; y++ ) {
				outputR.unsafe_set(x,y,columnR1[y]);
			}
		}
	}
}
