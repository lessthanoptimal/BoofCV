/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.intensity;

import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.ImageGray;


/**
 * @author Peter Abeles
 */
public class GIntegralImageFeatureIntensity {
	/**
	 * Computes an approximation to the Hessian's determinant.
	 *
	 * @param integral Integral image transform of input image. Not modified.
	 * @param skip How many pixels should it skip over.
	 * @param size Hessian kernel's size.
	 * @param intensity Output intensity image.
	 */
	public static <T extends ImageGray>
	void hessian( T integral, int skip , int size ,
				  GrayF32 intensity) {

		if( integral instanceof GrayF32) {
			IntegralImageFeatureIntensity.hessian((GrayF32)integral,skip,size,intensity);
		} else if( integral instanceof GrayS32) {
			IntegralImageFeatureIntensity.hessian((GrayS32)integral,skip,size,intensity);
		} else {
			throw new IllegalArgumentException("Unsupported input type");
		}
	}
}
