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
import boofcv.alg.shapes.polygon.RefineBinaryPolygon;
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

		PolygonEdgeScore<T> scorer = null;
		if( config.minimumEdgeIntensity > 0 ) {
			double cornerOffset = 1;
			int numSamples = 15;
			scorer = new PolygonEdgeScore<T>(cornerOffset,1.0,numSamples,config.minimumEdgeIntensity,imageType);
		}

		return new BinaryPolygonDetector<T>(config.minimumSides,config.maximumSides,contourToPolygon,
				scorer, refinePolygon,config.minContourImageWidthFraction,
				config.clockwise,config.convex, config.canTouchBorder, config.splitPenalty,imageType);
	}

	public static <T extends ImageSingleBand>
	RefineBinaryPolygon<T> refinePolygon( ConfigRefinePolygonLineToImage config , Class<T> imageType ) {
		return new RefinePolygonLineToImage<T>(
				config.cornerOffset, config.lineSamples,
				config.sampleRadius, config.maxIterations,
				config.convergeTolPixels, config.maxCornerChangePixel,
				imageType);
	}

	public static <T extends ImageSingleBand>
	RefineBinaryPolygon<T> refinePolygon( ConfigRefinePolygonCornersToImage config , Class<T> imageType ) {
		return new RefinePolygonCornersToImage<T>(
				config.endPointDistance,
				config.cornerOffset, config.lineSamples,
				config.sampleRadius, config.maxIterations,
				config.convergeTolPixels, config.maxCornerChangePixel,
				imageType);
	}
}
