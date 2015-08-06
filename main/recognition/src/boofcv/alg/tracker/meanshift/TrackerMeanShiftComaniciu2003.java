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

package boofcv.alg.tracker.meanshift;

import boofcv.struct.RectangleRotate_F32;
import boofcv.struct.image.ImageBase;
import georegression.struct.point.Point2D_F32;

import java.util.List;

/**
 * <p>
 * Mean shift tracker which adjusts the scale (or bandwidth) to account for changes in scale of the target
 * and is based off of [1].  The tracker seeks to minimize the histogram error within the sampled region.
 * The mean-shift region is sampled using an oriented rectangle and weighted using a 2D gaussian.  The target
 * is modeled using a color histogram of the input image, which can be optionally updated after each frame
 * is processed. It can also be configured to not allow scale changes, which can improve stability.
 * </p>
 *
 * <p>
 * Scale selection is done using sum-of-absolute-difference (SAD) error instead of Bhattacharyya as the paper
 * suggests.  Situations where found that two errors counteracted each other when using Bhattacharyya
 * and the incorrect scale would be selected even with perfect data.
 * </p>
 *
 * <p>
 * Another difference from the paper is that mean shift records which hypothesis has the best SAD error.  After
 * mean-shift stops iterating it selects the best solution.  This is primarily helpful in situations where mean-shift
 * doesn't converge in time and jumped away from the solution.
 * </p>
 *
 * <p>
 * [1] Dorin Comaniciu, Visvanathan Ramesh, and Peter Meer,"Kernel-Based Object Tracking." IEEE Transactions on
 * Pattern Analysis and Machine Intelligence 25.4 (2003): 1.
 * </p>
 * @author Peter Abeles
 */
public class TrackerMeanShiftComaniciu2003<T extends ImageBase> {

	// computes the histogram inside a rotated rectangle
	private LocalWeightedHistogramRotRect<T> calcHistogram;
	// the key-frame histogram which is being compared again
	protected float keyHistogram[];
	// weight each element contributes
	protected float weightHistogram[];
	// the amount the scale can change
	protected float scaleChange;

	// most recently select target region
	private RectangleRotate_F32 region = new RectangleRotate_F32();

	// maximum allowed mean-shift iterations
	private int maxIterations;
	// minimum change stopping condition
	private float minimumChange;

	// storage for the track region at different sizes
	private RectangleRotate_F32 region0 = new RectangleRotate_F32();
	private RectangleRotate_F32 region1 = new RectangleRotate_F32();
	private RectangleRotate_F32 region2 = new RectangleRotate_F32();
	private float histogram0[];
	private float histogram1[];
	private float histogram2[];

	// weighting factor for change in scale. 0 to 1.  0 is 100% selected region
	private float gamma;

	// should it update the histogram after tracking?
	private boolean updateHistogram;

	// if true assume the scale is constant
	private boolean constantScale;

	// ratio of the original object size that the track can become
	private float minimumSizeRatio;
	// stores the minimum width that the object can be
	private float minimumWidth;

	/**
	 * Configures tracker.
	 *
	 * @param updateHistogram If true the histogram will be updated using the most recent image. Try true.
	 * @param maxIterations Maximum number of mean-shift iterations.  Try 30
	 * @param minimumChange Mean-shift will stop when the change is below this threshold.  Try 1e-4f
	 * @param gamma Scale weighting factor.  Value from 0 to 1. Closer to 0 the more it will prefer
	 *              the most recent estimate.  Try 0.1
	 * @param minimumSizeRatio Fraction of the original region that the track is allowed to shrink to.  Try 0.25
	 * @param scaleChange The scale can be changed by this much between frames.  0 to 1.  0 = no scale change. 0.1 is
	 *                    recommended value in paper.  no scale change is more stable.
	 * @param calcHistogram Calculates the histogram
	 */
	public TrackerMeanShiftComaniciu2003(boolean updateHistogram,
										 int maxIterations,
										 float minimumChange,
										 float gamma ,
										 float minimumSizeRatio ,
										 float scaleChange,
										 LocalWeightedHistogramRotRect<T> calcHistogram) {
		if( scaleChange < 0 || scaleChange > 1 )
			throw new IllegalArgumentException("Scale change must be >= 0 and <= 1");

		this.updateHistogram = updateHistogram;
		this.maxIterations = maxIterations;
		this.minimumChange = minimumChange;
		this.gamma = gamma;
		this.scaleChange = scaleChange;
		this.constantScale = scaleChange == 0;
		this.minimumSizeRatio = minimumSizeRatio;
		this.calcHistogram = calcHistogram;

		keyHistogram = new float[ calcHistogram.getHistogram().length ];
		weightHistogram = new float[ keyHistogram.length ];
		if( updateHistogram ) {
			histogram0 = new float[ calcHistogram.getHistogram().length ];
			histogram1 = new float[ calcHistogram.getHistogram().length ];
			histogram2 = new float[ calcHistogram.getHistogram().length ];
		}
	}

	/**
	 * Specifies the initial image to learn the target description
	 * @param image Image
	 * @param initial Initial image which contains the target
	 */
	public void initialize( T image , RectangleRotate_F32 initial ) {
		this.region.set(initial);
		calcHistogram.computeHistogram(image,initial);
		System.arraycopy(calcHistogram.getHistogram(),0,keyHistogram,0,keyHistogram.length);

		this.minimumWidth = initial.width*minimumSizeRatio;
	}

