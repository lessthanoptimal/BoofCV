/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.detdesc;

import boofcv.alg.feature.detdesc.DetectDescribeSurfPlanar;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.core.image.GConvertImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.feature.BrightFeature;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;
import georegression.struct.point.Point2D_F64;

/**
 * Wrapper around {@link boofcv.alg.feature.detdesc.DetectDescribeSurfPlanar} for {@link DetectDescribePoint}.
 *
 * @param <T> Image band type
 * @param <II> Integral image type
 *
 * @author Peter Abeles
 */
public class SurfPlanar_to_DetectDescribePoint<T extends ImageGray<T>, II extends ImageGray<II>>
		implements DetectDescribePoint<Planar<T>,BrightFeature>
{
	DetectDescribeSurfPlanar<II> alg;

	T gray;
	II grayII;
	Planar<II> bandII;
	ImageType<Planar<T>> inputType;

	public SurfPlanar_to_DetectDescribePoint(DetectDescribeSurfPlanar<II> alg ,
											 Class<T> imageType, Class<II> integralType)
	{
		this.alg = alg;
		this.inputType = ImageType.pl(alg.getDescribe().getNumBands(),imageType);

		gray = GeneralizedImageOps.createSingleBand(imageType, 1, 1);
		grayII = GeneralizedImageOps.createSingleBand(integralType,1,1);
		bandII = new Planar<>(integralType, 1, 1, alg.getDescribe().getNumBands());
	}

	@Override
	public void detect(Planar<T> input) {
		if( input.getNumBands() != bandII.getNumBands() )
			throw new IllegalArgumentException("Unexpected number of bands. Expected "+
					bandII.getNumBands()+" found "+input.getNumBands());
		gray.reshape(input.width,input.height);
		grayII.reshape(input.width,input.height);
		bandII.reshape(input.width,input.height);

		GConvertImage.average(input,gray);
		GIntegralImageOps.transform(gray, grayII);
		for( int i = 0; i < input.getNumBands(); i++)
			GIntegralImageOps.transform(input.getBand(i), bandII.getBand(i));

		alg.detect(grayII,bandII);
	}

	@Override
	public int getNumberOfSets() {
		return 2;
	}

	@Override
	public int getSet(int index) {
		return alg.getDescription(index).white ? 0 : 1;
	}

	@Override
	public boolean hasScale() {
		return true;
	}

	@Override
	public boolean hasOrientation() {
		return true;
	}

	@Override
	public BrightFeature getDescription(int index) {
		return alg.getDescription(index);
	}

	@Override
	public ImageType<Planar<T>> getInputType() {
		return inputType;
	}

	@Override
	public BrightFeature createDescription() {
		return alg.createDescription();
	}

	@Override
	public Class<BrightFeature> getDescriptionType() {
		return BrightFeature.class;
	}

	@Override
	public int getNumberOfFeatures() {
		return alg.getNumberOfFeatures();
	}

	@Override
	public Point2D_F64 getLocation(int featureIndex) {
		return alg.getLocation(featureIndex);
	}

	@Override
	public double getRadius(int featureIndex) {
		return alg.getRadius(featureIndex);
	}

	@Override
	public double getOrientation(int featureIndex) {
		return alg.getOrientation(featureIndex);
	}
}
