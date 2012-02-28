/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.epipolar;

import boofcv.abst.geo.epipolar.PerspectiveNPoint;
import boofcv.alg.geo.PointPositionPair;
import boofcv.factory.geo.d3.epipolar.FactoryEpipolar;
import georegression.geometry.RotationMatrixGenerator;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author Peter Abeles
 */
public class BenchmarkStabilityPose extends ArtificialStereoScene {

	static final int NUM_POINTS = 500;

	PerspectiveNPoint target;
	
	public void evaluateObservationNoise( double min , double max , int N , boolean isPlanar )
	{
		System.out.println("------------------------");
		
		rand = new Random(234);
		List<PointPositionPair> inputs = new ArrayList<PointPositionPair>();
		
		for( int i = 0; i <= N; i++ ) {
			inputs.clear();
			double mag = (max-min)*i/N+min;
			
			init(NUM_POINTS,false,isPlanar);
			addObservationNoise(mag);
			
			for( int j = 0; j < NUM_POINTS; j++ ) {
				inputs.add(new PointPositionPair(pairs.get(j).currLoc, worldPoints.get(j)));
			}
			
			target.process(inputs);

			Se3_F64 found = target.getPose();
			
			double expectedEuler[] = RotationMatrixGenerator.matrixToEulerXYZ(motion.getR());
			double foundEuler[] = RotationMatrixGenerator.matrixToEulerXYZ(found.getR());

			Vector3D_F64 expectedTran = motion.getT();
			Vector3D_F64 foundTran = found.getT();

			double errorEuler = 0;
			double errorTran = expectedTran.distance(foundTran);
			for( int j = 0; j < 3; j++ ) {
				double e = expectedEuler[j]-foundEuler[j];
				errorEuler += e*e;
			}
			errorEuler = Math.sqrt(errorEuler);

			System.out.printf("%3d angle %6.2e  translation %6.2e\n", i,errorEuler, errorTran);
		}
	}
	
	public static void main( String args[] ) {
		double max = 0.05;
		boolean planar = false;

		BenchmarkStabilityPose app = new BenchmarkStabilityPose();
		app.target = FactoryEpipolar.pnpEfficientPnP(0,0.1);
		app.evaluateObservationNoise(0,max,20,planar);
		app.target = FactoryEpipolar.pnpEfficientPnP(10,0.1);
		app.evaluateObservationNoise(0,max,20,planar);

	}
}
