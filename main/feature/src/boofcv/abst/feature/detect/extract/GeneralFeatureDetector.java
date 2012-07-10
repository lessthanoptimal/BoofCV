/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.detect.extract;

import boofcv.abst.feature.detect.intensity.GeneralFeatureIntensity;
import boofcv.alg.feature.detect.extract.SelectNBestFeatures;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_I16;

import java.util.Arrays;

/**
 * <p>
 * Generic class for extracting point features of different types. Can return all the found features or
 * just the features with the highest intensity.  The main advantage of this class over
 * {@link boofcv.abst.feature.detect.interest.InterestPointDetector} is that it allows image derivatives
 * to be passed in, allowing for tighter integration of algorithms.
 * </p>
 *
 * @author Peter Abeles
 */
public class GeneralFeatureDetector<I extends ImageSingleBand, D extends ImageSingleBand> {

	// selects the features with the largest intensity
	private SelectNBestFeatures selectBest = new SelectNBestFeatures(10);
	// maximum number of features it will detect across the image
	protected int maxFeatures;

	// extracts corners from the intensity image
	protected FeatureExtractor extractor;

	// found corners in each region
	protected QueueCorner regionCorners = new QueueCorner(10);
	
	// list of corners found by the extractor
	protected QueueCorner foundCorners = new QueueCorner(10);

	// Corners which should be excluded
	protected QueueCorner excludedCorners;

	// optional: number of corners it should try to find
	protected int requestedFeatureNumber;

	// computes the corner intensity image
	protected GeneralFeatureIntensity<I,D> intensity;

	// description of sub-regions
	int numColumns=1; 
	int numRows=1;

	// number of excluded features in each region
	int regionCount[] = new int[1];
	
	/**
	 * @param intensity Computes how much like the feature the region around each pixel is.
	 * @param extractor   Extracts the corners from intensity image
	 */
	public GeneralFeatureDetector(GeneralFeatureIntensity<I, D> intensity,
								  FeatureExtractor extractor )
	{
		if( extractor.getUsesCandidates() && !intensity.hasCandidates() )
			throw new IllegalArgumentException("The extractor requires candidate features, which the intensity does not provide.");

		this.intensity = intensity;
		this.extractor = extractor;
	}

	/**
	 * Specifies which corners should be excluded and not detected a second time
	 *
	 * @param excludedCorners List of corners being excluded
	 */
	public void setExcludedCorners(QueueCorner excludedCorners) {
		this.excludedCorners = excludedCorners;
	}

	/**
	 * Number of sub-regions that features are independently extracted in.  The image is divided
	 * into rectangular segments along the horizontal and vertical direction.
	 * 
	 * @param numColumns Number of horizontal divisions.
	 * @param numRows Number of vertical divisions
	 */
	public void setRegions( int numColumns , int numRows ) {
		this.numColumns = numColumns;
		this.numRows = numRows;
		regionCount = new int[numColumns*numRows];
	}
	
	/**
	 * Computes corners from image gradients.
	 *
	 * @param image Original image.
	 * @param derivX image derivative in along the x-axis. Only needed if {@link #getRequiresGradient()} is true.
	 * @param derivY image derivative in along the y-axis. Only needed if {@link #getRequiresGradient()} is true.
	 * @param derivXX Second derivative.  Only needed if {@link #getRequiresHessian()} ()} is true.
	 * @param derivXY Second derivative.  Only needed if {@link #getRequiresHessian()} ()} is true.
	 * @param derivYY Second derivative.  Only needed if {@link #getRequiresHessian()} ()} is true.
	 */
	public void process(I image , D derivX, D derivY, D derivXX, D derivYY , D derivXY ) {
		intensity.process(image,derivX, derivY, derivXX, derivYY, derivXY );
		ImageFloat32 intensityImage = intensity.getIntensity();

		int regionWidth = image.width/numColumns + (image.width%numColumns);
		int regionHeight = image.height/numRows + (image.height%numRows);

		if( intensity.hasCandidates() && numColumns*numRows != 1 )
			throw new RuntimeException("Candidates with subregions is not yet supported");
		
		// mark features which are in the excluded list so that they are not returned again
		if( excludedCorners != null ) {
			Arrays.fill(regionCount,0);
			for (int i = 0; i < excludedCorners.size; i++) {
				Point2D_I16 pt = excludedCorners.get(i);
				intensityImage.set(pt.x, pt.y, Float.MAX_VALUE);

				int c=pt.x/regionWidth,r=pt.y/regionHeight;
				regionCount[r*numColumns+c]++;
			}
		}

		extractFromRegions(intensityImage, regionWidth, regionHeight);
	}

