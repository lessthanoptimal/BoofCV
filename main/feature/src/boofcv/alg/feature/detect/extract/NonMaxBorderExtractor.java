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

package boofcv.alg.feature.detect.extract;

import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageFloat32;

/**
 * <p/>
 * Non-max extractor which processes the image border only in situations where only a partial
 * region is observable.  It is designed to handle all cases, even very small images.
 * <p/>
 *
 * @author Peter Abeles
 */
public class NonMaxBorderExtractor  {

	// the minimum separation between features
	int minSeparation;
	// minimum intensity value
	private float thresh;
	// image border that should be skipped
	private int border;
	// the input image's border
	private int borderInput;

	// passed in intensity image
	ImageFloat32 inten;

	/**
	 * @param minSeparation How close features can be to each other.
	 * @param thresh What the minimum intensity a feature must have to be considered a feature.
	 */
	public NonMaxBorderExtractor( int minSeparation, float thresh) {
		this.border = minSeparation;
		this.minSeparation = minSeparation;
		this.thresh = thresh;
	}

	public float getThresh() {
		return thresh;
	}

	public void setThresh(float thresh) {
		this.thresh = thresh;
	}

	public void setMinSeparation(int minSeparation) {
		this.minSeparation = minSeparation;
	}

	public void setInputBorder( int borderInput ) {
		this.borderInput = borderInput;
		this.border = minSeparation + borderInput;
	}

	/**
	 * Detects corners in the image while excluding corners which are already contained in the corners list.
	 *
	 * @param intensityImage Feature intensity image. Can be modified.
	 * @param detected	Where found feature locations are stored.
	 */
	public void process(ImageFloat32 intensityImage, QueueCorner detected) {
		int imgWidth = intensityImage.getWidth()-borderInput;
		int imgHeight = intensityImage.getHeight()-borderInput;

		inten = intensityImage;

		// top border
		for( int y = borderInput; y < border; y++) {
			int y0 = Math.max(borderInput,y-minSeparation);
			int y1 = Math.min(imgHeight, y + minSeparation + 1);

			int index = intensityImage.startIndex + y * intensityImage.stride + borderInput;

			for( int x = borderInput; x < imgWidth; x++ , index++ ) {
				int x0 = Math.max(borderInput,x-minSeparation);
				int x1 = Math.min(imgWidth,x+minSeparation+1);

				if( isMax(index,x0,y0,x1,y1) ) {
					detected.add(x, y);
				}
			}
		}

		// bottom border
		for( int y = imgHeight-border; y < imgHeight; y++) {
			int y0 = Math.max(borderInput,y-minSeparation);
			int y1 = Math.min(imgHeight, y + minSeparation + 1);

			int index = intensityImage.startIndex + y * intensityImage.stride + borderInput;

			for( int x = borderInput; x < imgWidth; x++ , index++ ) {
				int x0 = Math.max(borderInput,x-minSeparation);
				int x1 = Math.min(imgWidth,x+minSeparation+1);

				if( isMax(index,x0,y0,x1,y1) ) {
					detected.add(x, y);
				}
			}
		}

		// side border
		for( int y = border; y < imgHeight-border; y++) {
			int y0 = Math.max(borderInput,y-minSeparation);
			int y1 = Math.min(imgHeight, y + minSeparation + 1);

			// left border
			int index = intensityImage.startIndex + y * intensityImage.stride + borderInput;

			for( int x = borderInput; x < border; x++ , index++ ) {
				int x0 = Math.max(borderInput,x-minSeparation);
				int x1 = Math.min(imgWidth,x+minSeparation+1);

				if( isMax(index,x0,y0,x1,y1) ) {
					detected.add(x, y);
				}
			}

			// right border
			index = intensityImage.startIndex + y * intensityImage.stride + imgWidth-border;

			for( int x = imgWidth-border; x < imgWidth; x++ , index++ ) {
				int x0 = Math.max(borderInput,x-minSeparation);
				int x1 = Math.min(imgWidth,x+minSeparation+1);

				if( isMax(index,x0,y0,x1,y1) ) {
					detected.add(x, y);
				}
			}
		}

	}

	/**
	 * Checks to see if the center pixel is the local maximum
	 */
	private boolean isMax(int indexCenter, int x0, int y0, int x1, int y1) {
		float center = inten.data[indexCenter];

		if( center < thresh || center == Float.MAX_VALUE )
			return false;

		for( int y = y0; y < y1; y++ ) {
			int i = inten.startIndex + y*inten.stride + x0;
			for( int x = x0; x < x1; x++ , i++) {
				if( i == indexCenter )
					continue;

				if( inten.data[i] >= center ) {
					return false;
				}
			}
		}
		return true;
	}
}