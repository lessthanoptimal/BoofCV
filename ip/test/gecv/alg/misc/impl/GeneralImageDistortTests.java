/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.misc.impl;

import gecv.alg.interpolate.FactoryInterpolation;
import gecv.alg.interpolate.InterpolatePixel;
import gecv.alg.misc.ImageDistort;
import gecv.core.image.GeneralizedImageOps;
import gecv.core.image.ImageGenerator;
import gecv.core.image.inst.FactoryImageGenerator;
import gecv.struct.distort.PixelDistort;
import gecv.struct.image.ImageBase;
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

	public GeneralImageDistortTests( Class<T> imageType ) {
		this.imageType = imageType;
		interp = FactoryInterpolation.nearestNeighborPixel(imageType);
		generator = FactoryImageGenerator.create((Class<T>)imageType);
	}

	public abstract ImageDistort<T> createDistort(PixelDistort dstToSrc, InterpolatePixel<T> interp);

	@Test
	public void testSkip() {
		T src = generator.createInstance(width,height);
		T dst = generator.createInstance(width,height);

		GeneralizedImageOps.randomize(src, rand, 0,10);

		ImageDistort<T> tran = createDistort(new BasicTransform(),interp);
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
					assertEquals(0f,dstVal,1e-4);
				}
			}
		}
	}

	@Test
	public void testDefaultValue() {
		T src = generator.createInstance(width,height);
		T dst = generator.createInstance(width,height);

		GeneralizedImageOps.randomize(src, rand, 0,10);

		int fillValue = 5;

		ImageDistort<T> tran = createDistort(new BasicTransform(),interp);
		tran.apply(src,dst,fillValue);

		for( int dstY = 0; dstY < height; dstY++ ) {
			for( int dstX = 0; dstX < width; dstX++ ) {
				int srcX = dstX + offX;
				int srcY = dstY + offY;

				double dstVal = GeneralizedImageOps.get(dst,dstX,dstY);

				if( src.isInBounds(srcX,srcY) ) {
					double srcVal = GeneralizedImageOps.get(src,srcX,srcY);
					assertEquals(srcVal,dstVal,1e-4);
				} else {
					assertEquals(fillValue,dstVal,1e-4);
				}
			}
		}
	}

	public class BasicTransform extends PixelDistort {


		@Override
		public void distort(int x, int y) {
			this.distX = x+offX;
			this.distY = y+offY;
		}
	}
}
