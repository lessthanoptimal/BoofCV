/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.transform.census;

import boofcv.override.BOverrideClass;
import boofcv.override.BOverrideManager;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayS64;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.InterleavedU16;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastAccess;
import org.jetbrains.annotations.Nullable;

/// Override for [CensusTransform]. An override declines an input by throwing a RuntimeException, and
/// [CensusTransform] then uses its own code
public class BOverrideCensusTransform extends BOverrideClass {

	static {
		BOverrideManager.register(BOverrideCensusTransform.class);
	}

	public static @Nullable Census census;

	/// Same arguments as the functions in [CensusTransform]. The output has already been reshaped
	public interface Census<T extends ImageGray<T>> {
		void dense3x3( T input, GrayU8 output, @Nullable ImageBorder<T> border );

		void dense5x5( T input, GrayS32 output, @Nullable ImageBorder<T> border );

		void sample_S64( T input, FastAccess<Point2D_I32> sample, GrayS64 output, @Nullable ImageBorder<T> border );

		void sample_IU16( T input, FastAccess<Point2D_I32> sample, InterleavedU16 output,
						  @Nullable ImageBorder<T> border );
	}

	/// Returns true if the override processed the image
	public static <T extends ImageGray<T>>
	boolean invokeNativeDense3x3( T input, GrayU8 output, @Nullable ImageBorder<T> border ) {
		if (census == null)
			return false;
		try {
			census.dense3x3(input, output, border);
			return true;
		} catch (RuntimeException ignore) {
			return false;
		}
	}

	/// Returns true if the override processed the image
	public static <T extends ImageGray<T>>
	boolean invokeNativeDense5x5( T input, GrayS32 output, @Nullable ImageBorder<T> border ) {
		if (census == null)
			return false;
		try {
			census.dense5x5(input, output, border);
			return true;
		} catch (RuntimeException ignore) {
			return false;
		}
	}

	/// Returns true if the override processed the image
	public static <T extends ImageGray<T>>
	boolean invokeNativeSample_S64( T input, FastAccess<Point2D_I32> sample, GrayS64 output,
									@Nullable ImageBorder<T> border ) {
		if (census == null)
			return false;
		try {
			census.sample_S64(input, sample, output, border);
			return true;
		} catch (RuntimeException ignore) {
			return false;
		}
	}

	/// Returns true if the override processed the image
	public static <T extends ImageGray<T>>
	boolean invokeNativeSample_IU16( T input, FastAccess<Point2D_I32> sample, InterleavedU16 output,
									 @Nullable ImageBorder<T> border ) {
		if (census == null)
			return false;
		try {
			census.sample_IU16(input, sample, output, border);
			return true;
		} catch (RuntimeException ignore) {
			return false;
		}
	}
}
