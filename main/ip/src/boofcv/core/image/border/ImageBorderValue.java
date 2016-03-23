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

package boofcv.core.image.border;

import boofcv.struct.image.*;

import java.util.Arrays;

/**
 * All points outside of the image will return the specified value
 *
 * @author Peter Abeles
 */
public class ImageBorderValue {

	public static ImageBorder wrap(ImageGray image , double value ) {
		if( image.getDataType().isInteger() ) {
			if( image.getDataType().getNumBits() <= 32 )
				return wrap((GrayI)image,(int)value);
			else
				return wrap((GrayS64)image,(long)value);
		} else if( image.getDataType().getDataType() == float.class ) {
			return wrap((GrayF32)image,(float)value);
		} else {
			return wrap((GrayF64)image,value);
		}
	}

	public static ImageBorder wrap( ImageInterleaved image , double value ) {
		if (image instanceof InterleavedF32) {
			return new Value_IL_F32((InterleavedF32) image, (float) value);
		} else if (image instanceof InterleavedF64) {
			return new Value_IL_F64((InterleavedF64) image, value);
		} else if (InterleavedInteger.class.isAssignableFrom(image.getClass())) {
			return new Value_IL_S32((InterleavedInteger)image,(int)value );
		} else if( image instanceof InterleavedS64 ) {
			return new Value_IL_S64((InterleavedS64)image,(long)value );
		} else {
			throw new RuntimeException("Add support for more types");
		}
	}

	public static ImageBorder_S64 wrap(GrayS64 image , long value ) {
		return new Value_I64(image,value);
	}

	public static ImageBorder_F64 wrap(GrayF64 image , double value ) {
		return new Value_F64(image,value);
	}

	public static ImageBorder_F32 wrap(GrayF32 image , float value ) {
		return new Value_F32(image,value);
	}

	public static ImageBorder_S32 wrap(GrayI image , int value ) {
		return new Value_I(image,value);
	}

	public static class Value_I64 extends ImageBorder_S64 {
		long value;

		public Value_I64(GrayS64 image , long value ) {
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

		public Value_F64(GrayF64 image , double value ) {
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

		public Value_F32(GrayF32 image , float value ) {
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

	public static class Value_I extends ImageBorder_S32 {
		int value;

		public Value_I(GrayI image , int value ) {
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

	public static class Value_IL_F32 extends ImageBorder_IL_F32 {
		float value;

		public Value_IL_F32(InterleavedF32 image, float value) {super(image); this.value = value; }

		public Value_IL_F32(float value) { this.value = value; }

		@Override
		public void getOutside(int x, int y, float[] pixel) {
			Arrays.fill(pixel,value);
		}

		@Override
		public void setOutside(int x, int y, float[] pixel) {}
	}

	public static class Value_IL_F64 extends ImageBorder_IL_F64 {
		double value;

		public Value_IL_F64(InterleavedF64 image, double value) {super(image); this.value = value; }

		public Value_IL_F64(double value) { this.value = value; }

		@Override
		public void getOutside(int x, int y, double[] pixel) {
			Arrays.fill(pixel,value);
		}

		@Override
		public void setOutside(int x, int y, double[] pixel) {}
	}

	public static class Value_IL_S32 extends ImageBorder_IL_S32 {
		int value;

		public Value_IL_S32(InterleavedInteger image, int value) {super(image); this.value = value; }

		public Value_IL_S32(int value) { this.value = value; }

		@Override
		public void getOutside(int x, int y, int[] pixel) {
			Arrays.fill(pixel,value);
		}

		@Override
		public void setOutside(int x, int y, int[] pixel) {}
	}

	public static class Value_IL_S64 extends ImageBorder_IL_S64 {
		long value;

		public Value_IL_S64(InterleavedS64 image, long value) {super(image); this.value = value; }

		public Value_IL_S64(long value) { this.value = value; }

		@Override
		public void getOutside(int x, int y, long[] pixel) {
			Arrays.fill(pixel,value);
		}

		@Override
		public void setOutside(int x, int y, long[] pixel) {}
	}
}
