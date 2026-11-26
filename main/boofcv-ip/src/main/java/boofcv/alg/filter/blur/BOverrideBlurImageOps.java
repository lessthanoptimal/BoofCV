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

package boofcv.alg.filter.blur;

import boofcv.override.BOverrideClass;
import boofcv.override.BOverrideManager;
import boofcv.struct.border.BorderType;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.image.ImageBase;
import org.jetbrains.annotations.Nullable;

/**
 * Override for blur image ops functions
 *
 * @author Peter Abeles
 */
public class BOverrideBlurImageOps extends BOverrideClass {

	static {
		BOverrideManager.register(BOverrideBlurImageOps.class);
	}

	public static @Nullable Mean mean;
	public static @Nullable Median median;
	public static @Nullable Gaussian gaussian;

	public interface Mean<T extends ImageBase<T>> {
		void processMean( T input, T output, int radiusX, int radiusY, BorderType border, @Nullable T storage );
	}

	public interface Median<T extends ImageBase<T>> {
		void processMedian( T input, T output, int radiusX, int radiusY );
	}

	public interface Gaussian<T extends ImageBase<T>> {
		void processGaussian( T input, T output, double sigmaX, int radiusX, double sigmaY, int radiusY,
							  @Nullable T storage );
	}

	/// Mean without a border, so NORMALIZED
	public static <T extends ImageBase<T>>
	boolean invokeNativeMean( T input, T output, int radiusX, int radiusY, @Nullable T storage ) {
		return invokeMean(input, output, radiusX, radiusY, BorderType.NORMALIZED, storage);
	}

	/// A null border is SKIP
	public static <T extends ImageBase<T>>
	boolean invokeNativeMeanBorder( T input, T output, int radiusX, int radiusY,
									@Nullable ImageBorder<T> border, @Nullable T storage ) {
		if (!canOverride(border))
			return false;
		return invokeMean(input, output, radiusX, radiusY, overrideBorderOrSkip(border), storage);
	}

	private static <T extends ImageBase<T>>
	boolean invokeMean( T input, T output, int radiusX, int radiusY, BorderType border, @Nullable T storage ) {
		if (mean == null)
			return false;
		try {
			mean.processMean(input, output, radiusX, radiusY, border, storage);
			return true;
		} catch (RuntimeException ignore) {
			return false;
		}
	}

	public static <T extends ImageBase<T>>
	boolean invokeNativeMedian( T input, T output, int radiusX, int radiusY ) {
		boolean processed = false;
		if (BOverrideBlurImageOps.median != null) {
			try {
				BOverrideBlurImageOps.median.processMedian(input, output, radiusX, radiusY);
				processed = true;
			} catch (RuntimeException ignore) {
			}
		}
		return processed;
	}

	// TODO replace with native normalized?
	public static <T extends ImageBase<T>>
	boolean invokeNativeGaussian( T input, T output, double sigmaX, int radiusX, double sigmaY, int radiusY,
								  @Nullable T storage ) {
		boolean processed = false;
		if (BOverrideBlurImageOps.gaussian != null) {
			try {
				BOverrideBlurImageOps.gaussian.processGaussian(input, output, sigmaX, radiusX, sigmaY, radiusY, storage);
				processed = true;
			} catch (RuntimeException ignore) {
			}
		}
		return processed;
	}
}
