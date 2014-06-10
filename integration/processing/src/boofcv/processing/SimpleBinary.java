/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.processing;

import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.Contour;
import boofcv.struct.ConnectRule;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageUInt8;
import processing.core.PConstants;
import processing.core.PImage;

import java.util.List;

/**
 * Simplified interface for handling binary images
 *
 * @author Peter Abeles
 */
public class SimpleBinary {
	ImageUInt8 image;

	public SimpleBinary(ImageUInt8 image) {
		this.image = image;
	}

	public SimpleBinary logicAnd( SimpleBinary imgB ) {
		ImageUInt8 out = new ImageUInt8(image.width, image.height);
		BinaryImageOps.logicAnd(image, imgB.image, out);
		return new SimpleBinary(out);
	}

	public SimpleBinary logicOr( SimpleBinary imgB ) {
		ImageUInt8 out = new ImageUInt8(image.width, image.height);
		BinaryImageOps.logicOr(image,imgB.image,out);
		return new SimpleBinary(out);
	}

	public SimpleBinary logicXor( SimpleBinary imgB ) {
		ImageUInt8 out = new ImageUInt8(image.width, image.height);
		BinaryImageOps.logicXor(image,imgB.image,out);
		return new SimpleBinary(out);
	}

	public SimpleBinary erode4( int numTimes ) {
		ImageUInt8 out = new ImageUInt8(image.width, image.height);
		BinaryImageOps.erode4(image,numTimes,out);
		return new SimpleBinary(out);
	}

	public SimpleBinary erode8( int numTimes ) {
		ImageUInt8 out = new ImageUInt8(image.width, image.height);
		BinaryImageOps.erode8(image,numTimes,out);
		return new SimpleBinary(out);
	}

	public SimpleBinary dilate4( int numTimes ) {
		ImageUInt8 out = new ImageUInt8(image.width, image.height);
		BinaryImageOps.dilate4(image,numTimes,out);
		return new SimpleBinary(out);
	}

	public SimpleBinary dilate8( int numTimes ) {
		ImageUInt8 out = new ImageUInt8(image.width, image.height);
		BinaryImageOps.dilate8(image,numTimes,out);
		return new SimpleBinary(out);
	}

	public SimpleBinary edge4() {
		ImageUInt8 out = new ImageUInt8(image.width,image.height);
		BinaryImageOps.edge4(image,out);
		return new SimpleBinary(out);
	}

	public SimpleBinary edge8() {
		ImageUInt8 out = new ImageUInt8(image.width,image.height);
		BinaryImageOps.edge8(image,out);
		return new SimpleBinary(out);
	}

	public SimpleBinary removePointNoise() {
		ImageUInt8 out = new ImageUInt8(image.width,image.height);
		BinaryImageOps.removePointNoise(image, out);
		return new SimpleBinary(out);
	}

	/**
	 * Finds the contours of blobs in a binary image.  Uses 8-connect rule
	 *
	 * @see BinaryImageOps#contour
	 */
	public ResultsBlob contour() {
		ImageSInt32 labeled = new ImageSInt32(image.width,image.height);

		List<Contour> contours = BinaryImageOps.contour(image, ConnectRule.EIGHT, labeled);

		return new ResultsBlob(contours,labeled);
	}

	public PImage visualize() {
		PImage out = new PImage(image.width, image.height, PConstants.RGB);

		int indexOut = 0;
		for (int y = 0; y < image.height; y++) {
			int indexIn = image.startIndex + image.stride*y;
			for (int x = 0; x < image.width; x++,indexIn++,indexOut++) {
				out.pixels[indexOut] = image.data[indexIn] == 0 ? 0xFF000000 : 0xFFFFFFFF;
			}
		}

		return out;
	}

	public ImageUInt8 getImage() {
		return image;
	}
}
