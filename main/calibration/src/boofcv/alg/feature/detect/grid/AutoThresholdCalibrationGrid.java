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

package boofcv.alg.feature.detect.grid;

import boofcv.alg.feature.detect.quadblob.QuadBlob;
import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.ImageRectangle;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.point.Point2D_I32;

import java.util.List;

/**
 * Automatically selects a threshold for detecting calibration targets. The initial threshold is found
 * by doing a binary search through the possible thresholds until it finds a valid target.  Once a
 * valid target has been found it computes the statistics of pixels around the corners.  Then a threshold
 * is selected based on the mean value of white and dark regions.
 *
 * @author Peter Abeles
 */
public class AutoThresholdCalibrationGrid {

	// Initial threshold specified by the user
	private double initialThreshold;
	// The refined threshold
	private double refinedThreshold;

	// binary image computed from the threshold
	private ImageUInt8 binary = new ImageUInt8(1,1);

	// pixel values around corners
	private IntensityHistogram histHighRes = new IntensityHistogram(256,256);
	private IntensityHistogram histLowRes = new IntensityHistogram(20,256);

	private HistogramTwoPeaks peaks = new HistogramTwoPeaks(2);

	// computes statistics of white and black sections
	private FitGaussianPrune low = new FitGaussianPrune(20,3,5);
	private FitGaussianPrune high = new FitGaussianPrune(20,3,5);

	// storage for image histogram
	private int histogram[] = new int[256];

	/**
	 * Configures auto threshold.
	 *
	 * @param initialThreshold Threshold used for computing binary image. If < 0 then mean intensity is used.
	 */
	public AutoThresholdCalibrationGrid(double initialThreshold)
	{
		this.initialThreshold = initialThreshold;
	}

	/**
	 * Processes the image and automatically detects calibration grid.  If successful then
	 * true is returned and the found target is contained in the target detector.
	 *
	 * @param detector Target detection algorithm.
	 * @param gray Gray scale image which is being thresholded
	 * @return true if a threshold was successfully found and target detected.
	 */
	public boolean process( DetectSquareCalibrationPoints detector , ImageFloat32 gray ) {

		binary.reshape(gray.width,gray.height);

		double threshold = initialThreshold;
		if( threshold < 0 )
			threshold = UtilCalibrationGrid.selectThreshold(gray,histogram);

		GThresholdImageOps.threshold(gray,binary,threshold,true);

		// see if the target was detected
		if( detector.process(binary) ) {
			refinedThreshold = refineThreshold(detector.getInterestSquares(),gray);
			GThresholdImageOps.threshold(gray,binary,threshold,true);
			if( !detector.process(binary) ) {
				throw new RuntimeException("Crap new threshold doesn't work!");
			}
			return true;
		}

		return false;
	}

	/**
	 * Optimal target threshold
	 *
	 * @return threshold
	 */
	public double getThreshold() {
		return refinedThreshold;
	}

	/**
	 * Samples pixels around corners, splits pixels into two groups of white and dark values, computes
	 * means and threshold from mean
	 *
	 * @return New threshold
	 */
	private double refineThreshold( List<QuadBlob> blobs , ImageFloat32 gray ) {

		// create a list of pixel intensity values around all the corners
		histHighRes.reset();
		for( QuadBlob b : blobs ) {
			int r = (int)Math.ceil(b.smallestSide)/3;
			
			for( Point2D_I32 p : b.corners ) {
				ImageRectangle rect = new ImageRectangle(p.x-r,p.y-r,p.x+r+1,p.y+r+1);
				BoofMiscOps.boundRectangleInside(gray,rect);
				
				for( int y = rect.y0; y < rect.y1; y++ ) {
					for( int x = rect.x0; x < rect.x1; x++ ) {
						histHighRes.add(gray.get(x,y));
					}
				}
			}
		}

		// Find the high and low peaks using a histogram and compute the threshold
		histLowRes.reset();
		histLowRes.downSample(histHighRes);

		peaks.computePeaks(histLowRes);

		int indexThresh = (int)((peaks.peakLow+peaks.peakHigh)/2.0);

		low.process(histHighRes,0,indexThresh);
		high.process(histHighRes,indexThresh,255);

		return (low.getMean()+high.getMean())/2.0;
	}

	/**
	 * Binary image that target was detected inside of
	 */
	public ImageUInt8 getBinary() {
		return binary;
	}
}
