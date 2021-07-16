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

import boofcv.alg.distort.pinhole.PinholePtoN_F64;
import boofcv.struct.calib.CameraKannalaBrandt;
import boofcv.struct.distort.Point2Transform3_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import org.ddogleg.solver.Polynomial;
import org.ddogleg.solver.PolynomialOps;
import org.ddogleg.solver.PolynomialRoots;
import org.ddogleg.solver.RootFinderType;
import org.ejml.UtilEjml;
import org.ejml.data.Complex_F64;

import java.util.Arrays;
import java.util.List;

//CUSTOM ignore Complex_F64

/**
 * Backwards project from a distorted 2D pixel to 3D unit sphere coordinate using the {@link CameraKannalaBrandt} model.
 *
 * @author Peter Abeles
 */
public class KannalaBrandtPtoS_F64 implements Point2Transform3_F64 {

	/** A complex number is considered real if the imaginary component has a magnitude &le; this value */
	public /**/double realNumberTol = /**/UtilEjml.TEST_F64;

	protected final CameraKannalaBrandt model;

	PinholePtoN_F64 pinholeNtoP = new PinholePtoN_F64();

	// "normalized" image coordinate from pinhole model
	Point2D_F64 norm = new Point2D_F64();

	/** Used to solve for theta */
	public PolynomialRoots rootFinder;
	private Polynomial polynomial = new Polynomial(5);

	public KannalaBrandtPtoS_F64( CameraKannalaBrandt model ) {
		this.model = new CameraKannalaBrandt(model);
		pinholeNtoP.setK(model);
		int numCoef = 1 + model.coefSymm.length*2;
		rootFinder = PolynomialOps.createRootFinder(numCoef, RootFinderType.EVD);
		polynomial.resize(numCoef);
	}

	@Override
	public void compute( double x, double y, Point3D_F64 out ) {
		// Undo projection on to pixels
		pinholeNtoP.compute(x, y, norm);

		double r, phi;
		if (model.coefRad.length > 0) {
			throw new RuntimeException("Not implemented yet");
		} else {
			r = norm.norm();
			phi = Math.atan2(norm.y/r, norm.x/r);
		}

		double theta = computeTheta(r);
		if (theta == Double.MAX_VALUE) {
			// It failed, so who give it a value that seems "reasonable" for failing and return
			out.setTo(0.0, 0.0, 0.0);
			return;
		}

		// go from angles to pointing
		double sintheta = Math.sin(theta);
		out.x = (double)(sintheta*Math.cos(phi));
		out.y = (double)(sintheta*Math.sin(phi));
		out.z = Math.cos(theta);
	}

	/**
	 * Compute theta by solving for the polynomial's roots. Go with the smallest real root
	 */
	double computeTheta( double r ) {
		// Pass in the polynomials coefficient
		// r = k1*theta + k2*theta**3 + k3*theta**5 ...
		Arrays.fill(polynomial.c, 0.0);
		polynomial.c[0] = -r;
		for (int i = 0; i < model.coefSymm.length; i++) {
			polynomial.c[i*2 + 1] = model.coefSymm[i];
		}
		if (!rootFinder.process(polynomial)) {
			// failed. not sure what else to do here
			return Double.MAX_VALUE;
		}

		// Pick the smallest real value of theta
		double theta = Double.MAX_VALUE;
		List<Complex_F64> roots = rootFinder.getRoots();
		for (int i = 0; i < roots.size(); i++) {
			Complex_F64 root = roots.get(i);
			if (Math.abs(root.imaginary) <= realNumberTol && theta > root.real && root.real > -realNumberTol) {
				theta = (double) root.real;
			}
		}

		return Math.max(0.0, theta);
	}

	@Override
	public Point2Transform3_F64 copyConcurrent() {
		return null;
	}
}
