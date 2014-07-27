/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.distort.impl;

import boofcv.alg.distort.ImageDistort;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.ImageGenerator;
import boofcv.core.image.border.BorderType;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.core.image.border.ImageBorder;
import boofcv.core.image.inst.FactoryImageGenerator;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.distort.PixelTransform_F32;
import boofcv.struct.image.ImageSingleBand;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;


/**
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public abstract class GeneralImageDistortTests<T extends ImageSingleBand> {

	Random rand = new Random(123);

	int width = 30;
	int height = 20;

	int offX = 2;
	int offY = 1;

	InterpolatePixelS<T> interp;
	Class<?> imageType;
	ImageGenerator<T> generator;
	ImageBorder border;

	public GeneralImageDistortTests( Class<T> imageType ) {
		this.imageType = imageType;
		interp = FactoryInterpolation.nearestNeighborPixelS(imageType);
		generator = FactoryImageGenerator.create((Class<T>)imageType);
		border = FactoryImageBorder.general(imageType, BorderType.EXTENDED);
	}

	public abstract ImageDistort<T,T> createDistort(PixelTransform_F32 dstToSrc,
													InterpolatePixelS<T> interp,
													ImageBorder<T> border );


	@Test
	public void testDefaultValue() {
		testDefaultValue(true);
		testDefaultValue(false);
	}

	public void testDefaultValue( boolean withBorder ) {
		T src = generator.createInstance(width,height);
		T dst = generator.createInstance(width,height);

		GImageMiscOps.fillUniform(src, rand, 0, 10);

		ImageBorder border = withBorder ? this.border : null;

		ImageDistort<T,T> tran = createDistort(new BasicTransform(),interp,border);
		tran.apply(src,dst);

		for( int dstY = 0; dstY < height; dstY++ ) {
			for( int dstX = 0; dstX < width; dstX++ ) {
				int srcX = dstX + offX;
				int srcY = dstY + offY;

				double dstVal = GeneralizedImageOps.get(dst,dstX,dstY);

				if( src.isInBounds(srcX,srcY) ) {
					double srcVal = GeneralizedImageOps.get(src,srcX,srcY);
					assertEquals(srcVal,dstVal,1e-4);
				} else if( withBorder ) {
					double expected = border.getGeneral(srcX,srcY);
					assertEquals(expected,dstVal,1e-4);
				}
			}
		}
	}
	
	@Test
	public void testCrop() {
		testCrop(true);
		testCrop(false);
	}

	public void testCrop( boolean withBorder ) {
		// the crop region
		int x0=12,y0=10,x1=17,y1=18;

		T src = generator.createInstance(width,height);
		T dst = generator.createInstance(width,height);

		GImageMiscOps.fillUniform(src, rand, 0, 10);

		ImageBorder border = withBorder ? this.border : null;

		ImageDistort<T,T> tran = createDistort(new BasicTransform(),interp,border);
		tran.apply(src,dst,x0,y0,x1,y1);

		for( int dstY = 0; dstY < height; dstY++ ) {
			for( int dstX = 0; dstX < width; dstX++ ) {
				// should be zero outside of the crop region
				if( dstX < x0 || dstX >= x1 || dstY < y0 || dstY >= y1 )
					assertEquals(0,GeneralizedImageOps.get(dst,dstX,dstY),1e-4);
				else {
					int srcX = dstX + offX;
					int srcY = dstY + offY;

					double dstVal = GeneralizedImageOps.get(dst,dstX,dstY);

					if( src.isInBounds(srcX,srcY) ) {
						double srcVal = GeneralizedImageOps.get(src,srcX,srcY);
						assertEquals(srcVal,dstVal,1e-4);
					} else if( withBorder ) {
						double expected = border.getGeneral(srcX,srcY);
						assertEquals(expected,dstVal,1e-4);
					}
				}
			}
		}
	}

	/**
	 * Request a pixel outside the image border
	 */
	@Test
	public void testOutsideCrop() {
		testOutsideCrop(true);
		testOutsideCrop(false);
	}

	public void testOutsideCrop( boolean withBorder ) {
		// the crop region
		int x0=12,y0=10,x1=width,y1=height;

		T src = generator.createInstance(width,height);
		T dst = generator.createInstance(width,height);

		GImageMiscOps.fillUniform(src, rand, 0, 10);

		ImageBorder border = withBorder ? this.border : null;

		ImageDistort<T,T> tran = createDistort(new BasicTransform(),interp,border);
		tran.apply(src,dst,x0,y0,x1,y1);

		for( int dstY = 0; dstY < height; dstY++ ) {
			for( int dstX = 0; dstX < width; dstX++ ) {
				// should be zero outside of the crop region
				if( dstX < x0 || dstX >= x1 || dstY < y0 || dstY >= y1 )
					assertEquals(0,GeneralizedImageOps.get(dst,dstX,dstY),1e-4);
				else {
					int srcX = dstX + offX;
					int srcY = dstY + offY;

					double dstVal = GeneralizedImageOps.get(dst,dstX,dstY);

					if( src.isInBounds(srcX,srcY) ) {
						double srcVal = GeneralizedImageOps.get(src,srcX,srcY);
						assertEquals(srcVal,dstVal,1e-4);
					} else if( withBorder ) {
						double expected = border.getGeneral(srcX,srcY);
						assertEquals(expected,dstVal,1e-4);
					}
				}
			}
		}
	}

	public class BasicTransform extends PixelTransform_F32 {


		@Override
		public void compute(int x, int y) {
			this.distX = x+offX;
			this.distY = y+offY;
		}
	}
}
