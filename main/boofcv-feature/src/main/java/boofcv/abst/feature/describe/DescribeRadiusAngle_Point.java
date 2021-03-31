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

import boofcv.abst.feature.orientation.OrientationImage;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import lombok.Getter;

/**
 * Convert {@link DescribePointRadiusAngle} into {@link DescribePoint}. Internally it uses {@link OrientationImage}
 * to estimate the feature's angle and {@link DescribePointRadiusAngle} to compute the descriptor. The region's
 * radius is user provided.
 *
 * @author Peter Abeles
 */
public class DescribeRadiusAngle_Point<T extends ImageGray<T>, TD extends TupleDesc<TD>>
		implements DescribePoint<T, TD> {

	/** Size of the region's radius. If set to a value &le; 0, then it will be changed to canonical radius */
	public double regionRadius;

	@Getter DescribePointRadiusAngle<T, TD> describer;
	@Getter OrientationImage<T> orientationEstimator;

	public DescribeRadiusAngle_Point( DescribePointRadiusAngle<T, TD> describer,
									  OrientationImage<T> orientationEstimator,
									  double regionRadius ) {
		this.describer = describer;
		this.orientationEstimator = orientationEstimator;
		this.regionRadius = regionRadius;
	}

	@Override public void setImage( T image ) {
		// If the radius is specified to be the default radius, update it
		if (regionRadius <= 0.0)
			regionRadius = describer.getCanonicalWidth()/2;

		// Tell the orientation estimator what the radius is
		orientationEstimator.setObjectRadius(regionRadius);

		// Pass in the image
		describer.setImage(image);
		orientationEstimator.setImage(image);
	}

	@Override public boolean process( double x, double y, TD description ) {
		// First figure out the feature's orientation
		double angle = orientationEstimator.compute(x, y);
		return describer.process(x, y, angle, regionRadius, description);
	}

	@Override public ImageType<T> getImageType() {return describer.getImageType();}

	@Override public TD createDescription() {return describer.createDescription();}

	@Override public Class<TD> getDescriptionType() {return describer.getDescriptionType();}
}
