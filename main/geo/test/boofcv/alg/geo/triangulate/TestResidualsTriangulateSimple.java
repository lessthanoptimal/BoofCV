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

package boofcv.alg.geo.triangulate;

import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.optimization.functions.FunctionNtoM;

import java.util.List;

/**
 * @author Peter Abeles
 */
public class TestResidualsTriangulateSimple
		extends ResidualTriangulateChecks
{
	@Override
	public FunctionNtoM createAlg(List<Point2D_F64> observations, List<Se3_F64> motionGtoC) {
		ResidualsTriangulateSimple alg = new ResidualsTriangulateSimple();
		alg.setObservations(observations,motionGtoC);
		return alg;
	}
}
