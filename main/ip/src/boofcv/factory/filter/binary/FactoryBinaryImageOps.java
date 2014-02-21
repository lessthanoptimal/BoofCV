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

package boofcv.factory.filter.binary;

import boofcv.abst.filter.FilterImageInterface;
import boofcv.abst.filter.FilterImageReflection;
import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageUInt8;

/**
 * {@link FilterImageInterface} wrappers around functions inside of {@link BinaryImageOps}.
 *
 * NOTE:: Not all functions inside of {@link BinaryImageOps} are contained here.
 */
public class FactoryBinaryImageOps {

	/**
	 * Filter implementation of {@link BinaryImageOps#erode4(boofcv.struct.image.ImageUInt8, int, boofcv.struct.image.ImageUInt8)}
	 */
	public static FilterImageInterface<ImageUInt8, ImageUInt8> erode4() {
		return new FilterImageReflection<ImageUInt8, ImageUInt8>(BinaryImageOps.class, "erode4", 0, 0, ImageUInt8.class, ImageUInt8.class);
	}

	/**
	 * Filter implementation of {@link BinaryImageOps#erode8(boofcv.struct.image.ImageUInt8, int, boofcv.struct.image.ImageUInt8)}
	 */
	public static FilterImageInterface<ImageUInt8, ImageUInt8> erode8() {
		return new FilterImageReflection<ImageUInt8, ImageUInt8>(BinaryImageOps.class, "erode8", 0, 0, ImageUInt8.class, ImageUInt8.class);
	}

	/**
	 * Filter implementation of {@link BinaryImageOps#dilate4(boofcv.struct.image.ImageUInt8, int, boofcv.struct.image.ImageUInt8)}
	 */
	public static FilterImageInterface<ImageUInt8, ImageUInt8> dilate4() {
		return new FilterImageReflection<ImageUInt8, ImageUInt8>(BinaryImageOps.class, "dilate4", 0, 0, ImageUInt8.class, ImageUInt8.class);
	}

	/**
	 * Filter implementation of {@link BinaryImageOps#dilate8(boofcv.struct.image.ImageUInt8, int, boofcv.struct.image.ImageUInt8)}
	 */
	public static FilterImageInterface<ImageUInt8, ImageUInt8> dilate8() {
		return new FilterImageReflection<ImageUInt8, ImageUInt8>(BinaryImageOps.class, "dilate8", 0, 0, ImageUInt8.class, ImageUInt8.class);
	}

	/**
	 * Filter implementation of {@link BinaryImageOps#edge4(boofcv.struct.image.ImageUInt8, boofcv.struct.image.ImageUInt8)}
	 */
	public static FilterImageInterface<ImageUInt8, ImageUInt8> edge4() {
		return new FilterImageReflection<ImageUInt8, ImageUInt8>(BinaryImageOps.class, "edge4", 0, 0, ImageUInt8.class, ImageUInt8.class);
	}

	/**
	 * Filter implementation of {@link BinaryImageOps#edge8(boofcv.struct.image.ImageUInt8, boofcv.struct.image.ImageUInt8)}
	 */
	public static FilterImageInterface<ImageUInt8, ImageUInt8> edge8() {
		return new FilterImageReflection<ImageUInt8, ImageUInt8>(BinaryImageOps.class, "edge8", 0, 0, ImageUInt8.class, ImageUInt8.class);
	}

	/**
	 * Filter implementation of {@link BinaryImageOps#removePointNoise(boofcv.struct.image.ImageUInt8, boofcv.struct.image.ImageUInt8)}
	 */
	public static FilterImageInterface<ImageUInt8, ImageUInt8> removePointNoise() {
		return new FilterImageReflection<ImageUInt8, ImageUInt8>(BinaryImageOps.class, "removePointNoise", 0, 0, ImageUInt8.class, ImageUInt8.class);
	}

	/**
	 * Filter implementation of {@link BinaryImageOps#labelToBinary(boofcv.struct.image.ImageSInt32, boofcv.struct.image.ImageUInt8)}
	 */
	public static FilterImageInterface<ImageSInt32, ImageUInt8> labelToBinary() {
		return new FilterImageReflection<ImageSInt32, ImageUInt8>(BinaryImageOps.class, "labelToBinary", 0, 0, ImageSInt32.class, ImageUInt8.class);
	}

}
