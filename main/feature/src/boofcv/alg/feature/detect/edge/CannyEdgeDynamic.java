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

package boofcv.alg.feature.detect.edge;

import boofcv.abst.filter.blur.BlurFilter;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;


/**
 * Canny edge detector where the thresholds are computed dynamically based upon the magnitude of the largest edge
 *
 * @author Peter Abeles
 */
public class CannyEdgeDynamic<T extends ImageGray, D extends ImageGray> extends CannyEdge<T,D>
{
	/**
	 * Constructor and configures algorithm
	 *
	 * @param blur Used during the image blur pre-process step.
	 * @param gradient Computes image gradient.
	 */
	public CannyEdgeDynamic(BlurFilter<T> blur, ImageGradient<T, D> gradient, boolean saveTrace) {
		super(blur, gradient,saveTrace);
	}

	@Override
	protected void performThresholding(float threshLow, float threshHigh, GrayU8 output) {

		if( threshLow < 0 || threshLow > 1 || threshHigh < 0 || threshHigh > 1 )
			throw new IllegalArgumentException("Relative thresholds must be from 0 to 1, inclusive.");

		// find the largest intensity value
		float max = ImageStatistics.max(suppressed);

		// set the threshold using that
		threshLow = max*threshLow;
		threshHigh = max*threshHigh;

		if( threshLow <= 0f && threshHigh <= 0f ) {
			// in this pathological case there is no texture in the image.  It is probably less harmful to return
			// nothing than a whole image
			if( hysteresisPts != null )
				hysteresisPts.getContours().clear();
			if( output != null )
				ImageMiscOps.fill(output,0);
		} else {
			super.performThresholding(threshLow, threshHigh, output);
		}
	}
}
