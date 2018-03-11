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

import boofcv.abst.shapes.polyline.*;
import boofcv.alg.shapes.polyline.splitmerge.SplitMergeLineFit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Factory for creating instances of {@link PointsToPolyline}
 *
 * @author Peter Abeles
 */
public class FactoryPointsToPolyline {

	/**
	 * Generic function for create polyline algorithms based on configuration type
	 */
	public static PointsToPolyline create( @Nonnull ConfigPolyline config ) {
		if( config instanceof ConfigSplitMergeLineFit ) {
			return splitMerge((ConfigSplitMergeLineFit)config);
		} else if( config instanceof ConfigPolylineSplitMerge ) {
			return splitMerge((ConfigPolylineSplitMerge)config);
		} else {
			throw new RuntimeException("Unknown");
		}
	}

	/**
	 * Use a split-merge strategy to fit the contour
	 *
	 * @see SplitMergeLineFit
	 *
	 * @param config Configuration. null if use default
	 * @return {@link SplitMergeLineRefine_to_PointsToPolyline}
	 */
	@Deprecated
	public static PointsToPolyline splitMerge(@Nullable ConfigSplitMergeLineFit config ) {
		if( config == null )
			config = new ConfigSplitMergeLineFit();
		config.checkValidity();
		return new SplitMergeLineRefine_to_PointsToPolyline(config);
	}

	public static PointsToPolyline splitMerge(@Nullable ConfigPolylineSplitMerge config ) {
		if( config == null )
			config = new ConfigPolylineSplitMerge();
		config.checkValidity();
		return new NewSplitMerge_to_PointsToPolyline(config);
	}
}
