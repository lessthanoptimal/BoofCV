/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.shapes.edge.PolygonEdgeScore;
import boofcv.alg.shapes.polygon.BinaryPolygonDetector;
import boofcv.alg.shapes.polygon.RefinePolygonCornersToImage;
import boofcv.alg.shapes.polygon.RefinePolygonLineToImage;
import boofcv.alg.shapes.polyline.SplitMergeLineFitLoop;
import boofcv.struct.image.ImageSingleBand;

/**
 * Factory for detecting higher level shapes
 *
 *
 * @author Peter Abeles
 */
public class FactoryShapeDetector {

	/**
	 * Creates a polygon detector.  The polygon is assumed to be a black shape with a much lighter background.
	 * The polygon can be found to sub-pixel accuracy, if configured to do so.
	 *
	 * @param config Configuration for polygon detector
	 * @param imageType Input image type
	 * @return Detector
	 */
	public static <T extends ImageSingleBand>
	BinaryPolygonDetector<T> polygon( ConfigPolygonDetector config,
											Class<T> imageType)
	{
		config.checkValidity();

		double cornerOffset = 2;
		int numSamples = 15;

		SplitMergeLineFitLoop contourToPolygon = new SplitMergeLineFitLoop(
				config.contour2Poly_splitFraction,
				0, // dynamically set later on
				config.contour2Poly_iterations);

		RefinePolygonLineToImage<T> refineLine = null;
		if( config.refineWithLines ) {
			cornerOffset = config.configRefineLines.cornerOffset;
			numSamples = config.configRefineLines.lineSamples;
			refineLine = new RefinePolygonLineToImage<T>(
					config.configRefineLines.cornerOffset, config.configRefineLines.lineSamples,
					config.configRefineLines.sampleRadius, config.configRefineLines.maxIterations,
					config.configRefineLines.convergeTolPixels, config.configRefineLines.maxCornerChangePixel,
					imageType);
		}
		RefinePolygonCornersToImage<T> refineCorner = null;
		if( config.refineWithCorners ) {
			cornerOffset = config.configRefineCorners.cornerOffset;
			numSamples = config.configRefineCorners.lineSamples;
			refineCorner = new RefinePolygonCornersToImage<T>(
					config.configRefineCorners.endPointDistance,
					config.configRefineCorners.cornerOffset, config.configRefineCorners.lineSamples,
					config.configRefineCorners.sampleRadius, config.configRefineCorners.maxIterations,
					config.configRefineCorners.convergeTolPixels, config.configRefineCorners.maxCornerChangePixel,
					imageType);
		}

		PolygonEdgeScore<T> scorer = null;
		if( config.minimumEdgeIntensity > 0 ) {
			scorer = new PolygonEdgeScore<T>(cornerOffset,1.0,numSamples,config.minimumEdgeIntensity,imageType);
		}

		return new BinaryPolygonDetector<T>(config.numberOfSides,contourToPolygon,
				scorer, refineLine,refineCorner,config.minContourImageWidthFraction,
				config.contour2Poly_minimumSplitFraction,config.clockwise,config.convex,imageType);
	}
}
