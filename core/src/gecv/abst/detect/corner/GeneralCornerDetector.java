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

import gecv.abst.detect.extract.CornerExtractor;
import gecv.alg.detect.extract.SelectNBestCorners;
import gecv.struct.QueueCorner;
import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageFloat32;

/**
 * Base class for extracting corners. Can return all the found corners or just the corners with the highest
 * intensity.
 *
 * @author Peter Abeles
 */
// todo exclude previously found features
public class GeneralCornerDetector<I extends ImageBase, D extends ImageBase > {

	// selects the features with the largest intensity
	protected SelectNBestCorners selectBest;

	// extracts corners from the intensity image
	protected CornerExtractor extractor;

	// list of corners found by the extractor
	protected QueueCorner foundCorners;

	// optional: number of corners it should try to find
	protected int requestedFeatureNumber;

	// computes the corner intensity image
	protected GeneralCornerIntensity<I,D> intensity;

	/**
	 * @param intensity Computes how much like the feature the region around each pixel is.
	 * @param extractor   Extracts the corners from intensity image
	 * @param maxFeatures If not zero then only the best features are returned up to this number
	 */
	public GeneralCornerDetector(GeneralCornerIntensity<I,D> intensity , CornerExtractor extractor, int maxFeatures) {
		if( extractor.getUsesCandidates() && !intensity.hasCandidates() )
			throw new IllegalArgumentException("The extractor requires candidate features, which the intensity does not provide.");

		this.intensity = intensity;
		this.extractor = extractor;
		this.foundCorners = new QueueCorner(maxFeatures);
		if (maxFeatures > 0) {
			selectBest = new SelectNBestCorners(maxFeatures);
		}
	}

	/**
	 * Computes corners from image gradients.
	 *
	 * @param derivX image derivative in along the x-axis.
	 * @param derivY image derivative in along the y-axis.
	 */
	public void process(I image , D derivX, D derivY, D derivXX, D derivYY , D derivXY ) {
		intensity.process(image,derivX, derivY, derivXX, derivYY, derivXY );
		foundCorners.reset();
		if( intensity.hasCandidates() ) {
			extractor.process(intensity.getIntensity(), intensity.getCandidates(), requestedFeatureNumber, foundCorners);
		} else {
			extractor.process(intensity.getIntensity(), null, requestedFeatureNumber, foundCorners);
		}
		if (selectBest != null) {
			selectBest.process(intensity.getIntensity(), foundCorners);
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

	public QueueCorner getCorners() {
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
}
