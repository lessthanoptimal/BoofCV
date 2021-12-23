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

package boofcv.abst.distort;

import boofcv.BoofTesting;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.PixelTransformAffine_F32;
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.interpolate.InterpolationType;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.core.image.border.FactoryImageBorderAlgs;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.border.BorderType;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.distort.PixelTransform;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.affine.Affine2D_F32;
import georegression.struct.point.Point2D_F32;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestFDistort extends BoofStandardJUnit {

	int width = 30, height = 40;
	GrayU8 input = new GrayU8(width, height);
	GrayU8 output = new GrayU8(width, height);

	@Test
	void scale() {
		ImageMiscOps.fillUniform(input, rand, 0, 200);

		new FDistort(input, output).scaleExt().apply();

		InterpolatePixelS<GrayU8> interp = FactoryInterpolation.bilinearPixelS(input, BorderType.EXTENDED);
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

	@Test
	void rotate() {
		ImageMiscOps.fillUniform(input, rand, 0, 200);
		GrayU8 output = new GrayU8(height, width);

		new FDistort(input, output).rotate(Math.PI/2).apply();

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
		assertTrue(error/(width*height*200) < 0.1);
	}

	@Test
	void affine() {
		ImageMiscOps.fillUniform(input, rand, 0, 200);

		Affine2D_F32 affine = new Affine2D_F32(2, 0.1f, -0.2f, 1.1f, 3, 4.5f);
		PixelTransform<Point2D_F32> transform = new PixelTransformAffine_F32(affine.invert(null));
		new FDistort(input, output).affine(2, 0.1f, -0.2f, 1.1f, 3, 4.5f).borderExt().apply();

		InterpolatePixelS<GrayU8> interp = FactoryInterpolation.bilinearPixelS(input, null);
		interp.setBorder(FactoryImageBorderAlgs.extend(input));
		interp.setImage(input);

		Point2D_F32 distorted = new Point2D_F32();
		if (input.getDataType().isInteger()) {
			for (int y = 0; y < output.height; y++) {
				for (int x = 0; x < output.width; x++) {
					transform.compute(x, y, distorted);
					float val = interp.get(distorted.x, distorted.y);
					assertEquals((int)val, output.get(x, y), 1e-4);
				}
			}
		} else {
			for (int y = 0; y < output.height; y++) {
				for (int x = 0; x < output.width; x++) {
					transform.compute(x, y, distorted);
					float val = interp.get(distorted.x, distorted.y);
					assertEquals(val, output.get(x, y), 1e-4);
				}
			}
		}
	}

	/**
	 * Makes sure that setRefs doesn't cause it to blow up
	 */
	@Test
	void setRefs() {
		ImageMiscOps.fillUniform(input, rand, 0, 200);
		var alg = new FDistort(input.getImageType());
		alg.setRefs(input, output).interp(InterpolationType.BILINEAR).scaleExt().apply();

		ImageDistort distorter = alg.distorter;
		InterpolatePixel interp = alg.interp;
		;
		PixelTransform<Point2D_F32> outputToInput = alg.outputToInput;

		// a new image shouldn't cause new memory to be declared bad stuff to happen
		var found = new GrayU8(width/2, height/2);
		var expected = new GrayU8(width/2, height/2);
		alg.setRefs(input, found).scale().apply();

		assertSame(distorter, alg.distorter);
		assertSame(interp, alg.interp);
		assertSame(outputToInput, alg.outputToInput);

		new FDistort(input, expected).scaleExt().apply();

		BoofTesting.assertEquals(expected, found, 1e-4);
	}

	/**
	 * Makes sure that border recycling doesn't mess things up
	 */
	@Test
	void setBorderChange() {
		ImageMiscOps.fillUniform(input, rand, 0, 200);
		var found = new GrayU8(width/2, height/2);
		var expected = new GrayU8(width/2, height/2);

		var alg = new FDistort(input.getImageType());

		alg.init(input, found).scaleExt().apply();

		ImageDistort distorter = alg.distorter;
		InterpolatePixel interp = alg.interp;
		;
		PixelTransform<Point2D_F32> outputToInput = alg.outputToInput;

		// Set it to the default border, nothing should change
		expected.setTo(found);
		alg.border(BorderType.EXTENDED).apply();
		assertSame(distorter, alg.distorter);
		assertSame(interp, alg.interp);
		assertSame(outputToInput, alg.outputToInput);
		BoofTesting.assertEquals(expected, found, 1e-4);

		// change border now a fixed value
		alg.border(10).apply();
		new FDistort(input, expected).scale().border(10).apply();
		BoofTesting.assertEquals(expected, found, 1e-4);

		// change value
		alg.border(1).apply();
		new FDistort(input, expected).scale().border(1).apply();
		BoofTesting.assertEquals(expected, found, 1e-4);
	}

	/**
	 * Checks to see if SKIP is properly implemented
	 */
	@Test
	void borderSkip() {
		var histogram = new int[256];

		// Skip the border by specify the border by Type
		ImageMiscOps.fill(input, 11);
		ImageMiscOps.fill(output, 12);
		new FDistort(input, output).affine(1, 0, 0, 1, 10, 5).border(BorderType.SKIP).apply();
		ImageStatistics.histogram(output, 0, histogram);
		assertEquals(500, histogram[12]);

		// Now do it by passing in null
		ImageMiscOps.fill(input, 11);
		ImageMiscOps.fill(output, 12);
		new FDistort(input, output).affine(1, 0, 0, 1, 10, 5).border((ImageBorder)null).apply();
		ImageStatistics.histogram(output, 0, histogram);
		assertEquals(500, histogram[12]);
	}
}
