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

import boofcv.override.BOverrideClass;
import boofcv.override.BOverrideManager;
import boofcv.struct.convolve.Kernel1D;
import boofcv.struct.convolve.Kernel2D;
import boofcv.struct.image.ImageBase;

/**
 * Override for normalized convolutions
 * 
 * @author Peter Abeles
 */
public class BOverrideConvolveImageNormalized extends BOverrideClass {

	static {
		BOverrideManager.register(BOverrideConvolveImageNormalized.class);
	}

	public static Horizontal horizontal;
	public static Vertical vertical;
	public static Convolve convolve;

	public interface Horizontal {
		void horizontal(Kernel1D kernel, ImageBase input, ImageBase output);
	}

	public interface Vertical {
		void vertical(Kernel1D kernel, ImageBase input, ImageBase output);
	}

	public interface Convolve {
		void convolve(Kernel2D kernel, ImageBase input, ImageBase output);
	}

	public static boolean invokeNativeHorizontal(Kernel1D kernel, ImageBase input, ImageBase output) {
		boolean processed = false;
		if( BOverrideConvolveImageNormalized.horizontal != null ) {
			try {
				BOverrideConvolveImageNormalized.horizontal.horizontal(kernel,input,output);
				processed = true;
			} catch( RuntimeException ignore ) {}
		}
		return processed;
	}

	public static boolean invokeNativeVertical(Kernel1D kernel, ImageBase input, ImageBase output) {
		boolean processed = false;
		if( BOverrideConvolveImageNormalized.vertical != null ) {
			try {
				BOverrideConvolveImageNormalized.vertical.vertical(kernel,input,output);
				processed = true;
			} catch( RuntimeException ignore ) {}
		}
		return processed;
	}

	public static boolean invokeNativeConvolve(Kernel2D kernel, ImageBase input, ImageBase output) {
		boolean processed = false;
		if( BOverrideConvolveImageNormalized.convolve != null ) {
			try {
				BOverrideConvolveImageNormalized.convolve.convolve(kernel,input,output);
				processed = true;
			} catch( RuntimeException ignore ) {}
		}
		return processed;
	}
}
