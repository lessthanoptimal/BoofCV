/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.feature.describe.DescribePointSurfPlanar;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.core.image.GConvertImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;

/**
 * Wrapper around {@link DescribePointSurfPlanar} for {@link DescribePointRadiusAngle}
 *
 * @param <T> Image band type
 * @param <II> Integral image type
 * @author Peter Abeles
 */
public class DescribeSurfPlanar_RadiusAngle<T extends ImageGray<T>, II extends ImageGray<II>>
		implements DescribePointRadiusAngle<Planar<T>, TupleDesc_F64> {
	DescribePointSurfPlanar<II> alg;

	T gray;
	II grayII;
	Planar<II> bandII;

	ImageType<Planar<T>> imageType;
	final double canonicalRadius;

	public DescribeSurfPlanar_RadiusAngle( DescribePointSurfPlanar<II> alg,
										   Class<T> imageType, Class<II> integralType ) {
		this.alg = alg;

		gray = GeneralizedImageOps.createSingleBand(imageType, 1, 1);
		grayII = GeneralizedImageOps.createSingleBand(integralType, 1, 1);
		bandII = new Planar<>(integralType, 1, 1, alg.getNumBands());

		this.imageType = ImageType.pl(alg.getNumBands(), imageType);

		canonicalRadius = alg.getDescribe().getCanonicalWidth()/2.0;
	}

	@Override
	public void setImage( Planar<T> image ) {
		gray.reshape(image.width, image.height);
		grayII.reshape(image.width, image.height);
		bandII.reshape(image.width, image.height);

		GConvertImage.average(image, gray);
		GIntegralImageOps.transform(gray, grayII);
		for (int i = 0; i < image.getNumBands(); i++)
			GIntegralImageOps.transform(image.getBand(i), bandII.getBand(i));

		alg.setImage(grayII, bandII);
	}

	@Override
	public boolean process( double x, double y, double orientation, double radius, TupleDesc_F64 description ) {

		double scale = radius/canonicalRadius;
		alg.describe(x, y, orientation, scale, description);

		return true;
	}

	@Override
	public boolean isScalable() {
		return true;
	}

	@Override
	public boolean isOriented() {
		return true;
	}

	@Override
	public ImageType<Planar<T>> getImageType() {
		return imageType;
	}

	@Override
	public double getCanonicalWidth() {
		return alg.getDescribe().getCanonicalWidth();
	}

	@Override
	public TupleDesc_F64 createDescription() {
		return alg.createDescription();
	}

	@Override
	public Class<TupleDesc_F64> getDescriptionType() {
		return TupleDesc_F64.class;
	}
}
