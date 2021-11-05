/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.factory.feature.detect.interest;

import boofcv.abst.feature.describe.ConfigSiftScaleSpace;
import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.abst.feature.detect.interest.ConfigPointDetector;
import boofcv.abst.feature.detect.interest.ConfigSiftDetector;
import boofcv.struct.Configuration;

/**
 * Configuration for detecting any built in interest point.
 *
 * @author Peter Abeles
 */
public class ConfigDetectInterestPoint implements Configuration {

	/** The feature detector is used. Not always used. */
	public Type type = Type.POINT;

	/** Describes the scale-space used by SIFT detector / descriptor. */
	public ConfigSiftScaleSpace scaleSpaceSift = new ConfigSiftScaleSpace();

	/** Configuration for point based detectors (e.g. corners and blob) */
	public ConfigPointDetector point = new ConfigPointDetector();

	/** Fast Hessian scale invariant blob detector. This is what SURF uses */
	public ConfigFastHessian fastHessian = new ConfigFastHessian();

	/** SIFT scale invariant blob detector */
	public ConfigSiftDetector sift = new ConfigSiftDetector();

	@Override
	public void checkValidity() {
		scaleSpaceSift.checkValidity();
		point.checkValidity();
		fastHessian.checkValidity();
		sift.checkValidity();
	}

	public enum Type {
		POINT, FAST_HESSIAN, SIFT,
	}

	public ConfigDetectInterestPoint setTo( ConfigDetectInterestPoint src ) {
		this.type = src.type;
		this.scaleSpaceSift.setTo(src.scaleSpaceSift);
		this.point.setTo(src.point);
		this.fastHessian.setTo(src.fastHessian);
		this.sift.setTo(src.sift);
		return this;
	}
}
