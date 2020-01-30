/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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
import boofcv.struct.distort.Point3Transform2_F64;
import georegression.geometry.UtilPoint3D_F64;
import georegression.struct.point.Point2D_F64;
import org.ejml.UtilEjml;

/**
 * Forward projection model for {@link CameraKannalaBrandt}.  Takes a 3D point in camera unit sphere
 * coordinates and converts it into a distorted pixel coordinate.  There are no checks to see if
 * it is physically possible to perform the forward projection, e.g. point could be outside the FOV.
 *
 * @author Peter Abeles
 */
public class KannalaBrandtStoP_F64 implements Point3Transform2_F64 {
	protected final CameraKannalaBrandt model;

	public KannalaBrandtStoP_F64( CameraKannalaBrandt model ) {
		this.model = new CameraKannalaBrandt(model);
	}

	@Override
	public void compute(double x, double y, double z, Point2D_F64 out) {
		// angle between incoming ray and principle axis
		//    Principle Axis = (0,0,z)
		//    Incoming Ray   = (x,y,z)
		double theta = Math.acos(z/UtilPoint3D_F64.norm(x,y,z)); // uses dot product

		// angle on the image plane of the incoming ray
		double phi_r = Math.max(Math.sqrt(x*x+y*y) , UtilEjml.EPS); // eps = avoid divide by zero
		double cosphi = x/phi_r;
		double sinphi = y/phi_r;

		// compute symmetric projection function
		double r = polynomial(model.coefSymm,theta);

		// normalized image coordinates
		double dx,dy;
		if( model.coefRad.length > 0 ) {
			// distortion terms. radial and tangential
			double dr = polynomial(model.coefRad, theta) * polytrig(model.coefRadTrig, cosphi, sinphi);
			double dt = polynomial(model.coefTan, theta) * polytrig(model.coefRadTrig, cosphi, sinphi);

			// put it all together to get normalized image coordinates
			dx = (r + dr) * cosphi - dt * sinphi;
			dy = (r + dr) * sinphi + dt * cosphi;
		} else {
			dx = r*cosphi;
			dy = r*sinphi;
		}
		// project into pixels
		out.x = model.fx*dx + model.skew*dy + model.cx;
		out.y = model.fy*dy + model.cy;
	}

	private double polynomial( double[] coefs, double x ) {
		double pow = x;
		double result = 0;
		for (int i = 0; i < coefs.length; i++) {
			result += coefs[i]*pow;
			pow *= x*x;
		}
		return result;
	}

	private double polytrig(double[] coefs, double cos, double sin ) {
		double result = 0;
		for (int i = 0; i < coefs.length; i+=2) {
			result += coefs[i  ] * cos;
			result += coefs[i+1] * sin;

			// sin(2*phi) = 2*cos(phi)*sin(phi)
			// cos(2*phi) = 2*cos^2(phi)-1
			sin = 2*cos*sin;
			cos = 2*cos*cos-1.0;
		}
		return result;
	}


	@Override
	public Point3Transform2_F64 copyConcurrent() {
		return this;
	}
}
