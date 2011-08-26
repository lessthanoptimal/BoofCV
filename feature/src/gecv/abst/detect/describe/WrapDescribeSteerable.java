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

package gecv.abst.detect.describe;

import gecv.abst.filter.derivative.ImageGradient;
import gecv.alg.distort.DistortImageOps;
import gecv.alg.feature.describe.DescribePointSteerable2D;
import gecv.alg.feature.orientation.OrientationGradient;
import gecv.alg.interpolate.TypeInterpolate;
import gecv.core.image.GeneralizedImageOps;
import gecv.misc.GecvMiscOps;
import gecv.struct.ImageRectangle;
import gecv.struct.feature.TupleFeature_F64;
import gecv.struct.image.ImageBase;


/**
 * Computes image features using a steerable filter.  An image patch is rescaled
 * to the size of the steerable filter.  The size of the image patch is dependent
 * upon estimated scale of the feature.
 *
 * @author Peter Abeles
 */
public class WrapDescribeSteerable <T extends ImageBase, D extends ImageBase>
		implements ExtractFeatureDescription<T>
{
	DescribePointSteerable2D<T,?> steer;
	OrientationGradient<D> orientation;
	ImageGradient<T,D> gradient;

	T image;
	T scaledImage;
	D scaledDerivX;
	D scaledDerivY;

	int steerR;

	public WrapDescribeSteerable(DescribePointSteerable2D<T,?> steer,
								 OrientationGradient<D> orientation,
								 ImageGradient<T, D> gradient,
								 Class<T> inputType ,
								 Class<D> derivType ) {
		this.steer = steer;
		this.orientation = orientation;
		this.gradient = gradient;

		steerR = steer.getRadius();
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
	public TupleFeature_F64 process(int x, int y, double scale) {

		// compute the size of the region at this scale
		int r = (int)Math.ceil(scale*3)+1;

		ImageRectangle area = new ImageRectangle(x-r,y-r,x+r+1,y+r+1);
		if( !GecvMiscOps.checkInside(image,area) )
			return null;

		// create a subimage of this region
		T subImage = (T)image.subimage(area.x0,area.y0,area.x1,area.y1);

		DistortImageOps.scale(subImage,scaledImage, TypeInterpolate.BILINEAR);

		// compute the gradient
		gradient.process(scaledImage, scaledDerivX, scaledDerivY);

		// estimate the angle
		orientation.setImage(scaledDerivX,scaledDerivY);
		double angle = orientation.compute(steerR+1,steerR+1);

		// extract descriptor
		steer.setImage(scaledImage);
		return steer.describe(steerR+1,steerR+1,angle);
		// +1 to avoid edge conditions
	}
}
