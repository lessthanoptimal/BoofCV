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

import boofcv.abst.segmentation.ImageSuperpixels;
import boofcv.struct.ConnectRule;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageSInt32;
import processing.core.PImage;

/**
 * Simplified interface around {@link boofcv.abst.segmentation.ImageSuperpixels}.
 *
 * @author Peter Abeles
 */
public class SimpleImageSegmentation {
	ImageBase image;
	SimpleLabeledImage output = new SimpleLabeledImage(new ImageSInt32(1,1));
	ImageSuperpixels segmentation;

	public SimpleImageSegmentation(ImageSuperpixels segmentation) {
		this.segmentation = segmentation;
		image = segmentation.getImageType().createImage(1,1);
	}


	public void segment( PImage input ) {
		ConvertProcessing.convertFromRGB(input,image);
		segmentation.segment(image,output.getImage());
	}

	public void segment( SimpleImage input ) {
		output.getImage().reshape(input.image.width,input.image.height);

		segmentation.segment(input.getImage(),output.getImage());
	}

	public SimpleLabeledImage getOutput() {
		return output;
	}

	public int getTotalSegments() {
		return segmentation.getTotalSuperpixels();
	}

	public ConnectRule getRule() {
		return segmentation.getRule();
	}
}
