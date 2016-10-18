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

package boofcv.alg.geo.pose;

import boofcv.alg.geo.GeoTestingOps;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.sfm.Stereo2D3D;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author Peter Abeles
 */
public class CommonStereoMotionNPoint {
	protected Random rand = new Random(234);

	// the true motion
	protected Se3_F64 worldToLeft;
	protected Se3_F64 leftToRight;
	protected Se3_F64 worldToRight;


	// list of points in world reference frame
	protected List<Point3D_F64> worldPts;
	// list of points is camera reference frame
	protected List<Point3D_F64> cameraLeftPts;
	protected List<Point3D_F64> cameraRightPts;
	// list of point pairs
	protected List<Stereo2D3D> pointPose;

	protected StereoParameters param;

	public CommonStereoMotionNPoint() {
		leftToRight = new Se3_F64();
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,0.01,-0.001,0.005,leftToRight.getR());
		leftToRight.getT().set(-0.1,0.02,-0.03);

		param = new StereoParameters();
		param.rightToLeft = leftToRight.invert(null);

		param.left = new CameraPinholeRadial(400,500,0.1,160,120,320,240).fsetRadial(0, 0);
		param.right = new CameraPinholeRadial(380,505,0.05,165,115,320,240).fsetRadial(0,0);

		worldToLeft = new Se3_F64();
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,0.01, 0.04, -0.05, worldToLeft.getR());
		worldToLeft.getT().set(0.1,-0.1,0.2);

		worldToRight = worldToLeft.concat(leftToRight,null);
	}

	protected void generateScene(int N, Se3_F64 worldToLeft, boolean planar) {
		if( worldToLeft == null ) {
			this.worldToLeft = worldToLeft = new Se3_F64();
			ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,0.1, 1, -0.2, worldToLeft.getR());
			worldToLeft.getT().set(-0.3,0.4,1);
		} else {
			this.worldToLeft = worldToLeft;
		}

		// randomly generate points in space
		if( planar ) {
			worldPts = createRandomPlane(rand, 3, N);
		} else {
			worldPts = GeoTestingOps.randomPoints_F64(-1, 1, -1, 1, 2, 3, N, rand);
		}

		cameraLeftPts = new ArrayList<>();
		cameraRightPts = new ArrayList<>();

		// transform points into second camera's reference frame
		pointPose = new ArrayList<>();
		for(Point3D_F64 p1 : worldPts ) {
			Point3D_F64 leftPt = SePointOps_F64.transform(worldToLeft, p1, null);
			Point3D_F64 rightPt = SePointOps_F64.transform(leftToRight, leftPt, null);

			Point2D_F64 leftObs = new Point2D_F64(leftPt.x/leftPt.z,leftPt.y/leftPt.z);
			Point2D_F64 rightObs = new Point2D_F64(rightPt.x/rightPt.z,rightPt.y/rightPt.z);

			pointPose.add( new Stereo2D3D(leftObs,rightObs,p1));

			cameraLeftPts.add(leftPt);
			cameraRightPts.add(rightPt);
		}
	}

	public void addNoise( double sigma ) {
		for( Stereo2D3D o : pointPose ) {
			o.leftObs.x += rand.nextGaussian()*sigma;
			o.leftObs.y += rand.nextGaussian()*sigma;
			o.rightObs.x += rand.nextGaussian()*sigma;
			o.rightObs.y += rand.nextGaussian()*sigma;
		}
	}

	/**
	 * Creates a set of random points along the (X,Y) plane
	 */
	public static List<Point3D_F64> createRandomPlane( Random rand , double d , int N )
	{
		List<Point3D_F64> ret = new ArrayList<>();

		for( int i = 0; i < N; i++ ) {
			double x = (rand.nextDouble()-0.5)*2;
			double y = (rand.nextDouble()-0.5)*2;

			ret.add( new Point3D_F64(x,y,d));
		}

		return ret;
	}
}
