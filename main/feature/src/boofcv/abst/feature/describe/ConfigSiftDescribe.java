/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.describe;

import boofcv.alg.feature.describe.DescribePointSift;
import boofcv.struct.Configuration;

/**
 * Configuration for {@link DescribePointSift}
 * 
 * @author Peter Abeles
 */
public class ConfigSiftDescribe implements Configuration {

	/** widthSubregion Width of sub-region in samples.  Try 4 */
	public int widthSubregion=4;

	/** widthGrid Width of grid in subregions.  Try 4. */
	public int widthGrid=4;

	/** numHistogramBins Number of bins in histogram.  Try 8 */
	public int numHistogramBins=8;

	/** sigmaToPixels Conversion of sigma to pixels.  Used to scale the descriptor region.  Try 1.0  */
	public double sigmaToPixels=1.0;

	/** weightingSigmaFraction Sigma for Gaussian weighting function is set to this value * region width.  Try 0.5  */
	public double weightingSigmaFraction=0.5;

	/** maxDescriptorElementValue Helps with non-affine changes in lighting. See paper.  Try 0.2  */
	public double maxDescriptorElementValue=0.2;

	@Override
	public void checkValidity() {
		
	}
}
