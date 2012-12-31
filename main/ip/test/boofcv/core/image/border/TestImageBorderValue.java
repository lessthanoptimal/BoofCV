/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.core.image.border;

import boofcv.core.image.GImageSingleBand;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageUInt8;

/**
 * @author Peter Abeles
 */
public class TestImageBorderValue extends GenericImageBorderTests {

	float value = 43;

	@Override
	public ImageBorder_I32 wrap(ImageUInt8 image) {
		return ImageBorderValue.wrap(image,(int)value);
	}

	@Override
	public ImageBorder_F32 wrap(ImageFloat32 image) {
		return ImageBorderValue.wrap(image,value);
	}

	@Override
	public Number get(GImageSingleBand img, int x, int y) {
		if( img.getImage().isInBounds(x,y))
			return img.get(x,y);
		return value;
	}

	@Override
	public void checkBorderSet(int x, int y, Number val,
							GImageSingleBand border, GImageSingleBand orig) {
		// the original image should not be modified
	}
}
