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

import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.shapes.polygon.BinaryPolygonConvexDetector;
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
	 * @param inputToBinary Class which is used to convert the gray scale image into a binary one
	 * @param config Configuration for polygon detector
	 * @param imageType Input image type
	 * @return Detector
	 */
	public static <T extends ImageSingleBand>
	BinaryPolygonConvexDetector<T> polygon( InputToBinary<T> inputToBinary,
											ConfigPolygonDetector config,
											Class<T> imageType)
	{
		config.checkValidity();

		SplitMergeLineFitLoop contourToPolygon = new SplitMergeLineFitLoop(0,config.contour2Poly_mergeTolerance,
				config.contour2Poly_iterations);

		RefinePolygonLineToImage<T> refineLine = null;
		if( config.refineWithLines )
			refineLine = new RefinePolygonLineToImage<T>(config.numberOfSides,
					config.configRefineLines.cornerOffset,config.configRefineLines.lineSamples,
					config.configRefineLines.sampleRadius,config.configRefineLines.maxIterations,
					config.configRefineLines.convergeTolPixels,
					true,imageType);
		RefinePolygonCornersToImage<T> refineCorner = null;
		if( config.refineWithCorners )
			refineCorner = new RefinePolygonCornersToImage<T>(
					config.configRefineCorners.endPointDistance,true,
					config.configRefineCorners.cornerOffset,config.configRefineCorners.lineSamples,
					config.configRefineCorners.sampleRadius,config.configRefineCorners.maxIterations,
					config.configRefineCorners.convergeTolPixels,imageType);

		return new BinaryPolygonConvexDetector<T>(config.numberOfSides,inputToBinary,contourToPolygon,
				refineLine,refineCorner,config.minContourImageWidthFraction,
				config.contour2Poly_splitDistanceFraction,config.clockwise,imageType);
	}
}
