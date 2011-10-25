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

package boofcv.abst.feature.describe;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.distort.DistortImageOps;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.ImageRectangle;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageBase;


/**
 * Wrapper for steerable filters.  The input image patch is resized to a characteristic size and then
 * feed into the filter.
 *
 * @author Peter Abeles
 */
public abstract class WrapScaleToCharacteristic <T extends ImageBase, D extends ImageBase>
		implements DescribeRegionPoint<T>
{
	protected ImageGradient<T,D> gradient;

	protected T image;
	protected T scaledImage;
	protected D scaledDerivX;
	protected D scaledDerivY;

	int steerR;

	public WrapScaleToCharacteristic( int radiusR ,
								   ImageGradient<T, D> gradient,
								   Class<T> inputType ,
								   Class<D> derivType ) {
		this.gradient = gradient;

		steerR = radiusR;
		int w = steerR*2+1+2;
		// +2 is for image border when computing the image derivative

		scaledImage = GeneralizedImageOps.createImage(inputType,w,w);
		scaledDerivX = GeneralizedImageOps.createImage(derivType,w,w);
		scaledDerivY = GeneralizedImageOps.createImage(derivType,w,w);
	}

	@Override
	public void setImage(T image) {
		this.image = image;

	}


	@Override
	public TupleDesc_F64 process(double x, double y, double theta , double scale, TupleDesc_F64 ret ) {
		// compute the size of the region at this scale
		int r = (int)Math.ceil(scale*3)+1;

		int pixelX = (int)x;
		int pixelY = (int)y;

		ImageRectangle area = new ImageRectangle(pixelX-r,pixelY-r,pixelX+r+1,pixelY+r+1);
		if( !BoofMiscOps.checkInside(image,area) )
			return null;

		// create a subimage of this region
		T subImage = (T)image.subimage(area.x0,area.y0,area.x1,area.y1);

		DistortImageOps.scale(subImage,scaledImage, TypeInterpolate.BILINEAR);

		// compute the gradient
		gradient.process(scaledImage, scaledDerivX, scaledDerivY);


		return describe(steerR+1,steerR+1,theta,ret);
		// +1 to avoid edge conditions
	}

	protected abstract TupleDesc_F64 describe( int x , int y , double angle , TupleDesc_F64 ret );

	@Override
	public boolean requiresScale() {
		return true;
	}

	@Override
	public boolean requiresOrientation() {
		return true;
	}
}
