/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.core.image.border;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


/**
 * Common tests for implementers of {@link ImageBorder}.
 *
 * @author Peter Abeles
 */
public abstract class GenericImageBorderTests<T extends ImageBase> {

	Random rand = new Random(234);
	List<ImageType<T>> imageTypes = new ArrayList<>();

	int width = 20;
	int height = 25;

	double tmp0[];
	double tmp1[];

	public GenericImageBorderTests(ImageType<T> ...imageTypes) {
		for( int i = 0; i < imageTypes.length; i++ ) {
			this.imageTypes.add( imageTypes[i]);
		}
	}


	public abstract ImageBorder<T> wrap( T image );

	/**
	 * Checks to see if the image at the specified coordinate has the specified pixel value.
	 */
	public abstract void checkBorderSet( int x , int y , double[] pixel , T image );

	/**
	 * Check to see if the provided pixel is the value it should be at the specified coordinate
	 */
	public abstract void checkBorderGet(int x, int y, T image, double[] pixel);

	protected void init( ImageType<T> imageType ) {
		tmp0 = new double[imageType.getNumBands()];
		tmp1 = new double[imageType.getNumBands()];
	}

	@Test
	public void get() {
		for( ImageType<T> imageType : imageTypes ) {
			init(imageType);

			T img = imageType.createImage(width, height);
			GImageMiscOps.fillUniform(img, rand, 0, 100);

			ImageBorder<T> border = wrap(img);

			checkGet(img, border);
		}
	}

	private void checkGet(T image, ImageBorder<T> border) {
		// test the image's inside where there is no border condition

		checkEquals(1, 1, image, border);
		checkEquals(0, 0, image, border);
		checkEquals(width - 1, height - 1, image, border);

		checkEquals(-1,0,image,border);
		checkEquals(-2,0,image,border);
		checkEquals(0,-1,image,border);
		checkEquals(0,-2,image,border);

		checkEquals(width, height - 1, image, border);
		checkEquals(width + 1, height - 1, image, border);
		checkEquals(width - 1, height, image, border);
		checkEquals(width - 1, height + 1, image, border);
	}

	private void checkEquals( int x , int y , T orig, ImageBorder<T> border ) {
		border.getGeneral(x, y, tmp0);
		checkBorderGet(x,y,orig,tmp0);
	}

	@Test
	public void set() {
		for( ImageType<T> imageType : imageTypes ) {
			init(imageType);
			T img = imageType.createImage(width, height);
			GImageMiscOps.fillUniform(img, rand, 0, 100);

			ImageBorder<T> border = wrap(img);

			checkSet(img, border);
		}
	}


	private void checkSet(T image, ImageBorder<T> border) {
		// test the image's inside where there is no border condition
		checkSet(0, 0, 1, image, border);
		checkSet(width - 1, height - 1, 2, image, border);

		// test border conditions
		checkSet(-1,0,2,image,border);
		checkSet(-2,0,3,image,border);
		checkSet(0,-1,4,image,border);
		checkSet(0,-2,5,image,border);

		checkSet(width, height - 1, 6, image, border);
		checkSet(width + 1, height - 1, 7, image, border);

		checkSet(width - 1, height, 8, image, border);
		checkSet(width - 1, height + 1, 9, image, border);
	}

	private void checkSet( int x , int y , double value, T orig, ImageBorder<T> border ) {
		for (int i = 0; i < tmp0.length; i++) {
			tmp0[i] = value;
		}

		border.setGeneral(x, y, tmp0);
		checkBorderSet(x,y, tmp0,orig);
	}

}
