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

import boofcv.alg.distort.PointTransformHomography_F32;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_F32;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestQrCodeAlignmentPatternLocator {

	/**
	 * Everything all together on a simple code
	 */
	@Test
	public void simple() {
		QrCode qr = new QrCode();
		qr.version = 7;

		QrCodeGeneratorImage generator = new QrCodeGeneratorImage(4);
		generator.render(qr);

		QrCodeAlignmentPatternLocator<GrayU8> alg = new QrCodeAlignmentPatternLocator<>(GrayU8.class);
		assertTrue(alg.process(generator.gray,qr));

		assertEquals(6,qr.alignment.size);
	}

	/**
	 * Give it bad guess but all within the white or black dot
	 */
	@Test
	public void centerOnSquare() {
		QrCode qr = new QrCode();
		qr.version = 2;

		centerOnSquare(qr,4);
		centerOnSquare(qr,1);
	}

	public void centerOnSquare( QrCode qr, int scale ) {
		QrCodeGeneratorImage generator = new QrCodeGeneratorImage(scale);
		generator.render(qr);

		QrCodeAlignmentPatternLocator<GrayU8> alg = new QrCodeAlignmentPatternLocator<>(GrayU8.class);
		alg.interpolate.setImage(generator.gray);
		alg.initializePatterns(qr);
		alg.computeHomography(qr);

		QrCode.Alignment a = qr.alignment.get(0);

		// Offset it from truth exhaustively within the white square boundary
		for (int i = 0; i < 5; i++) {
			float noiseY = -1f + 2f*i/4f; // should the range be -1.5 to 1.5?

			for (int j = 0; j < 5; j++) {
				float noiseX = -1f + 2f*j/4f;

				assertTrue(alg.centerOnSquare(a,a.moduleX+0.5f+noiseX,a.moduleY+0.5f+noiseY));

				// samples +- 1 around the center. can't get closer than 0.5 squares. Could be off up to 0.999
				assertEquals((a.moduleX+0.5)*scale,a.pixel.x,0.5*scale);
				assertEquals((a.moduleY+0.5)*scale,a.pixel.y,0.5*scale);
			}
		}

	}

	@Test
	public void localize() {
		QrCode qr = new QrCode();
		qr.version = 2;
		localize(qr, 4);
	}

	private void localize(QrCode qr, int scale) {
		QrCodeGeneratorImage generator = new QrCodeGeneratorImage(scale);
		generator.render(qr);

		QrCodeAlignmentPatternLocator<GrayU8> alg = new QrCodeAlignmentPatternLocator<>(GrayU8.class);
		alg.interpolate.setImage(generator.gray);
		alg.initializePatterns(qr);
		alg.computeHomography(qr);

		QrCode.Alignment a = qr.alignment.get(0);
		assertTrue(alg.localize(a,a.moduleX+0.5f,a.moduleY+0.5f));

		assertEquals(a.moduleX+0.5,a.moduleFound.x,0.5);
		assertEquals(a.moduleY+0.5,a.moduleFound.y,0.5);

		assertEquals((a.moduleX+0.5)*scale,a.pixel.x,0.5);
		assertEquals((a.moduleY+0.5)*scale,a.pixel.y,0.5);
	}

	/**
	 * The smallest possible configuration for a QR code.  See if it still works
	 */
	@Test
	public void localize_OnePixelModules() {
		QrCode qr = new QrCode();
		qr.version = 2;

		localize(qr, 1);
	}


	@Test
	public void greatestDown() {
		float values[] = new float[]{200,210,190,20,25,18,0,255,255,255};

		assertEquals(3,QrCodeAlignmentPatternLocator.greatestDown(values));
	}

	@Test
	public void greatestUp() {
		float values[] = new float[]{200,0,255,20,25,18,0,200,255,255};
		assertEquals(6,QrCodeAlignmentPatternLocator.greatestUp(values,3));
	}

	@Test
	public void initializePatterns() {
		QrCodeAlignmentPatternLocator<GrayU8> alg = new QrCodeAlignmentPatternLocator<>(GrayU8.class);

		QrCode qr = new QrCode();
		qr.version = 2;

		alg.initializePatterns(qr);
		assertEquals(1,qr.alignment.size);
		assertEquals(25-7,qr.alignment.get(0).moduleX);
		assertEquals(25-7,qr.alignment.get(0).moduleY);

		qr.reset();qr.version=7;
		alg.initializePatterns(qr);
		assertEquals(6,qr.alignment.size);
		assertEquals(22,qr.alignment.get(0).moduleX);
		assertEquals(6,qr.alignment.get(0).moduleY);
		assertEquals(6,qr.alignment.get(1).moduleX);
		assertEquals(22,qr.alignment.get(1).moduleY);
		assertEquals(22,qr.alignment.get(2).moduleX);
		assertEquals(22,qr.alignment.get(2).moduleY);
	}

	@Test
	public void computeHomography() {
		QrCode truthQr = new QrCode();
		truthQr.version = 2;
		QrCodeGeneratorImage generator = new QrCodeGeneratorImage(4);
		generator.render(truthQr);

		QrCodeAlignmentPatternLocator<GrayU8> alg = new QrCodeAlignmentPatternLocator<>(GrayU8.class);

		assertTrue(alg.computeHomography(truthQr));

		check(alg.gridToImage,0,0,0,0);
		check(alg.gridToImage,7,0,7*4,0);
		check(alg.gridToImage,7,7,7*4,7*4);

	}

	private void check(PointTransformHomography_F32 gridToImage ,
					   float x , float y ,
					   float expectedX , float expectedY )
	{
		Point2D_F32 found = new Point2D_F32();

		gridToImage.compute(x,y,found);

		assertEquals(expectedX,found.x,1e-4f);
		assertEquals(expectedY,found.y,1e-4f);
	}
}
