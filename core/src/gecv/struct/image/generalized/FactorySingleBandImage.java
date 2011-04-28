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

package gecv.struct.image.generalized;

import gecv.struct.image.*;

/**
 * Factory for creating generalized images
 *
 * @author Peter Abeles
 */
public class FactorySingleBandImage {

	public static SingleBandImage wrap( ImageBase image ) {
		if( image.getClass() == ImageInt8.class )
			return new SingleBandInt8( (ImageInt8)image );
		else if( image.getClass() == ImageInt16.class )
			return new SingleBandInt16( (ImageInt16)image );
		else if( image.getClass() == ImageInt32.class )
			return new SingleBandInt32( (ImageInt32)image );
		else if( image.getClass() == ImageFloat32.class )
			return new SingleBandFloat32( (ImageFloat32)image );
		else
			throw new IllegalArgumentException("Unknown image type");
	}

	public static class SingleBandInt8 extends SingleBaseInt<ImageInt8>
	{
		public SingleBandInt8(ImageInt8 image) {
			super(image);
		}

		@Override
		public Number get(int x, int y) {
			return image.get(x,y);
		}

		@Override
		public void set(int x, int y, Number num) {
			image.set(x,y,num.intValue());
		}
	}

	public static class SingleBandInt16 extends SingleBaseInt<ImageInt16>
	{
		public SingleBandInt16(ImageInt16 image) {
			super(image);
		}

		@Override
		public Number get(int x, int y) {
			return image.get(x,y);
		}

		@Override
		public void set(int x, int y, Number num) {
			image.set(x,y,num.intValue());
		}
	}

	public static class SingleBandInt32 extends SingleBaseInt<ImageInt32>
	{
		public SingleBandInt32(ImageInt32 image) {
			super(image);
		}

		@Override
		public Number get(int x, int y) {
			return image.get(x,y);
		}

		@Override
		public void set(int x, int y, Number num) {
			image.set(x,y,num.intValue());
		}
	}

	public static class SingleBandFloat32 extends SingleBase<ImageFloat32>
	{
		public SingleBandFloat32(ImageFloat32 image) {
			super(image);
		}

		@Override
		public boolean isFloatingPoint() {
			return true;
		}

		@Override
		public Number get(int x, int y) {
			return image.get(x,y);
		}

		@Override
		public void set(int x, int y, Number num) {
			image.set(x,y,num.floatValue());
		}
	}

	public static abstract class SingleBaseInt<T extends ImageBase> extends SingleBase<T>
	{
		public SingleBaseInt(T image) {
			super(image);
		}

		@Override
		public boolean isFloatingPoint() {
			return false;
		}
	}

	public static abstract class SingleBase<T extends ImageBase> implements SingleBandImage {

		protected T image;

		public SingleBase(T image) {
			this.image = image;
		}

		@Override
		public int getWidth() {
			return image.getWidth();
		}

		@Override
		public int getHeight() {
			return image.getHeight();
		}
	}
}
