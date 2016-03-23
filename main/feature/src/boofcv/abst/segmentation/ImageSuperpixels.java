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

package boofcv.abst.segmentation;

import boofcv.struct.ConnectRule;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

/**
 * High level interface for computing superpixels.  Superpixels are sets of connected adjacent pixels which belong.
 * They are intended to break the image up intelligently along object boundaries allowing for easier processing.
 * Each pixel in the output segmented image is assigned an integer label to identify which region in the image it
 * belongs to.  A region is a continuous connected set of pixels.
 *
 * @author Peter Abeles
 */
public interface ImageSuperpixels<T extends ImageBase> {

	/**
	 * Segments the input image into superpixels and puts the output in labeled image.
	 * @param input (Input) image.
	 * @param output (Output) Labeled image
	 */
	public void segment( T input , GrayS32 output );

	/**
	 * Returns the total number of image segments/superpixels found
	 * @return Number of superpixels
	 */
	public int getTotalSuperpixels();

	/**
	 * Connectivity rule used to determine if a pixel is connected
	 * @return Connectivity rule
	 */
	public ConnectRule getRule();

	/**
	 * Type of input image it can process
	 *
	 * @return Input image type
	 */
	public ImageType<T> getImageType();
}
