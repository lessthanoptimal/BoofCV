/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.distort.kanbra;

import boofcv.struct.calib.CameraKannalaBrandt;
import boofcv.struct.distort.Point3Transform2_F32;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;

import javax.annotation.Generated;

/**
 * Forward projection model for {@link CameraKannalaBrandt}.  Takes a 3D point in camera unit sphere
 * coordinates and converts it into a distorted pixel coordinate.  There are no checks to see if
 * it is physically possible to perform the forward projection, e.g. point could be outside the FOV.
 *
 * @author Peter Abeles
 */
@Generated("boofcv.alg.distort.kanbra.KannalaBrandtStoP_F64")
public class KannalaBrandtStoP_F32 implements Point3Transform2_F32 {
	KannalaBrandtStoP_F64 s_to_p;

	Point2D_F64 tmp = new Point2D_F64();

	public KannalaBrandtStoP_F32( CameraKannalaBrandt model ) {
		this.s_to_p = new KannalaBrandtStoP_F64(model);
	}

	@Override
	public void compute( float x, float y, float z, Point2D_F32 out ) {
		s_to_p.compute(x, y, z, tmp);
		out.x = (float)tmp.x;
		out.y = (float)tmp.y;
	}

	@Override
	public Point3Transform2_F32 copyConcurrent() {
		return new KannalaBrandtStoP_F32(s_to_p.model);
	}
}
