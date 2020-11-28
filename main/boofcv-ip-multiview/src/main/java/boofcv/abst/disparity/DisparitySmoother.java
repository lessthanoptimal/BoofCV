/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.disparity;

import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import org.ddogleg.struct.VerbosePrint;

/**
 * High level API for algorithm which attempt to reduce the noise in disparity images in a post processing step
 *
 * @author Peter Abeles
 */
public interface DisparitySmoother<Image extends ImageBase<Image>, Disparity extends ImageGray<Disparity>>
		extends VerbosePrint {
	/**
	 * Process the disparity image and smooth it. The input disparity is modified.
	 *
	 * @param image (Input) Rectified image the disparity is computed from. Pixels should line up.
	 * @param disparity (Input, Output) The disparity image which is to be smoothed.
	 * @param disparityRange (Input) Range of values in the disparity image.
	 */
	void process( Image image, Disparity disparity, int disparityRange );
}
