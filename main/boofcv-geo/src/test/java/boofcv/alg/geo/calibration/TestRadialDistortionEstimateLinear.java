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

package boofcv.alg.geo.calibration;

import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import org.ejml.data.DMatrixRMaj;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestRadialDistortionEstimateLinear extends BoofStandardJUnit {

	/**
	 * Given perfect observations and a random scenario see if it can compute the distortion parameters
	 */
	@Test void perfect() {
		perfect(false);
		perfect(true);
	}

	public void perfect( boolean partial ) {

		double distort[] = new double[]{0.01,-0.002};
		DMatrixRMaj K = GenericCalibrationGrid.createStandardCalibration();
		List<DMatrixRMaj> homographies = GenericCalibrationGrid.createHomographies(K, 2, rand);

		List<Point2D_F64> layout = GenericCalibrationGrid.standardLayout();

		List<CalibrationObservation> observations = new ArrayList<>();

		for( DMatrixRMaj H : homographies ) {
			// in calibrated image coordinates
			List<Point2D_F64> pixels = GenericCalibrationGrid.observations(H, layout);
			// apply distortion
			for( Point2D_F64 p : pixels ) {
				distort(p, distort);
			}
			// put into pixel coordinates
			for( Point2D_F64 p : pixels ) {
				GeometryMath_F64.mult(K,p,p);
			}

			CalibrationObservation set = new CalibrationObservation();
			for (int i = 0; i < pixels.size(); i++) {
				set.add(pixels.get(i),i);
			}

			observations.add( set );
		}

		if( partial ) {
			for (int i = 0; i < observations.size(); i++) {
				CalibrationObservation c = observations.get(i);
				for (int j = 0; j < 5; j++) {
					c.points.remove(2 * i + j);
				}
			}
		}

		RadialDistortionEstimateLinear alg = new RadialDistortionEstimateLinear(layout,distort.length);

		alg.process(K,homographies,observations);

		double found[] = alg.getParameters();

		for( int i = 0; i < distort.length; i++ )
			assertEquals(distort[i],found[i],1e-6);
	}

	/**
	 * Applies distortion to the provided pixel in calibrated coordinates
	 */
	private static void distort( Point2D_F64 p , double coef[] ) {
		double r = p.x*p.x + p.y*p.y;

		double m = 0;
		for( int i = 0; i < coef.length; i++ ) {
			m += coef[i]*Math.pow(r,i+1);
		}
		p.x += p.x*m;
		p.y += p.y*m;
	}
}
