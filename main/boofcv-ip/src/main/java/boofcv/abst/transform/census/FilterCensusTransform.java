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

package boofcv.abst.transform.census;

import boofcv.abst.filter.FilterImageInterface;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastAccess;

/**
 * Filter implementation of {@link boofcv.alg.transform.census.CensusTransform}.
 *
 * @author Peter Abeles
 */
public abstract class FilterCensusTransform<In extends ImageGray<In>, Out extends ImageBase<Out>>
		implements FilterImageInterface<In, Out>
{
	ImageBorder<In> border;       // How the border for the input image is handled
	ImageType<In> inputType;      // Input image type
	ImageType<Out> outType;       // Output image type
	int ignoreRadius;             // Size of the region along the image border which is ignored
	int sampleRadius;             // Radius of the local region sampled along y-axis

	public FilterCensusTransform(int radius, ImageBorder<In> border, Class<In> imageType , ImageType<Out> outType ) {
		this.border = border;
		this.inputType = ImageType.single(imageType);
		this.outType = outType;
		this.sampleRadius = radius;
		this.ignoreRadius = border == null ? radius : 0;
	}

	/**
	 * return the maximum distance away a coordinate is sampled
	 */
	static protected int computeRadius( final FastAccess<Point2D_I32> sample ) {
		int radius = 0;
		for (int i = 0; i < sample.size; i++) {
			Point2D_I32 p = sample.get(i);
			radius = Math.max(radius,Math.abs(p.x));
			radius = Math.max(radius,Math.abs(p.y));
		}
		return radius;
	}

	/**
	 * Radius of the local region sampled along x-axis
	 */
	public int getRadiusX() {
		return sampleRadius;
	}

	/**
	 * Radius of the local region sampled along y-axis
	 */
	public int getRadiusY() {
		return sampleRadius;
	}

	@Override
	public int getBorderX() {
		return ignoreRadius;
	}

	@Override
	public int getBorderY() {
		return ignoreRadius;
	}

	@Override
	public ImageType<In> getInputType() {
		return inputType;
	}

	@Override
	public ImageType<Out> getOutputType() {
		return outType;
	}
}