	/**
	 * Extracts corners from each region individually
	 */
	private void extractFromRegions(ImageFloat32 intensityImage, int regionWidth, int regionHeight) {
		// compute the number requested per region
		int regionRequest = requestedFeatureNumber/(numColumns*numRows);
		int regionMax = maxFeatures/(numColumns*numRows);

		int ignoreBorder = intensity.getIgnoreBorder();

		foundCorners.reset();
		for( int i = 0; i < numRows; i++ ) {
			int y0 = i*regionHeight;
			int y1 = Math.min(intensityImage.height,y0+regionHeight);

			// remove the ignore border from the region being processed
			if( i == 0 ) y0 += ignoreBorder;
			if( i == numRows-1 ) y1 -= ignoreBorder;

			for( int j = 0; j < numColumns; j++ ) {
				int x0 = j*regionWidth;
				int x1 = Math.min(intensityImage.width,x0+regionWidth);
				if( j == 0 ) x0 += ignoreBorder;
				if( j == numColumns-1 ) x1 -= ignoreBorder;

				// extract features from inside the sub-image in question
				ImageFloat32 intenSub = intensityImage.subimage(x0,y0,x1,y1);

				regionCorners.reset();
				if( intensity.hasCandidates() ) {
					extractor.process(intenSub, intensity.getCandidates(), regionRequest, regionCorners);
				} else {
					extractor.process(intenSub, null, regionRequest, regionCorners);
				}

				QueueCorner q;
				if (maxFeatures > 0 ) {
					int numSelect = regionMax-regionCount[i*numColumns+j];
//					System.out.println("Region "+i+" "+j+"  target select "+numSelect);
					if( numSelect > 0 ) {
						selectBest.setN(numSelect);
						selectBest.process(intensityImage, regionCorners);
						q = selectBest.getBestCorners();
					} else {
						continue;
					}
				} else {
					q = regionCorners;
				}

				for( int k = 0; k < q.size; k++ ) {
					Point2D_I16 p = q.get(k);
					foundCorners.pop().set(p.x+x0,p.y+y0);
				}
			}
		}
	}

	/**
	 * Turns on select best features and sets the number it should return.  If a list of excluded features
	 * is passed in, then the maximum number of returned features is 'numFeatures' minus the number of
	 * excluded features.
	 *
	 * @param numFeatures Return at most this many features, which are the best.
	 */
	public void setMaxFeatures(int numFeatures) {
		this.maxFeatures = numFeatures;
	}

	/**
	 * <p>
	 * Specifies how many corners should be returned.
	 * </p>
	 * <p/>
	 * <p>
	 * If the provided corner extractor does not support this feature then an exception is thrown.
	 * </p>
	 *
	 * @param requestedFeatureNumber The number of corners it should return.
	 */
	public void setRequestedFeatureNumber(int requestedFeatureNumber) {
		if (!extractor.getAcceptRequest())
			throw new IllegalArgumentException("The provided corner extractor does not accept requests for the number of detected features.");

		this.requestedFeatureNumber = requestedFeatureNumber;
	}

	public QueueCorner getFeatures() {
		return foundCorners;
	}

	/**
	 * If the image gradient is required for calculations.
	 *
	 * @return true if the image gradient is required.
	 */
	public boolean getRequiresGradient() {
		return intensity.getRequiresGradient();
	}

	/**
	 * If the image Hessian is required for calculations.
	 *
	 * @return true if the image Hessian is required.
	 */
	public boolean getRequiresHessian() {
		return intensity.getRequiresHessian();
	}

	public ImageFloat32 getIntensity() {
		return intensity.getIntensity();
	}

	/**
	 * Changes feature extraction threshold.
	 *
	 * @param threshold The new feature extraction threshold.
	 */
	public void setThreshold( float threshold ) {
		extractor.setThreshold(threshold);
	}

	/**
	 * Returns the current feature extraction threshold.
	 * @return feature extraction threshold.
	 */
	public float getThreshold() {
		return extractor.getThreshold();
	}
}
