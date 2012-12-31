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

package boofcv.benchmark.feature.detect;

import boofcv.struct.image.ImageSingleBand;


/**
 * @author Peter Abeles
 */
public class BenchmarkInterestParameters<T extends ImageSingleBand, D extends ImageSingleBand>
{
	// radius of the feature it is detecting
	public int radius = 2;
	// the number of features it will search for
	public int maxFeatures = 2000;
	// max features per scale in scale-space features
	public int maxScaleFeatures = maxFeatures/2;
	// which scales are examined in scale-space features
	public double[] scales = new double[]{1,1.5,2,3,4,6,8,12,16};
	// types of images being processed
	public Class<T> imageType;
	public Class<D> derivType;
}
