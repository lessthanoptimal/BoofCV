/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.Point2D3D;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
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
	protected List<Point2D3D> observationPose;
	protected boolean isPixels;

	public ArtificialStereoScene() {
		CommonOps.invert(K,K_inv);
	}

	public void init( int N , boolean isPixels , boolean planar ) {
		this.isPixels = isPixels;
		// define the camera's motion
		motion = new Se3_F64();
		motion.getR().set(ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,0.5 , -0.2, 0.15,null));
		motion.getT().set(0.1,-0.2,0.01);

		// randomly generate points in space
		if( planar )
			worldPoints = createPlanarScene(N);
		else
			worldPoints = GeoTestingOps.randomPoints_F64(-1, 1, -1, 1, 2, 3, N, rand);

		// transform points into second camera's reference frame
		pairs = new ArrayList<>();
		observationCurrent = new ArrayList<>();
		observationPose = new ArrayList<>();

		for(Point3D_F64 p1 : worldPoints) {
			Point3D_F64 p2 = SePointOps_F64.transform(motion, p1, null);

			AssociatedPair pair = new AssociatedPair();
			pair.p1.set(p1.x/p1.z,p1.y/p1.z);
			pair.p2.set(p2.x/p2.z,p2.y/p2.z);
			pairs.add(pair);

			observationCurrent.add(pair.p2);
			observationPose.add( new Point2D3D(pair.p2,p1));

			if( isPixels ) {
				PerspectiveOps.convertNormToPixel(K,pair.p1,pair.p1);
				PerspectiveOps.convertNormToPixel(K,pair.p2,pair.p2);
			}
		}
	}
	
	public void addPixelNoise( double noiseSigma ) {

		for( AssociatedPair p : pairs ) {

			if( !isPixels ) {
				PerspectiveOps.convertNormToPixel(K, p.p1, p.p1);
				PerspectiveOps.convertNormToPixel(K, p.p2, p.p2);
			}

			p.p2.x += rand.nextGaussian()*noiseSigma;
			p.p2.y += rand.nextGaussian()*noiseSigma;

			p.p1.x += rand.nextGaussian()*noiseSigma;
			p.p1.y += rand.nextGaussian()*noiseSigma;

			if( !isPixels ) {
				PerspectiveOps.convertPixelToNorm(K, p.p1, p.p1);
				PerspectiveOps.convertPixelToNorm(K, p.p2, p.p2);
			}
		}

		// observationCurrent simply references the data in pairs
	}
	
	private List<Point3D_F64> createPlanarScene( int N ) {
		List<Point3D_F64> ret = new ArrayList<>();

		for( int i = 0; i < N; i++ ) {
			double x = (rand.nextDouble()-0.5)*2;
			double y = (rand.nextDouble()-0.5)*2;

			ret.add( new Point3D_F64(x,y,3));
		}

		return ret;
	}
}
