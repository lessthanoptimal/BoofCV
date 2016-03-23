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

import boofcv.alg.distort.ImageDistortBasic_SB;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.struct.image.GrayI8;
import boofcv.struct.image.ImageGray;


/**
 * <p>Implementation of {@link boofcv.alg.distort.ImageDistortBasic_SB}.</p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"UnnecessaryLocalVariable"})
public class ImplImageDistort_I8<Input extends ImageGray, Output extends GrayI8>
		extends ImageDistortBasic_SB<Input,Output> {

	public ImplImageDistort_I8(InterpolatePixelS<Input> interp) {
		super(interp);
	}

	@Override
	protected void assign(int indexDst, float value) {
		dstImg.data[indexDst] = (byte)value;
	}
}
