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
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageUInt8;
import processing.core.PImage;

/**
 * Simplified interface for labeled images.  A labeled is an image which
 *
 * @author Peter Abeles
 */
public class SimpleLabeledImage extends SimpleImage<ImageSInt32> {

	public SimpleLabeledImage(ImageSInt32 image) {
		super(image);
	}

	public SimpleBinary convertBinary() {
		ImageUInt8 binary = new ImageUInt8(image.width, image.height);
		BinaryImageOps.labelToBinary(image,binary);
		return new SimpleBinary(binary);
	}

	public SimpleBinary convertBinary( boolean selectedBlobs[] ) {
		ImageUInt8 binary = new ImageUInt8(image.width, image.height);
		BinaryImageOps.labelToBinary(image,binary,selectedBlobs);
		return new SimpleBinary(binary);
	}

	public PImage visualize() {
		return VisualizeProcessing.labeled(image);
	}
}
