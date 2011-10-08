/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
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

package boofcv.factory.feature.detect.line;


import boofcv.abst.feature.detect.line.DetectLineHoughFoot;
import boofcv.abst.feature.detect.line.DetectLineHoughFootSubimage;
import boofcv.abst.feature.detect.line.DetectLineHoughPolar;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.struct.image.ImageBase;

/**
 * Factory for creating line and line segment detectors.
 *
 * @author Peter Abeles
 */
public class FactoryDetectLine {

	public static <I extends ImageBase, D extends ImageBase>
	DetectLineHoughFoot<I,D> houghFoot(int localMaxRadius,
									   int minCounts ,
									   int minDistanceFromOrigin ,
									   float thresholdEdge ,
									   Class<I> imageType ,
									   Class<D> derivType ) {

		ImageGradient<I,D> gradient = FactoryDerivative.sobel(imageType,derivType);

		return new DetectLineHoughFoot<I,D>(localMaxRadius,minCounts,minDistanceFromOrigin,thresholdEdge,gradient);
	}

	public static <I extends ImageBase, D extends ImageBase>
	DetectLineHoughFootSubimage<I,D> houghFootSub(int localMaxRadius,
									   int minCounts ,
									   int minDistanceFromOrigin ,
									   float thresholdEdge ,
									   int totalHorizontalDivisions ,
									   int totalVerticalDivisions ,
									   Class<I> imageType ,
									   Class<D> derivType ) {

		ImageGradient<I,D> gradient = FactoryDerivative.sobel(imageType,derivType);

		return new DetectLineHoughFootSubimage<I,D>(localMaxRadius,
				minCounts,minDistanceFromOrigin,thresholdEdge,
				totalHorizontalDivisions,totalVerticalDivisions,gradient);
	}

	public static <I extends ImageBase, D extends ImageBase>
	DetectLineHoughPolar<I,D> houghPolar(int localMaxRadius,
										 int minCounts,
										 int numBinsRange ,
										 int numBinsAngle ,
										 float thresholdEdge,
										 Class<I> imageType ,
										 Class<D> derivType ) {

		ImageGradient<I,D> gradient = FactoryDerivative.sobel(imageType,derivType);

		return new DetectLineHoughPolar<I,D>(localMaxRadius,minCounts,numBinsRange,numBinsAngle,thresholdEdge,gradient);
	}

}
