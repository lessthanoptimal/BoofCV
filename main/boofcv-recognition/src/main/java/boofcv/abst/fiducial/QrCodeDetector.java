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

package boofcv.abst.fiducial;

import boofcv.alg.fiducial.qrcode.QrCode;
import boofcv.struct.image.ImageGray;

import java.util.List;

/**
 * High level interface for reading QR Codes from gray scale images
 *
 * @author Peter Abeles
 */
public interface QrCodeDetector<T extends ImageGray<T>> {

	/**
	 * Processes the image and searches for markers
	 * @param image The image being processed
	 */
	void process( T image );

	/**
	 * List of detected markers
	 *
	 * @return List of detected QR-Codes. WARNING: Data might be recycled on next call to {@link #process}
	 */
	List<QrCode> getDetections();

	/**
	 * List of likely markers that were rejected.
	 *
	 * @return List of rejected candidate QR-Codes. WARNING: Data might be recycled on next call to {@link #process}
	 */
	List<QrCode> getFailures();

	/**
	 * Type of image it can process
	 * @return input image type
	 */
	Class<T> getImageType();
}
