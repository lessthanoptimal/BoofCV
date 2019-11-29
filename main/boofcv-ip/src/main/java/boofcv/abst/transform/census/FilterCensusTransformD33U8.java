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
import boofcv.alg.transform.census.GCensusTransform;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;

/**
 * @author Peter Abeles
 */
public class FilterCensusTransformD33U8<In extends ImageGray<In>>
		implements FilterImageInterface<In, GrayU8>
{
	ImageBorder<In> border;
	ImageType<In> inputType;

	public FilterCensusTransformD33U8(ImageBorder<In> border, Class<In> imageType ) {
		this.border = border;
		this.inputType = ImageType.single(imageType);
	}

	@Override
	public void process(In in, GrayU8 out) {
		GCensusTransform.dense3x3(in,out,border);
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
	public ImageType<GrayU8> getOutputType() {
		return ImageType.single(GrayU8.class);
	}
}
