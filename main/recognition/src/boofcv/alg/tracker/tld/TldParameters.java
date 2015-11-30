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

package boofcv.alg.tracker.tld;

import boofcv.alg.tracker.klt.KltConfig;

/**
 * Configuration file for TLD tracker.
 *
 * @see TldTracker
 *
 * @author Peter Abeles
 */
public class TldParameters {

	/**
	 * Maximum number of NCC templates it will examine inside the detection cascade.  Used to limit the amount
	 * of processing used during detection.  To disable set to Integer.MAX_VALUE
	 */
	public int maximumCascadeConsider = 500;

	/**
	 * Number of ferns it will use to train the negative classifier
	 */
	public int numNegativeFerns = 1000;

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
	 * The minimum number of pixels along a side in a detection rectangle which will be considered.
	 */
	public int detectMinimumSide = 20;

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
	 * If two regions have an overlap more than or equal to this value they are considered to be strongly connected
	 */
	public double overlapUpper = 0.8;

	/**
	 * If two regions have an overlap less than this value they are considered to be disconnected
	 */
	public double overlapLower = 0.2;

	/**
	 * How similar the area needs to be for it to be considered a continuation of the previous track and will
	 * update the description
	 */
	public double thresholdSimilarArea = 0.2;

	/**
	 * A track must have a confidence above this value to be considered highly confident, allowing learning
	 * to be activated again.
	 */
	public double confidenceThresholdStrong = 0.75;

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
	 * Number of sample points pairs.  0 &lt; N &le; 32.  A value of 10 is recommended.
	 */
	public int fernSize = 10;

	/**
	 * The minimum value for a region's confidence that will be accepted.  When the tracking hypothesis is
	 * accepted its value can dip very low.
	 */
	public double confidenceAccept = 0.4;

	/**
	 * Determines the number of scales it will search in increments of powers of 1.2.  All scales from
	 * 1.2^(-scaleSpread) to 1.2^scaleSpread are checked.
	 */
	public int scaleSpread = 10;

	/**
	 * Basic parameters for tracker.  KltConfig.createDefault() with maxIterations = 50 is suggested.
	 */
	public KltConfig trackerConfig;

	public TldParameters() {
		trackerConfig = new KltConfig();
		trackerConfig.maxIterations = 50;
	}
}
