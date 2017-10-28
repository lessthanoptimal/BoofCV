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

package boofcv.factory.shape;

import boofcv.abst.shapes.polyline.PointsToPolyline;
import boofcv.abst.shapes.polyline.SplitMergeLineFit_to_PointsToPolyline;
import boofcv.alg.shapes.polyline.splitmerge.SplitMergeLineFit;

/**
 * Factory for creating instances of {@link PointsToPolyline}
 *
 * @author Peter Abeles
 */
public class FactoryPointsToPolyline {

	/**
	 * Use a split-merge strategy to fit the contour
	 *
	 * @see SplitMergeLineFit
	 *
	 * @param config Configuration. null if use default
	 * @return SplitMergeLineFit_to_PointsToPolyline
	 */
	public static PointsToPolyline splitMerge(ConfigSplitMergeLineFit config ) {
		if( config == null )
			config = new ConfigSplitMergeLineFit();
		config.checkValidity();
		return new SplitMergeLineFit_to_PointsToPolyline(
				config.splitFraction, config.minimumSide,config.iterations);
	}

}
