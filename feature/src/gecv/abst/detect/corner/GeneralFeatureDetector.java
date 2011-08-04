/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.abst.detect.corner;

import gecv.abst.detect.extract.FeatureExtractor;
import gecv.alg.detect.extract.SelectNBestCorners;
import gecv.struct.QueueCorner;
import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageFloat32;

/**
 * Generic class for extracting corners of different types. Can return all the found corners or just the corners with the highest
 * intensity.
 *
 * @author Peter Abeles
 */
public class GeneralFeatureDetector<I extends ImageBase, D extends ImageBase > {

	// selects the features with the largest intensity
	protected SelectNBestCorners selectBest;

	// extracts corners from the intensity image
	protected FeatureExtractor extractor;

	// list of corners found by the extractor
	protected QueueCorner foundCorners;

	// Corners which should be excluded
	protected QueueCorner excludedCorners;

	// optional: number of corners it should try to find
	protected int requestedFeatureNumber;

	// computes the corner intensity image
	protected GeneralFeatureIntensity<I,D> intensity;

	/**
	 * @param intensity Computes how much like the feature the region around each pixel is.
	 * @param extractor   Extracts the corners from intensity image
	 * @param maxFeatures If not zero then only the best features are returned up to this number
	 */
	public GeneralFeatureDetector(GeneralFeatureIntensity<I,D> intensity ,
								 FeatureExtractor extractor,
								 int maxFeatures)
	{
		if( extractor.getUsesCandidates() && !intensity.hasCandidates() )
			throw new IllegalArgumentException("The extractor requires candidate features, which the intensity does not provide.");

		this.intensity = intensity;
		this.extractor = extractor;
		this.foundCorners = new QueueCorner(maxFeatures);
		if (maxFeatures > 0) {
			selectBest = new SelectNBestCorners(maxFeatures);
		}
	}

	public void setExcludedCorners(QueueCorner excludedCorners) {
		this.excludedCorners = excludedCorners;
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
		foundCorners.reset();
		if( intensity.hasCandidates() ) {
			extractor.process(intensity.getIntensity(), intensity.getCandidates(), requestedFeatureNumber,
					excludedCorners,foundCorners);
		} else {
			extractor.process(intensity.getIntensity(), null, requestedFeatureNumber, 
					excludedCorners,foundCorners);
		}
		if (selectBest != null) {
			selectBest.process(intensity.getIntensity(), foundCorners);
		}
	}

	/**
	 * Turns on select best features and sets the number it should return.
	 *
	 * @param numFeatures Return at most this many features, which are the best.
	 */
	public void setBestNumber(int numFeatures) {
		if( selectBest == null ) {
			selectBest = new SelectNBestCorners(numFeatures);
		} else {
			selectBest.setN(numFeatures);
		}
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
		if (selectBest != null) {
			return selectBest.getBestCorners();
		} else
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
