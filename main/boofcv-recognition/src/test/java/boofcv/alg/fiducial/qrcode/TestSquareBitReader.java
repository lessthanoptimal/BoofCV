/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.GrayU8;
import georegression.struct.shapes.Polygon2D_F64;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestSquareBitReader {

	int block = 2;
	GrayU8 gray = new GrayU8(200,230);

	@Before
	public void before() {
		ImageMiscOps.fill(gray,200);
	}

	/**
	 * Paints the square black. Everything else is white
	 */
	@Test
	public void squareIsBlack() {
		ImageMiscOps.fillRectangle(gray,0,50,60,7*block,7*block);

		SquareBitReader<GrayU8> reader = new SquareBitReader<>(GrayU8.class);
		reader.setImage(gray);

		// square detector will return a square which contains all the black pixel's area,
		// which is why this is expanded. In integer coordinates black from 0 to 7*block-1.
		// Detected rect will be 0 to 7*block
		Polygon2D_F64 s = TestQrCodePositionPatternDetector.square(50,60,7*block);
		reader.setSquare(s,100);

		for (int i = -1; i < 8; i++) {
			for (int j = -1; j < 8; j++) {
				System.out.println("(i,j) = ("+i+","+j+")");
				if( i >= 0 && i < 7 && j >= 0 && j < 7 ) {
					assertEquals(0, reader.read(i, j));
				} else {
					assertEquals(1, reader.read(i, j));
				}
			}
		}
	}

	@Test
	public void readBytesAroundSquare() {
		ImageMiscOps.fillRectangle(gray,0,50,60,7*block,7*block);

		SquareBitReader<GrayU8> reader = new SquareBitReader<>(GrayU8.class);
		reader.setImage(gray);

		Polygon2D_F64 s = TestQrCodePositionPatternDetector.square(50,60,7*block);
		reader.setSquare(s,100);

		assertEquals(1, reader.read(-1, 3));
		assertEquals(1, reader.read(8, 8));

		setBit(-1,3);
		setBit(8,8);

		assertEquals(0, reader.read(-1, 3));
		assertEquals(0, reader.read(8, 8));
	}

	private void setBit( int row , int col ) {
		ImageMiscOps.fillRectangle(gray,0,50+col*block,60+row*block,block,block);
	}


	@Test
	public void outsideImageRead() {
		SquareBitReader<GrayU8> reader = new SquareBitReader<>(GrayU8.class);
		reader.setImage(gray);

		assertEquals(-1,reader.read(-1000,0));
		assertEquals(-1,reader.read(1000,0));
		assertEquals(-1,reader.read(0,1000));
		assertEquals(-1,reader.read(0,-1000));
	}



}