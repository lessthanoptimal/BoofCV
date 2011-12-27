/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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
public class FactoryGeneralizedSingleBand {

	public static GImageSingleBand wrap( ImageSingleBand image ) {
		if( ImageInteger.class.isAssignableFrom(image.getClass()) )
			return new GSingle_I32( (ImageInteger)image );
		else if( image.getClass() == ImageSInt64.class )
			return new GSingle_I64( (ImageSInt64)image );
		else if( image.getClass() == ImageFloat32.class )
			return new GSingle_F32( (ImageFloat32)image );
		else if( image.getClass() == ImageFloat64.class )
			return new GSingle_F64( (ImageFloat64)image );
		else
			throw new IllegalArgumentException("Unknown image type: "+image.getClass());
	}

	public static GImageSingleBand wrap( ImageBorder image ) {
		if( ImageInteger.class.isAssignableFrom(image.getImage().getClass()) )
			return new Border_I32( (ImageBorder_I32)image );
		else if( image.getImage().getClass() == ImageFloat32.class )
			return new Border_F32( (ImageBorder_F32)image );
		else if( image.getImage().getClass() == ImageFloat64.class )
			return new Border_F64( (ImageBorder_F64)image );
		else
			throw new IllegalArgumentException("Unknown image type: "+image.getClass());
	}

	public static class Border_I32 extends GSingleBorder<ImageBorder_I32>
	{
		public Border_I32(ImageBorder_I32 image) {
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

	public static class Border_F32 extends GSingleBorder<ImageBorder_F32>
	{
		public Border_F32(ImageBorder_F32 image) {
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

	public static class Border_F64 extends GSingleBorder<ImageBorder_F64>
	{
		public Border_F64(ImageBorder_F64 image) {
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

	public static class GSingle_I32 extends GSingleBaseInt<ImageInteger>
	{
		public GSingle_I32(ImageInteger image) {
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

	public static class GSingle_I64 extends GSingleBaseInt<ImageSInt64>
	{
		public GSingle_I64(ImageSInt64 image) {
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

	public static class GSingle_F32 extends GSingleBase<ImageFloat32>
	{
		public GSingle_F32(ImageFloat32 image) {
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

	public static class GSingle_F64 extends GSingleBase<ImageFloat64>
	{
		public GSingle_F64(ImageFloat64 image) {
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

	public static abstract class GSingleBaseInt<T extends ImageSingleBand> extends GSingleBase<T>
	{
		public GSingleBaseInt(T image) {
			super(image);
		}

		@Override
		public boolean isFloatingPoint() {
			return false;
		}
	}

	public static abstract class GSingleBase<T extends ImageSingleBand> implements GImageSingleBand {

		protected T image;

		public GSingleBase(T image) {
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
		public ImageSingleBand getImage() {
			return image;
		}
	}

	public static abstract class GSingleBorder<T extends ImageBorder> implements GImageSingleBand {

		protected T image;

		public GSingleBorder(T image) {
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
		public ImageSingleBand getImage() {
			return image.getImage();
		}
	}
}
