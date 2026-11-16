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

package boofcv.alg.filter.misc;

import boofcv.override.BOverrideClass;
import boofcv.override.BOverrideManager;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import org.jetbrains.annotations.Nullable;

/// Override for [AverageDownSampleOps]. If a hook throws a RuntimeException then the Java implementation is used.
public class BOverrideAverageDownSampleOps extends BOverrideClass {

	static {
		BOverrideManager.register(BOverrideAverageDownSampleOps.class);
	}

	public static @Nullable Square square;
	public static @Nullable General general;
	public static @Nullable Horizontal horizontal;
	public static @Nullable Vertical vertical;

	/// Averages square regions which are 'sampleWidth' wide. See [AverageDownSampleOps#down(ImageGray, int, ImageGray)]
	public interface Square {
		void processDown( ImageGray input, int sampleWidth, ImageGray output );
	}

	/// Averages rectangular regions so that the input fills the output. See
	/// [AverageDownSampleOps#down(ImageBase, boolean, ImageBase, ImageGray)]
	public interface General {
		void processDown( ImageBase input, boolean centered, ImageBase output, @Nullable ImageGray middle );
	}

	/// Down samples along the x-axis only. See [AverageDownSampleOps#horizontal(ImageBase, boolean, ImageBase)]
	public interface Horizontal {
		void processHorizontal( ImageBase input, boolean centered, ImageBase output );
	}

	/// Down samples along the y-axis only. See
	/// [AverageDownSampleOps#vertical(ImageBase, boolean, ImageBase, pabeles.concurrency.GrowArray)]
	public interface Vertical {
		void processVertical( ImageBase input, boolean centered, ImageBase output );
	}

	public static boolean invokeNativeDown( ImageGray input, int sampleWidth, ImageGray output ) {
		boolean processed = false;
		if (BOverrideAverageDownSampleOps.square != null) {
			try {
				BOverrideAverageDownSampleOps.square.processDown(input, sampleWidth, output);
				processed = true;
			} catch (RuntimeException ignore) {}
		}
		return processed;
	}

	public static boolean invokeNativeDown( ImageBase input, boolean centered, ImageBase output,
											@Nullable ImageGray middle ) {
		boolean processed = false;
		if (BOverrideAverageDownSampleOps.general != null) {
			try {
				BOverrideAverageDownSampleOps.general.processDown(input, centered, output, middle);
				processed = true;
			} catch (RuntimeException ignore) {}
		}
		return processed;
	}

	public static boolean invokeNativeHorizontal( ImageBase input, boolean centered, ImageBase output ) {
		boolean processed = false;
		if (BOverrideAverageDownSampleOps.horizontal != null) {
			try {
				BOverrideAverageDownSampleOps.horizontal.processHorizontal(input, centered, output);
				processed = true;
			} catch (RuntimeException ignore) {}
		}
		return processed;
	}

	public static boolean invokeNativeVertical( ImageBase input, boolean centered, ImageBase output ) {
		boolean processed = false;
		if (BOverrideAverageDownSampleOps.vertical != null) {
			try {
				BOverrideAverageDownSampleOps.vertical.processVertical(input, centered, output);
				processed = true;
			} catch (RuntimeException ignore) {}
		}
		return processed;
	}
}
