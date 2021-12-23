/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.distort;

import boofcv.struct.image.*;

/**
 * Abstract way to assign pixel values to {@link ImageGray} without knowing the underlying data type.
 *
 * @author Peter Abeles
 */
public interface AssignPixelValue_SB<T extends ImageGray<T>> {
	void setImage( T image );

	void assign( int indexDst, float value );

	@SuppressWarnings({"NullAway.Init"})
	class F32 implements AssignPixelValue_SB<GrayF32> {
		GrayF32 image;

		@Override public void setImage( GrayF32 image ) {this.image = image;}

		@Override public void assign( int indexDst, float value ) {
			this.image.data[indexDst] = value;
		}
	}

	@SuppressWarnings({"NullAway.Init"})
	class I8<T extends GrayI8<T>> implements AssignPixelValue_SB<T> {
		T image;

		@Override public void setImage( T image ) {this.image = image;}

		@Override public void assign( int indexDst, float value ) {
			this.image.data[indexDst] = (byte)value;
		}
	}

	@SuppressWarnings({"NullAway.Init"})
	class I16<T extends GrayI16<T>> implements AssignPixelValue_SB<T> {
		T image;

		@Override public void setImage( T image ) {this.image = image;}

		@Override public void assign( int indexDst, float value ) {
			this.image.data[indexDst] = (short)value;
		}
	}

	@SuppressWarnings({"NullAway.Init"})
	class S32 implements AssignPixelValue_SB<GrayS32> {
		GrayS32 image;

		@Override public void setImage( GrayS32 image ) {this.image = image;}

		@Override public void assign( int indexDst, float value ) {
			this.image.data[indexDst] = (int)value;
		}
	}
}
