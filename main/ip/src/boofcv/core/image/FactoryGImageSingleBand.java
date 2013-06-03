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
public class FactoryGImageSingleBand {

	public static GImageSingleBand wrap( ImageSingleBand image ) {
		if( image.getClass() == ImageUInt8.class )
			return new GSingle_U8( (ImageUInt8)image );
		else if( image.getClass() == ImageSInt8.class )
			return new GSingle_S8( (ImageSInt8)image );
		else if( image.getClass() == ImageUInt16.class )
			return new GSingle_U16( (ImageUInt16)image );
		else if( image.getClass() == ImageSInt16.class )
			return new GSingle_S16( (ImageSInt16)image );
		else if( image.getClass() == ImageSInt32.class )
			return new GSingle_S32( (ImageSInt32)image );
		else if( image.getClass() == ImageSInt64.class )
			return new GSingle_I64( (ImageSInt64)image );
		else if( image.getClass() == ImageFloat32.class )
			return new GSingle_F32( (ImageFloat32)image );
		else if( image.getClass() == ImageFloat64.class )
			return new GSingle_F64( (ImageFloat64)image );
		else
			throw new IllegalArgumentException("Unknown image type: "+image.getClass());
	}

	public static GImageSingleBand wrap( ImageSingleBand image , GImageSingleBand output ) {
		if( output == null )
			return wrap(image);

		if( image.getClass() == ImageUInt8.class )
			((GSingle_U8)output).image = (ImageUInt8)image;
		else if( image.getClass() == ImageSInt8.class )
			((GSingle_S8)output).image = (ImageSInt8)image;
		else if( image.getClass() == ImageUInt16.class )
			((GSingle_U16)output).image = (ImageUInt16)image;
		else if( image.getClass() == ImageSInt16.class )
			((GSingle_S16)output).image = (ImageSInt16)image;
		else if( image.getClass() == ImageSInt32.class )
			((GSingle_S32)output).image = (ImageSInt32)image;
		else if( image.getClass() == ImageSInt64.class )
			((GSingle_I64)output).image = (ImageSInt64)image;
		else if( image.getClass() == ImageFloat32.class )
			((GSingle_F32)output).image = (ImageFloat32)image;
		else if( image.getClass() == ImageFloat64.class )
			((GSingle_F64)output).image = (ImageFloat64)image;
		else
			throw new IllegalArgumentException("Unknown image type: "+image.getClass());

		return output;
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

		@Override
		public void set(int index, float value) {
			throw new RuntimeException("Operation not supported by inner data type");
		}

		@Override
		public float getF(int index) {
			throw new RuntimeException("Operation not supported by inner data type");
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

		@Override
		public void set(int index, float value) {
			throw new RuntimeException("Operation not supported by inner data type");
		}

		@Override
		public float getF(int index) {
			throw new RuntimeException("Operation not supported by inner data type");
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

		@Override
		public void set(int index, float value) {
			throw new RuntimeException("Operation not supported by inner data type");
		}

		@Override
		public float getF(int index) {
			throw new RuntimeException("Operation not supported by inner data type");
		}
	}

	public static class GSingle_U8 extends GSingleBaseInt<ImageUInt8>
	{
		public GSingle_U8(ImageUInt8 image) {
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

		@Override
		public void set(int index, float value) {
			image.data[index] = (byte)value;
		}

		@Override
		public float getF(int index) {
			return image.data[index] & 0xFF;
		}
	}

	public static class GSingle_S8 extends GSingleBaseInt<ImageSInt8>
	{
		public GSingle_S8(ImageSInt8 image) {
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

		@Override
		public void set(int index, float value) {
			image.data[index] = (byte)value;
		}

		@Override
		public float getF(int index) {
			return image.data[index];
		}
	}

	public static class GSingle_U16 extends GSingleBaseInt<ImageUInt16>
	{
		public GSingle_U16(ImageUInt16 image) {
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

		@Override
		public void set(int index, float value) {
			image.data[index] = (short)value;
		}

		@Override
		public float getF(int index) {
			return image.data[index] & 0xFFFF;
		}
	}

	public static class GSingle_S16 extends GSingleBaseInt<ImageSInt16>
	{
		public GSingle_S16(ImageSInt16 image) {
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

		@Override
		public void set(int index, float value) {
			image.data[index] = (short)value;
		}

		@Override
		public float getF(int index) {
			return image.data[index];
		}
	}

	public static class GSingle_S32 extends GSingleBaseInt<ImageSInt32>
	{
		public GSingle_S32(ImageSInt32 image) {
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

		@Override
		public void set(int index, float value) {
			image.data[index] = (short)value;
		}

		@Override
		public float getF(int index) {
			return image.data[index];
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

		@Override
		public void set(int index, float value) {
			image.data[index] = (long)value;
		}

		@Override
		public float getF(int index) {
			return image.data[index];
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

		@Override
		public void set(int index, float value) {
			image.data[index] = value;
		}

		@Override
		public float getF(int index) {
			return image.data[index];
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

		@Override
		public void set(int index, float value) {
			image.data[index] = value;
		}

		@Override
		public float getF(int index) {
			return (float)image.data[index];
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
