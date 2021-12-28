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

package boofcv.io;

import boofcv.io.image.SimpleImageSequence;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.io.Reader;

/**
 * Abstract interface for accessing files, images, and videos. Intended to help
 * handle regular applications and applets
 *
 * @author Peter Abeles
 */
public interface MediaManager {

	@Nullable Reader openFile( String fileName );

	default Reader openFileNotNull( String fileName ) {
		Reader r = openFile(fileName);
		if (r == null)
			throw new RuntimeException("Failed to open file=" + fileName);
		return r;
	}

	@Nullable BufferedImage openImage( String fileName );

	default BufferedImage openImageNotNull( String fileName ) {
		BufferedImage image = openImage(fileName);
		if (image == null)
			throw new RuntimeException("Failed to open image=" + fileName);
		return image;
	}

	<T extends ImageBase<T>>
	@Nullable SimpleImageSequence<T> openVideo( String fileName, ImageType<T> imageInfo );

	default <T extends ImageBase<T>> SimpleImageSequence<T>
	openVideoNotNull( String fileName, ImageType<T> imageInfo ) {
		SimpleImageSequence<T> video = openVideo(fileName, imageInfo);
		if (video == null)
			throw new RuntimeException("Failed to open video=" + fileName);
		return video;
	}

	/**
	 * Opens the specified webcam.
	 *
	 * @param device Reference to the webcam device. null if you just want to open the default
	 * @param width requested image width. Default resolution is any value less than 0
	 * @param height requested image height. Default resolution is any value less than 0
	 * @param imageType Type of input image
	 * @return The image sequence.
	 */
	<T extends ImageBase<T>>
	@Nullable SimpleImageSequence<T> openCamera( String device, int width, int height, ImageType<T> imageType );

	default <T extends ImageBase<T>> SimpleImageSequence<T>
	openCameraNotNull( String fileName, int width, int height, ImageType<T> imageType ) {
		SimpleImageSequence<T> camera = openCamera(fileName, width, height, imageType);
		if (camera == null)
			throw new RuntimeException("Failed to open camera=" + fileName);
		return camera;
	}
}
