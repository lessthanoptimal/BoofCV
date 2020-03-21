/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.struct.border;

import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayI;
import boofcv.struct.image.ImageGray;
import lombok.Getter;
import lombok.Setter;

/**
 * Wraps a larger image and treats the inner portion as a regular image and uses the border pixels as a look up
 * table for external ones. Primarily used as a debugging class and not very memory efficient.
 *
 * @author Peter Abeles
 */
public abstract class ImageBorderWrapped {
	/**
	 * Creates an ImageBorder for the two specified images. The offsets are created by dividing the difference
	 * inside by 2. Border must be bigger the image.
	 */
	public static <T extends ImageGray<T>> ImageBorder<T> wrap( T border , T image ) {
		int offsetX = (border.width-image.width)/2;
		int offsetY = (border.height-image.height)/2;

		ImageBorder<T> ret;
		if( border instanceof GrayI ) {
			ret = new S32(offsetX,offsetY,(GrayI)border);
		} else if( border instanceof GrayF32 ) {
			ret = (ImageBorder)new F32(offsetX,offsetY,(GrayF32)border);
		} else {
			throw new RuntimeException("Not supported yet");
		}
		ret.setImage(image);
		return ret;
	}

	public static class S32<T extends GrayI<T>> extends ImageBorder_S32<T> {
		/**
		 * Specifies where the image starts inside the larger image
		 */
		public @Getter @Setter int offsetX,offsetY;

		public @Getter @Setter T borderImage;

		public S32(int offsetX, int offsetY, T borderImage) {
			this.offsetX = offsetX;
			this.offsetY = offsetY;
			this.borderImage = borderImage;
		}

		public S32() {
		}

		@Override
		public int getOutside(int x, int y) {
			return borderImage.get(x+offsetX, y+offsetY);
		}

		@Override
		public void setOutside(int x, int y, int value) {
			throw new RuntimeException("Not supported");
		}

		@Override
		public ImageBorder<T> copy() {
			var copy = new S32<T>();
			copy.offsetX = offsetX;
			copy.offsetY = offsetY;
			copy.borderImage = borderImage;
			copy.image = image;
			return copy;
		}
	}

	public static class F32 extends ImageBorder_F32 {
		/**
		 * Specifies where the image starts inside the larger image
		 */
		public @Getter @Setter int offsetX,offsetY;

		public @Getter @Setter GrayF32 borderImage;

		public F32(int offsetX, int offsetY, GrayF32 borderImage) {
			this.offsetX = offsetX;
			this.offsetY = offsetY;
			this.borderImage = borderImage;
		}

		public F32() {}

		@Override
		public float getOutside(int x, int y) {
			return borderImage.get(x+offsetX, y+offsetY);
		}

		@Override
		public void setOutside(int x, int y, float value) {
			throw new RuntimeException("Not supported");
		}

		@Override
		public ImageBorder<GrayF32> copy() {
			var copy = new F32();
			copy.offsetX = offsetX;
			copy.offsetY = offsetY;
			copy.borderImage = borderImage;
			copy.image = image;
			return copy;
		}
	}
}
