/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.factory.fiducial;

import boofcv.abst.shapes.polyline.ConfigPolylineSplitMerge;
import boofcv.factory.shape.ConfigPolygonDetector;
import boofcv.struct.ConfigLength;
import boofcv.struct.Configuration;

/**
 * TODO Comment
 *
 * @author Peter Abeles
 */
public class ConfigQrCode implements Configuration {

	public ConfigPolygonDetector polygon = new ConfigPolygonDetector(false,4,4);

	public int versionMinimum = 2;
	public int versionMaximum = 40;

	{
		polygon.detector.clockwise = false;
		((ConfigPolylineSplitMerge)polygon.detector.contourToPoly).cornerScorePenalty = 0.2;
		// 28 pixels = 7 by 7 square viewed head on. Each cell is then 1 pixel. Any slight skew results in
		// aliasing and will most likely not be read well.
		polygon.detector.minimumContour = ConfigLength.fixed(27);
		polygon.detector.minimumEdgeIntensity = 15;
		polygon.minimumRefineEdgeIntensity = 20;
	}

	@Override
	public void checkValidity() {
		if( polygon.detector.clockwise )
			throw new IllegalArgumentException("Must be counter clockwise");
		if( polygon.detector.minimumSides != 4 || polygon.detector.maximumSides != 4)
			throw new IllegalArgumentException("Must detect 4 sides and only 4 sides");

	}
}
