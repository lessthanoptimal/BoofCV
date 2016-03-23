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

package boofcv.abst.feature.detect.intensity;

import boofcv.alg.filter.derivative.LaplacianEdge;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;

/**
 *
 *
 * @author Peter Abeles
 */
public class WrapperLaplacianBlobIntensity<I extends ImageGray>
		extends BaseGeneralFeatureIntensity<I,ImageGray> {

	@Override
	public void process(I image, ImageGray derivX, ImageGray derivY, ImageGray derivXX, ImageGray derivYY, ImageGray derivXY) {
		init(image.width,image.height);
		if( image instanceof GrayU8) {
			LaplacianEdge.process((GrayU8)image,intensity);
		} else if( image instanceof GrayF32) {
			LaplacianEdge.process((GrayF32)image,intensity);
		} else {
			throw new IllegalArgumentException("Unsupported input image type");
		}
	}

	@Override
	public QueueCorner getCandidatesMin() {
		return null;
	}

	@Override
	public QueueCorner getCandidatesMax() {
		return null;
	}

	@Override
	public boolean getRequiresGradient() {
		return false;
	}

	@Override
	public boolean getRequiresHessian() {
		return false;
	}

	@Override
	public boolean hasCandidates() {
		return false;
	}

	@Override
	public int getIgnoreBorder() {
		return 1;
	}

	@Override
	public boolean localMinimums() {
		return true;
	}

	@Override
	public boolean localMaximums() {
		return true;
	}
}
