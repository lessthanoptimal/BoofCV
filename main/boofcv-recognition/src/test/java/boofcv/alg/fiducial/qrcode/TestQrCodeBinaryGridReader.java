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

import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_I32;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestQrCodeBinaryGridReader {

	/**
	 * Create a perfect undistorted image and read from it
	 */
	@Test
	public void simpleChecks() {
		QrCodeGeneratorImage generator = new QrCodeGeneratorImage(4);
		int border = generator.borderModule*4;
		QrCode qr = new QrCodeEncoder().addAlphanumeric("HI1234").fixate();
		generator.render(qr);
		QrCodeBinaryGridReader<GrayU8> reader = new QrCodeBinaryGridReader<>(GrayU8.class);

		reader.setImage(generator.gray);
		reader.setMarker(qr);

		// check coordinate transforms
		Point2D_F32 pixel = new Point2D_F32();
		Point2D_F32 grid = new Point2D_F32();

		reader.imageToGrid(border+4*6+1,border+4*10+1,grid);
		assertEquals(10.25,grid.y,0.1);
		assertEquals(6.25,grid.x,0.1);

		reader.gridToImage(10,6,pixel);
		assertEquals(border+10*4,pixel.y,0.1);
		assertEquals(border+6*4,pixel.x,0.1);

		// check reading of bits
		QrCodeMaskPattern mask = qr.mask;
		List<Point2D_I32> locations = QrCode.LOCATION_BITS[qr.version];
		PackedBits8 bits = PackedBits8.wrap(qr.rawbits,qr.rawbits.length*8);

		for (int i = 0; i < bits.size; i++) {
			Point2D_I32 p = locations.get(i);
			int value = mask.apply(p.y,p.x,reader.readBit(p.y,p.x));
			assertEquals(value,bits.get(i));
		}
	}
}