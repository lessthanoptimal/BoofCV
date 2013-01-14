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

package boofcv.alg.feature.detect.interest;

import boofcv.abst.feature.detect.extract.NonMaxSuppression;
import boofcv.abst.feature.detect.intensity.GeneralFeatureIntensity;
import boofcv.alg.feature.detect.extract.SelectNBestFeatures;
import boofcv.alg.misc.PixelMath;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_I16;

/**
 * <p>
 * Detects features which are local maximums and/or local minimums in the feature intensity image.
 * A list of pixels to exclude as candidates can be provided.  Image derivatives need to be computed
 * externally and provided as needed. The passed in {@link GeneralFeatureIntensity} is used to determine if local
 * maximums or minimums should be detected.
 * </p>
 *
 * @param <I> Input image type.
 * @param <D> Image derivative type.
 *
 * @author Peter Abeles
 */
public class GeneralFeatureDetector<I extends ImageSingleBand, D extends ImageSingleBand>
{
	// storage for inverted image that minimums are detected inside of
	protected ImageFloat32 inverted = new ImageFloat32(1,1);

	// list of feature locations found by the extractor
	protected QueueCorner foundMaximum = new QueueCorner(10);
	protected QueueCorner foundMinimum = new QueueCorner(10);

	// Corners which should be excluded.
	protected QueueCorner excludeMaximum;
	protected QueueCorner excludeMinimum;

	// selects the features with the largest intensity
	protected SelectNBestFeatures selectBest = new SelectNBestFeatures(10);
	// maximum number of features it will detect across the image
	protected int maxFeatures;

	// extracts corners from the intensity image
	protected NonMaxSuppression extractor;

	// Maximums in the feature intensity
	protected QueueCorner detected = new QueueCorner(10);

	// computes the feature intensity image
	protected GeneralFeatureIntensity<I, D> intensity;

	/**
	 * Specifies which algorithms to use and configures the detector.
	 *
	 * @param intensity Computes how much like the feature the region around each pixel is.
	 * @param extractor Extracts the corners from intensity image
	 */
	public GeneralFeatureDetector(GeneralFeatureIntensity<I, D> intensity,
								  NonMaxSuppression extractor ) {
		if (extractor.getUsesCandidates() && !intensity.hasCandidates())
			throw new IllegalArgumentException("The extractor requires candidate features, which the intensity does not provide.");

		this.intensity = intensity;
		this.extractor = extractor;

		// sanity check ignore borders and increase the size of the extractor's ignore border
		// if its ignore border is too small then false positive are highly likely
		if( intensity.getIgnoreBorder() > extractor.getIgnoreBorder() )
			extractor.setIgnoreBorder(intensity.getIgnoreBorder());
	}

	protected GeneralFeatureDetector() {
	}


	/**
	 * Computes point features from image gradients.
	 *
	 * @param image   Original image.
	 * @param derivX  image derivative in along the x-axis. Only needed if {@link #getRequiresGradient()} is true.
	 * @param derivY  image derivative in along the y-axis. Only needed if {@link #getRequiresGradient()} is true.
	 * @param derivXX Second derivative.  Only needed if {@link #getRequiresHessian()} ()} is true.
	 * @param derivXY Second derivative.  Only needed if {@link #getRequiresHessian()} ()} is true.
	 * @param derivYY Second derivative.  Only needed if {@link #getRequiresHessian()} ()} is true.
	 */
	public void process(I image, D derivX, D derivY, D derivXX, D derivYY, D derivXY) {
		intensity.process(image, derivX, derivY, derivXX, derivYY, derivXY);
		ImageFloat32 intensityImage = intensity.getIntensity();

		// detection minimums by inverting the original image and running non-maximum suppression
		if( intensity.localMinimums() ) {
			inverted.reshape(intensityImage.width,intensityImage.height);
			PixelMath.invert(intensityImage, inverted);
			foundMinimum.reset();
			extract(inverted, excludeMinimum, foundMinimum);
		}

		if( intensity.localMaximums() ) {
			foundMaximum.reset();
			extract(intensityImage, excludeMaximum, foundMaximum);
		}
	}

	/**
	 * Performs non-maximum detection on the provided intensity image.  If a maximum feature limit has been
	 * specified then the features are sorted by intensity values and only the most intense features are returned.
	 */
	protected void extract( ImageFloat32 intensityImage , QueueCorner exclude , QueueCorner found ) {

		int numSelect = -1;
		if( maxFeatures > 0 ) {
			numSelect = exclude == null ? maxFeatures : maxFeatures - exclude.size;

			// return without processing if there is no room to detect any more features
			if( numSelect <= 0 )
				return;
		}

		if( exclude != null ) {
			// mark pixels that should be excluded
			for( int i = 0; i < exclude.size; i++ ) {
				Point2D_I16 p = exclude.get(i);
				intensityImage.set(p.x,p.y,Float.MAX_VALUE);
			}
		}

		detected.reset();
		if (intensity.hasCandidates()) {
			extractor.process(intensityImage, null, intensity.getCandidates(),null, detected);
		} else {
			extractor.process(intensityImage, null, null,null,detected);
		}

		// optionally select the most intense features only
		QueueCorner q;
		if (numSelect > 0) {
			selectBest.setN(numSelect);
			selectBest.process(intensityImage, this.detected);
			q = selectBest.getBestCorners();
		} else {
			q = detected;
		}

		// save the found features
		for (int k = 0; k < q.size; k++) {
			Point2D_I16 p = q.get(k);
			found.grow().set(p.x , p.y );
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
	public void setThreshold(float threshold) {
		extractor.setThresholdMaximum(threshold);
	}

	/**
	 * Returns the current feature extraction threshold.
	 *
	 * @return feature extraction threshold.
	 */
	public float getThreshold() {
		return extractor.getThresholdMaximum();
	}

	/**
	 * Specify points which are excluded when detecting maximums
	 *
	 * @param exclude List of points being excluded
	 */
	public void setExcludeMaximum(QueueCorner exclude) {
		this.excludeMaximum = exclude;
	}

	/**
	 * Returns a list of all the found maximums.
	 * @return found point features
	 */
	public QueueCorner getMaximums() {
		return foundMaximum;
	}

	/**
	 * Specify points which are excluded when detecting maximums
	 *
	 * @param exclude List of points being excluded
	 */
	public void setExcludeMinimum(QueueCorner exclude) {
		this.excludeMinimum = exclude;
	}

	/**
	 * Returns a list of all the found maximums.
	 * @return found point features
	 */
	public QueueCorner getMinimums() {
		return foundMinimum;
	}

	/**
	 * true if it will detector local minimums
	 */
	public boolean isDetectMinimums() {
		return intensity.localMinimums();
	}

	/**
	 * true if it will detector local maximums
	 */
	public boolean isDetectMaximums() {
		return intensity.localMaximums();
	}
}
