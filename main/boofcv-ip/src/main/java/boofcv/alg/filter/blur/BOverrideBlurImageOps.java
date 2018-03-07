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

package boofcv.alg.filter.blur;

import boofcv.override.BOverrideClass;
import boofcv.override.BOverrideManager;
import boofcv.struct.image.ImageBase;

/**
 * Override for blur image ops functions
 * 
 * @author Peter Abeles
 */
public class BOverrideBlurImageOps extends BOverrideClass {

	static {
		BOverrideManager.register(BOverrideBlurImageOps.class);
	}

	public static Mean mean;
	public static Median median;
	public static Gaussian gaussian;

	public interface Mean<T extends ImageBase<T>> {
		void processMean(T input, T output, int radius, T storage);
	}

	public interface Median<T extends ImageBase<T>> {
		void processMedian(T input, T output, int radius);
	}
	
	public interface Gaussian<T extends ImageBase<T>> {
		void processGaussian(T input, T output, double sigma , int radius, T storage );
	}

	public static <T extends ImageBase<T>>
	boolean invokeNativeMean(T input, T output, int radius, T storage) {
		boolean processed = false;
		if( BOverrideBlurImageOps.mean != null ) {
			try {
				BOverrideBlurImageOps.mean.processMean(input,output,radius,storage);
				processed = true;
			} catch( RuntimeException ignore ) {}
		}
		return processed;
	}

	public static <T extends ImageBase<T>>
	boolean invokeNativeMedian(T input, T output, int radius) {
		boolean processed = false;
		if( BOverrideBlurImageOps.median != null ) {
			try {
				BOverrideBlurImageOps.median.processMedian(input,output,radius);
				processed = true;
			} catch( RuntimeException ignore ) {}
		}
		return processed;
	}

	// TODO replace with native normalized?
	public static <T extends ImageBase<T>>
	boolean invokeNativeGaussian(T input, T output, double sigma , int radius, T storage) {
		boolean processed = false;
		if( BOverrideBlurImageOps.gaussian != null ) {
			try {
				BOverrideBlurImageOps.gaussian.processGaussian(input,output,sigma,radius,storage);
				processed = true;
			} catch( RuntimeException ignore ) {}
		}
		return processed;
	}
}
