/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.describe;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.distort.DistortImageOps;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.ImageRectangle;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageSingleBand;


/**
 * Wrapper for steerable filters.  The input image patch is resized to a characteristic size and then
 * feed into the filter.
 *
 * @author Peter Abeles
 */
public abstract class WrapScaleToCharacteristic <T extends ImageSingleBand, D extends ImageSingleBand>
		implements DescribeRegionPoint<T,TupleDesc_F64>
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

		scaledImage = GeneralizedImageOps.createSingleBand(inputType, w, w);
		scaledDerivX = GeneralizedImageOps.createSingleBand(derivType, w, w);
		scaledDerivY = GeneralizedImageOps.createSingleBand(derivType, w, w);
	}

	@Override
	public TupleDesc_F64 createDescription() {
		return new TupleDesc_F64(getDescriptorLength());
	}

	@Override
	public void setImage(T image) {
		this.image = image;
	}

	@Override
	public boolean isInBounds(double x, double y, double orientation, double scale) {
		int r = (int)Math.ceil(scale*3)+1;

		return BoofMiscOps.checkInside(image,(int)x, (int)y, r+1);
	}

	@Override
	public TupleDesc_F64 process(double x, double y, double theta , double scale, TupleDesc_F64 ret ) {
		if( ret == null )
			ret = createDescription();

		// compute the size of the region at this scale
		int r = (int)Math.ceil(scale*3)+1;

		int pixelX = (int)x;
		int pixelY = (int)y;

		ImageRectangle area = new ImageRectangle(pixelX-r,pixelY-r,pixelX+r+1,pixelY+r+1);

		// create a subimage of this region
		T subImage = (T)image.subimage(area.x0,area.y0,area.x1,area.y1);

		DistortImageOps.scale(subImage,scaledImage, TypeInterpolate.BILINEAR);

		// compute the gradient
		gradient.process(scaledImage, scaledDerivX, scaledDerivY);


		// +1 to avoid edge conditions
		describe(steerR+1,steerR+1,theta,ret);

		return ret;
	}

	protected abstract void describe( int x , int y , double angle , TupleDesc_F64 ret );

	@Override
	public boolean requiresScale() {
		return true;
	}

	@Override
	public boolean requiresOrientation() {
		return true;
	}

	@Override
	public Class<TupleDesc_F64> getDescriptorType() {
		return TupleDesc_F64.class;
	}
}
