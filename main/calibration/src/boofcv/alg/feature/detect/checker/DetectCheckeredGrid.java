/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.feature.detect.checker;

import boofcv.abst.feature.detect.extract.FeatureExtractor;
import boofcv.abst.feature.detect.extract.GeneralFeatureDetector;
import boofcv.abst.feature.detect.intensity.GeneralFeatureIntensity;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.BorderType;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.factory.feature.detect.intensity.FactoryIntensityPoint;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageSingleBand;

/**
 * @author Peter Abeles
 */
public class DetectCheckeredGrid
		<T extends ImageSingleBand, D extends ImageSingleBand>
{
	D derivX;
	D derivY;

	int numCols;
	int numRows;

	GeneralFeatureIntensity<T,D> intensityAlg;
	GeneralFeatureDetector<T,D> detectorAlg;

	public DetectCheckeredGrid( int numCols , int numRows , int radius , Class<T> imageType ) {
		Class<D> derivType = GImageDerivativeOps.getDerivativeType(imageType);
		this.numCols = numCols;
		this.numRows = numRows;

		derivX = GeneralizedImageOps.createSingleBand(derivType,1,1);
		derivY = GeneralizedImageOps.createSingleBand(derivType,1,1);

		intensityAlg = FactoryIntensityPoint.harris(radius,0.04f,true,derivType);

		FeatureExtractor extractor = FactoryFeatureExtractor.nonmax(radius,0,radius,false,true);
		detectorAlg = new GeneralFeatureDetector<T, D>(intensityAlg,extractor,numCols*numRows*2);

	}

	public boolean process( T gray ) {
		derivX.reshape(gray.width,gray.height);
		derivY.reshape(gray.width,gray.height);

		// extended border to avoid false positives
		GImageDerivativeOps.sobel(gray, derivX, derivY, BorderType.EXTENDED);

		// detect interest points
		detectorAlg.process(gray,derivX,derivY, null,null,null);

		QueueCorner points = detectorAlg.getFeatures();

		// find a valid grid

		// refine the position estimate

		return false;
	}
}
