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

import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageInteger;

/**
 * All points outside of the image will return the specified value
 *
 * @author Peter Abeles
 */
public class ImageBorderValue {

	public static ImageBorder_F32 wrap( ImageFloat32 image , float value ) {
		return new Value_F32(image,value);
	}

	public static ImageBorder_I wrap( ImageInteger image , int value ) {
		return new Value_I(image,value);
	}

	public static class Value_F32 extends ImageBorder_F32 {
		float value;

		public Value_F32( ImageFloat32 image , float value ) {
			super(image);
			this.value = value;
		}

		@Override
		public float getOutside( int x , int y ) {
			return value;
		}

		@Override
		public void setOutside(int x, int y, float val) {
			// do nothing since it is a constant value
		}
	}

	public static class Value_I extends ImageBorder_I {
		int value;

		public Value_I( ImageInteger image , int value ) {
			super(image);
			this.value = value;
		}

		@Override
		public int getOutside( int x , int y ) {
			return value;
		}

		@Override
		public void setOutside(int x, int y, int value) {
			// do nothing since it is a constant
		}
	}
}
