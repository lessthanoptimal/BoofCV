/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import org.ddogleg.struct.FastQueue;

/**
 * Dense feature computation which uses {@link boofcv.abst.feature.describe.DescribeRegionPoint} internally.
 *
 * @author Peter Abeles
 */
public class GenericDenseDescribeImageDense<T extends ImageBase, Desc extends TupleDesc>
	implements DescribeImageDense<T,Desc>
{
	// Computes the iamge feature
	DescribeRegionPoint<T,Desc> alg;

	// the scale all the features will be sampled at
	double scale;
	// The width of the area the feature will sample
	int featureWidth;
	// the period at which it will be sampled inside the image
	int periodX;
	int periodY;

	public GenericDenseDescribeImageDense(DescribeRegionPoint<T, Desc> alg, double scale,
										  int featureWidth, int periodX, int periodY) {
		this.alg = alg;
		this.scale = scale;
		this.featureWidth = featureWidth;
		this.periodX = periodX;
		this.periodY = periodY;
	}

	@Override
	public void process(T input, FastQueue<Desc> descriptions) {
		alg.setImage(input);

		int x0 = featureWidth/2;
		int x1 = input.getWidth()-featureWidth/2;
		int y0 = featureWidth/2;
		int y1 = input.getHeight()-featureWidth/2;

		for (int y = y0; y < y1; y += periodY ) {
			for (int x = x0; x < x1; x += periodX ) {
				Desc d = descriptions.grow();

				if( !alg.process(x,y,0,scale,d) ) {
					descriptions.removeTail();
				}
			}
		}
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
