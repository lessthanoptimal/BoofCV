/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.distort.impl;

import boofcv.alg.distort.ImageDistort;
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.ImageGenerator;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.core.image.border.ImageBorder;
import boofcv.core.image.inst.FactoryImageGenerator;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.distort.PixelTransform;
import boofcv.struct.image.ImageBase;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;


/**
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public abstract class GeneralImageDistortTests<T extends ImageBase> {

	Random rand = new Random(123);

	int width = 30;
	int height = 20;

	int offX = 2;
	int offY = 1;

	InterpolatePixel<T> interp;
	Class<?> imageType;
	ImageGenerator<T> generator;
	ImageBorder border;

	public GeneralImageDistortTests( Class<T> imageType ) {
		this.imageType = imageType;
		interp = FactoryInterpolation.nearestNeighborPixel(imageType);
		generator = FactoryImageGenerator.create((Class<T>)imageType);
		border = FactoryImageBorder.value(imageType,5);
	}

	public abstract ImageDistort<T> createDistort(PixelTransform dstToSrc, InterpolatePixel<T> interp, ImageBorder border );


	@Test
	public void testDefaultValue() {
		T src = generator.createInstance(width,height);
		T dst = generator.createInstance(width,height);

		GeneralizedImageOps.randomize(src, rand, 0,10);

		ImageDistort<T> tran = createDistort(new BasicTransform(),interp,border);
		tran.apply(src,dst);

		for( int dstY = 0; dstY < height; dstY++ ) {
			for( int dstX = 0; dstX < width; dstX++ ) {
				int srcX = dstX + offX;
				int srcY = dstY + offY;

				double dstVal = GeneralizedImageOps.get(dst,dstX,dstY);

				if( src.isInBounds(srcX,srcY) ) {
					double srcVal = GeneralizedImageOps.get(src,srcX,srcY);
					assertEquals(srcVal,dstVal,1e-4);
				} else {
					assertEquals(5,dstVal,1e-4);
				}
			}
		}
	}

	public class BasicTransform extends PixelTransform {


		@Override
		public void compute(int x, int y) {
			this.distX = x+offX;
			this.distY = y+offY;
		}
	}
}
