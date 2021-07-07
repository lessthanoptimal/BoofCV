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

package boofcv.alg.sfm.d2;

import boofcv.abst.sfm.d2.ImageMotion2D;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.interpolate.InterpolationType;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.factory.distort.FactoryDistort;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.border.BorderType;
import boofcv.struct.distort.PixelTransform;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.affine.Affine2D_F64;
import georegression.struct.point.Point2D_F32;
import georegression.struct.shapes.Quadrilateral_F64;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestStitchingFromMotion2D extends BoofStandardJUnit {

	GrayF32 image = new GrayF32(100, 150);
	Affine2D_F64 translation = new Affine2D_F64(1, 0, 0, 1, 1, -2);
	Affine2D_F64 motion0 = new Affine2D_F64(1, 2, 3, 4, 5, 6);

	/**
	 * Given fake internal algorithms see if it performs as expected. tests several functions
	 */
	@Test void basicTest() {
		HelperMotion motion = new HelperMotion();
		HelperDistort distort = new HelperDistort();

		StitchingTransform trans = FactoryStitchingTransform.createAffine_F64();

		StitchingFromMotion2D<GrayF32, Affine2D_F64> alg =
				new StitchingFromMotion2D<>(motion, distort, trans, 0.3);

		alg.configure(200, 300, null);
		assertTrue(alg.process(image));

		assertEquals(0, motion.numReset);
		assertEquals(1, motion.numProcess);
		assertEquals(1, distort.numSetModel);
		assertEquals(1, distort.numApply);

		assertEquals(200, alg.getStitchedImage().width);
		assertEquals(300, alg.getStitchedImage().height);
		Affine2D_F64 found = alg.getWorldToCurr();
		assertEquals(1, found.tx, 1e-5);
		assertEquals(-2, found.ty, 1e-5);

		assertTrue(alg.process(image));

		assertEquals(0, motion.numReset);
		assertEquals(2, motion.numProcess);
		assertEquals(2, distort.numSetModel);
		assertEquals(2, distort.numApply);

		found = alg.getWorldToCurr();
		assertEquals(1, found.tx, 1e-5);
		assertEquals(-2, found.ty, 1e-5);

		// test reset
		alg.reset();
		assertEquals(1, motion.numReset);
		found = alg.getWorldToCurr();
		assertEquals(0, found.tx, 1e-5);
		assertEquals(0, found.ty, 1e-5);
	}

	/**
	 * Checks to see if the user specified initial transformation is correctly applied
	 */
	@Test void checkInitialTransform() {
		HelperMotion motion = new HelperMotion();
		HelperDistort distort = new HelperDistort();

		StitchingTransform trans = FactoryStitchingTransform.createAffine_F64();

		StitchingFromMotion2D<GrayF32, Affine2D_F64> alg =
				new StitchingFromMotion2D<>(motion, distort, trans, 0.3);

		alg.configure(200, 300, motion0);
		assertTrue(alg.process(image));

		Affine2D_F64 expected = motion0.concat(translation, null);

		Affine2D_F64 found = alg.getWorldToCurr();
		assertEquals(expected.a11, found.a11, 1e-5);
		assertEquals(expected.tx, found.tx, 1e-5);
		assertEquals(expected.ty, found.ty, 1e-5);
	}

	/**
	 * Provide an extremely different transformation and see if that causes an exception
	 */
	@Test void checkMaxJump() {
		HelperMotion motion = new HelperMotion();
		HelperDistort distort = new HelperDistort();

		StitchingTransform trans = FactoryStitchingTransform.createAffine_F64();

		StitchingFromMotion2D<GrayF32, Affine2D_F64> alg =
				new StitchingFromMotion2D<>(motion, distort, trans, 0.3);

		alg.configure(200, 300, null);
		assertTrue(alg.process(image));

		// this is very different from what it had before
		motion.found = motion0;

		assertFalse(alg.process(image));
	}

	/**
	 * Note that this test does not actually check to see if the correct transform is applied
	 */
	@Test void setOriginToCurrent() {
		HelperMotion motion = new HelperMotion();
		HelperDistort distort = new HelperDistort();

		StitchingTransform trans = FactoryStitchingTransform.createAffine_F64();

		StitchingFromMotion2D<GrayF32, Affine2D_F64> alg =
				new StitchingFromMotion2D<>(motion, distort, trans, 0.3);

		alg.configure(200, 300, null);
		assertTrue(alg.process(image));

		alg.setOriginToCurrent();

		assertEquals(2, distort.numSetModel);
		assertEquals(2, distort.numApply);
	}

	@Test void resizeStitchImage_noTransform() {
		HelperMotion motion = new HelperMotion();
		HelperDistort distort = new HelperDistort();

		StitchingTransform trans = FactoryStitchingTransform.createAffine_F64();

		StitchingFromMotion2D<GrayF32, Affine2D_F64> alg =
				new StitchingFromMotion2D<>(motion, distort, trans, 0.3);

		alg.configure(200, 300, null);
		assertTrue(alg.process(image));

		ImageMiscOps.fill(alg.getStitchedImage().subimage(2, 3, 30, 40, null), 1);
		alg.resizeStitchImage(250, 400, null);

		// see if the image is where it should be
		checkBlock(2, 3, 30, 40, alg.getStitchedImage());
		// check the stiched image size
		assertEquals(250, alg.getStitchedImage().width);
		assertEquals(400, alg.getStitchedImage().height);

		// no transform provided, should be the same
		Affine2D_F64 found = alg.getWorldToCurr();
		assertEquals(1, found.tx, 1e-5);
		assertEquals(-2, found.ty, 1e-5);
	}

	@Test void resizeStitchImage_Transform() {
		HelperMotion motion = new HelperMotion();
		InterpolatePixelS interp = FactoryInterpolation.createPixelS(0, 255,
				InterpolationType.BILINEAR, BorderType.EXTENDED, GrayF32.class);
		ImageDistort distorter = FactoryDistort.distortSB(false, interp, GrayF32.class);

		StitchingTransform trans = FactoryStitchingTransform.createAffine_F64();

		StitchingFromMotion2D<GrayF32, Affine2D_F64> alg =
				new StitchingFromMotion2D<>(motion, distorter, trans, 0.3);

		alg.configure(200, 300, null);
		assertTrue(alg.process(image));

		ImageMiscOps.fill(alg.getStitchedImage().subimage(2, 3, 30, 40, null), 1);
		Affine2D_F64 transform = new Affine2D_F64(1, 0, 0, 1, -2, 4);
		alg.resizeStitchImage(250, 400, transform);

		// see if the image is where it should be
		checkBlock(4, 0, 32, 36, alg.getStitchedImage());
		// check the stitched image size
		assertEquals(250, alg.getStitchedImage().width);
		assertEquals(400, alg.getStitchedImage().height);

		// check to see if translation was correctly applied
		Affine2D_F64 found = alg.getWorldToCurr();
		assertEquals(1 - 2, found.tx, 1e-5);
		assertEquals(-2 + 4, found.ty, 1e-5);
	}

	private void checkBlock( int x0, int y0, int x1, int y1, GrayF32 image ) {

		for (int y = 0; y < image.height; y++) {
			for (int x = 0; x < image.width; x++) {
				float v = image.get(x, y);
				if (x >= x0 && x < x1 && y >= y0 && y < y1) {
					assertEquals(1, v, 1e-5);
				} else {
					assertEquals(0, v, 1e-5);
				}
			}
		}
	}

	@Test void getImageCorners() {
		HelperMotion motion = new HelperMotion();
		HelperDistort distort = new HelperDistort();

		StitchingTransform trans = FactoryStitchingTransform.createAffine_F64();

		StitchingFromMotion2D<GrayF32, Affine2D_F64> alg =
				new StitchingFromMotion2D<>(motion, distort, trans, 0.3);

		alg.configure(200, 300, null);
		assertTrue(alg.process(image));

		int w = 100, h = 150;

		Quadrilateral_F64 corners = new Quadrilateral_F64();
		alg.getImageCorners(w, h, corners);
		assertEquals(-1, corners.a.x, 1e-5);
		assertEquals(2, corners.a.y, 1e-5);
		assertEquals(-1 + w, corners.b.x, 1e-5);
		assertEquals(2, corners.b.y, 1e-5);
		assertEquals(-1 + w, corners.c.x, 1e-5);
		assertEquals(2 + h, corners.c.y, 1e-5);
		assertEquals(-1, corners.d.x, 1e-5);
		assertEquals(2 + h, corners.d.y, 1e-5);
	}

	/**
	 * Make sure it doesn't blow up if reset is called before anythign is processed
	 */
	@Test void resetBeforeProcess() {
		HelperMotion motion = new HelperMotion();
		HelperDistort distort = new HelperDistort();

		StitchingTransform trans = FactoryStitchingTransform.createAffine_F64();

		StitchingFromMotion2D<GrayF32, Affine2D_F64> alg =
				new StitchingFromMotion2D<>(motion, distort, trans, 0.3);

		alg.reset();
	}

	private class HelperMotion implements ImageMotion2D<GrayF32, Affine2D_F64> {

		int numProcess = 0;
		int numReset = 0;
		int numSetToFirst = 0;
		Affine2D_F64 found = translation;

		@Override
		public boolean process( GrayF32 input ) {
			numProcess++;
			return true;
		}

		@Override
		public void reset() {
			numReset++;
		}

		@Override
		public void setToFirst() {
			numSetToFirst++;
		}

		@Override
		public long getFrameID() { return 0; }

		@Override
		public Affine2D_F64 getFirstToCurrent() {
			return found;
		}

		@Override
		public Class<Affine2D_F64> getTransformType() {
			return Affine2D_F64.class;
		}
	}

	private class HelperDistort implements ImageDistort<GrayF32, GrayF32> {

		int numSetModel = 0;
		int numApply = 0;

		@Override
		public void setModel( PixelTransform<Point2D_F32> dstToSrc ) {
			numSetModel++;
		}

		@Override
		public void apply( GrayF32 srcImg, GrayF32 dstImg ) {
			numApply++;
		}

		@Override
		public void apply( GrayF32 srcImg, GrayF32 dstImg, GrayU8 mask ) {numApply++;}

		@Override
		public void apply( GrayF32 srcImg, GrayF32 dstImg, int dstX0, int dstY0, int dstX1, int dstY1 ) {
			numApply++;
		}

		@Override
		public void setRenderAll( boolean renderAll ) {}

		@Override
		public boolean getRenderAll() {return false;}

		@Override
		public PixelTransform<Point2D_F32> getModel() {return null;}
	}
}
