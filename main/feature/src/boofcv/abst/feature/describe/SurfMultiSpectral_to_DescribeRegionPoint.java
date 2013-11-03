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

import boofcv.alg.feature.describe.DescribePointSurfMultiSpectral;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.core.image.GConvertImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.feature.SurfFeature;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.MultiSpectral;

/**
 * Wrapper around {@link DescribePointSurfMultiSpectral} for {@link DescribeRegionPoint}
 *
 * @param <T> Image band type
 * @param <II> Integral image type
 *
 * @author Peter Abeles
 */
public class SurfMultiSpectral_to_DescribeRegionPoint<T extends ImageSingleBand, II extends ImageSingleBand>
	implements DescribeRegionPoint<MultiSpectral<T>,SurfFeature>
{
	DescribePointSurfMultiSpectral<II> alg;

	T gray;
	II grayII;
	MultiSpectral<II> bandII;

	ImageType<MultiSpectral<T>> imageType;

	public SurfMultiSpectral_to_DescribeRegionPoint(DescribePointSurfMultiSpectral<II> alg,
													Class<T> imageType, Class<II> integralType ) {
		this.alg = alg;

		gray = GeneralizedImageOps.createSingleBand(imageType, 1, 1);
		grayII = GeneralizedImageOps.createSingleBand(integralType,1,1);
		bandII = new MultiSpectral<II>(integralType,1,1,alg.getNumBands());

		this.imageType = ImageType.ms(alg.getNumBands(), imageType);
	}

	@Override
	public void setImage(MultiSpectral<T> image) {
		gray.reshape(image.width,image.height);
		grayII.reshape(image.width,image.height);
		bandII.reshape(image.width,image.height);

		GConvertImage.average(image, gray);
		GIntegralImageOps.transform(gray, grayII);
		for( int i = 0; i < image.getNumBands(); i++)
			GIntegralImageOps.transform(image.getBand(i), bandII.getBand(i));

		alg.setImage(grayII,bandII);
	}

	@Override
	public boolean process(double x, double y, double orientation, double scale, SurfFeature description) {

		alg.describe(x,y,orientation,scale,description);

		return true;
	}

	@Override
	public boolean requiresScale() {
		return true;
	}

	@Override
	public boolean requiresOrientation() {
		return true;
	}

	@Override
	public ImageType<MultiSpectral<T>> getImageType() {
		return imageType;
	}

	@Override
	public SurfFeature createDescription() {
		return alg.createDescription();
	}

	@Override
	public Class<SurfFeature> getDescriptionType() {
		return SurfFeature.class;
	}
}
