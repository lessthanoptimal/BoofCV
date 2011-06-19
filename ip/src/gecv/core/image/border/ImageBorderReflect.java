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

import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageInteger;

/**
 * Maps outside pixels back into the image.  It is assumed that a pixels is not more than
 * the image's width/height outside of the image.
 *
 * @author Peter Abeles
 */
public class ImageBorderReflect {

	@SuppressWarnings({"unchecked"})
	public static <T extends ImageBase> ImageBorder<T> wrap( T image ) {
		if( ImageFloat32.class == image.getClass()) {
			return (ImageBorder<T>)wrap(((ImageFloat32)image));
		} else if( ImageInteger.class.isAssignableFrom(image.getClass())) {
			return (ImageBorder<T>)wrap(((ImageInteger)image));
		} else {
			throw new IllegalArgumentException("Unknown image type");
		}
	}

	public static ImageBorder_F32 wrap( ImageFloat32 image ) {
		return new ImageBorder_F32( image ) {
			@Override
			public float getOutside( int x , int y ) {
				if( x < 0 ) {
					x = -1-x;
				} else if( x >= image.width)
					x = image.width-1-(x-image.width);

				if( y < 0 ) {
					y = -1-y;
				} else if( y >= image.height)
					y = image.height-1-(y-image.height);

				return image.get(x,y);
			}

			@Override
			public void setOutside(int x, int y, float val) {
				if( x < 0 ) {
					x = -1-x;
				} else if( x >= image.width)
					x = image.width-1-(x-image.width);

				if( y < 0 ) {
					y = -1-y;
				} else if( y >= image.height)
					y = image.height-1-(y-image.height);

				image.set(x,y,val);
			}
		};
	}

	public static ImageBorder_I wrap( ImageInteger image ) {
		return new ImageBorder_I( image ) {
			@Override
			public int getOutside( int x , int y ) {
				if( x < 0 ) {
					x = -1-x;
				} else if( x >= image.width)
					x = image.width-1-(x-image.width);

				if( y < 0 ) {
					y = -1-y;
				} else if( y >= image.height)
					y = image.height-1-(y-image.height);

				return image.get(x,y);
			}

			@Override
			public void setOutside(int x, int y, int val) {
				if( x < 0 ) {
					x = -1-x;
				} else if( x >= image.width)
					x = image.width-1-(x-image.width);

				if( y < 0 ) {
					y = -1-y;
				} else if( y >= image.height)
					y = image.height-1-(y-image.height);

				image.set(x,y,val);
			}
		};
	}
}
