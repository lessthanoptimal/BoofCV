/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.feature.detect.line.gridline;


import boofcv.numerics.fitting.modelset.ModelFitter;
import georegression.fitting.line.FitLine_F32;
import georegression.metric.UtilAngle;
import georegression.struct.line.LinePolar2D_F32;

import java.util.List;

/**
 * Used by {@link boofcv.alg.feature.detect.line.GridRansacLineDetector} to fit edgels inside a region to a line.  If only
 * two Edgels are considered then their angles are checked for consistency.  If the orientations
 * are too different then the match is discarded.
 *
 * @author Peter Abeles
 */
public class GridLineModelFitter implements ModelFitter<LinePolar2D_F32,Edgel> {

	// maximum allowed difference in angle
	float angleTol;

	public GridLineModelFitter(float angleTol) {
		this.angleTol = angleTol;
	}

	@Override
	public LinePolar2D_F32 declareModel() {
		return new LinePolar2D_F32();
	}

	@Override
	public boolean fitModel(List<Edgel> dataSet, LinePolar2D_F32 initParam, LinePolar2D_F32 foundModel) {
		if( dataSet.size() == 2 ) {
			// todo find angle between the points
			Edgel a = dataSet.get(0);
			Edgel b = dataSet.get(1);

			// see if their orientations are alisgned with the line's agle
			if(UtilAngle.dist(a.theta,b.theta) > angleTol )
				return false;
		}

		// edgel extends Point2D_F32 so this should be legal
		FitLine_F32.polar((List)dataSet,foundModel);

		return true;
	}

	@Override
	public int getMinimumPoints() {
		return 2;
	}
}
