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
import gecv.alg.feature.describe.DescribePointSteerable2D;
import gecv.alg.filter.blur.GBlurImageOps;
import gecv.core.image.GeneralizedImageOps;
import gecv.factory.filter.kernel.FactoryKernelGaussian;
import gecv.misc.GecvMiscOps;
import gecv.struct.ImageRectangle;
import gecv.struct.feature.TupleFeature_F64;
import gecv.struct.image.ImageBase;

import java.util.Random;


/**
 * @author Peter Abeles
 */
public class WrapDescribeSteerable <T extends ImageBase, D extends ImageBase>
		implements ExtractFeatureDescription<T>
{
	DescribePointSteerable2D<T,D,?> steer;
	ImageGradient<T,D> gradient;

	T image;
	D derivX;
	D derivY;
	T blurredRegion;

	Random rand = new Random();

	public WrapDescribeSteerable(DescribePointSteerable2D<T, D, ?> steer,
								 ImageGradient<T, D> gradient,
								 Class<D> derivType ) {
		this.steer = steer;
		this.gradient = gradient;
		derivX = GeneralizedImageOps.createImage(derivType,1,1);
		derivY = GeneralizedImageOps.createImage(derivType,1,1);
	}

	@Override
	public void setImage(T image) {
		this.image = image;
		if( blurredRegion == null ) {
			blurredRegion = (T)image._createNew(1,1);
		}
		derivX.reshape(image.width,image.height);
		derivY.reshape(image.width,image.height);
		gradient.process(image,derivX,derivY);
	}

	@Override
	public TupleFeature_F64 process(int x, int y, double scale) {
		// determine the bounds of a region surrounding the pixel
		int r = FactoryKernelGaussian.radiusForSigma(scale,0);

		ImageRectangle area = new ImageRectangle(x-r*2,y-r*2,x+r*2+1,y+r*2+1);
		GecvMiscOps.boundRectangleInside(image,area);

		// create a subimage of this region
		T subImage = (T)image.subimage(area.x0,area.y0,area.x1,area.y1);
		D subDX = (D)derivX.subimage(area.x0,area.y0,area.x1,area.y1);
		D subDY = (D)derivY.subimage(area.x0,area.y0,area.x1,area.y1);

		// blur this subimage to the appropriate scale
		blurredRegion.reshape(subImage.width,subImage.height);
		GBlurImageOps.gaussian(subImage,blurredRegion,scale,r,null);

		// extract descriptor
		steer.setImage(blurredRegion,subDX,subDY);
		return steer.describe(x-area.x0,y-area.y0);
	}
}
