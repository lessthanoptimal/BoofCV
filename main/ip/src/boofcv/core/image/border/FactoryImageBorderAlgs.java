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

package boofcv.core.image.border;

import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageFloat64;
import boofcv.struct.image.ImageInteger;

/**
 * @author Peter Abeles
 */
public class FactoryImageBorderAlgs {

	public static ImageBorder1D_F64 extend( ImageFloat64 image ) {
		ImageBorder1D_F64 ret = new ImageBorder1D_F64(BorderIndex1D_Extend.class);
		ret.setImage(image);
		return ret;
	}

	public static ImageBorder1D_F64 reflect( ImageFloat64 image ) {
		ImageBorder1D_F64 ret = new ImageBorder1D_F64(BorderIndex1D_Reflect.class);
		ret.setImage(image);
		return ret;
	}

	public static ImageBorder1D_F64 wrap( ImageFloat64 image ) {
		ImageBorder1D_F64 ret = new ImageBorder1D_F64(BorderIndex1D_Wrap.class);
		ret.setImage(image);
		return ret;
	}

	public static ImageBorder_F64 value( ImageFloat64 image , double value ) {
		return ImageBorderValue.wrap(image,value);
	}

	public static ImageBorder1D_F32 extend( ImageFloat32 image ) {
		ImageBorder1D_F32 ret = new ImageBorder1D_F32(BorderIndex1D_Extend.class);
		ret.setImage(image);
		return ret;
	}

	public static ImageBorder1D_F32 reflect( ImageFloat32 image ) {
		ImageBorder1D_F32 ret = new ImageBorder1D_F32(BorderIndex1D_Reflect.class);
		ret.setImage(image);
		return ret;
	}

	public static ImageBorder1D_F32 wrap( ImageFloat32 image ) {
		ImageBorder1D_F32 ret = new ImageBorder1D_F32(BorderIndex1D_Wrap.class);
		ret.setImage(image);
		return ret;
	}

	public static ImageBorder_F32 value( ImageFloat32 image , float value ) {
		return ImageBorderValue.wrap(image,value);
	}

	public static ImageBorder1D_I32 extend( ImageInteger image ) {
		ImageBorder1D_I32 ret = new ImageBorder1D_I32((Class)BorderIndex1D_Extend.class);
		ret.setImage(image);
		return ret;
	}

	public static ImageBorder1D_I32 reflect( ImageInteger image ) {
		ImageBorder1D_I32 ret = new ImageBorder1D_I32((Class)BorderIndex1D_Reflect.class);
		ret.setImage(image);
		return ret;
	}

	public static ImageBorder1D_I32 wrap( ImageInteger image ) {
		ImageBorder1D_I32 ret = new ImageBorder1D_I32((Class)BorderIndex1D_Wrap.class);
		ret.setImage(image);
		return ret;
	}

	public static ImageBorder_I32 value( ImageInteger image , int value ) {
		return ImageBorderValue.wrap(image,value);
	}
}
