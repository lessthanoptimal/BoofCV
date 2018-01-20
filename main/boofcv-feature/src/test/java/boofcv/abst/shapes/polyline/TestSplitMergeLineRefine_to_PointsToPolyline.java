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

package boofcv.abst.shapes.polyline;

import boofcv.factory.shape.ConfigSplitMergeLineFit;
import boofcv.factory.shape.FactoryPointsToPolyline;

/**
 * @author Peter Abeles
 */
public class TestSplitMergeLineRefine_to_PointsToPolyline extends ChecksGenericPointsToPolyline{

	@Override
	public PointsToPolyline createAlg(boolean loop) {
		ConfigSplitMergeLineFit config = new ConfigSplitMergeLineFit();
		config.loop = loop;

		return FactoryPointsToPolyline.splitMerge(config);
	}
}