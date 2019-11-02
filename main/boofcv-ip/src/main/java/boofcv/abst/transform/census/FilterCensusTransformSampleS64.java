/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.transform.census.CensusTransform;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.image.GrayS64;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;

/**
 * @author Peter Abeles
 */
public class FilterCensusTransformSampleS64<In extends ImageGray<In>>
		implements FilterImageInterface<In, GrayS64>
{
	ImageBorder<In> border;
	ImageType<In> inputType;
	FastQueue<Point2D_I32> samples;
	GrowQueue_I32 workSpace = new GrowQueue_I32();

	public FilterCensusTransformSampleS64(FastQueue<Point2D_I32> samples , ImageBorder<In> border, Class<In> imageType ) {
		this.samples = samples;
		this.border = border;
		this.inputType = ImageType.single(imageType);
	}

	@Override
	public void process(In in, GrayS64 out) {
		CensusTransform.sample(in,samples,out,border,workSpace);
	}

	@Override
	public int getHorizontalBorder() {
		return 0;
	}

	@Override
	public int getVerticalBorder() {
		return 0;
	}

	@Override
	public ImageType<In> getInputType() {
		return inputType;
	}

	@Override
	public ImageType<GrayS64> getOutputType() {
		return ImageType.single(GrayS64.class);
	}
}