	/**
	 * Searches for the target in the most recent image.
	 * @param image Most recent image in the sequence
	 */
	public void track( T image ) {
		// configure the different regions based on size
		region0.set( region );
		region1.set( region );
		region2.set( region );

		region0.width  *= 1-scaleChange;
		region0.height *= 1-scaleChange;

		region2.width  *= 1+scaleChange;
		region2.height *= 1+scaleChange;

		// distance from histogram
		double distance0=1,distance1,distance2=1;

		// perform mean-shift at the different sizes and compute their distance
		if( !constantScale ) {
			if( region0.width >= minimumWidth ) {
				updateLocation(image,region0);
				distance0 = distanceHistogram(keyHistogram, calcHistogram.getHistogram());
				if( updateHistogram ) System.arraycopy(calcHistogram.getHistogram(),0,histogram0,0,histogram0.length);
			}
			updateLocation(image,region2);
			distance2 = distanceHistogram(keyHistogram, calcHistogram.getHistogram());
			if( updateHistogram ) System.arraycopy(calcHistogram.getHistogram(),0,histogram2,0,histogram2.length);
		}
		// update the no scale change hypothesis
		updateLocation(image,region1);
		if( !constantScale ) {
			distance1 = distanceHistogram(keyHistogram, calcHistogram.getHistogram());
		} else {
			// force it to select
			distance1 = 0;
		}
		if( updateHistogram ) System.arraycopy(calcHistogram.getHistogram(),0,histogram1,0,histogram1.length);

		RectangleRotate_F32 selected = null;
		float selectedHist[] = null;
		switch( selectBest(distance0,distance1,distance2)) {
			case 0: selected = region0; selectedHist = histogram0; break;
			case 1: selected = region1; selectedHist = histogram1; break;
			case 2: selected = region2; selectedHist = histogram2; break;
			default: throw new RuntimeException("Bug in selectBest");
		}

		// Set region to the best scale, but reduce sensitivity by weighting it against the original size
		// equation 14
		float w = selected.width*(1-gamma) + gamma*region.width;
		float h = selected.height*(1-gamma) + gamma*region.height;

		region.set(selected);
		region.width = w;
		region.height = h;

		if( updateHistogram ) {
			System.arraycopy(selectedHist,0,keyHistogram,0,keyHistogram.length);
		}
	}

	/**
	 * Given the 3 scores return the index of the best
	 */
	private int selectBest( double a , double b , double c ) {
		if( a < b ) {
			if( a < c )
				return 0;
			else
				return 2;
		} else if( b <= c ) {
			return 1;
		} else {
			return 2;
		}
	}

	/**
	 * Updates the region's location using the standard mean-shift algorithm
	 */
	protected void updateLocation( T image , RectangleRotate_F32 region ) {

		double bestHistScore = Double.MAX_VALUE;
		float bestX = -1, bestY = -1;

		for( int i = 0; i < maxIterations; i++ ) {
			calcHistogram.computeHistogram(image,region);

			float histogram[] = calcHistogram.getHistogram();
			updateWeights(histogram);

			// the histogram fit doesn't always improve with each mean-shift iteration
			// save the best one and use it later on
			double histScore = distanceHistogram(keyHistogram, histogram);
			if( histScore < bestHistScore ) {
				bestHistScore = histScore;
				bestX = region.cx;
				bestY = region.cy;
			}

			List<Point2D_F32> samples = calcHistogram.getSamplePts();
			int sampleHistIndex[] = calcHistogram.getSampleHistIndex();

			// Compute equation 13
			float meanX = 0;
			float meanY = 0;
			float totalWeight = 0;
			for( int j = 0; j < samples.size(); j++ ) {
				Point2D_F32 samplePt = samples.get(j);

				int histIndex = sampleHistIndex[j];

				if( histIndex < 0 )
					continue;

				// compute the weight derived from the Bhattacharyya coefficient.  Equation 10.
				float w = weightHistogram[histIndex];

				meanX += w*samplePt.x;
				meanY += w*samplePt.y;
				totalWeight += w;
			}
			meanX /= totalWeight;
			meanY /= totalWeight;

			// convert to image pixels
			calcHistogram.squareToImageSample(meanX, meanY, region);
			meanX = calcHistogram.imageX;
			meanY = calcHistogram.imageY;

			// see if the change is below the threshold
			boolean done = Math.abs(meanX-region.cx ) <= minimumChange && Math.abs(meanY-region.cy ) <= minimumChange;
			region.cx = meanX;
			region.cy = meanY;

			if( done ) {
				break;
			}
		}

		// use the best location found
		region.cx = bestX;
		region.cy = bestY;
	}

	/**
	 * Update the weights for each element in the histogram.  Weights are used to favor colors which are
	 * less than expected.
	 */
	private void updateWeights(float[] histogram) {
		for( int j = 0; j < weightHistogram.length; j++ ) {
			float h = histogram[j];
			if( h != 0 ) {
				weightHistogram[j] = (float)Math.sqrt(keyHistogram[j]/h);
			}
		}
	}

	/**
	 * Computes the difference between two histograms using SAD.
	 *
	 * This is a change from the paper, which uses Bhattacharyya.  Bhattacharyya could give poor performance
	 * even with perfect data since two errors can cancel each other out.  For example, part of the histogram
	 * is too small and another part is too large.
	 */
	protected double distanceHistogram(float histogramA[], float histogramB[]) {
		double sumP = 0;
		for( int i = 0; i < histogramA.length; i++ ) {
			float q = histogramA[i];
			float p = histogramB[i];
			sumP += Math.abs(q-p);
		}
		return sumP;
	}

	public RectangleRotate_F32 getRegion() {
		return region;
	}
}
