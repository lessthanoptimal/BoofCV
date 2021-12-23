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

package boofcv.alg.filter.blur;

import boofcv.override.BOverrideClass;
import boofcv.override.BOverrideManager;
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
		void processMeanWeighted( T input, T output, int radiusX, int radiusY, T storage );

		void processMeanBorder( T input, T output, int radiusX, int radiusY, @Nullable ImageBorder<T> border, T storage );
	}

	public interface Median<T extends ImageBase<T>> {
		void processMedian( T input, T output, int radiusX, int radiusY );
	}

	public interface Gaussian<T extends ImageBase<T>> {
		void processGaussian( T input, T output, double sigmaX, int radiusX, double sigmaY, int radiusY, T storage );
	}

	/**
	 * Weighted average mean
	 */
	public static <T extends ImageBase<T>>
	boolean invokeNativeMeanWeighted( T input, T output, int radiusX, int radiusY, T storage ) {
		boolean processed = false;
		if (BOverrideBlurImageOps.mean != null) {
			try {
				BOverrideBlurImageOps.mean.processMeanWeighted(input, output, radiusX, radiusY, storage);
				processed = true;
			} catch (RuntimeException ignore) {}
		}
		return processed;
	}

	/**
	 * Extended border mean
	 */
	public static <T extends ImageBase<T>>
	boolean invokeNativeMeanBorder( T input, T output, int radiusX, int radiusY, @Nullable ImageBorder<T> border, T storage ) {
		boolean processed = false;
		if (BOverrideBlurImageOps.mean != null) {
			try {
				BOverrideBlurImageOps.mean.processMeanBorder(input, output, radiusX, radiusY, border, storage);
				processed = true;
			} catch (RuntimeException ignore) {}
		}
		return processed;
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
	boolean invokeNativeGaussian( T input, T output, double sigmaX, int radiusX, double sigmaY, int radiusY, T storage ) {
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
