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

package boofcv.factory.feature.detect.line;


import boofcv.abst.feature.detect.line.DetectLineHoughFoot;
import boofcv.abst.feature.detect.line.DetectLineHoughFootSubimage;
import boofcv.abst.feature.detect.line.DetectLineHoughPolar;
import boofcv.abst.feature.detect.line.DetectLineSegmentsGridRansac;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.feature.detect.line.ConnectLinesGrid;
import boofcv.alg.feature.detect.line.GridRansacLineDetector;
import boofcv.alg.feature.detect.line.gridline.*;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt16;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.line.LinePolar2D_F32;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.fitting.modelset.ransac.Ransac;

/**
 * Factory for creating line and line segment detectors.
 *
 * @author Peter Abeles
 */
public class FactoryDetectLineAlgs {

	/**
	 * Detects line segments inside an image using the {@link DetectLineSegmentsGridRansac} algorithm.
	 *
	 * @see DetectLineSegmentsGridRansac
	 *
	 * @param regionSize Size of the region considered.  Try 40 and tune.
	 * @param thresholdEdge Threshold for determining which pixels belong to an edge or not. Try 30 and tune.
	 * @param thresholdAngle Tolerance in angle for allowing two edgels to be paired up, in radians.  Try 2.36
	 * @param connectLines Should lines be connected and optimized.
	 * @param imageType Type of single band input image.
	 * @param derivType Image derivative type.
	 * @return Line segment detector
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
				new Ransac<LinePolar2D_F32,Edgel>(123123,fitter,distance,25,1);

		GridRansacLineDetector<D> alg;
		if( derivType == ImageFloat32.class )  {
			alg = (GridRansacLineDetector)new ImplGridRansacLineDetector_F32(regionSize,10,matcher);
		} else if( derivType == ImageSInt16.class ) {
			alg = (GridRansacLineDetector)new ImplGridRansacLineDetector_S16(regionSize,10,matcher);
		} else {
			throw new IllegalArgumentException("Unsupported derivative type");
		}


		ConnectLinesGrid connect = null;
		if( connectLines )
			connect = new ConnectLinesGrid(Math.PI*0.01,1,8);

		return new DetectLineSegmentsGridRansac<I,D>(alg,connect,gradient,thresholdEdge,imageType,derivType);
	}

	/**
	 * Detects lines using the foot of norm parametrization, see {@link DetectLineHoughFoot}.  The polar
	 * parametrization is more common, but more difficult to tune.
	 *
	 * @see DetectLineHoughFoot
	 *
	 * @param localMaxRadius Lines in transform space must be a local max in a region with this radius. Try 5;
	 * @param minCounts Minimum number of counts/votes inside the transformed image. Try 5.
	 * @param minDistanceFromOrigin Lines which are this close to the origin of the transformed image are ignored.  Try 5.
	 * @param thresholdEdge Threshold for classifying pixels as edge or not.  Try 30.
	 * @param maxLines Maximum number of lines to return. If <= 0 it will return them all.
	 * @param imageType Type of single band input image.
	 * @param derivType Image derivative type.                    
	 * @param <I> Input image type.
	 * @param <D> Image derivative type.
	 * @return Line detector.
	 */
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

	/**
	 * Detects lines using a foot of norm parametrization and sub images to reduce degenerate
	 * configurations, see {@link DetectLineHoughFootSubimage} for details.
	 *
	 * @see DetectLineHoughFootSubimage
	 *
	 * @param localMaxRadius Lines in transform space must be a local max in a region with this radius. Try 5;
	 * @param minCounts Minimum number of counts/votes inside the transformed image. Try 5.
	 * @param minDistanceFromOrigin Lines which are this close to the origin of the transformed image are ignored.  Try 5.
	 * @param thresholdEdge Threshold for classifying pixels as edge or not.  Try 30.
	 * @param maxLines Maximum number of lines to return. If <= 0 it will return them all.
	 * @param totalHorizontalDivisions Number of sub-images in horizontal direction Try 2
	 * @param totalVerticalDivisions Number of sub images in vertical direction.  Try 2
	 * @param imageType Type of single band input image.
	 * @param derivType Image derivative type.
	 * @param <I> Input image type.
	 * @param <D> Image derivative type.
	 * @return Line detector.
	 */
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
	 * Creates a Hough line detector based on polar parametrization.
	 *
	 * @see DetectLineHoughPolar
	 *
	 * @param localMaxRadius Radius for local maximum suppression.  Try 2.
	 * @param minCounts Minimum number of counts for detected line.  Critical tuning parameter and image dependent.
	 * @param resolutionRange Resolution of line range in pixels.  Try 2
	 * @param resolutionAngle Resolution of line angle in radius.  Try PI/180
	 * @param thresholdEdge Edge detection threshold. Try 50.
	 * @param maxLines Maximum number of lines to return. If <= 0 it will return them all.
	 * @param imageType Type of single band input image.
	 * @param derivType Image derivative type.                    
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
