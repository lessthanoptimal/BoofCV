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
import boofcv.struct.convolve.Kernel1D;
import boofcv.struct.convolve.Kernel2D;
import boofcv.struct.image.ImageBase;
import org.jetbrains.annotations.Nullable;

/**
 * Override for {@link ConvolveImage}.
 *
 * @author Peter Abeles
 */
public class BOverrideConvolveImage extends BOverrideClass {

	static {
		BOverrideManager.register(BOverrideConvolveImage.class);
	}

	public static @Nullable Horizontal horizontal;
	public static @Nullable Vertical vertical;
	public static @Nullable Convolve convolve;

	public interface Horizontal {
		void horizontal( Kernel1D kernel, ImageBase input, ImageBase output, BorderType border );
	}

	public interface Vertical {
		void vertical( Kernel1D kernel, ImageBase input, ImageBase output, BorderType border );
	}

	public interface Convolve {
		void convolve( Kernel2D kernel, ImageBase input, ImageBase output, BorderType border );
	}

	public static boolean invokeNativeHorizontal( Kernel1D kernel, ImageBase input, ImageBase output, ImageBorder border ) {
		boolean processed = false;
		if( horizontal != null && canOverride(border) ) {
			try {
				horizontal.horizontal(kernel, input, output, overrideBorderOrSkip(border));
				processed = true;
			} catch( RuntimeException ignore ) {}
		}
		return processed;
	}

	public static boolean invokeNativeVertical( Kernel1D kernel, ImageBase input, ImageBase output, ImageBorder border ) {
		boolean processed = false;
		if( vertical != null && canOverride(border) ) {
			try {
				vertical.vertical(kernel, input, output, overrideBorderOrSkip(border));
				processed = true;
			} catch( RuntimeException ignore ) {}
		}
		return processed;
	}

	public static boolean invokeNativeConvolve( Kernel2D kernel, ImageBase input, ImageBase output, ImageBorder border ) {
		boolean processed = false;
		if( convolve != null && canOverride(border) ) {
			try {
				convolve.convolve(kernel, input, output, overrideBorderOrSkip(border));
				processed = true;
			} catch( RuntimeException ignore ) {}
		}
		return processed;
	}
}
