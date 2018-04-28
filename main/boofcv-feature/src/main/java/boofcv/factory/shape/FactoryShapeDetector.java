/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.filter.binary.BinaryContourFinder;
import boofcv.abst.shapes.polyline.PointsToPolyline;
import boofcv.alg.shapes.ellipse.BinaryEllipseDetector;
import boofcv.alg.shapes.ellipse.BinaryEllipseDetectorPixel;
import boofcv.alg.shapes.ellipse.EdgeIntensityEllipse;
import boofcv.alg.shapes.ellipse.SnapToEllipseEdge;
import boofcv.alg.shapes.polygon.*;
import boofcv.factory.filter.binary.FactoryBinaryContourFinder;
import boofcv.struct.image.ImageGray;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
	public static <T extends ImageGray<T>>
	BinaryEllipseDetector<T> ellipse(@Nullable ConfigEllipseDetector config , Class<T> imageType )
	{
		if( config == null )
			config = new ConfigEllipseDetector();

		config.checkValidity();

		BinaryEllipseDetectorPixel detector = new BinaryEllipseDetectorPixel(config.contourRule);
		detector.setMaxDistanceFromEllipse(config.maxDistanceFromEllipse);
		detector.setMaximumContour(config.maximumContour);
		detector.setMinimumContour(config.minimumContour);
		detector.setMinimumMinorAxis(config.minimumMinorAxis);
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
	public static <T extends ImageGray<T>>
	DetectPolygonBinaryGrayRefine<T> polygon(@Nullable ConfigPolygonDetector config, Class<T> imageType)
	{
		config.checkValidity();

		RefinePolygonToContour refineContour = config.refineContour ? new RefinePolygonToContour() : null;

		RefinePolygonToGray<T> refineGray = config.refineGray != null ?
				refinePolygon(config.refineGray,imageType) : null;

		DetectPolygonFromContour<T> detector = polygonContour(config.detector,imageType);

		return new DetectPolygonBinaryGrayRefine<>(detector,refineContour,refineGray,
				config.minimumRefineEdgeIntensity,
				config.adjustForThresholdBias);
	}

	public static <T extends ImageGray<T>>
	DetectPolygonFromContour<T> polygonContour(@Nonnull ConfigPolygonFromContour config, Class<T> imageType)
	{
		config.checkValidity();

		PointsToPolyline contourToPolygon =
				FactoryPointsToPolyline.create(config.contourToPoly);

		BinaryContourFinder contour = FactoryBinaryContourFinder.linearExternal();
		contour.setConnectRule(config.contourRule);

		return new DetectPolygonFromContour<>(contourToPolygon,
				config.minimumContour,
				config.clockwise, config.canTouchBorder,
				config.minimumEdgeIntensity, config.tangentEdgeIntensity,contour, imageType);
	}

	public static <T extends ImageGray<T>>
	RefinePolygonToGray<T> refinePolygon(@Nonnull ConfigRefinePolygonLineToImage config , Class<T> imageType ) {
		return new RefinePolygonToGrayLine<>(
				config.cornerOffset, config.lineSamples,
				config.sampleRadius, config.maxIterations,
				config.convergeTolPixels, config.maxCornerChangePixel,
				imageType);
	}
}
