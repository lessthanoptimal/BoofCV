/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.image.*;

/**
 * All points outside of the image will return the specified value
 *
 * @author Peter Abeles
 */
public class ImageBorderValue {

	public static ImageBorder wrap( ImageSingleBand image , double value ) {
		if( image.getDataType().isInteger() ) {
			if( image.getDataType().getNumBits() <= 32 )
				return wrap((ImageInteger)image,(int)value);
			else
				return wrap((ImageSInt64)image,(long)value);
		} else if( image.getDataType().getDataType() == float.class ) {
			return wrap((ImageFloat32)image,(float)value);
		} else {
			return wrap((ImageFloat64)image,value);
		}
	}

	public static ImageBorder_I64 wrap( ImageSInt64 image , long value ) {
		return new Value_I64(image,value);
	}

	public static ImageBorder_F64 wrap( ImageFloat64 image , double value ) {
		return new Value_F64(image,value);
	}

	public static ImageBorder_F32 wrap( ImageFloat32 image , float value ) {
		return new Value_F32(image,value);
	}

	public static ImageBorder_I32 wrap( ImageInteger image , int value ) {
		return new Value_I(image,value);
	}

	public static class Value_I64 extends ImageBorder_I64 {
		long value;

		public Value_I64( ImageSInt64 image , long value ) {
			super(image);
			this.value = value;
		}

		public Value_I64(long value) {
			this.value = value;
		}

		@Override
		public long getOutside( int x , int y ) {
			return value;
		}

		@Override
		public void setOutside(int x, int y, long val) {
			// do nothing since it is a constant value
		}
	}

	public static class Value_F64 extends ImageBorder_F64 {
		double value;

		public Value_F64( ImageFloat64 image , double value ) {
			super(image);
			this.value = value;
		}

		public Value_F64(double value) {
			this.value = value;
		}

		@Override
		public double getOutside( int x , int y ) {
			return value;
		}

		@Override
		public void setOutside(int x, int y, double val) {
			// do nothing since it is a constant value
		}
	}

	public static class Value_F32 extends ImageBorder_F32 {
		float value;

		public Value_F32( ImageFloat32 image , float value ) {
			super(image);
			this.value = value;
		}

		public Value_F32(float value) {
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

	public static class Value_I extends ImageBorder_I32 {
		int value;

		public Value_I( ImageInteger image , int value ) {
			super(image);
			this.value = value;
		}

		public Value_I(int value) {
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
