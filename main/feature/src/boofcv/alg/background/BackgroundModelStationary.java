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

package boofcv.alg.background;

import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

/**
 * <p>
 * Base class for classifying pixels as background based on the apparent motion of pixels when the camera is static.
 * </p>
 *
 * @author Peter Abeles
 */
public abstract class BackgroundModelStationary<T extends ImageBase> extends BackgroundModel<T> {

	public BackgroundModelStationary(ImageType<T> imageType) {
		super(imageType);
	}

	/**
	 *  Updates the background with new image information.
	 */
	public abstract void updateBackground( T frame );

	/**
	 * Invoke to use the background image to segment the current frame into background and foreground pixels
	 *
	 * @param frame current image
	 * @param segmented Segmented image. 0 = background, 1 = foreground/moving
	 */
	public abstract void segment( T frame , GrayU8 segmented );
}
