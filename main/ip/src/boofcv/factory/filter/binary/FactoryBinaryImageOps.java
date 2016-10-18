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

package boofcv.factory.filter.binary;

import boofcv.abst.filter.FilterImageInterface;
import boofcv.abst.filter.FilterImageReflection;
import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;

/**
 * {@link FilterImageInterface} wrappers around functions inside of {@link BinaryImageOps}.
 *
 * NOTE:: Not all functions inside of {@link BinaryImageOps} are contained here.
 */
public class FactoryBinaryImageOps {

	/**
	 * Filter implementation of {@link BinaryImageOps#erode4(GrayU8, int, GrayU8)}
	 */
	public static FilterImageInterface<GrayU8, GrayU8> erode4() {
		return new FilterImageReflection<>(BinaryImageOps.class, "erode4", 0, 0, GrayU8.class, GrayU8.class);
	}

	/**
	 * Filter implementation of {@link BinaryImageOps#erode8(GrayU8, int, GrayU8)}
	 */
	public static FilterImageInterface<GrayU8, GrayU8> erode8() {
		return new FilterImageReflection<>(BinaryImageOps.class, "erode8", 0, 0, GrayU8.class, GrayU8.class);
	}

	/**
	 * Filter implementation of {@link BinaryImageOps#dilate4(GrayU8, int, GrayU8)}
	 */
	public static FilterImageInterface<GrayU8, GrayU8> dilate4() {
		return new FilterImageReflection<>(BinaryImageOps.class, "dilate4", 0, 0, GrayU8.class, GrayU8.class);
	}

	/**
	 * Filter implementation of {@link BinaryImageOps#dilate8(GrayU8, int, GrayU8)}
	 */
	public static FilterImageInterface<GrayU8, GrayU8> dilate8() {
		return new FilterImageReflection<>(BinaryImageOps.class, "dilate8", 0, 0, GrayU8.class, GrayU8.class);
	}

	/**
	 * Filter implementation of {@link BinaryImageOps#edge4(GrayU8, GrayU8)}
	 */
	public static FilterImageInterface<GrayU8, GrayU8> edge4() {
		return new FilterImageReflection<>(BinaryImageOps.class, "edge4", 0, 0, GrayU8.class, GrayU8.class);
	}

	/**
	 * Filter implementation of {@link BinaryImageOps#edge8(GrayU8, GrayU8)}
	 */
	public static FilterImageInterface<GrayU8, GrayU8> edge8() {
		return new FilterImageReflection<>(BinaryImageOps.class, "edge8", 0, 0, GrayU8.class, GrayU8.class);
	}

	/**
	 * Filter implementation of {@link BinaryImageOps#removePointNoise(GrayU8, GrayU8)}
	 */
	public static FilterImageInterface<GrayU8, GrayU8> removePointNoise() {
		return new FilterImageReflection<>(BinaryImageOps.class, "removePointNoise", 0, 0, GrayU8.class, GrayU8.class);
	}

	/**
	 * Filter implementation of {@link BinaryImageOps#labelToBinary(GrayS32, GrayU8)}
	 */
	public static FilterImageInterface<GrayS32, GrayU8> labelToBinary() {
		return new FilterImageReflection<>(BinaryImageOps.class, "labelToBinary", 0, 0, GrayS32.class, GrayU8.class);
	}

}
