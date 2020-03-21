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

import boofcv.alg.transform.census.GCensusTransform;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;

/**
 * @author Peter Abeles
 */
public class FilterCensusTransformD33U8<In extends ImageGray<In>>
		extends FilterCensusTransform<In, GrayU8>
{
	public FilterCensusTransformD33U8(ImageBorder<In> border, Class<In> imageType ) {
		super(1,border,imageType, ImageType.SB_U8);
	}

	@Override
	public void process(In in, GrayU8 out) {
		GCensusTransform.dense3x3(in,out,border);
	}
}
