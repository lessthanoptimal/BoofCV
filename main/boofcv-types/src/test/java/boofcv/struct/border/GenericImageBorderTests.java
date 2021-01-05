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

package boofcv.struct.border;

import boofcv.core.image.FactoryGImageMultiBand;
import boofcv.core.image.GImageMultiBand;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofStandardJUnit;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Common tests for implementers of {@link ImageBorder}.
 *
 * @author Peter Abeles
 */
public abstract class GenericImageBorderTests<T extends ImageBase<T>> extends BoofStandardJUnit {

	List<ImageType<T>> imageTypes = new ArrayList<>();

	int width = 20;
	int height = 25;

	double[] tmp0;
	double[] tmp1;

	protected GenericImageBorderTests( ImageType<T>... imageTypes ) {
		for (int i = 0; i < imageTypes.length; i++) {
			this.imageTypes.add(imageTypes[i]);
		}
	}

	public abstract ImageBorder<T> wrap( T image );

	/**
	 * Checks to see if the image at the specified coordinate has the specified pixel value.
	 */
	public abstract void checkBorderSet( int x, int y, double[] pixel, T image );

	/**
	 * Check to see if the provided pixel is the value it should be at the specified coordinate
	 */
	public abstract void checkBorderGet( int x, int y, T image, double[] pixel );

	protected void init( ImageType<T> imageType ) {
		tmp0 = new double[imageType.getNumBands()];
		tmp1 = new double[imageType.getNumBands()];
	}

	@Test void get() {
		for (ImageType<T> imageType : imageTypes) {
			init(imageType);

			T img = imageType.createImage(width, height);
			fillUniform(img, rand, 0, 100);

			ImageBorder<T> border = wrap(img);

			checkGet(img, border);
		}
	}

	public static void fillUniform( ImageBase image, Random rand, double min, double max ) {
		GImageMultiBand wrap = FactoryGImageMultiBand.wrap(image);
		float[] values = new float[wrap.getNumberOfBands()];
		for (int y = 0; y < wrap.getHeight(); y++) {
			for (int x = 0; x < wrap.getWidth(); x++) {
				for (int i = 0; i < values.length; i++) {
					values[i] = (float)(rand.nextDouble()*(max-min)+min);
				}
				wrap.set(x,y,values);
			}
		}
	}

	private void checkGet( T image, ImageBorder<T> border ) {
		// test the image's inside where there is no border condition

		checkEquals(1, 1, image, border);
		checkEquals(0, 0, image, border);
		checkEquals(width - 1, height - 1, image, border);

		checkEquals(-1, 0, image, border);
		checkEquals(-2, 0, image, border);
		checkEquals(0, -1, image, border);
		checkEquals(0, -2, image, border);

		checkEquals(width, height - 1, image, border);
		checkEquals(width + 1, height - 1, image, border);
		checkEquals(width - 1, height, image, border);
		checkEquals(width - 1, height + 1, image, border);
	}

	private void checkEquals( int x, int y, T orig, ImageBorder<T> border ) {
		border.getGeneral(x, y, tmp0);
		checkBorderGet(x, y, orig, tmp0);
	}

	@Test void set() {
		for (ImageType<T> imageType : imageTypes) {
			init(imageType);
			T img = imageType.createImage(width, height);
			fillUniform(img, rand, 0, 100);

			ImageBorder<T> border = wrap(img);

			checkSet(img, border);
		}
	}

	@Test void copy() {
		for (ImageType<T> imageType : imageTypes) {
			init(imageType);
			T img = imageType.createImage(width, height);
			fillUniform(img, rand, 0, 100);

			ImageBorder<T> borderA = wrap(img);
			ImageBorder<T> borderB = borderA.copy();
			borderB.setImage(img);

			checkEquals(1, 1, borderA, borderB);
			checkEquals(0, 0, borderA, borderB);
			checkEquals(width - 1, height - 1, borderA, borderB);

			checkEquals(-1, 0, borderA, borderB);
			checkEquals(-2, 0, borderA, borderB);
			checkEquals(0, -1, borderA, borderB);
			checkEquals(0, -2, borderA, borderB);

			checkEquals(width, height - 1, borderA, borderB);
			checkEquals(width + 1, height - 1, borderA, borderB);
			checkEquals(width - 1, height, borderA, borderB);
			checkEquals(width - 1, height + 1, borderA, borderB);
		}
	}

	private void checkEquals( int x, int y, ImageBorder<T> borderA, ImageBorder<T> borderB ) {
		borderA.getGeneral(x, y, tmp0);
		borderB.getGeneral(x, y, tmp1);
		for (int i = 0; i < tmp0.length; i++) {
			assertEquals(tmp0[i], tmp1[i], UtilEjml.TEST_F64);
		}
	}

	private void checkSet( T image, ImageBorder<T> border ) {
		// test the image's inside where there is no border condition
		checkSet(0, 0, 1, image, border);
		checkSet(width - 1, height - 1, 2, image, border);

		// test border conditions
		checkSet(-1, 0, 2, image, border);
		checkSet(-2, 0, 3, image, border);
		checkSet(0, -1, 4, image, border);
		checkSet(0, -2, 5, image, border);

		checkSet(width, height - 1, 6, image, border);
		checkSet(width + 1, height - 1, 7, image, border);

		checkSet(width - 1, height, 8, image, border);
		checkSet(width - 1, height + 1, 9, image, border);
	}

	private void checkSet( int x, int y, double value, T orig, ImageBorder<T> border ) {
		for (int i = 0; i < tmp0.length; i++) {
			tmp0[i] = value;
		}

		border.setGeneral(x, y, tmp0);
		checkBorderSet(x, y, tmp0, orig);
	}
}
