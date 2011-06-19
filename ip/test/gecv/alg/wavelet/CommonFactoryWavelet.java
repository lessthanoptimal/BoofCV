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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * Common testing functions for wavelet factories
 *
 * @author Peter Abeles
 */
public class CommonFactoryWavelet {
	/**
	 * Computes the dot product of two wavelets separated by different offsets.  If
	 * the offset is zero and they have an orthogonal/biorothogonal relationship then
	 * the dot product should be one.  Otherwise it will be zero.
	 */
	public static void checkBiorthogonal( int offset ,
										  float supportA[] , int startA ,
										  float supportB[] , int startB )
	{
		int length = Math.max(supportA.length,supportB.length)+offset;

		float valA[] = new float[length];
		float valB[] = new float[length];

		int m = Math.min(startA,startB);

		for( int i = 0; i < supportA.length; i++ ) {
			valA[i+startA-m] = supportA[i];
		}

		for( int i = 0; i < supportB.length; i++ ) {
			valB[i+startB-m+offset] = supportB[i];
		}

		double total = 0;

		for( int i = 0; i < length; i++ ) {
			total += valA[i]*valB[i];
		}

		if( offset == 0 )
			assertEquals(1.0,total,1e-4);
		else
			assertEquals(0.0,total,1e-4);
	}

	/**
	 * Computes the dot product of two wavelets separated by different offsets.  If
	 * the offset is zero and they have an orthogonal/biorothogonal relationship then
	 * the dot product should be one.  Otherwise it will be zero.
	 *
	 * @param strict When offset is zero should the dot product be one or just not zero.
	 */
	public static void checkBiorthogonal(int offset,
										 int supportA[], int startA, int divA,
										 int supportB[], int startB, int divB, boolean strict)
	{
		int length = Math.max(supportA.length,supportB.length)+offset;

		int valA[] = new int[length];
		int valB[] = new int[length];

		int m = Math.min(startA,startB);

		for( int i = 0; i < supportA.length; i++ ) {
			valA[i+startA-m] = supportA[i];
		}

		for( int i = 0; i < supportB.length; i++ ) {
			valB[i+startB-m+offset] = supportB[i];
		}

		int total = 0;

		for( int i = 0; i < length; i++ ) {
			total += valA[i]*valB[i];
		}

		if( offset == 0 ) {
			if( strict )
				assertEquals(divA*divB,total);
			else
				assertTrue(total != 1);
		} else
			assertEquals(0,total);
	}

	public static void checkPolySumToZero(float support[], int polyOrder, int offset ) {
		for( int o = 1; o <= polyOrder; o++ ) {
			double total = 0;
			for( int j = 0; j < support.length; j++ ) {
				double coef = Math.pow(j+offset,o);
				total += coef*support[j];
			}
			assertEquals("Failed poly test at order "+o,0,total,1e-4);
		}
	}

	public static void checkPolySumToZero(int support[], int polyOrder, int offset ) {
		for( int o = 1; o <= polyOrder; o++ ) {
			double total = 0;
			for( int j = 0; j < support.length; j++ ) {
				double coef = Math.pow(j+offset,o);
				total += coef*support[j];
			}
			assertEquals("Failed poly test at order "+o,0,total,1e-4);
		}
	}
}
