/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.io.wrapper;

import boofcv.io.image.SimpleImageSequence;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

/**
 * High level interface for opening a webcam. Easy to use but you can't do much configuration.
 *
 * @author Peter Abeles
 */
public interface WebcamInterface {
	/**
	 * Opens the webcam. The specified resolution is a suggestion only.
	 *
	 * @param device Which webcam to open
	 * @param width Desired image width
	 * @param height Desired image height
	 * @param imageType Type of image
	 * @return {@link SimpleImageSequence} for the webcam.
	 */
	<T extends ImageBase<T>> SimpleImageSequence<T> open( String device , int width , int height , ImageType<T> imageType );
}
