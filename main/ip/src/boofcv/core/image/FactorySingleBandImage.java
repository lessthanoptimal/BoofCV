/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

import boofcv.core.image.border.ImageBorder;
import boofcv.core.image.border.ImageBorder_F32;
import boofcv.core.image.border.ImageBorder_F64;
import boofcv.core.image.border.ImageBorder_I32;
import boofcv.struct.image.*;

/**
 * Factory for creating generalized images
 *
 * @author Peter Abeles
 */
public class FactorySingleBandImage {

	public static SingleBandImage wrap( ImageBase image ) {
		if( ImageInteger.class.isAssignableFrom(image.getClass()) )
			return new SingleBandInt( (ImageInteger)image );
		else if( image.getClass() == ImageSInt64.class )
			return new SingleBandInt64( (ImageSInt64)image );
		else if( image.getClass() == ImageFloat32.class )
			return new SingleBandFloat32( (ImageFloat32)image );
		else if( image.getClass() == ImageFloat64.class )
			return new SingleBandFloat64( (ImageFloat64)image );
		else
			throw new IllegalArgumentException("Unknown image type: "+image.getClass());
	}

	public static SingleBandImage wrap( ImageBorder image ) {
		if( ImageInteger.class.isAssignableFrom(image.getImage().getClass()) )
			return new SingleBandBorderInt( (ImageBorder_I32)image );
		else if( image.getImage().getClass() == ImageFloat32.class )
			return new SingleBandBorderFloat32( (ImageBorder_F32)image );
		else if( image.getImage().getClass() == ImageFloat64.class )
			return new SingleBandBorderFloat64( (ImageBorder_F64)image );
		else
			throw new IllegalArgumentException("Unknown image type: "+image.getClass());
	}

	public static class SingleBandBorderInt extends SingleBorder<ImageBorder_I32>
	{
		public SingleBandBorderInt(ImageBorder_I32 image) {
			super(image);
		}

		@Override
		public boolean isFloatingPoint() {
			return false;
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

	public static class SingleBandBorderFloat32 extends SingleBorder<ImageBorder_F32>
	{
		public SingleBandBorderFloat32(ImageBorder_F32 image) {
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

	public static class SingleBandBorderFloat64 extends SingleBorder<ImageBorder_F64>
	{
		public SingleBandBorderFloat64(ImageBorder_F64 image) {
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

	public static class SingleBandInt extends SingleBaseInt<ImageInteger>
	{
		public SingleBandInt(ImageInteger image) {
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

	public static class SingleBandInt64 extends SingleBaseInt<ImageSInt64>
	{
		public SingleBandInt64(ImageSInt64 image) {
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

	public static class SingleBandFloat64 extends SingleBase<ImageFloat64>
	{
		public SingleBandFloat64(ImageFloat64 image) {
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
			image.set(x,y,num.doubleValue());
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

		@Override
		public ImageBase getImage() {
			return image;
		}
	}

	public static abstract class SingleBorder<T extends ImageBorder> implements SingleBandImage {

		protected T image;

		public SingleBorder(T image) {
			this.image = image;
		}

		@Override
		public int getWidth() {
			return image.getImage().getWidth();
		}

		@Override
		public int getHeight() {
			return image.getImage().getHeight();
		}

		@Override
		public ImageBase getImage() {
			return image.getImage();
		}
	}
}
