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

package boofcv.io.image;

import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

/**
 * Simplified interface for reading in a sequence of images. This interface hides the complexities of reading
 * from different file formats and from live video streams.
 *
 * @author Peter Abeles
 */
public interface SimpleImageSequence<T extends ImageBase<T>> {

	/**
	 * Returns the width of the current image
	 */
	int getWidth();

	/**
	 * Returns the height of the current image
	 */
	int getHeight();

	/**
	 * If a new image is available.
	 *
	 * @return true if a new image is available.
	 */
	boolean hasNext();

	/**
	 * Steps to the next image in the sequence and loads it.
	 * @return The next image in the sequence, which is now the current image.
	 */
	T next();

	/**
	 * Returns the currently loaded image in the sequence
	 */
	T getImage();

	/**
	 * Returns the image in the original format that it was read in as. When dealing with swing or any standard
	 * Java SE environment this will almost always be BufferedImage. The type has been abstracted out
	 * to provide better Android support.
	 */
	<InternalImage> InternalImage getGuiImage();

	/**
	 * Call when done reading the image sequence.
	 */
	void close();

	/**
	 * Returns the number of the current frame in the sequence.
	 *
	 * @return Frame ID number.
	 */
	int getFrameNumber();

	/**
	 * Sets if the video should loop or not
	 *
	 * @param loop true for looping forever, false for once
	 */
	void setLoop( boolean loop );

	/**
	 * Returns the type of class used to store the output image
	 */
	ImageType<T> getImageType();

	/**
	 * Start reading the sequence from the start
	 */
	void reset();
}
