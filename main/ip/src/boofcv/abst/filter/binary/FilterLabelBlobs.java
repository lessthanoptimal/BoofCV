/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

import boofcv.abst.filter.FilterImageInterface;
import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageUInt8;

/**
 * Wrapper around labelBlobs4
 */
public class FilterLabelBlobs implements FilterImageInterface<ImageUInt8,ImageSInt32> {

	boolean isFour;
	int numObjects;

	public FilterLabelBlobs(boolean four) {
		isFour = four;
	}

	@Override
	public void process(ImageUInt8 input, ImageSInt32 output) {
		if( isFour )
			numObjects = BinaryImageOps.labelBlobs4(input,output);
		else
			numObjects = BinaryImageOps.labelBlobs4(input,output);
	}

	/**
	 * Number of detected blobs.
	 *
	 * @return number of blobs.
	 */
	public int getNumBlobs() {
		return numObjects;
	}

	@Override
	public int getHorizontalBorder() {
		return 0;
	}

	@Override
	public int getVerticalBorder() {
		return 0;
	}

	@Override
	public Class<ImageUInt8> getInputType() {
		return ImageUInt8.class;
	}
}
