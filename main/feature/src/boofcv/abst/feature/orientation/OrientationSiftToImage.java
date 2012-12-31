/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.orientation;

import boofcv.alg.feature.detect.interest.SiftImageScaleSpace;
import boofcv.alg.feature.orientation.OrientationHistogramSift;
import boofcv.struct.image.ImageFloat32;

/**
 * Wrapper around {@link OrientationHistogramSift} for {@link OrientationImage}.  Selects
 * the best solution from the multiple solutions.
 *
 * @author Peter Abeles
 */
public class OrientationSiftToImage implements OrientationImage<ImageFloat32>
{
	SiftImageScaleSpace ss;
	OrientationHistogramSift alg;
	double scale;

	public OrientationSiftToImage(OrientationHistogramSift alg, SiftImageScaleSpace ss) {
		this.alg = alg;
		this.ss = ss;
	}

	@Override
	public void setImage(ImageFloat32 image) {
		ss.constructPyramid(image);
		ss.computeDerivatives();
		alg.setScaleSpace(ss);
	}

	@Override
	public Class<ImageFloat32> getImageType() {
		return ImageFloat32.class;
	}

	@Override
	public void setScale(double scale) {
		this.scale = scale;
	}

	@Override
	public double compute(double c_x, double c_y) {
		alg.process(c_x,c_y,scale);

		return alg.getPeakOrientation();
	}
}
