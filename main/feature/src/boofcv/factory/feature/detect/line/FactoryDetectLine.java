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
import boofcv.abst.feature.detect.line.DetectLineSegmentsGridRansac;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.feature.detect.line.ConnectLinesGrid;
import boofcv.alg.feature.detect.line.GridRansacLineDetector;
import boofcv.alg.feature.detect.line.gridline.Edgel;
import boofcv.alg.feature.detect.line.gridline.GridLineModelDistance;
import boofcv.alg.feature.detect.line.gridline.GridLineModelFitter;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.numerics.fitting.modelset.ModelMatcher;
import boofcv.numerics.fitting.modelset.ransac.SimpleInlierRansac;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.line.LinePolar2D_F32;

/**
 * Factory for creating line and line segment detectors.
 *
 * @author Peter Abeles
 */
public class FactoryDetectLine {

	/**
	 *
	 * @param regionSize
	 * @param thresholdEdge
	 * @param thresholdAngle Tolerance in angle for allowing two edgels to be paired up, in radians.  Try 2.36
	 * @param connectLines Should lines be connected and optimized.
	 * @param imageType
	 * @param derivType
	 * @param <I>
	 * @param <D>
	 * @return
	 */
	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	DetectLineSegmentsGridRansac<I,D> lineRansac(int regionSize ,
												 double thresholdEdge ,
												 double thresholdAngle ,
												 boolean connectLines,
												 Class<I> imageType ,
												 Class<D> derivType ) {

		ImageGradient<I,D> gradient = FactoryDerivative.sobel(imageType,derivType);

		GridLineModelDistance distance = new GridLineModelDistance((float)thresholdAngle);
		GridLineModelFitter fitter = new GridLineModelFitter((float)thresholdAngle);

		ModelMatcher<LinePolar2D_F32, Edgel> matcher =
				new SimpleInlierRansac<LinePolar2D_F32,Edgel>(123123,fitter,distance,25,2,2*regionSize/3,1000,1);

		GridRansacLineDetector alg = new GridRansacLineDetector(regionSize,10,matcher);

		ConnectLinesGrid connect = null;
		if( connectLines )
			connect = new ConnectLinesGrid(Math.PI*0.01,1,8);

		return new DetectLineSegmentsGridRansac<I,D>(alg,connect,gradient,thresholdEdge,imageType,derivType);
	}

	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	DetectLineHoughFoot<I,D> houghFoot(int localMaxRadius,
									   int minCounts ,
									   int minDistanceFromOrigin ,
									   float thresholdEdge ,
									   int maxLines ,
									   Class<I> imageType ,
									   Class<D> derivType ) {

		ImageGradient<I,D> gradient = FactoryDerivative.sobel(imageType,derivType);

		return new DetectLineHoughFoot<I,D>(localMaxRadius,minCounts,minDistanceFromOrigin,thresholdEdge,maxLines,gradient);
	}

	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	DetectLineHoughFootSubimage<I,D> houghFootSub(int localMaxRadius,
									   int minCounts ,
									   int minDistanceFromOrigin ,
									   float thresholdEdge ,
									   int maxLines ,
									   int totalHorizontalDivisions ,
									   int totalVerticalDivisions ,
									   Class<I> imageType ,
									   Class<D> derivType ) {

		ImageGradient<I,D> gradient = FactoryDerivative.sobel(imageType,derivType);

		return new DetectLineHoughFootSubimage<I,D>(localMaxRadius,
				minCounts,minDistanceFromOrigin,thresholdEdge,
				totalHorizontalDivisions,totalVerticalDivisions,maxLines,gradient);
	}

	/**
	 * Creates a Hough line detector based on polar paramterization.
	 *
	 * @param localMaxRadius Radius for local maximum suppression.  Try 2.
	 * @param minCounts Minimum number of counts for detected line.  Critical tuning parameter and image dependent.
	 * @param resolutionRange Resolution of line range in pixels.  Try 2
	 * @param resolutionAngle Resolution of line angle in radius.  Try PI/180
	 * @param thresholdEdge Edge detection threshold. Try 50.
	 * @param maxLines Maximum number of lines to return. If <= 0 it will return them all.
	 * @param <I> Input image type.
	 * @param <D> Image derivative type.
	 * @return Line detector.
	 */
	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	DetectLineHoughPolar<I,D> houghPolar(int localMaxRadius,
										 int minCounts,
										 double resolutionRange ,
										 double resolutionAngle ,
										 float thresholdEdge,
										 int maxLines ,
										 Class<I> imageType ,
										 Class<D> derivType ) {

		ImageGradient<I,D> gradient = FactoryDerivative.sobel(imageType,derivType);

		return new DetectLineHoughPolar<I,D>(localMaxRadius,minCounts,resolutionRange,resolutionAngle,thresholdEdge,maxLines,gradient);
	}

}
