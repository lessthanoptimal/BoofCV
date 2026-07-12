/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.flow;

import boofcv.BoofTesting;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.InterleavedF32;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public abstract class GeneralDenseOpticalFlowChecks<T extends ImageGray<T>> extends BoofStandardJUnit {
	Class<T> imageType;

	T orig;
	T shifted;
	InterleavedF32 found;

	boolean justCorrectSign = false;

	protected GeneralDenseOpticalFlowChecks( Class<T> imageType ) {
		this.imageType = imageType;

		orig = GeneralizedImageOps.createSingleBand(imageType, 20, 25);
		shifted = GeneralizedImageOps.createSingleBand(imageType, 20, 25);

		found = new InterleavedF32(20, 25, 2);

		GImageMiscOps.fillUniform(orig, rand, 0, 256);
	}

	public void setJustCorrectSign( boolean justCorrectSign ) {
		this.justCorrectSign = justCorrectSign;
	}

	public void allTests( boolean justCorrectSign ) {
		this.justCorrectSign = justCorrectSign;
		allTests();
	}

	public void allTests() {
		processEdges();
		checkPlanarMotion();
		checkChangeInputSize();
		checkSubImage();
		attributes();
	}

	public abstract DenseOpticalFlow<T> createAlg( Class<T> imageType );

	/** True if the pixel has a valid flow estimate (x-band is not NaN) */
	private static boolean isValid( InterleavedF32 flow, int x, int y ) {
		return !Float.isNaN(flow.getBand(x, y, 0));
	}

	/** Marks every pixel as invalid by setting the x-band to NaN */
	private static void invalidateAll( InterleavedF32 flow ) {
		int N = flow.width*flow.height;
		for (int i = 0; i < N; i++)
			flow.data[i*2] = Float.NaN;
	}

	/**
	 * Attributes are optional. If an implementation provides them, they must match the flow's shape and have a
	 * value everywhere the flow is valid.
	 */
	@Test void attributes() {
		DenseOpticalFlow<T> alg = createAlg(imageType);
		shift(orig, 1, 0, shifted);
		alg.process(orig, shifted, found);

		InterleavedF32 attributes = alg.getAttributes();
		if (attributes == null)
			return;

		// shape must match the input images
		assertEquals(orig.width, attributes.width);
		assertEquals(orig.height, attributes.height);

		// every pixel with a valid flow must have a valid (non-NaN) attribute
		for (int y = 0; y < found.height; y++) {
			for (int x = 0; x < found.width; x++) {
				if (isValid(found, x, y))
					assertFalse(Float.isNaN(attributes.getBand(x, y, 0)));
			}
		}
	}

	/**
	 * Makes sure it attempts to compute flow through out the whole image. Specially checks the image border
	 * to see if those are skipped
	 */
	@Test void processEdges() {
		shift(orig, 1, 0, shifted);

		DenseOpticalFlow<T> alg = createAlg(imageType);

		invalidateAll(found);
		alg.process(orig, shifted, found);

		int count0 = 0, count1 = 0;
		for (int x = 0; x < shifted.width; x++) {
			if (isValid(found, x, 0))
				count0++;
			if (isValid(found, x, found.height - 2))
				count1++;
		}

		assertTrue(count0 >= found.width/3);
		assertTrue(count1 >= found.width/3);


		// process it again so that there should be an obvious solution along the left and right sides
		invalidateAll(found);
		shift(orig, 0, 1, shifted);
		alg.process(orig, shifted, found);

		int count2 = 0, count3 = 0;
		for (int y = 0; y < shifted.height; y++) {
			if (isValid(found, 0, y))
				count2++;
			if (isValid(found, found.width - 2, y))
				count3++;
		}

		assertTrue(count2 >= found.height/3);
		assertTrue(count3 >= found.height/3);
	}

	/**
	 * Very simple test where every pixel moves at the same speed along x and or y direction
	 */
	@Test void checkPlanarMotion() {

		for (int dy = -1; dy <= 1; dy++) {
			for (int dx = -1; dx <= 1; dx++) {
				DenseOpticalFlow<T> alg = createAlg(imageType);
				shift(orig, dx, dy, shifted);

				invalidateAll(found);
				alg.process(orig, shifted, found);

				assertTrue(isValid(found, 10, 10));
				if (justCorrectSign) {
					// if the two flows are in agreement then sum will be positive
					float sum = 0;
					for (int y = 0; y < found.height; y++) {
						for (int x = 0; x < found.width; x++) {
							sum += found.getBand(x, y, 0)*dx;
							sum += found.getBand(x, y, 1)*dy;
						}
					}
					assertTrue(sum >= 0);
				} else {
					assertEquals(dx, found.getBand(10, 10, 0), 0.2);
					assertEquals(dy, found.getBand(10, 10, 1), 0.2);
				}
			}
		}
	}

	/**
	 * Does it handle the input image size being changed after the first image?
	 */
	@Test void checkChangeInputSize() {
		DenseOpticalFlow<T> alg = createAlg(imageType);

		alg.process(orig, shifted, found);

		T larger0 = GeneralizedImageOps.createSingleBand(imageType, 40, 35);
		T larger1 = GeneralizedImageOps.createSingleBand(imageType, 40, 35);

		// if it doesn't blow up it worked
		alg.process(larger0, larger1, new InterleavedF32(40, 35, 2));
	}

	@Test void checkSubImage() {
		DenseOpticalFlow<T> alg = createAlg(imageType);

		shift(orig, 1, -1, shifted);
		alg.process(orig, shifted, found);

		// should produce identical solution
		T subOrig = BoofTesting.createSubImageOf(orig);
		T subShifted = BoofTesting.createSubImageOf(shifted);
		InterleavedF32 found2 = new InterleavedF32(found.width, found.height, 2);
		alg.process(subOrig, subShifted, found2);

		for (int y = 0; y < found.height; y++) {
			for (int x = 0; x < found.width; x++) {
				if (isValid(found, x, y)) {
					assertTrue(found.getBand(x, y, 0) == found2.getBand(x, y, 0));
					assertTrue(found.getBand(x, y, 1) == found2.getBand(x, y, 1));
				} else {
					assertFalse(isValid(found2, x, y));
				}
			}
		}
	}

	private void shift( T input, int dx, int dy, T output ) {

		int w = input.width;
		int h = input.height;

		if (dx >= 0) {
			output.subimage(dx, 0, w, h).setTo(input.subimage(0, 0, w - dx, h));
			output.subimage(0, 0, dx, h).setTo(input.subimage(w - dx, 0, w, h));
		} else {
			output.subimage(0, 0, w + dx, h).setTo(input.subimage(-dx, 0, w, h));
			output.subimage(w + dx, 0, w, h).setTo(input.subimage(0, 0, -dx, h));
		}

		T tmp = (T)output.clone();

		if (dy >= 0) {
			output.subimage(0, dy, w, h).setTo(tmp.subimage(0, 0, w, h - dy));
			output.subimage(0, 0, w, dy).setTo(tmp.subimage(0, h - dy, w, h));
		} else {
			output.subimage(0, 0, w, h + dy).setTo(tmp.subimage(0, -dy, w, h));
			output.subimage(0, h + dy, w, h).setTo(tmp.subimage(0, 0, w, -dy));
		}
	}
}
