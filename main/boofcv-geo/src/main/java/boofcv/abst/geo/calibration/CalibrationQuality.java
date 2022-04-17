/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.geo.calibration;

/**
 * Provides information on how good calibration images are and the calibration results can be trusted.
 *
 * Many of these metrics are based on general "rules of thumb". For example, getting observations close to the
 * image border will in general result in better calibration as that's where the most distortion is.
 */
public class CalibrationQuality {
	/** How well the image border has been observed. 0 to 1. 1 = best */
	public double borderFill;

	/** How well the inner image has been observed. 0 to 1. 1 = best */
	public double innerFill;

	/** Indicates how much geometric diversity there is. Higher the better */
	public double geometric;

	public void reset() {
		borderFill = 0;
		innerFill = 0;
		geometric = 0;
	}
}
