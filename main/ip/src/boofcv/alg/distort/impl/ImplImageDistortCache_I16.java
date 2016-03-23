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

package boofcv.alg.distort.impl;

import boofcv.alg.distort.ImageDistortCache_SB;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.struct.image.GrayI16;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.ImageGray;

/**
 * Implementation of {@link ImageDistortCache_SB} for {@link GrayS16}.
 *
 * @author Peter Abeles
 */
public class ImplImageDistortCache_I16<Input extends ImageGray, Output extends GrayI16>
		extends ImageDistortCache_SB<Input,Output> {
	public ImplImageDistortCache_I16( InterpolatePixelS<Input> interp)
	{
		super(interp);
	}

	@Override
	protected void assign(int indexDst, float value) {
		dstImg.data[indexDst] = (short)value;
	}
}
