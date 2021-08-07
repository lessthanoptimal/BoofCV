/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.DogArray_F32;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestQrCodeBinaryGridReader extends BoofStandardJUnit {

	/**
	 * Create a perfect undistorted image and read from it
	 */
	@Test void simpleChecks() {
		QrCodeGeneratorImage generator = new QrCodeGeneratorImage(4);
		int border = generator.borderModule*4;
		QrCode qr = new QrCodeEncoder().addAlphanumeric("HI1234").fixate();
		generator.render(qr);
		QrCodeBinaryGridReader<GrayU8> reader = new QrCodeBinaryGridReader<>(GrayU8.class);

		reader.setImage(generator.getGray());
		reader.setMarker(qr);

		// check coordinate transforms
		Point2D_F32 pixel = new Point2D_F32();
		Point2D_F32 grid = new Point2D_F32();

		reader.imageToGrid(border + 4*6 + 1, border + 4*10 + 1, grid);
		assertEquals(10.25, grid.y, 0.1);
		assertEquals(6.25, grid.x, 0.1);

		reader.gridToImage(10, 6, pixel);
		assertEquals(border + 10*4, pixel.y, 0.1);
		assertEquals(border + 6*4, pixel.x, 0.1);

		// check reading of bits
		QrCodeMaskPattern mask = qr.mask;
		List<Point2D_I32> locations = QrCode.LOCATION_BITS[qr.version];
		PackedBits8 bits = PackedBits8.wrap(qr.rawbits, qr.rawbits.length*8);

		for (int i = 0; i < bits.size; i++) {
			Point2D_I32 p = locations.get(i);
			int value = mask.apply(p.y, p.x, reader.readBit(p.y, p.x));
			assertEquals(value, bits.get(i));
		}
	}

	/**
	 * See if imageToGrid behaves as expected. Coordinates it gets should already be undistorted. So it should not
	 * be applying undistort twice or adding distortion.
	 *
	 * This also acts as a less trivial version of the previous unit test
	 */
	@Test void imageToGrid_withLensDistortion() {
		QrCodeDistortedChecks helper = createDistortionCheck();
		double r = helper.r;
		int N = helper.qr.getNumberOfModules();

		QrCodeBinaryGridReader<GrayF32> reader = new QrCodeBinaryGridReader<>(GrayF32.class);

		reader.setLensDistortion(helper.image.width, helper.image.height, helper.distortion);
		reader.setImage(helper.image);
		reader.setMarker(helper.qr);

		Point2D_F64 pixel = new Point2D_F64();
		Point2D_F32 found = new Point2D_F32();

		// grid coordinate system has its origin bottom left corner
		double tol = 0.05; // could be even more precise
		helper.simulator.computePixel(0, -r, r, pixel);
		helper.p2p.compute(pixel.x, pixel.y, pixel);
		reader.imageToGrid((float)pixel.x, (float)pixel.y, found);
		assertEquals(0, found.x, tol);
		assertEquals(0, found.y, tol);

		helper.simulator.computePixel(0, -r, -r, pixel);
		helper.p2p.compute(pixel.x, pixel.y, pixel);
		reader.imageToGrid((float)pixel.x, (float)pixel.y, found);
		assertEquals(0, found.x, tol);
		assertEquals(N, found.y, tol);

		helper.simulator.computePixel(0, r, r, pixel);
		helper.p2p.compute(pixel.x, pixel.y, pixel);
		reader.imageToGrid((float)pixel.x, (float)pixel.y, found);
		assertEquals(N, found.x, tol);
		assertEquals(0, found.y, tol);
	}

	/**
	 * To read a bit lens distortion needs to be removed.
	 */
	@Test void readBit_withLensDistortion() {
		QrCodeDistortedChecks helper = createDistortionCheck();
		QrCode qr = helper.qr;

		QrCodeBinaryGridReader<GrayF32> reader = new QrCodeBinaryGridReader<>(GrayF32.class);
		reader.setImage(helper.image);
		reader.setMarker(helper.qr);

//		ShowImages.showWindow(helper.image,"ASDASDASD");
//		BoofMiscOps.sleep(10_000);

		// Should fail without the distortion model
		assertTrue(countCorrectRead(qr, reader) < 1.0);

		// should work now
		reader.setLensDistortion(helper.image.width, helper.image.height, helper.distortion);
		reader.setImage(helper.image);
		reader.setMarker(helper.qr);
		assertEquals(countCorrectRead(qr, reader), 1.0);
	}

	/**
	 * Checks to see if the expected number of points was read
	 */
	@Test void readBitIntensity_count() {
		DogArray_F32 intensity = new DogArray_F32();
		// add a value to it to make sure it's not cleared
		intensity.add(5);

		QrCodeDistortedChecks helper = createDistortionCheck();

		QrCodeBinaryGridReader<GrayF32> reader = new QrCodeBinaryGridReader<>(GrayF32.class);
		reader.setImage(helper.image);
		reader.setMarker(helper.qr);

		reader.readBitIntensity(10, 11, intensity);

		// Make sure the expected number of points was read
		// If that changes you need to hunt down places '5' was hard coded. Sorry for that.
		assertEquals(1 + QrCodeBinaryGridReader.BIT_INTENSITY_SAMPLES, intensity.size);
	}

	private double countCorrectRead( QrCode qr, QrCodeBinaryGridReader<GrayF32> reader ) {
		QrCodeMaskPattern mask = qr.mask;
		List<Point2D_I32> locations = QrCode.LOCATION_BITS[qr.version];
		PackedBits8 bits = PackedBits8.wrap(qr.rawbits, qr.rawbits.length*8);

		int success = 0;
		for (int i = 0; i < bits.size; i++) {
			Point2D_I32 p = locations.get(i);
			int value = mask.apply(p.y, p.x, reader.readBit(p.y, p.x));
			if (value == bits.get(i))
				success++;
		}
		return success/(double)bits.size;
	}

	private QrCodeDistortedChecks createDistortionCheck() {
		// Will render an image (not needed) but also computed all the distortion parameters (which we need)
		QrCodeDistortedChecks helper = new QrCodeDistortedChecks();

		helper.render();

		helper.locateQrFeatures();

		return helper;
	}
}
