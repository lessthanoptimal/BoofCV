/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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
	Class<T> imageType;
	ImageGenerator<T> generator;

	public GeneralImageDistortTests( Class<T> imageType ) {
		this.imageType = imageType;
		interp = FactoryInterpolation.nearestNeighborPixelS(imageType);
		interp.setBorder(FactoryImageBorder.single(imageType, BorderType.VALUE));
		generator = FactoryImageGenerator.create((Class<T>) imageType);
	}

	public abstract ImageDistort<T,T> createDistort(PixelTransform_F32 dstToSrc, InterpolatePixelS<T> interp );


	/**
	 * Makes sure it renders all by default
	 */
	@Test
	public void defaultRenderAllIsTrue() {
		T src = generator.createInstance(width,height);
		T dst = generator.createInstance(width, height);

		GImageMiscOps.fillUniform(src, rand, 0, 10);
		GImageMiscOps.fill(dst, 50);

		ImageDistort<T,T> tran = createDistort(new BasicTransform(),interp);
		tran.apply(src, dst);


		double value = GeneralizedImageOps.get(dst,dst.getWidth()-1,dst.getHeight()-1);
		assertEquals(0,value,1e-8);

		// sanity check
		tran.setRenderAll(false);
		GImageMiscOps.fill(dst, 50);
		tran.apply(src, dst);
		value = GeneralizedImageOps.get(dst,dst.getWidth()-1,dst.getHeight()-1);
		assertEquals(50, value, 1e-8);
	}

	@Test
	public void checkRenderAll() {
		testDefaultValue(true);
		testDefaultValue(false);
	}

	public void testDefaultValue( boolean renderAll ) {
		T src = generator.createInstance(width,height);
		T dst = generator.createInstance(width, height);

		GImageMiscOps.fillUniform(src, rand, 0, 10);
		GImageMiscOps.fill(dst,50);

		ImageDistort<T,T> tran = createDistort(new BasicTransform(),interp);
		tran.setRenderAll(renderAll);
		tran.apply(src, dst);

		for( int dstY = 0; dstY < height; dstY++ ) {
			for( int dstX = 0; dstX < width; dstX++ ) {
				int srcX = dstX + offX;
				int srcY = dstY + offY;

				double dstVal = GeneralizedImageOps.get(dst,dstX,dstY);

				if( src.isInBounds(srcX,srcY) ) {
					double srcVal = GeneralizedImageOps.get(src,srcX,srcY);
					assertEquals(srcVal,dstVal,1e-4);
				} else if( renderAll ){
					// background set it to 0
					assertEquals(0.0,dstVal,1e-4);
				} else {
					// original value of 50 wasn't modified
					assertEquals(50.0,dstVal,1e-4);
				}
			}
		}
	}
	
	@Test
	public void testCrop() {
		testCrop(true);
		testCrop(false);
	}

	public void testCrop( boolean renderAll ) {
		// the crop region
		int x0=12,y0=10,x1=17,y1=18;

		T src = generator.createInstance(width,height);
		T dst = generator.createInstance(width, height);

		GImageMiscOps.fillUniform(src, rand, 0, 10);
		GImageMiscOps.fill(dst, 50);

		ImageDistort<T,T> tran = createDistort(new BasicTransform(),interp);
		tran.setRenderAll(renderAll);
		tran.apply(src,dst,x0,y0,x1,y1);

		for( int dstY = 0; dstY < height; dstY++ ) {
			for( int dstX = 0; dstX < width; dstX++ ) {
				// should be 50 outside of the crop region
				if( dstX < x0 || dstX >= x1 || dstY < y0 || dstY >= y1 )
					assertEquals(50,GeneralizedImageOps.get(dst,dstX,dstY),1e-4);
				else {
					int srcX = dstX + offX;
					int srcY = dstY + offY;

					double dstVal = GeneralizedImageOps.get(dst,dstX,dstY);

					if( src.isInBounds(srcX,srcY) ) {
						double srcVal = GeneralizedImageOps.get(src,srcX,srcY);
						assertEquals(srcVal,dstVal,1e-4);
					} else if( renderAll ) {
						assertEquals(0,dstVal,1e-4);
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

	public void testOutsideCrop( boolean renderAll ) {
		// the crop region
		int x0=12,y0=10,x1=width,y1=height;

		T src = generator.createInstance(width,height);
		T dst = generator.createInstance(width,height);

		GImageMiscOps.fillUniform(src, rand, 0, 10);
		GImageMiscOps.fill(dst, 50);

		ImageDistort<T,T> tran = createDistort(new BasicTransform(),interp);
		tran.setRenderAll(renderAll);
		tran.apply(src,dst,x0,y0,x1,y1);

		for( int dstY = 0; dstY < height; dstY++ ) {
			for( int dstX = 0; dstX < width; dstX++ ) {
				// should be 50 outside of the crop region
				if( dstX < x0 || dstX >= x1 || dstY < y0 || dstY >= y1 )
					assertEquals(50,GeneralizedImageOps.get(dst,dstX,dstY),1e-4);
				else {
					int srcX = dstX + offX;
					int srcY = dstY + offY;

					double dstVal = GeneralizedImageOps.get(dst,dstX,dstY);

					if( src.isInBounds(srcX,srcY) ) {
						double srcVal = GeneralizedImageOps.get(src,srcX,srcY);
						assertEquals(srcVal,dstVal,1e-4);
					} else if( renderAll ) {
						assertEquals(0,dstVal,1e-4);
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
