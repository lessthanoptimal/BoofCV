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

package boofcv.abst.geo.triangulate;

import boofcv.abst.geo.TriangulateNViewsCalibrated;
import boofcv.alg.geo.GeometricResult;
import boofcv.alg.geo.triangulate.TriangulateCalibratedLinearDLT;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import georegression.struct.se.Se3_F64;

import java.util.List;

/**
 * Wrapper around {@link TriangulateCalibratedLinearDLT} for {@link boofcv.abst.geo.TriangulateTwoViewsCalibrated}.
 *
 * @author Peter Abeles
 */
public class WrapNViewsTriangulateCalibratedDLT implements TriangulateNViewsCalibrated {

	TriangulateCalibratedLinearDLT alg = new TriangulateCalibratedLinearDLT();

	Point4D_F64 pointH = new Point4D_F64();

	@Override
	public boolean triangulate(List<Point2D_F64> observations, List<Se3_F64> worldToView ,
							   Point3D_F64 location ) {

		if(GeometricResult.SUCCESS == alg.triangulate(observations,worldToView,pointH) ) {
			// can't handle points at infinity with this interface
			if( pointH.w == 0 )
				return false;
			location.x = pointH.x/pointH.w;
			location.y = pointH.y/pointH.w;
			location.z = pointH.z/pointH.w;
			return true;
		}

		return false;
	}

	public TriangulateCalibratedLinearDLT getAlgorithm() {
		return alg;
	}
}