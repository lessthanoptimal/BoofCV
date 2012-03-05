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

package boofcv.alg.geo;

import georegression.geometry.GeometryMath_F64;
import georegression.geometry.RotationMatrixGenerator;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author Peter Abeles
 */
public class ArtificialStereoScene {
	Random rand = new Random(234234);

	// create a reasonable calibration matrix
	DenseMatrix64F K = new DenseMatrix64F(3,3,true,705,0.001,326,0,704,224,0,0,1);
	DenseMatrix64F K_inv = new DenseMatrix64F(3,3);

	protected Se3_F64 motion;
	protected List<AssociatedPair> pairs;
	protected List<Point2D_F64> observationCurrent;
	protected List<Point3D_F64> worldPoints;
	protected boolean isPixels;

	public ArtificialStereoScene() {
		CommonOps.invert(K,K_inv);
	}

	public void init( int N , boolean isPixels , boolean planar ) {
		this.isPixels = isPixels;
		// define the camera's motion
		motion = new Se3_F64();
		motion.getR().set(RotationMatrixGenerator.eulerArbitrary(0, 1, 2, 0.5 , -1, 1));
		motion.getT().set(0.1,-0.1,0.01);

		// randomly generate points in space
		if( planar )
			worldPoints = createPlanarScene(N);
		else
			worldPoints = GeoTestingOps.randomPoints_F64(-1, 1, -1, 1, 2, 3, N, rand);

		// transform points into second camera's reference frame
		pairs = new ArrayList<AssociatedPair>();
		observationCurrent = new ArrayList<Point2D_F64>();
		for(Point3D_F64 p1 : worldPoints) {
			Point3D_F64 p2 = SePointOps_F64.transform(motion, p1, null);

			AssociatedPair pair = new AssociatedPair();
			pair.keyLoc.set(p1.x/p1.z,p1.y/p1.z);
			pair.currLoc.set(p2.x/p2.z,p2.y/p2.z);
			pairs.add(pair);

			observationCurrent.add(pair.currLoc);

			if( isPixels ) {
				GeometryMath_F64.mult(K, pair.keyLoc, pair.keyLoc);
				GeometryMath_F64.mult(K,pair.currLoc,pair.currLoc);
			}
		}
	}
	
	public void addPixelNoise( double mag ) {

		Point3D_F64 noiseCurr = new Point3D_F64();
		Point3D_F64 noiseKey = new Point3D_F64();

		for( AssociatedPair p : pairs ) {
			double cx=0,cy=0,kx=0,ky=0;

			noiseCurr.x = rand.nextGaussian()*mag;
			noiseCurr.y = rand.nextGaussian()*mag;
			noiseKey.x = rand.nextGaussian()*mag;
			noiseKey.y = rand.nextGaussian()*mag;
			
			if( !isPixels ) {
				GeometryMath_F64.mult(K_inv,noiseCurr,noiseCurr);
				GeometryMath_F64.mult(K_inv,noiseKey,noiseKey);
			}

			p.currLoc.x += noiseCurr.x;
			p.currLoc.y += noiseCurr.y;

			p.keyLoc.x += noiseKey.x;
			p.keyLoc.y += noiseKey.y;
		}

		// observationCurrent simply references the data in pairs
	}
	
	private List<Point3D_F64> createPlanarScene( int N ) {
		List<Point3D_F64> ret = new ArrayList<Point3D_F64>();

		for( int i = 0; i < N; i++ ) {
			double x = (rand.nextDouble()-0.5)*2;
			double y = (rand.nextDouble()-0.5)*2;

			ret.add( new Point3D_F64(x,y,3));
		}

		return ret;
	}
}
