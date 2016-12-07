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

package boofcv.factory.shape;

import boofcv.alg.shapes.ellipse.BinaryEllipseDetector;
import boofcv.alg.shapes.ellipse.BinaryEllipseDetectorPixel;
import boofcv.alg.shapes.ellipse.EdgeIntensityEllipse;
import boofcv.alg.shapes.ellipse.SnapToEllipseEdge;
import boofcv.alg.shapes.polygon.BinaryPolygonDetector;
import boofcv.alg.shapes.polygon.RefineBinaryPolygon;
import boofcv.alg.shapes.polygon.RefinePolygonCornersToImage;
import boofcv.alg.shapes.polygon.RefinePolygonLineToImage;
import boofcv.alg.shapes.polyline.SplitMergeLineFitLoop;
import boofcv.struct.image.ImageGray;

/**
 * Factory for detecting higher level shapes
 *
 *
 * @author Peter Abeles
 */
public class FactoryShapeDetector {

	/**
	 * Creates an ellipse detector which will detect all ellipses in the image initially using a binary image and
	 * then refine the estimate using a subpixel algorithm in the gray scale image.
	 *
	 * @param config Configuration for ellipse detector.  null == default
	 * @param imageType Input image type
	 * @return Detecto
	 */
	public static <T extends ImageGray>
	BinaryEllipseDetector<T> ellipse(ConfigEllipseDetector config , Class<T> imageType )
	{
		if( config == null )
			config = new ConfigEllipseDetector();

		config.checkValidity();

		BinaryEllipseDetectorPixel detector = new BinaryEllipseDetectorPixel();
		detector.setMaxDistanceFromEllipse(config.maxDistanceFromEllipse);
		detector.setMaximumContour(config.maximumContour);
		detector.setMinimumContour(config.minimumContour);
		detector.setInternalContour(config.processInternal);
		detector.setMaxMajorToMinorRatio(config.maxMajorToMinorRatio);

		SnapToEllipseEdge<T> refine = new SnapToEllipseEdge<>(config.numSampleContour, config.refineRadialSamples, imageType);
		refine.setConvergenceTol(config.convergenceTol);
		refine.setMaxIterations(config.maxIterations);

		if( config.maxIterations <= 0 || config.numSampleContour <= 0 ) {
			refine = null;
		}

		EdgeIntensityEllipse<T> check = new EdgeIntensityEllipse<>(
				config.checkRadialDistance,
				config.numSampleContour,
				config.minimumEdgeIntensity, imageType);

		return new BinaryEllipseDetector<>(detector, refine, check, imageType);
	}

	/**
	 * Creates a polygon detector.  The polygon is assumed to be a black shape with a much lighter background.
	 * The polygon can be found to sub-pixel accuracy, if configured to do so.
	 *
	 * @param config Configuration for polygon detector
	 * @param imageType Input image type
	 * @return Detector
	 */
	public static <T extends ImageGray>
	BinaryPolygonDetector<T> polygon( ConfigPolygonDetector config, Class<T> imageType)
	{
		config.checkValidity();

		SplitMergeLineFitLoop contourToPolygon = new SplitMergeLineFitLoop(
				config.contour2Poly_splitFraction,
				config.contour2Poly_minimumSideFraction,
				config.contour2Poly_iterations);

		RefineBinaryPolygon<T> refinePolygon = null;
		if( config.refine != null ) {
			if( config.refine instanceof ConfigRefinePolygonLineToImage ) {
				refinePolygon = refinePolygon((ConfigRefinePolygonLineToImage)config.refine,imageType);
			} else if( config.refine instanceof ConfigRefinePolygonCornersToImage ) {
				refinePolygon = refinePolygon((ConfigRefinePolygonCornersToImage)config.refine,imageType);
			} else {
				throw new IllegalArgumentException("Unknown refine config type");
			}
		}

		return new BinaryPolygonDetector<>(config.minimumSides, config.maximumSides, contourToPolygon,
				refinePolygon, config.minContourImageWidthFraction,
				config.clockwise, config.convex, config.canTouchBorder, config.splitPenalty,
				config.minimumEdgeIntensity, imageType);
	}

	public static <T extends ImageGray>
	RefineBinaryPolygon<T> refinePolygon( ConfigRefinePolygonLineToImage config , Class<T> imageType ) {
		return new RefinePolygonLineToImage<>(
				config.cornerOffset, config.lineSamples,
				config.sampleRadius, config.maxIterations,
				config.convergeTolPixels, config.maxCornerChangePixel,
				imageType);
	}

	public static <T extends ImageGray>
	RefineBinaryPolygon<T> refinePolygon( ConfigRefinePolygonCornersToImage config , Class<T> imageType ) {
		return new RefinePolygonCornersToImage<>(
				config.endPointDistance,
				config.cornerOffset, config.lineSamples,
				config.sampleRadius, config.maxIterations,
				config.convergeTolPixels, config.maxCornerChangePixel,
				imageType);
	}
}
