/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.d2.stabilization;

import boofcv.alg.distort.DistortImageOps;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.impl.DistortSupport;
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.alg.misc.GPixelMath;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.distort.FactoryDistort;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.distort.PixelTransform_F32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.MultiSpectral;
import georegression.struct.shapes.Rectangle2D_I32;

import java.awt.image.BufferedImage;

/**
 * Creates a mosaic by batching together smaller images with their respective transforms.  This class lends itself
 * towards mosaics that are sequentially built from video images as they arrive.
 *
 * @author Peter Abeles
 */
public class RenderImageMotion<I extends ImageSingleBand, O extends ImageBase> {

	// output a color image or gray scale
	boolean colorOutput;

	// type of input gray scale image
	Class<I> imageType;

	// dimension of the output mosaic
	int mosaicWidth;
	int mosaicHeight;

	// the output mosaic
	O imageMosaic;
	// stores the mosaic when it is transformed
	O tempMosaic;

	// Color input image in a format BoofCV understands
	O frameMulti;

	ImageDistort<O> distorter;

	public RenderImageMotion(int mosaicWidth, int mosaicHeight, Class<I> imageType, boolean color) {
		this.mosaicWidth = mosaicWidth;
		this.mosaicHeight = mosaicHeight;
		this.imageType = imageType;

		setColorOutput(color);
	}
	
	public synchronized void setColorOutput( boolean color ) {
		this.colorOutput = color;

		InterpolatePixel<I> interp = FactoryInterpolation.createPixel(0, 255, TypeInterpolate.BILINEAR, imageType);

		if( colorOutput ) {
			// convert the single band mosaic into a MultiSpectral mosaic
			MultiSpectral<I> temp = new MultiSpectral<I>(imageType,mosaicWidth,mosaicHeight,3);
			if( imageMosaic != null ) {
				for( int i = 0; i < temp.getNumBands(); i++ )
					temp.getBand(i).setTo((ImageSingleBand)imageMosaic);
			}

			imageMosaic = (O)temp;
			tempMosaic = (O)new MultiSpectral<I>(imageType,mosaicWidth,mosaicHeight,3);
			frameMulti = (O)new MultiSpectral<I>(imageType,1,1,3);
			distorter = (ImageDistort<O>) DistortSupport.createDistortMS(imageType, null, interp, null);
		} else {
			// convert the previous mosaic into a gray scale image
			ImageSingleBand temp = GeneralizedImageOps.createSingleBand(imageType, mosaicWidth, mosaicHeight);
			if( imageMosaic != null ) {
				GPixelMath.bandAve((MultiSpectral)imageMosaic,temp);
			}
			imageMosaic = (O) temp;
			tempMosaic = (O)GeneralizedImageOps.createSingleBand(imageType, mosaicWidth, mosaicHeight);
			distorter = (ImageDistort<O>) FactoryDistort.distort( interp,null,imageType);
		}
	}

	/**
	 * Adds the current frame to the mosaic
	 *
	 * @param frame Gray scale image of the current frame.
	 * @param buffImage Color image of the current frame.
	 * @param worldToCurr Transformation from the world into the current frame.
	 */
	public synchronized void update(I frame, BufferedImage buffImage ,
									PixelTransform_F32 worldToCurr ,
									PixelTransform_F32 currToWorld  ) {

		// only process a cropped portion to speed up processing
		Rectangle2D_I32 box = DistortImageOps.boundBox(frame.width,frame.height,
				imageMosaic.width,imageMosaic.height,currToWorld);
		int x0 = box.tl_x;
		int y0 = box.tl_y;
		int x1 = box.tl_x + box.width;
		int y1 = box.tl_y + box.height;

		distorter.setModel(worldToCurr);

		if( colorOutput ) {
			frameMulti.reshape(frame.width,frame.height);
			ConvertBufferedImage.convertFrom(buffImage, frameMulti);

			// make sure it's in an RGB format
			ConvertBufferedImage.orderBandsIntoRGB((MultiSpectral)frameMulti,buffImage);

			distorter.apply(frameMulti, imageMosaic,x0,y0,x1,y1);
		} else {
			distorter.apply((O)frame, imageMosaic,x0,y0,x1,y1);
		}
	}

	/**
	 * Distorts the mosaic's image using the specified transform
	 *
	 * @param oldToNew  Transform from the old mosaic to the new mosaic's coordinate system
	 */
	public synchronized void distortMosaic( PixelTransform_F32 oldToNew ) {
		distorter.setModel(oldToNew);
		GeneralizedImageOps.fill(tempMosaic,0);
		distorter.apply(imageMosaic, tempMosaic);

		// swap the two images
		O s = imageMosaic;
		imageMosaic = tempMosaic;
		tempMosaic = s;
	}

	/**
	 * Returns the mosaic image
	 */
	public O getMosaic() {
		return imageMosaic;
	}

	/**
	 * Makes the mosaic all black.
	 */
	public void clear() {
		GeneralizedImageOps.fill(imageMosaic,0);
	}

	/**
	 * If the output is in color or not
	 */
	public boolean getColorOutput() {
		return colorOutput;
	}
}
