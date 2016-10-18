/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.ImageGray;
import georegression.fitting.line.ModelManagerLinePolar2D_F32;
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
	public static <I extends ImageGray, D extends ImageGray>
	DetectLineSegmentsGridRansac<I,D> lineRansac(int regionSize ,
												 double thresholdEdge ,
												 double thresholdAngle ,
												 boolean connectLines,
												 Class<I> imageType ,
												 Class<D> derivType ) {

		ImageGradient<I,D> gradient = FactoryDerivative.sobel(imageType,derivType);

		ModelManagerLinePolar2D_F32 manager = new ModelManagerLinePolar2D_F32();
		GridLineModelDistance distance = new GridLineModelDistance((float)thresholdAngle);
		GridLineModelFitter fitter = new GridLineModelFitter((float)thresholdAngle);

		ModelMatcher<LinePolar2D_F32, Edgel> matcher =
				new Ransac<>(123123, manager, fitter, distance, 25, 1);

		GridRansacLineDetector<D> alg;
		if( derivType == GrayF32.class )  {
			alg = (GridRansacLineDetector)new ImplGridRansacLineDetector_F32(regionSize,10,matcher);
		} else if( derivType == GrayS16.class ) {
			alg = (GridRansacLineDetector)new ImplGridRansacLineDetector_S16(regionSize,10,matcher);
		} else {
			throw new IllegalArgumentException("Unsupported derivative type");
		}

		ConnectLinesGrid connect = null;
		if( connectLines )
			connect = new ConnectLinesGrid(Math.PI*0.01,1,8);

		return new DetectLineSegmentsGridRansac<>(alg, connect, gradient, thresholdEdge, imageType, derivType);
	}

	/**
	 * Detects lines using the foot of norm parametrization, see {@link DetectLineHoughFoot}.  The polar
	 * parametrization is more common, but more difficult to tune.
	 *
	 * @see DetectLineHoughFoot
	 *
	 * @param config Configuration for line detector.  If null then default will be used.
	 * @param imageType Type of single band input image.
	 * @param derivType Image derivative type.                    
	 * @param <I> Input image type.
	 * @param <D> Image derivative type.
	 * @return Line detector.
	 */
	public static <I extends ImageGray, D extends ImageGray>
	DetectLineHoughFoot<I,D> houghFoot(ConfigHoughFoot config ,
									   Class<I> imageType ,
									   Class<D> derivType ) {

		if( config == null )
			config = new ConfigHoughFoot();

		ImageGradient<I,D> gradient = FactoryDerivative.sobel(imageType,derivType);

		return new DetectLineHoughFoot<>(config.localMaxRadius, config.minCounts, config.minDistanceFromOrigin,
				config.thresholdEdge, config.maxLines, gradient);
	}

	/**
	 * Detects lines using a foot of norm parametrization and sub images to reduce degenerate
	 * configurations, see {@link DetectLineHoughFootSubimage} for details.
	 *
	 * @see DetectLineHoughFootSubimage
	 *
	 * @param config Configuration for line detector.  If null then default will be used.
	 * @param imageType Type of single band input image.
	 * @param derivType Image derivative type.
	 * @param <I> Input image type.
	 * @param <D> Image derivative type.
	 * @return Line detector.
	 */
	public static <I extends ImageGray, D extends ImageGray>
	DetectLineHoughFootSubimage<I,D> houghFootSub(ConfigHoughFootSubimage config ,
												  Class<I> imageType ,
												  Class<D> derivType ) {

		if( config == null )
			config = new ConfigHoughFootSubimage();

		ImageGradient<I,D> gradient = FactoryDerivative.sobel(imageType,derivType);

		return new DetectLineHoughFootSubimage<>(config.localMaxRadius,
				config.minCounts, config.minDistanceFromOrigin, config.thresholdEdge,
				config.totalHorizontalDivisions, config.totalVerticalDivisions, config.maxLines, gradient);
	}

	/**
	 * Creates a Hough line detector based on polar parametrization.
	 *
	 * @see DetectLineHoughPolar
	 *
	 * @param config Configuration for line detector.  Can't be null.
	 * @param imageType Type of single band input image.
	 * @param derivType Image derivative type.                    
	 * @param <I> Input image type.
	 * @param <D> Image derivative type.
	 * @return Line detector.
	 */
	public static <I extends ImageGray, D extends ImageGray>
	DetectLineHoughPolar<I,D> houghPolar(ConfigHoughPolar config ,
										 Class<I> imageType ,
										 Class<D> derivType ) {

		if( config == null )
			throw new IllegalArgumentException("This is no default since minCounts must be specified");

		ImageGradient<I,D> gradient = FactoryDerivative.sobel(imageType,derivType);

		return new DetectLineHoughPolar<>(config.localMaxRadius, config.minCounts, config.resolutionRange,
				config.resolutionAngle, config.thresholdEdge, config.maxLines, gradient);
	}

}
