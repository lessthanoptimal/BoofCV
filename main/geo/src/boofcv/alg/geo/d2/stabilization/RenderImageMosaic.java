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

package boofcv.alg.geo.d2.stabilization;

import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.PixelTransformHomography_F32;
import boofcv.alg.distort.impl.DistortSupport;
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.MultiSpectral;
import georegression.struct.homo.Homography2D_F32;

import java.awt.image.BufferedImage;

/**
 * Creates a mosaic by batching together smaller images with their respective transforms.  This class lends itself
 * towards mosaics that are sequentially built from video images as they arrive.
 *
 * @author Peter Abeles
 */
public class RenderImageMosaic<I extends ImageSingleBand, O extends ImageBase> {

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

	PixelTransformHomography_F32 distort = new PixelTransformHomography_F32();
	ImageDistort<O> distorter;

	public RenderImageMosaic( int mosaicWidth , int mosaicHeight , Class<I> imageType , boolean color ) {
		this.mosaicWidth = mosaicWidth;
		this.mosaicHeight = mosaicHeight;
		this.imageType = imageType;
		
		setColorOutput(color);
	}
	
	public void setColorOutput( boolean color ) {
		this.colorOutput = color;

		InterpolatePixel<I> interp = FactoryInterpolation.createPixel(0, 255, TypeInterpolate.BILINEAR, imageType);

		if( colorOutput ) {
			imageMosaic = (O)new MultiSpectral<I>(imageType,mosaicWidth,mosaicHeight,3);
			tempMosaic = (O)new MultiSpectral<I>(imageType,mosaicWidth,mosaicHeight,3);
			frameMulti = (O)new MultiSpectral<I>(imageType,1,1,3);
			distorter = (ImageDistort<O>) DistortSupport.createDistortMS(imageType, null, interp, null);
		} else {
			imageMosaic = (O) GeneralizedImageOps.createSingleBand(imageType, mosaicWidth, mosaicHeight);
			tempMosaic = (O)GeneralizedImageOps.createSingleBand(imageType, mosaicWidth, mosaicHeight);
			distorter = (ImageDistort<O>)DistortSupport.createDistort(imageType,null,interp,null);
		}
	}

	/**
	 * Adds the current frame to the mosaic
	 *
	 * @param frame Gray scale image of the current frame.
	 * @param buffImage Color image of the current frame.
	 * @param worldToCurr Transformation from the world into the current frame.
	 */
	public void update(I frame, BufferedImage buffImage , Homography2D_F32 worldToCurr ) {

		distort.set(worldToCurr);
		distorter.setModel(distort);

		if( colorOutput ) {
			frameMulti.reshape(frame.width,frame.height);
			ConvertBufferedImage.convertFrom(buffImage, frameMulti);

			distorter.apply(frameMulti, imageMosaic);
		} else {
			distorter.apply((O)frame, imageMosaic);
		}
	}

	/**
	 * Distorts the mosaic's image using the specified transform
	 *
	 * @param oldToNew  Transform from the old mosaic to the new mosaic's coordinate system
	 */
	public void distortMosaic( Homography2D_F32 oldToNew ) {
		distort.set(oldToNew);
		distorter.setModel(distort);
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
}
