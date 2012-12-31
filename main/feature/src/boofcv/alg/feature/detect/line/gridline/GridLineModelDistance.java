/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.line.gridline;


import georegression.geometry.UtilLine2D_F32;
import georegression.metric.Distance2D_F32;
import georegression.metric.UtilAngle;
import georegression.struct.line.LineParametric2D_F32;
import georegression.struct.line.LinePolar2D_F32;
import org.ddogleg.fitting.modelset.DistanceFromModel;

import java.util.List;

/**
 * Computes the distance of a point from the line.
 *
 * @author Peter Abeles
 */
public class GridLineModelDistance implements DistanceFromModel<LinePolar2D_F32,Edgel> {

	LineParametric2D_F32 line = new LineParametric2D_F32();
	float theta;

	// maximum distance between line slope and point gradient
	float angleTolerance;

	public GridLineModelDistance(float angleTolerance) {
		this.angleTolerance = angleTolerance;
	}

	@Override
	public void setModel(LinePolar2D_F32 lineParam) {
		UtilLine2D_F32.convert(lineParam,line);
		theta = lineParam.angle;
	}

	@Override
	public double computeDistance(Edgel pt) {
		if(UtilAngle.distHalf(pt.theta, theta) > angleTolerance )
			return Double.MAX_VALUE;

		return Distance2D_F32.distance(line,pt);
	}

	@Override
	public void computeDistance(List<Edgel> edgels, double[] distance) {
		for( int i = 0; i < edgels.size(); i++ ) {
			distance[i] = computeDistance(edgels.get(i));
		}
	}
}
