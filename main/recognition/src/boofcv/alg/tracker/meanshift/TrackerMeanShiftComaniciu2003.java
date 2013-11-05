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

package boofcv.alg.tracker.meanshift;

import boofcv.struct.RectangleRotate_F32;
import boofcv.struct.image.ImageMultiBand;
import georegression.struct.point.Point2D_F32;

import java.util.List;

/**
 * <p>
 * Mean shift tracker which adjusts the scale (or bandwidth) to account for changes in scale of the target [1].
 * The mean-shift region is sampled using an oriented rectangle and weighted using a 2D gaussian.  The target
 * is modeled using a color histogram of the input image, which can be optionally updated after each frame
 * is processed.  The scale is selected by scoring different sized regions using the Bhattacharyya coefficient
 * </p>
 *
 * <p>
 * [1] Meer, Peter. "Kernel-Based Object Tracking." IEEE Transactions on Pattern Analysis and Machine Intelligence
 * 25.4 (2003): 1.
 * </p>
 * @author Peter Abeles
 */
public class TrackerMeanShiftComaniciu2003<T extends ImageMultiBand> {

	// computes the histogram inside a rotated rectangle
	private LocalWeightedHistogramRotRect<T> calcHistogram;
	// the key-frame histogram which is being compared again
	protected float keyHistogram[];

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

	/**
	 * Configures tracker.
	 *
	 * @param updateHistogram If true the histogram will be updated using the most recent image. Try true.
	 * @param maxIterations Maximum number of mean-shift iterations.  Try 30
	 * @param minimumChange Mean-shift will stop when the change is below this threshold.  Try 1e-4f
	 * @param gamma Scale weighting factor.  Value from 0 to 1. Closer to 0 the more it will prefer
	 *              the most recent estimate.  Try 0.1
	 * @param constantScale If true it will assume the scale is known.  If false it will estimate the change in scale.
	 * @param calcHistogram Calculates the histogram
	 */
	public TrackerMeanShiftComaniciu2003(boolean updateHistogram,
										 int maxIterations,
										 float minimumChange,
										 float gamma ,
										 boolean constantScale,
										 LocalWeightedHistogramRotRect<T> calcHistogram) {
		this.updateHistogram = updateHistogram;
		this.maxIterations = maxIterations;
		this.minimumChange = minimumChange;
		this.gamma = gamma;
		this.constantScale = constantScale;
		this.calcHistogram = calcHistogram;

		keyHistogram = new float[ calcHistogram.getHistogram().length ];
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

		region0.width  *= 0.9;
		region0.height *= 0.9;

		region2.width  *= 1.1;
		region2.height *= 1.1;

		// distance from histogram
		double distance0=1,distance1,distance2=1;

		// perform mean-shift at the different sizes and compute their distance
		if( !constantScale ) {
			updateLocation(image,region0);
			distance0 = distanceHistogram(keyHistogram,calcHistogram.getHistogram());
			if( updateHistogram ) System.arraycopy(calcHistogram.getHistogram(),0,histogram0,0,histogram0.length);
			updateLocation(image,region2);
			distance2 = distanceHistogram(keyHistogram,calcHistogram.getHistogram());
			if( updateHistogram ) System.arraycopy(calcHistogram.getHistogram(),0,histogram2,0,histogram2.length);
		}
		// update the no scale change hypothesis
		updateLocation(image,region1);
		if( !constantScale ) {
			distance1 = distanceHistogram(keyHistogram,calcHistogram.getHistogram());
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

		for( int i = 0; i < maxIterations; i++ ) {
			calcHistogram.computeHistogram(image,region);

			List<Point2D_F32> samples = calcHistogram.getSamplePts();
			int sampleHistIndex[] = calcHistogram.getSampleHistIndex();
			float histogram[] = calcHistogram.getHistogram();

			// Compute equation 13
			float meanX = 0;
			float meanY = 0;
			float totalWeight = 0;
			for( int j = 0; j < samples.size(); j++ ) {
				Point2D_F32 samplePt = samples.get(j);

				int histIndex = sampleHistIndex[j];

				if( histIndex < 0 )
					continue;

				float q = keyHistogram[histIndex];
				float p = histogram[histIndex];

				// compute the weight derived from the Bhattacharyya coefficient.  Equation 10.
				float w = (float)Math.sqrt(q/p);

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
	}

	/**
	 * Compute the distance between the two distributions using Bhattacharyya coefficient
	 * Equations 6 and 7.
	 * Must be called immediately after {@link #updateLocation}.
	 */
	protected double distanceHistogram( float histogramA[] , float histogramB[] ) {
		double sumP = 0;
		for( int i = 0; i < histogramA.length; i++ ) {
			float q = histogramA[i];
			float p = histogramB[i];
			sumP += Math.sqrt(q*p);
		}
		// will get same solution without sqrt() and less hassle
		return 1-sumP;
//		if( sumP > 1)
//			return 0;
//		return Math.sqrt(1-sumP);
	}

	public RectangleRotate_F32 getRegion() {
		return region;
	}
}
