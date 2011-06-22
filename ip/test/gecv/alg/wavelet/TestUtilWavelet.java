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

import gecv.struct.image.ImageFloat32;
import gecv.struct.wavelet.WaveletCoefficient_F32;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


/**
 * @author Peter Abeles
 */
public class TestUtilWavelet {
	@Test
	public void checkShape_positive() {
		ImageFloat32 orig = new ImageFloat32(10,20);
		ImageFloat32 transformed = new ImageFloat32(10,20);

		UtilWavelet.checkShape(orig,transformed);

		orig = new ImageFloat32(9,19);
		UtilWavelet.checkShape(orig,transformed);
	}

	@Test
	public void checkShape_negative() {
		ImageFloat32 orig = new ImageFloat32(9,19);
		ImageFloat32 transformed = new ImageFloat32(19,19);

		try {
		UtilWavelet.checkShape(orig,transformed);
			fail("transformed image can't be odd");
		} catch( IllegalArgumentException e ){}

		orig = new ImageFloat32(12,22);
		transformed = new ImageFloat32(10,20);

		try {
		UtilWavelet.checkShape(orig,transformed);
			fail("Image are not compatible shapes");
		} catch( IllegalArgumentException e ){}
	}

	@Test
	public void computeEnergy_F32() {
		float[] a = new float[]{-2,3,4.67f,7,-10.2f};

		assertEquals(187.8489f,UtilWavelet.computeEnergy(a),1e-4f);
	}

	@Test
	public void computeEnergy_I32() {
		int[] a = new int[]{-2,3,4,7,-10};

		assertEquals(11.125f,UtilWavelet.computeEnergy(a,4),1e-4f);
	}

	@Test
	public void sumCoefficients_F32() {
		float[] a = new float[]{-2,3,4,7,-10};

		assertEquals(2f,UtilWavelet.sumCoefficients(a),1e-4f);
	}

	@Test
	public void computeBorderEnd() {
		int O[][]=new int[][]{
				{0,1,0},{0,2,0},{0,3,2},{0,4,2},{0,5,4},{0,6,4},
				{-1,2,0},{-1,3,0},{-1,4,2},{-1,5,2},{-1,6,4},{-1,7,4},
				{-2,3,0},{-2,4,0},{-2,5,2},{-2,6,2},{-2,7,4}};

		for (int[] w : O) {
			checkEnd(w[0], 0, w[1], 1, 8, 8 , w[2]);
			checkEnd(0, w[0], 1, w[1], 8, 8 , w[2]);
		}

		// check odd lengths
		O = new int[][]{{0,1,0},{0,2,2},{0,3,2},{0,4,4}};
		for (int[] w : O) {
			checkEnd(w[0], 0, w[1], 1, 7, 8 , w[2]);
			checkEnd(0, w[0], 1, w[1], 7, 8 , w[2]);
		}
	}

	private void checkEnd( int offsetA , int offsetB ,
						   int lengthA , int lengthB ,
						   int imgLength ,
						   int tranLength ,
						   int expected ) {
		WaveletCoefficient_F32 desc = new WaveletCoefficient_F32();
		desc.offsetScaling = offsetA;
		desc.offsetWavelet = offsetB;
		desc.scaling = new float[lengthA];
		desc.wavelet = new float[lengthB];

		assertEquals(expected,UtilWavelet.computeBorderEnd(desc,imgLength,tranLength));
	}

	@Test
	public void computeBorderStart() {
		checkStart(0,0,0);
		checkStart(-1,0,2);
		checkStart(0,-1,2);
		checkStart(-2,0,2);
		checkStart(0,-2,2);
		checkStart(-3,0,4);
		checkStart(0,-3,4);
		checkStart(-3,0,4);
		checkStart(-4,-4,4);
	}

	private void checkStart( int offsetA , int offsetB ,
							int expected ) {
		WaveletCoefficient_F32 desc = new WaveletCoefficient_F32();
		desc.offsetScaling = offsetA;
		desc.offsetWavelet = offsetB;

		assertEquals(expected,UtilWavelet.computeBorderStart(desc));
	}
}
