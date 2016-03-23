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

import boofcv.struct.image.GrayF64;

/**
 * Direct implementation of a Fourier transform.  Horribly inefficient.
 *
 * @author Peter Abeles
 */
public class FourierTransformNaive_F64 {

	public static double PI2 = 2.0*Math.PI;

	public static void forward( double inputR[] , double outputR[] , double outputI[] , int start , int length ) {
		for( int k = 0; k < length; k++ ) {

			double real = 0;
			double img = 0;

			for( int l = 0; l < length; l++ ) {
				double theta = -PI2*l*k/(double)length;

				real += inputR[start+l]*Math.cos(theta);
				img += inputR[start+l]*Math.sin(theta);

			}

			outputR[start+k] = real/length;
			outputI[start+k] = img/length;
		}
	}

	public static void inverse( double inputR[] , double inputI[] , double outputR[] , int start , int length ) {
		for( int k = 0; k < length; k++ ) {

			double real = 0;

			for( int l = 0; l < length; l++ ) {
				double theta = PI2*l*k/(double)length;

				double ra = Math.cos(theta);
				double ia = Math.sin(theta);

				double rb = inputR[start+l];
				double ib = inputI[start+l];

				real += ra*rb - ia*ib;
				// assume imaginary component is zero
			}

			outputR[start+k] = real;
		}
	}

	public static void transform( boolean forward  ,
								  double inputR[] , double inputI[],
								  double outputR[] , double outputI[] ,
								  int start , int length )
	{

		double coef = PI2/(double)length;
		if( forward )
			coef = -coef;

		for( int k = 0; k < length; k++ ) {

			double real = 0;
			double img = 0;

			for( int l = 0; l < length; l++ ) {
				double theta = coef*l*k;

				double ra = Math.cos(theta);
				double ia = Math.sin(theta);

				double rb = inputR[start+l];
				double ib = inputI[start+l];

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

	public static void forward(GrayF64 inputR , GrayF64 outputR , GrayF64 outputI ) {

		GrayF64 tempR = new GrayF64(inputR.width,inputR.height);
		GrayF64 tempI = new GrayF64(outputI.width,outputI.height);

		for( int y = 0; y < inputR.height; y++ ) {
			int index = inputR.startIndex + inputR.stride*y;
			forward( inputR.data , tempR.data , tempI.data , index , inputR.width);
		}

		double columnR0[] = new double[ inputR.height ];
		double columnI0[] = new double[ inputR.height ];

		double columnR1[] = new double[ inputR.height ];
		double columnI1[] = new double[ inputR.height ];

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

	public static void inverse(GrayF64 inputR , GrayF64 inputI, GrayF64 outputR ) {
		GrayF64 tempR = new GrayF64(inputR.width,inputR.height);
		GrayF64 tempI = new GrayF64(inputI.width,inputI.height);

		for( int y = 0; y < inputR.height; y++ ) {
			int index = inputR.startIndex + inputR.stride*y;
			transform(false,inputR.data , inputI.data, tempR.data , tempI.data,index,inputR.width);
		}

		double columnR0[] = new double[ inputR.height ];
		double columnI0[] = new double[ inputR.height ];

		double columnR1[] = new double[ inputR.height ];

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
