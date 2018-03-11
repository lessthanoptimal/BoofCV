/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

import boofcv.core.image.border.ImageBorder;
import boofcv.override.BOverrideClass;
import boofcv.override.BOverrideManager;
import boofcv.struct.convolve.Kernel1D;
import boofcv.struct.convolve.Kernel2D;
import boofcv.struct.image.ImageBase;

/**
 * @author Peter Abeles
 */
public class BOverrideConvolveImage extends BOverrideClass {

	static {
		BOverrideManager.register(BOverrideConvolveImage.class);
	}

	public static Horizontal horizontal;
	public static Vertical vertical;
	public static Convolve convolve;

	public interface Horizontal {
		void horizontal(Kernel1D kernel, ImageBase input, ImageBase output , ImageBorder border);
	}

	public interface Vertical {
		void vertical(Kernel1D kernel, ImageBase input, ImageBase output , ImageBorder border);
	}

	public interface Convolve {
		void convolve(Kernel2D kernel, ImageBase input, ImageBase output , ImageBorder border);
	}

	public static boolean invokeNativeHorizontal(Kernel1D kernel, ImageBase input, ImageBase output , ImageBorder border) {
		boolean processed = false;
		if( horizontal != null ) {
			try {
				horizontal.horizontal(kernel, input, output, border);
				processed = true;
			} catch( RuntimeException ignore ) {}
		}
		return processed;
	}

	public static boolean invokeNativeVertical(Kernel1D kernel, ImageBase input, ImageBase output , ImageBorder border) {
		boolean processed = false;
		if( vertical != null ) {
			try {
				vertical.vertical(kernel, input, output, border);
				processed = true;
			} catch( RuntimeException ignore ) {}
		}
		return processed;
	}

	public static boolean invokeNativeConvolve(Kernel2D kernel, ImageBase input, ImageBase output , ImageBorder border) {
		boolean processed = false;
		if( convolve != null ) {
			try {
				convolve.convolve(kernel, input, output, border);
				processed = true;
			} catch( RuntimeException ignore ) {}
		}
		return processed;
	}
}

