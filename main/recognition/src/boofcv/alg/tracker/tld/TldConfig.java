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

package boofcv.alg.tracker.tld;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.alg.tracker.klt.KltConfig;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.ImageSingleBand;

/**
 * Configuration file for TLD tracker.
 *
 * @see TldTracker
 *
 * @author Peter Abeles
 */
public class TldConfig<T extends ImageSingleBand, D extends ImageSingleBand> {

	public Class<T> imageType;
	public Class<D> derivType;


	/**
	 * Maximum number of NCC templates it will examine inside the detection cascade.  Used to limit the amount
	 * of processing used during detection.  To disable set to Integer.MAX_VALUE
	 */
	public int maximumCascadeConsider = 500;

	/**
	 * The maximum allowed forwards-backwards error (pixels) for a track.  Suggested value is 10.
	 */
	public double maximumErrorFB = 10;

	/**
	 * Tracks are spawned in an evenly spaced grid inside the previous region.  This value specifies the
	 * number of rows and columns in the grid.
	 */
	public int trackerGridWidth = 10;

	/**
	 * Radius of KLT tracks.  A radius of 5 is recommended.
	 */
	public int trackerFeatureRadius = 5;

	/**
	 * Number of iterations in LSMeD to estimate the region's motion.
	 */
	public int motionIterations = 50;

	/**
	 * If two regions have an overlap fraction more than or equal to this value then they will be considered connected.
	 * Used inside of non-maximum suppression. A value of 0.5 is suggested.
	 */
	public double regionConnect = 0.5;

	/**
	 * When initializing, this is the maximum number of positive examples it will learn.  Regions with the most
	 * overlap are selected first.
	 */
	public int initLearnPositive = 10;

	/**
	 * When initializing, this is the maximum number of negative examples it will learn.  The specific regions
	 * are randomly selected.
	 */
	public int initLearnNegative = 1000;

	/**
	 * If two regions have an overlap more than this value they are considered to be connected
	 */
	public double overlapUpper = 0.6;

	/**
	 * If two regions have an overlap less than this value they are considered to be disconnected
	 */
	public double overlapLower = 0.2;

	/**
	 * Upper acceptance threshold for confidence.  Suggested value is 0.65
	 */
	public double confidenceThresholdUpper = 0.65;

	/**
	 * Lower acceptance threshold for confidence.  Used during hypothesis fusion.
	 *
	 * Suggested value is 0.5
	 */
	public double confidenceThresholdLower = 0.5;

	/**
	 * Random number seed.  Used to create ferns and perform robust model fitting.
	 */
	public long randomSeed = 0xDEADBEE;

	/**
	 * Number of fern descriptors.  A value of 10 is recommended.
	 */
	public int numFerns = 10;

	/**
	 * Number of sample points pairs.  0 < N <= 32.  A value of 10 is recommended.
	 */
	public int fernSize = 10;

	/**
	 * The minimum value for a region's confidence that will be accepted.  When the tracking hypothesis is
	 * accepted its value can dip very low.
	 */
	public double confidenceAccept = 0.4;

	/**
	 * Basic parameters for tracker.  KltConfig.createDefault() is suggested.
	 */
	public KltConfig trackerConfig;

	/**
	 * Interpolation routine. Bilinear is recommended.
	 */
	InterpolatePixel<T> interpolate;

	/**
	 * Computes image gradient. Sobel is recommended.
	 */
	ImageGradient<T, D> gradient;

	/**
	 * Creates a configuration using default values.
	 * @param imageType Type of gray-scale image it processes.
	 */
	public TldConfig(Class<T> imageType) {
		this.imageType = imageType;
		this.derivType = GImageDerivativeOps.getDerivativeType(imageType);

		interpolate = FactoryInterpolation.bilinearPixel(imageType);
		gradient = FactoryDerivative.sobel(imageType, derivType);

		trackerConfig = KltConfig.createDefault();
		trackerConfig.maxIterations = 50;
	}

	public TldConfig() {
	}
}
