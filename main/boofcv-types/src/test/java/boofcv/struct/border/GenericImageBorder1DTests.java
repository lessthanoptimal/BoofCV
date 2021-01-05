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

import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public abstract class GenericImageBorder1DTests<T extends ImageBase<T>> extends GenericImageBorderTests<T> {

	DummyBorderIndex1D_Wrap wrap = new DummyBorderIndex1D_Wrap();

	protected GenericImageBorder1DTests( ImageType<T> imageType ) {
		super(imageType);
	}

	@Override
	public void checkBorderSet( int x, int y, double[] pixel, T image ) {
		wrap.setLength(image.getWidth());
		x = wrap.getIndex(x);
		wrap.setLength(image.getHeight());
		y = wrap.getIndex(y);

		for (int i = 0; i < pixel.length; i++) {
			double value = GeneralizedImageOps.get(image, x, y, i);
			assertEquals(pixel[i], value, 1e-4);
		}
	}

	@Override
	public void checkBorderGet( int x, int y, T image, double[] pixel ) {
		checkBorderSet(x, y, pixel, image);
	}
}
