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

package boofcv.abst.filter.derivative;

import boofcv.struct.image.ImageGray;


/**
 * A generic interface for computing image's second derivatives directly from the source image.  This is typically
 * slower than computing it from the image's gradient {@link ImageHessian}, even when the time to compute the image's
 * gradient is taken in account.
 *
 * @author Peter Abeles
 */
public interface ImageHessianDirect<Input extends ImageGray, Output extends ImageGray>
		extends ImageDerivative<Input,Output> 
{

	/**
	 * Computes all the second derivative terms in the image.
	 *
	 * @param inputImage Original image.
	 * @param derivXX Second derivative x-axis x-axis
	 * @param derivYY Second derivative x-axis y-axis
	 * @param derivXY Second derivative x-axis y-axis
	 */
	public void process( Input inputImage , Output derivXX, Output derivYY, Output derivXY  );

}
