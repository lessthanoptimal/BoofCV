/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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
import boofcv.core.image.border.ImageBorder_S32;
import boofcv.struct.image.*;

/**
 * Factory for creating generalized images
 *
 * @author Peter Abeles
 */
public class FactoryGImageGray {

	public static GImageGray create( Class imageType ) {
		if( imageType == GrayU8.class )
			return new GSingle_U8(null);
		else if( imageType == GrayS8.class )
			return new GSingle_S8( null );
		else if( imageType == GrayU16.class )
			return new GSingle_U16( null );
		else if( imageType == GrayS16.class )
			return new GSingle_S16( null );
		else if( imageType == GrayS32.class )
			return new GSingle_S32( null );
		else if( imageType == GrayS64.class )
			return new GSingle_I64( null );
		else if( imageType == GrayF32.class )
			return new GSingle_F32( null );
		else if( imageType == GrayF64.class )
			return new GSingle_F64( null );
		else
			throw new IllegalArgumentException("Unknown image type: "+imageType);
	}

	public static GImageGray wrap( ImageGray image ) {
		if( image.getClass() == GrayU8.class )
			return new GSingle_U8( (GrayU8)image );
		else if( image.getClass() == GrayS8.class )
			return new GSingle_S8( (GrayS8)image );
		else if( image.getClass() == GrayU16.class )
			return new GSingle_U16( (GrayU16)image );
		else if( image.getClass() == GrayS16.class )
			return new GSingle_S16( (GrayS16)image );
		else if( image.getClass() == GrayS32.class )
			return new GSingle_S32( (GrayS32)image );
		else if( image.getClass() == GrayS64.class )
			return new GSingle_I64( (GrayS64)image );
		else if( image.getClass() == GrayF32.class )
			return new GSingle_F32( (GrayF32)image );
		else if( image.getClass() == GrayF64.class )
			return new GSingle_F64( (GrayF64)image );
		else
			throw new IllegalArgumentException("Unknown image type: "+image.getClass());
	}

	public static GImageGray wrap(ImageGray image , GImageGray output ) {
		if( output == null )
			return wrap(image);

		if( image.getClass() == GrayU8.class )
			((GSingle_U8)output).image = (GrayU8)image;
		else if( image.getClass() == GrayS8.class )
			((GSingle_S8)output).image = (GrayS8)image;
		else if( image.getClass() == GrayU16.class )
			((GSingle_U16)output).image = (GrayU16)image;
		else if( image.getClass() == GrayS16.class )
			((GSingle_S16)output).image = (GrayS16)image;
		else if( image.getClass() == GrayS32.class )
			((GSingle_S32)output).image = (GrayS32)image;
		else if( image.getClass() == GrayS64.class )
			((GSingle_I64)output).image = (GrayS64)image;
		else if( image.getClass() == GrayF32.class )
			((GSingle_F32)output).image = (GrayF32)image;
		else if( image.getClass() == GrayF64.class )
			((GSingle_F64)output).image = (GrayF64)image;
		else
			throw new IllegalArgumentException("Unknown image type: "+image.getClass());

		return output;
	}

	public static GImageGray wrap( ImageBorder image ) {
		if( GrayI.class.isAssignableFrom(image.getImage().getClass()) )
			return new Border_I32( (ImageBorder_S32)image );
		else if( image.getImage().getClass() == GrayF32.class )
			return new Border_F32( (ImageBorder_F32)image );
		else if( image.getImage().getClass() == GrayF64.class )
			return new Border_F64( (ImageBorder_F64)image );
		else
			throw new IllegalArgumentException("Unknown image type: "+image.getClass());
	}

