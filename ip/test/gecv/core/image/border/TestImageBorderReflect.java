/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.core.image.border;

import gecv.core.image.SingleBandImage;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageUInt8;

import static org.junit.Assert.assertEquals;


/**
 * @author Peter Abeles
 */
public class TestImageBorderReflect extends GenericImageBorderTests{


	@Override
	public ImageBorder_I wrap(ImageUInt8 image) {
		return ImageBorderReflect.wrap(image);
	}

	@Override
	public ImageBorder_F32 wrap(ImageFloat32 image) {
		return ImageBorderReflect.wrap(image);
	}

	@Override
	public Number get(SingleBandImage img, int x, int y) {
		if( x < 0 )
			x = -x-1;
		else if( x >= width )
			x = width-1-(x-width);

		if( y < 0 )
			y = -y-1;
		else if( y >= height )
			y = height-1-(y-height);
		
		return img.get(x,y);
	}

	@Override
	public void checkBorderSet(int x, int y, Number val,
							SingleBandImage border, SingleBandImage orig) {
		if( x < 0 )
			x = -x-1;
		else if( x >= width )
			x = width-1-(x-width);

		if( y < 0 )
			y = -y-1;
		else if( y >= height )
			y = height-1-(y-height);

		assertEquals(val.floatValue(),orig.get(x,y).floatValue(),1e-4f);
	}
}
