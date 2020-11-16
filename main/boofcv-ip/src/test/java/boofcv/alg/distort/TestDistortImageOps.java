/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.distort;

import boofcv.abst.distort.FDistort;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.border.BorderType;
import boofcv.struct.image.GrayF32;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.affine.Affine2D_F32;
import georegression.struct.affine.Affine2D_F64;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.RectangleLength2D_F32;
import georegression.struct.shapes.RectangleLength2D_F64;
import georegression.struct.shapes.RectangleLength2D_I32;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
class TestDistortImageOps extends BoofStandardJUnit {

	int width = 20;
	int height = 30;

	/**
	 * Checks to see if the two ways of specifying interpolation work
	 */
	@Test
	void scale_InterpTypeStyle() {
		GrayF32 input = new GrayF32(width, height);
		GrayF32 output = input.createSameShape();

		GImageMiscOps.fillUniform(input, rand, 0, 100);

		new FDistort(input, output).border(BorderType.ZERO).scale().apply();

		InterpolatePixelS<GrayF32> interp = FactoryInterpolation.bilinearPixelS(input, BorderType.EXTENDED);
		interp.setImage(input);

		float scaleX = (float)input.width/(float)output.width;
		float scaleY = (float)input.height/(float)output.height;

		if (input.getDataType().isInteger()) {
			for (int i = 0; i < output.height; i++) {
				for (int j = 0; j < output.width; j++) {
					float val = interp.get(j*scaleX, i*scaleY);
					assertEquals((int)val, output.get(j, i), 1e-4);
				}
			}
		} else {
			for (int i = 0; i < output.height; i++) {
				for (int j = 0; j < output.width; j++) {
					float val = interp.get(j*scaleX, i*scaleY);
					assertEquals(val, output.get(j, i), 1e-4);
				}
			}
		}
	}

	/**
	 * Very simple test for rotation accuracy.
	 */
	@Test
	void rotate_SanityCheck() {
		var input = new GrayF32(width, height);
		var output = new GrayF32(height, width);

		GImageMiscOps.fillUniform(input, rand, 0, 100);

		new FDistort(input, output).border(BorderType.ZERO).rotate(Math.PI/2.0).apply();

		double error = 0;
		// the outside pixels are ignored because numerical round off can cause those to be skipped
		for (int y = 1; y < input.height - 1; y++) {
			for (int x = 1; x < input.width - 1; x++) {
				int xx = output.width - y;
				int yy = x;

				double e = input.get(x, y) - output.get(xx, yy);
				error += Math.abs(e);
			}
		}
		assertTrue(error/(width*height) < 0.1);
	}

	/**
	 * boundBox that checks to see if it is contained inside the output image.
	 */
	@Test
	void boundBox_check() {
		Point2D_F32 work = new Point2D_F32();

		// basic sanity check
		var affine = new Affine2D_F32(1, 0, 0, 1, 2, 3);
		var transform = new PixelTransformAffine_F32(affine);
		RectangleLength2D_I32 found = DistortImageOps.boundBox(10, 20, 30, 40, work, transform);

		assertEquals(2, found.x0);
		assertEquals(3, found.y0);
		assertEquals(10, found.width);
		assertEquals(20, found.height);

		// bottom right border
		found = DistortImageOps.boundBox(10, 20, 8, 18, work, transform);
		assertEquals(2, found.x0);
		assertEquals(3, found.y0);
		assertEquals(6, found.width);
		assertEquals(15, found.height);

		// top right border
		transform.getModel().setTo(new Affine2D_F32(1, 0, 0, 1, -2, -3));
		found = DistortImageOps.boundBox(10, 20, 8, 18, work, transform);
		assertEquals(0, found.x0);
		assertEquals(0, found.y0);
		assertEquals(8, found.width);
		assertEquals(17, found.height);
	}

	@Test
	void boundBox() {
		Point2D_F32 work = new Point2D_F32();

		// basic sanity check
		var affine = new Affine2D_F32(1, 0, 0, 1, 2, 3);
		var transform = new PixelTransformAffine_F32(affine);
		RectangleLength2D_I32 found = DistortImageOps.boundBox(10, 20, work, transform);

		assertEquals(2, found.x0);
		assertEquals(3, found.y0);
		assertEquals(10, found.width);
		assertEquals(20, found.height);
	}

	@Test
	void boundBox_F32() {
		Point2D_F32 transformed = new Point2D_F32();

		// basic sanity check
		var affine = new Affine2D_F32(1, 0, 0, 1, 2, 3);
		var transform = new PixelTransformAffine_F32(affine);
		RectangleLength2D_F32 found = DistortImageOps.boundBox_F32(10, 20, transform, transformed);

		assertEquals(2, found.x0, 1e-4);
		assertEquals(3, found.y0, 1e-4);
		assertEquals(10, found.width, 1e-4);
		assertEquals(20, found.height, 1e-4);
	}

	/** The transform does a simple translation */
	@Test void boundBox_F64_shifted() {
		Point2D_F64 transformed = new Point2D_F64();

		// basic sanity check
		var affine = new Affine2D_F64(1, 0, 0, 1, 2, 3);
		var transform = new PixelTransformAffine_F64(affine);
		RectangleLength2D_F64 found = DistortImageOps.boundBox_F64(10, 20, transform, transformed);

		assertEquals(2, found.x0, 1e-8);
		assertEquals(3, found.y0, 1e-8);
		assertEquals(10, found.width, 1e-8);
		assertEquals(20, found.height, 1e-8);
	}

	/** The view is now flipped. This was causing problems for a long time */
	@Test void boundBox_F64_flipped() {
		Point2D_F64 transformed = new Point2D_F64();

		// basic sanity check
		var affine = new Affine2D_F64(-1, 0, 0, -1, 10, 20);
		var transform = new PixelTransformAffine_F64(affine);
		RectangleLength2D_F64 found = DistortImageOps.boundBox_F64(10, 20, transform, transformed);

		assertEquals(1, found.x0, 1e-8);
		assertEquals(1, found.y0, 1e-8);
		assertEquals(10, found.width, 1e-8);
		assertEquals(20, found.height, 1e-8);
	}
}
