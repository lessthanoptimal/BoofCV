/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.feature.dense.DescribeDenseHogAlg;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastQueue;

import java.util.List;

/**
 * Implementation of {@link DescribeImageDense} for {@link DescribeDenseHogAlg}.  This adds the capability to scale
 * the
 *
 * @author Peter Abeles
 */
public class DescribeImageDenseHoG<T extends ImageBase> implements DescribeImageDense<T,TupleDesc_F64> {

	DescribeDenseHogAlg<T,?> hog;

	public DescribeImageDenseHoG(DescribeDenseHogAlg<T, ?> hog) {
		this.hog = hog;
	}

	// TODO remove period from interface.
	// TODO rescale the image
	@Override
	public void configure(double descriptorScale, double periodX, double periodY) {

	}

	@Override
	public void process(T input) {

		// TODO handle scaling
		hog.setInput(input);
		hog.process();

		// center region locations to make it compliant with this interface
		FastQueue<Point2D_I32> locations = hog.getLocations();
		int r = hog.getRegionWidthPixel()/2;
		for (int i = 0; i < locations.size(); i++) {
			Point2D_I32 p = locations.get(i);
			p.x += r;
			p.y += r;
		}
	}

	@Override
	public List<TupleDesc_F64> getDescriptions() {
		return hog.getDescriptions().toList();
	}

	@Override
	public List<Point2D_I32> getLocations() {
		return hog.getLocations().toList();
	}

	@Override
	public ImageType<T> getImageType() {
		return hog.getImageType();
	}

	@Override
	public TupleDesc_F64 createDescription() {
		return hog.createDescription();
	}

	@Override
	public Class<TupleDesc_F64> getDescriptionType() {
		return TupleDesc_F64.class;
	}
}
