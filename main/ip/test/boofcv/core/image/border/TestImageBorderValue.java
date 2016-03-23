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

import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.*;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestImageBorderValue extends GenericImageBorderTests {

	int value = 43;

	public TestImageBorderValue() {
		super(ImageType.single(GrayU8.class),
				ImageType.single(GrayU16.class),
				ImageType.single(GrayS32.class),
				ImageType.single(GrayS64.class),
				ImageType.single(GrayF32.class),
				ImageType.single(GrayF64.class),
				ImageType.il(2,InterleavedU8.class),
				ImageType.il(2,InterleavedU16.class),
				ImageType.il(2,InterleavedS32.class),
				ImageType.il(2,InterleavedS64.class),
				ImageType.il(2,InterleavedF32.class),
				ImageType.il(2,InterleavedF64.class)
				);

	}

	@Override
	public ImageBorder<ImageBase> wrap(ImageBase image) {
		if( image instanceof ImageGray)
			return ImageBorderValue.wrap((ImageGray)image,value);
		else if( image instanceof ImageInterleaved )
			return ImageBorderValue.wrap((ImageInterleaved)image,value);
		else
			throw new RuntimeException("asdfasdf");
	}

	@Override
	public void checkBorderSet(int x, int y, double[] pixel, ImageBase image) {
	}

	@Override
	public void checkBorderGet(int x, int y, ImageBase image, double[] pixel) {
		if( image.isInBounds(x,y)) {
			for (int i = 0; i < pixel.length; i++) {
				assertEquals(pixel[i], GeneralizedImageOps.get(image,x,y,i), 1e-5);
			}
		} else {
			for (int i = 0; i < pixel.length; i++) {
				assertEquals(value, (int) pixel[i]);
			}
		}
	}
}
