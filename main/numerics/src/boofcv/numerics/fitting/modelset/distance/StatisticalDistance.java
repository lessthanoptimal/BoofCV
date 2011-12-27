/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.numerics.fitting.modelset.distance;


/**
 * Statistic used by {@link StatisticalDistanceModelMatcher}.
 *
 * @author Peter Abeles
 */
public enum StatisticalDistance {
	/**
	 * Mean metric is used to evaluate goodness.  Threshold is in standard deviations. 2
	 */
	MEAN,
	/**
	 * Median is used to evaluate goodness.  Threshold is fraction of points saved. 0.95
	 */
	PERCENTILE
}
