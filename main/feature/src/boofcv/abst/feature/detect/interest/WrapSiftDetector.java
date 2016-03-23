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

package boofcv.abst.feature.detect.interest;

import boofcv.alg.feature.detect.interest.SiftDetector;
import boofcv.core.image.GConvertImage;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageBase;
import georegression.struct.point.Point2D_F64;

/**
 * Wrapper around {@link SiftDetector} for {@link InterestPointDetector}.
 *
 * @author Peter Abeles
 */
public class WrapSiftDetector<T extends ImageBase>
		implements InterestPointDetector<T>
{
	SiftDetector detector;

	GrayF32 imageFloat = new GrayF32(1,1);

	Class<T> inputType;

	public WrapSiftDetector(SiftDetector detector, Class<T> inputType ) {
		this.detector = detector;
		this.inputType = inputType;
	}

	@Override
	public void detect(T image) {

		GrayF32 input;
		if( image instanceof GrayF32) {
			input = (GrayF32)image;
		} else {
			imageFloat.reshape(image.width,image.height);
			GConvertImage.convert(image,imageFloat);
			input = imageFloat;
		}

		detector.process(input);
	}

	@Override
	public int getNumberOfFeatures() {
		return detector.getDetections().size();
	}

	@Override
	public Point2D_F64 getLocation(int featureIndex) {
		return detector.getDetections().get(featureIndex);
	}

	@Override
	public double getRadius(int featureIndex) {
		return detector.getDetections().get(featureIndex).scale;
	}

	@Override
	public double getOrientation(int featureIndex) {
		return 0;
	}

	@Override
	public boolean hasScale() {
		return true;
	}

	@Override
	public boolean hasOrientation() {
		return false;
	}
}
