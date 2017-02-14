/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.core.image;

import boofcv.abst.filter.FilterImageInterface;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

/**
 * Use the filter interface to convert the image type using {@link GConvertImage}.
 *
 * @author Peter Abeles
 */
public class ConvertImageFilter<I extends ImageBase<I>,O extends ImageBase<O>>
	implements FilterImageInterface<I,O>
{
	ImageType<I> typeSrc;
	ImageType<O> typeDst;

	public ConvertImageFilter(ImageType<I> typeSrc, ImageType<O> typeDst) {
		this.typeSrc = typeSrc;
		this.typeDst = typeDst;
	}

	@Override
	public void process(I src, O dst) {
		GConvertImage.convert(src,dst);
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
	public ImageType<I> getInputType() {
		return typeSrc;
	}

	@Override
	public ImageType<O> getOutputType() {
		return typeDst;
	}
}
