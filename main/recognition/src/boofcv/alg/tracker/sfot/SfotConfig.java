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

package boofcv.alg.tracker.sfot;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.tracker.klt.KltConfig;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.struct.image.ImageSingleBand;

/**
 * fContains configuration parameters for {@link SparseFlowObjectTracker}.
 *
 * @author Peter Abeles
 */
public class SfotConfig <T extends ImageSingleBand, D extends ImageSingleBand> {

	public Class<T> imageType;
	public Class<D> derivType;

	/**
	 * Random seed used by random number generator
	 */
	public long randSeed = 0xFEED;
	/**
	 * Number of iterative cycles used by LeastMedianOfSquares
	 */
	public int robustCycles = 50;
	/**
	 * Maximum allowed error in pixels when performing robust model fitting using LeastMedianOfSquares
	 */
	public double robustMaxError = 10;
	public int trackerFeatureRadius = 5;


	/**
	 * Number of points it samples along one side of the grid.
	 */
	public int numberOfSamples = 15;

	/**
	 * Maximum allowed forward-backwards error in pixels
	 */
	public double maximumErrorFB = 10;

	/**
	 * Basic parameters for tracker.  KltConfig.createDefault() with maxIterations = 50 is suggested.
	 */
	public KltConfig trackerConfig;

	ImageGradient<T, D> gradient;

	/**
	 * Creates a configuration using default values.
	 * @param imageType Type of gray-scale image it processes.
	 */
	public SfotConfig(  Class<T> imageType) {
		this.imageType = imageType;
		this.derivType = GImageDerivativeOps.getDerivativeType(imageType);

		gradient = FactoryDerivative.sobel(imageType, derivType);

		trackerConfig = new KltConfig();
		trackerConfig.maxIterations = 50;
	}

	public SfotConfig() {
	}
}
