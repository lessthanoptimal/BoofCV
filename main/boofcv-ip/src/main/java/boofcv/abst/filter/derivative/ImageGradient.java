/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.image.ImageBase;


/**
 * A generic interface for computing first order image derivative along the x and y axes.
 *
 * @author Peter Abeles
 */
public interface ImageGradient<Input extends ImageBase<Input>, Output extends ImageBase<Output>>
		extends ImageDerivative<Input,Output> {

	/**
	 * Computes the image gradient from the input image and stores the results into
	 * 'derivX' and 'derivY'
	 *
	 * @param inputImage Original input image. Not modified.
	 * @param derivX First order image derivative along the x-axis. Modified.
	 * @param derivY First order image derivative along the y-axis. Modified.
	 */
	public void process( Input inputImage , Output derivX, Output derivY );

}
