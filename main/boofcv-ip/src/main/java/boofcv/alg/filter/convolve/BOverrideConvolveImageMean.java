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
import boofcv.struct.image.ImageBase;

/**
 * @author Peter Abeles
 */
public class BOverrideConvolveImageMean extends BOverrideClass {

	static {
		BOverrideManager.register(BOverrideConvolveImageMean.class);
	}

	public static Horizontal horizontal;
	public static Vertical vertical;

	public interface Horizontal {
		void horizontal(ImageBase input, ImageBase output, int radius);
	}

	public interface Vertical {
		void vertical(ImageBase input, ImageBase output, int radius);
	}

	public static boolean invokeNativeHorizontal(ImageBase input, ImageBase output, int radius) {
		boolean processed = false;
		if( BOverrideConvolveImageMean.horizontal != null ) {
			try {
				BOverrideConvolveImageMean.horizontal.horizontal(input,output,radius);
				processed = true;
			} catch( RuntimeException ignore ) {}
		}
		return processed;
	}

	public static boolean invokeNativeVertical(ImageBase input, ImageBase output, int radius) {
		boolean processed = false;
		if( BOverrideConvolveImageMean.vertical != null ) {
			try {
				BOverrideConvolveImageMean.vertical.vertical(input,output,radius);
				processed = true;
			} catch( RuntimeException ignore ) {}
		}
		return processed;
	}
}