	public static class Border_I32 extends GSingleBorder<ImageBorder_S32>
	{
		public Border_I32(ImageBorder_S32 image) {
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
		public double unsafe_getD(int x, int y) {
			return image.get(x,y);
		}

		@Override
		public float unsafe_getF(int x, int y) {
			return image.get(x,y);
		}

		@Override
		public void set(int index, float value) {
			throw new RuntimeException("Operation not supported by inner data type");
		}

		@Override
		public float getF(int index) {
			throw new RuntimeException("Operation not supported by inner data type");
		}

		@Override
		public Class getImageType() {
			return getImage().getImageType().getImageClass();
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
		public double unsafe_getD(int x, int y) {
			return image.get(x,y);
		}

		@Override
		public float unsafe_getF(int x, int y) {
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

		@Override
		public Class getImageType() {
			return GrayF32.class;
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
		public double unsafe_getD(int x, int y) {
			return image.get(x,y);
		}

		@Override
		public float unsafe_getF(int x, int y) {
			return (float)image.get(x,y);
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

		@Override
		public Class getImageType() {
			return GrayF64.class;
		}
	}

	public static class GSingle_U8 extends GSingleBaseInt<GrayU8>
	{
		public GSingle_U8(GrayU8 image) {
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
		public double unsafe_getD(int x, int y) {
			return image.unsafe_get(x,y);
		}

		@Override
		public float unsafe_getF(int x, int y) {
			return image.unsafe_get(x,y);
		}

		@Override
		public void set(int index, float value) {
			image.data[index] = (byte)value;
		}

		@Override
		public float getF(int index) {
			return image.data[index] & 0xFF;
		}

		@Override
		public Class getImageType() {
			return GrayU8.class;
		}
	}

	public static class GSingle_S8 extends GSingleBaseInt<GrayS8>
	{
		public GSingle_S8(GrayS8 image) {
			super(image);
		}

		@Override
		public Number get(int x, int y) {
			return image.get(x,y);
		}

		@Override
		public double unsafe_getD(int x, int y) {
			return image.unsafe_get(x,y);
		}

		@Override
		public float unsafe_getF(int x, int y) {
			return image.unsafe_get(x,y);
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

		@Override
		public Class getImageType() {
			return GrayS8.class;
		}
	}

	public static class GSingle_U16 extends GSingleBaseInt<GrayU16>
	{
		public GSingle_U16(GrayU16 image) {
			super(image);
		}

		@Override
		public Number get(int x, int y) {
			return image.get(x,y);
		}

		@Override
		public double unsafe_getD(int x, int y) {
			return image.unsafe_get(x,y);
		}

		@Override
		public float unsafe_getF(int x, int y) {
			return image.unsafe_get(x,y);
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

		@Override
		public Class getImageType() {
			return GrayU16.class;
		}
	}

	public static class GSingle_S16 extends GSingleBaseInt<GrayS16>
	{
		public GSingle_S16(GrayS16 image) {
			super(image);
		}

		@Override
		public Number get(int x, int y) {
			return image.get(x,y);
		}

		@Override
		public double unsafe_getD(int x, int y) {
			return image.unsafe_get(x,y);
		}

		@Override
		public float unsafe_getF(int x, int y) {
			return image.unsafe_get(x,y);
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

		@Override
		public Class getImageType() {
			return GrayS16.class;
		}
	}

	public static class GSingle_S32 extends GSingleBaseInt<GrayS32>
	{
		public GSingle_S32(GrayS32 image) {
			super(image);
		}

		@Override
		public Number get(int x, int y) {
			return image.get(x,y);
		}

		@Override
		public double unsafe_getD(int x, int y) {
			return image.unsafe_get(x,y);
		}

		@Override
		public float unsafe_getF(int x, int y) {
			return image.unsafe_get(x,y);
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

		@Override
		public Class getImageType() {
			return GrayS32.class;
		}
	}

	public static class GSingle_I64 extends GSingleBaseInt<GrayS64>
	{
		public GSingle_I64(GrayS64 image) {
			super(image);
		}

		@Override
		public Number get(int x, int y) {
			return image.get(x,y);
		}

		@Override
		public double unsafe_getD(int x, int y) {
			return image.unsafe_get(x,y);
		}

		@Override
		public float unsafe_getF(int x, int y) {
			return image.unsafe_get(x,y);
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

		@Override
		public Class getImageType() {
			return GrayS64.class;
		}
	}

	public static class GSingle_F32 extends GSingleBase<GrayF32>
	{
		public GSingle_F32(GrayF32 image) {
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
		public double unsafe_getD(int x, int y) {
			return image.unsafe_get(x,y);
		}

		@Override
		public float unsafe_getF(int x, int y) {
			return image.data[image.getIndex(x,y)];
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

		@Override
		public Class getImageType() {
			return GrayF32.class;
		}
	}

	public static class GSingle_F64 extends GSingleBase<GrayF64>
	{
		public GSingle_F64(GrayF64 image) {
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
		public double unsafe_getD(int x, int y) {
			return image.data[image.getIndex(x, y)];
		}

		@Override
		public float unsafe_getF(int x, int y) {
			return (float)image.unsafe_get(x,y);
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

		@Override
		public Class getImageType() {
			return GrayF64.class;
		}
	}

	public static abstract class GSingleBaseInt<T extends ImageGray> extends GSingleBase<T>
	{
		public GSingleBaseInt(T image) {
			super(image);
		}

		@Override
		public boolean isFloatingPoint() {
			return false;
		}
	}

	public static abstract class GSingleBase<T extends ImageGray> implements GImageGray {

		protected T image;

		public GSingleBase(T image) {
			this.image = image;
		}

		@Override
		public void wrap(ImageGray image) {
			this.image = (T)image;
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
		public ImageGray getImage() {
			return image;
		}
	}

	public static abstract class GSingleBorder<T extends ImageBorder> implements GImageGray {

		protected T image;

		public GSingleBorder(T image) {
			this.image = image;
		}

		@Override
		public void wrap(ImageGray image) {
			this.image.setImage(image);
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
		public ImageGray getImage() {
			return (ImageGray)image.getImage();
		}
	}
}
