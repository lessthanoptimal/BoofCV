/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.filter.binary;

import boofcv.struct.image.GrayU8;

/**
 * Helper function that makes it easier to adjust the size of the binary image when working with a padded or unpadded
 * contour finding algorithm. Creating a copy of the input image can be avoided when finding contours if
 * the binary image is expanded to have a contour in advance. Slight speed improvement and lower memory usage.
 *
 * @author Peter Abeles
 */
public class BinaryContourHelper {
	// if not null then the binary image will be modified by the contour algorithm and will be padded
	BinaryContourInterface.Padded padded;
	// contains a sub image without padding
	GrayU8 subimage;
	GrayU8 binary = new GrayU8(1,1);

	public BinaryContourHelper( BinaryContourInterface alg , boolean copyBinary ) {
		if( !copyBinary && alg instanceof BinaryContourInterface.Padded ) {
			padded = (BinaryContourInterface.Padded)alg;
			subimage = new GrayU8();
			padded.setCreatePaddedCopy(false);
			padded.setCoordinateAdjustment(1,1);
		}
	}

	/**
	 * Reshapes data so that the un-padded image has the specified shape.
	 */
	public void reshape(int width , int height ) {
		if( padded == null ) {
			binary.reshape(width, height);
		} else {
			binary.reshape(width + 2, height + 2);
			binary.subimage(1, 1, width + 1, height + 1, subimage);
		}
	}

	/**
	 * Returns the binary image with padding
	 */
	public GrayU8 padded() {
		return binary;
	}

	/**
	 * Returns the image without padding. This might be a sub-image
	 */
	public GrayU8 withoutPadding() {
		if( padded == null )
			return binary;
		else
			return subimage;
	}
}
