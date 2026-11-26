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

package boofcv.alg.filter.convolve;

import boofcv.override.BOverrideClass;
import boofcv.override.BOverrideManager;
import boofcv.struct.border.BorderType;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.image.ImageBase;
import org.jetbrains.annotations.Nullable;

/**
 * Override for {@link ConvolveImageMean}. Same arguments as its functions.
 *
 * @author Peter Abeles
 */
public class BOverrideConvolveImageMean extends BOverrideClass {

	static {
		BOverrideManager.register(BOverrideConvolveImageMean.class);
	}

	public static @Nullable Horizontal horizontal;
	public static @Nullable Vertical vertical;

	public interface Horizontal {
		void horizontal( ImageBase input, ImageBase output, int offset, int length, BorderType border );
	}

	public interface Vertical {
		void vertical( ImageBase input, ImageBase output, int offset, int length, BorderType border );
	}

	/// The functions without a border, so NORMALIZED
	public static boolean invokeNativeHorizontal( ImageBase input, ImageBase output, int offset, int length ) {
		return invokeHorizontal(input, output, offset, length, BorderType.NORMALIZED);
	}

	/// The functions without a border, so NORMALIZED
	public static boolean invokeNativeVertical( ImageBase input, ImageBase output, int offset, int length ) {
		return invokeVertical(input, output, offset, length, BorderType.NORMALIZED);
	}

	/// A null border is SKIP
	public static boolean invokeNativeHorizontalBorder( ImageBase input, ImageBase output, int offset, int length,
														@Nullable ImageBorder border ) {
		if (!canOverride(border))
			return false;
		return invokeHorizontal(input, output, offset, length, overrideBorderOrSkip(border));
	}

	/// A null border is SKIP
	public static boolean invokeNativeVerticalBorder( ImageBase input, ImageBase output, int offset, int length,
													  @Nullable ImageBorder border ) {
		if (!canOverride(border))
			return false;
		return invokeVertical(input, output, offset, length, overrideBorderOrSkip(border));
	}

	private static boolean invokeHorizontal( ImageBase input, ImageBase output, int offset, int length,
											 BorderType border ) {
		if (horizontal == null)
			return false;
		try {
			horizontal.horizontal(input, output, offset, length, border);
			return true;
		} catch (RuntimeException ignore) {
			return false;
		}
	}

	private static boolean invokeVertical( ImageBase input, ImageBase output, int offset, int length,
										   BorderType border ) {
		if (vertical == null)
			return false;
		try {
			vertical.vertical(input, output, offset, length, border);
			return true;
		} catch (RuntimeException ignore) {
			return false;
		}
	}
}
