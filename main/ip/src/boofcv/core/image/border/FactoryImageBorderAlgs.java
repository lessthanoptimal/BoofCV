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

import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayF64;
import boofcv.struct.image.GrayI;

/**
 * @author Peter Abeles
 */
public class FactoryImageBorderAlgs {

	public static ImageBorder1D_F64 extend( GrayF64 image ) {
		ImageBorder1D_F64 ret = new ImageBorder1D_F64(BorderIndex1D_Extend.class);
		ret.setImage(image);
		return ret;
	}

	public static ImageBorder1D_F64 reflect( GrayF64 image ) {
		ImageBorder1D_F64 ret = new ImageBorder1D_F64(BorderIndex1D_Reflect.class);
		ret.setImage(image);
		return ret;
	}

	public static ImageBorder1D_F64 wrap( GrayF64 image ) {
		ImageBorder1D_F64 ret = new ImageBorder1D_F64(BorderIndex1D_Wrap.class);
		ret.setImage(image);
		return ret;
	}

	public static ImageBorder_F64 value(GrayF64 image , double value ) {
		return ImageBorderValue.wrap(image,value);
	}

	public static ImageBorder1D_F32 extend( GrayF32 image ) {
		ImageBorder1D_F32 ret = new ImageBorder1D_F32(BorderIndex1D_Extend.class);
		ret.setImage(image);
		return ret;
	}

	public static ImageBorder1D_F32 reflect( GrayF32 image ) {
		ImageBorder1D_F32 ret = new ImageBorder1D_F32(BorderIndex1D_Reflect.class);
		ret.setImage(image);
		return ret;
	}

	public static ImageBorder1D_F32 wrap( GrayF32 image ) {
		ImageBorder1D_F32 ret = new ImageBorder1D_F32(BorderIndex1D_Wrap.class);
		ret.setImage(image);
		return ret;
	}

	public static ImageBorder_F32 value(GrayF32 image , float value ) {
		return ImageBorderValue.wrap(image,value);
	}

	public static <T extends GrayI> ImageBorder1D_S32<T> extend(T image ) {
		ImageBorder1D_S32<T> ret = new ImageBorder1D_S32<>((Class) BorderIndex1D_Extend.class);
		ret.setImage(image);
		return ret;
	}

	public static <T extends GrayI> ImageBorder1D_S32<T> reflect(T image ) {
		ImageBorder1D_S32<T> ret = new ImageBorder1D_S32<>((Class) BorderIndex1D_Reflect.class);
		ret.setImage(image);
		return ret;
	}

	public static <T extends GrayI> ImageBorder1D_S32<T> wrap(T image ) {
		ImageBorder1D_S32<T> ret = new ImageBorder1D_S32<>((Class) BorderIndex1D_Wrap.class);
		ret.setImage(image);
		return ret;
	}

	public static ImageBorder_S32 value(GrayI image , int value ) {
		return ImageBorderValue.wrap(image,value);
	}
}
