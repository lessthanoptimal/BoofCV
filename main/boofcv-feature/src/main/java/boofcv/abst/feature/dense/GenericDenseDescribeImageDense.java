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

package boofcv.abst.feature.dense;

import boofcv.abst.feature.describe.DescribePointRadiusAngle;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.DogArray;

import java.util.List;

/**
 * Dense feature computation which uses {@link DescribePointRadiusAngle} internally.
 *
 * @author Peter Abeles
 */
public class GenericDenseDescribeImageDense<T extends ImageBase<T>, Desc extends TupleDesc<Desc>>
		implements DescribeImageDense<T, Desc> {
	// Computes the image feature
	DescribePointRadiusAngle<T, Desc> alg;

	// Radius of the "detected" feature
	double radius;
	// The width of the area the feature will sample
	int featureWidth;
	// the period at which it will be sampled inside the image
	int periodX;
	int periodY;

	DogArray<Desc> descriptions;
	DogArray<Point2D_I32> locations = new DogArray<>(Point2D_I32::new);

	/**
	 * Configures dense description.
	 *
	 * @param alg Sparse feature sampler.
	 * @param descriptorScale Relative scale of the descriptor's region
	 * @param samplePeriodX How frequently the image is sampled in pixels. X-axis
	 * @param samplePeriodY How frequently the image is sampled in pixels. Y-axis
	 */
	public GenericDenseDescribeImageDense( DescribePointRadiusAngle<T, Desc> alg,
										   double descriptorScale,
										   double samplePeriodX,
										   double samplePeriodY ) {
		this.alg = alg;
		descriptions = new DogArray<>(alg.getDescriptionType(), alg::createDescription);
		configure(descriptorScale, samplePeriodX, samplePeriodY);
	}

	/**
	 * Configures size of the descriptor and the frequency at which it's computed
	 *
	 * @param descriptorRegionScale Relative size of the descriptor region to its canonical size
	 * @param periodX Period in pixels along x-axis of samples
	 * @param periodY Period in pixels along y-axis of samples
	 */
	public void configure( double descriptorRegionScale, double periodX, double periodY ) {
		this.radius = (alg.getCanonicalWidth()/2.0)*descriptorRegionScale;
		this.periodX = (int)(periodX + 0.5);
		this.periodY = (int)(periodY + 0.5);
		this.featureWidth = (int)(alg.getCanonicalWidth()*descriptorRegionScale + 0.5);
	}

	@Override
	public void process( T input ) {
		if (periodX <= 0 || periodY <= 0)
			throw new IllegalArgumentException("Must call configure() first");

		alg.setImage(input);

		int x0 = featureWidth/2;
		int x1 = input.getWidth() - featureWidth/2;
		int y0 = featureWidth/2;
		int y1 = input.getHeight() - featureWidth/2;

		descriptions.reset();
		locations.reset();

		for (int y = y0; y < y1; y += periodY) {
			for (int x = x0; x < x1; x += periodX) {
				Desc d = descriptions.grow();

				if (!alg.process(x, y, 0, radius, d)) {
					descriptions.removeTail();
				} else {
					locations.grow().setTo(x, y);
				}
			}
		}
	}

	@Override
	public List<Desc> getDescriptions() {
		return descriptions.toList();
	}

	@Override
	public List<Point2D_I32> getLocations() {
		return locations.toList();
	}

	@Override
	public ImageType<T> getImageType() {
		return alg.getImageType();
	}

	@Override
	public Desc createDescription() {
		return alg.createDescription();
	}

	@Override
	public Class<Desc> getDescriptionType() {
		return alg.getDescriptionType();
	}
}
