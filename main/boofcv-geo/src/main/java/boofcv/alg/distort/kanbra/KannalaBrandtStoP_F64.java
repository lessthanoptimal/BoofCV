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

import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.CameraKannalaBrandt;
import boofcv.struct.distort.Point3Transform2_F64;
import georegression.geometry.UtilPoint3D_F64;
import georegression.struct.point.Point2D_F64;
import lombok.Getter;

import static boofcv.alg.distort.kanbra.KannalaBrandtUtils_F64.polynomial;
import static boofcv.alg.distort.kanbra.KannalaBrandtUtils_F64.polytrig;

//CUSTOM ignore KannalaBrandtUtils_F64

/**
 * Forward projection model for {@link CameraKannalaBrandt}. Takes a 3D point in camera unit sphere
 * coordinates and converts it into a distorted pixel coordinate. There are no checks to see if
 * it is physically possible to perform the forward projection, e.g. point could be outside the FOV.
 *
 * @author Peter Abeles
 */
public class KannalaBrandtStoP_F64 implements Point3Transform2_F64 {
	@Getter protected final CameraKannalaBrandt model;

	public KannalaBrandtStoP_F64( CameraKannalaBrandt model ) {
		BoofMiscOps.checkTrue(model.radialTrig.length == 0 || model.radialTrig.length == 4);

		this.model = new CameraKannalaBrandt(model);
	}

	@Override
	public void compute( double x, double y, double z, Point2D_F64 out ) {
		// angle between incoming ray and principle axis
		//    Principle Axis = (0,0,z)
		//    Incoming Ray   = (x,y,z)
		double theta = Math.acos(z/UtilPoint3D_F64.norm(x, y, z)); // uses dot product

		// compute symmetric projection function
		double r = (double)polynomial(model.symmetric, theta);

		// angle on the image plane of the incoming ray
		double phi = Math.atan2(y, x);
		double cosphi = Math.cos(phi); // u_r[0] or u_phi[1]
		double sinphi = Math.sin(phi); // u_r[1] or -u_phi[0]

		// distorted (normalized) coordinates
		double dx, dy;
		if (model.isAsymmetricModel()) {
			// distortion terms. radial and tangential
			double disRad = (double)(polynomial(model.radial, theta)*polytrig(model.radialTrig, cosphi, sinphi));
			double disTan = (double)(polynomial(model.tangent, theta)*polytrig(model.tangentTrig, cosphi, sinphi));

			// put it all together to get normalized image coordinates
			dx = (r + disRad)*cosphi - disTan*sinphi;
			dy = (r + disRad)*sinphi + disTan*cosphi;
		} else {
			dx = r*cosphi;
			dy = r*sinphi;
		}

		// project into pixels
		out.x = (double)(model.fx*dx + model.skew*dy + model.cx);
		out.y = (double)(model.fy*dy + model.cy);
	}

	@Override
	public Point3Transform2_F64 copyConcurrent() {
		return this;
	}
}
