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

import static org.junit.Assert.assertEquals;


/**
 * @author Peter Abeles
 */
public class TestImageBorder1D extends GenericImageBorderTests {

	BorderIndex1D_Wrap wrap = new BorderIndex1D_Wrap();

	@Override
	public ImageBorder_I32 wrap(ImageUInt8 image) {
		ImageBorder_I32 ret = new ImageBorder1D_I32(BorderIndex1D_Wrap.class);
		ret.setImage(image);
		return ret;
	}

	@Override
	public ImageBorder_F32 wrap(ImageFloat32 image) {
		ImageBorder_F32 ret = new ImageBorder1D_F32(BorderIndex1D_Wrap.class);
		ret.setImage(image);
		return ret;
	}

	@Override
	public Number get(GImageSingleBand img, int x, int y) {
		wrap.setLength(img.getWidth());
		x = wrap.getIndex(x);
		wrap.setLength(img.getHeight());
		y = wrap.getIndex(y);

		return img.get(x,y);
	}

	@Override
	public void checkBorderSet(int x, int y, Number val, GImageSingleBand border, GImageSingleBand orig) {
		border.set(x,y,val);

		wrap.setLength(orig.getWidth());
		x = wrap.getIndex(x);
		wrap.setLength(orig.getHeight());
		y = wrap.getIndex(y);

		assertEquals(val.floatValue(),orig.get(x,y).floatValue(),1e-4);
	}
}
