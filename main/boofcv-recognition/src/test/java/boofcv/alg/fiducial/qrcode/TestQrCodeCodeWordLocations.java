/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.qrcode;

import georegression.struct.point.Point2D_I32;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestQrCodeCodeWordLocations {

	/**
	 * Used specification example to test bit ordering
	 */
	@Test
	public void manualCheckOfBitOrderVersion2() {
		QrCodeCodeWordLocations mask = setup(2);

		// Module D11
		assertEquals(0,mask.bits.get(8*10).distance2(20,11));
		assertEquals(0,mask.bits.get(8*10+1).distance2(19,11));
		assertEquals(0,mask.bits.get(8*10+2).distance2(20,10));
		assertEquals(0,mask.bits.get(8*10+3).distance2(19,10));
		assertEquals(0,mask.bits.get(8*10+4).distance2(20,9));
		assertEquals(0,mask.bits.get(8*10+5).distance2(19,9));
		assertEquals(0,mask.bits.get(8*10+6).distance2(18,9));
		assertEquals(0,mask.bits.get(8*10+7).distance2(17,9));

		// Module D14
		assertEquals(0,mask.bits.get(8*14).distance2(16,22));
		assertEquals(0,mask.bits.get(8*14+1).distance2(15,22));
		assertEquals(0,mask.bits.get(8*14+2).distance2(16,21));
		assertEquals(0,mask.bits.get(8*14+3).distance2(15,21));
		assertEquals(0,mask.bits.get(8*14+4).distance2(15,20));
		assertEquals(0,mask.bits.get(8*14+5).distance2(15,19));
		assertEquals(0,mask.bits.get(8*14+6).distance2(15,18));
		assertEquals(0,mask.bits.get(8*14+7).distance2(15,17));

		// Module D13
		assertEquals(0,mask.bits.get(8*13).distance2(18,23));
		assertEquals(0,mask.bits.get(8*13+1).distance2(17,23));
		assertEquals(0,mask.bits.get(8*13+2).distance2(18,24));
		assertEquals(0,mask.bits.get(8*13+3).distance2(17,24));
		assertEquals(0,mask.bits.get(8*13+4).distance2(16,24));
		assertEquals(0,mask.bits.get(8*13+5).distance2(15,24));
		assertEquals(0,mask.bits.get(8*13+6).distance2(16,23));
		assertEquals(0,mask.bits.get(8*13+7).distance2(15,23));
	}

	/**
	 * See if the codewords fill in all the bits which are not filled by a pattern
	 */
	@Test
	public void codeWordsFillAll() {
		for (int version = 2; version <= 40; version++) {
			codeWordsFillAll(version);
			break;
		}
	}

	public void codeWordsFillAll( int version ) {
		QrCodeCodeWordLocations mask = setup(version);

		for(Point2D_I32 c : mask.bits ) {
			mask.set(c.y,c.x,true);
		}

//		print(mask);
//		printCodeWordsBits(mask);
//		printCodeWords(mask);

		assertEquals( 0, countFalse(mask));
	}

	private QrCodeCodeWordLocations setup(int version) {
		return new QrCodeCodeWordLocations(version);
	}

	void printCodeWordsBits( QrCodeCodeWordLocations alg ) {
		int N = alg.numRows;
		int [][]m = new int[N][N];

		for( int i = 0; i < alg.bits.size(); i++) {
				Point2D_I32 c = alg.bits.get(i);
				m[c.y][c.x] = (i%8)+1;
		}

		System.out.println("Shape "+N+" "+N);
		for (int row = 0; row < N; row++) {
			for (int col = 0; col < N; col++) {
				if( m[row][col] == 0 )
					if( alg.get(row,col) )
						System.out.print("-");
					else
						System.out.print("_");
				else
					System.out.print(""+m[row][col]);
			}
			System.out.println();
		}
	}

	void printCodeWords( QrCodeCodeWordLocations alg ) {
		int N = alg.numRows;
		char [][]m = new char[N][N];

		for( int i = 0; i < alg.bits.size(); i++) {
			Point2D_I32 c = alg.bits.get(i);
			m[c.y][c.x] = (char)((i/8)+48);
		}

		System.out.println("Shape "+N+" "+N);
		for (int row = 0; row < N; row++) {
			for (int col = 0; col < N; col++) {
				if( m[row][col] == 0 )
					if( alg.get(row,col) )
						System.out.print("-");
					else
						System.out.print("_");
				else
					System.out.print(""+m[row][col]);
			}
			System.out.println();
		}
	}


	/**
	 * Compare the number of modules available to store data against the specification
	 */
	@Test
	public void dataCapability() {
		dataCapability(1,208);
		dataCapability(2,359);
		dataCapability(3,567);
		dataCapability(4,807);
		dataCapability(5,1079);
		dataCapability(6,1383);
		dataCapability(7,1568);
		dataCapability(9,2336);
		dataCapability(20,8683);
		dataCapability(40,29648);
	}

	private void dataCapability(int version , int expected ) {
		QrCodeCodeWordLocations mask = setup(version);
		int N = mask.numRows*mask.numCols - mask.sum();
		assertEquals(expected,N);
	}

	private int countFalse(QrCodeCodeWordLocations mask) {
		return mask.getNumElements()-mask.sum();
	}
}