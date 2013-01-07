/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.sfm.robust;

import boofcv.alg.geo.GeoTestingOps;
import boofcv.alg.geo.h.CommonHomographyChecks;
import boofcv.alg.sfm.d3.Stereo2D3D;
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

	// list of points in world reference frame
	protected List<Point3D_F64> worldPts;
	// list of points is camera reference frame
	protected List<Point3D_F64> cameraLeftPts;
	protected List<Point3D_F64> cameraRightPts;
	// list of point pairs
	protected List<Stereo2D3D> pointPose;

	protected void generateScene(int N, Se3_F64 worldToLeft, boolean planar) {
		this.worldToLeft = worldToLeft;
		leftToRight = new Se3_F64();
		leftToRight.getT().set(-0.1,0,0);

		// randomly generate points in space
		if( planar ) {
			worldPts = CommonHomographyChecks.createRandomPlane(rand, 3, N);
		} else {
			worldPts = GeoTestingOps.randomPoints_F64(-1, 1, -1, 1, 2, 3, N, rand);
		}

		cameraLeftPts = new ArrayList<Point3D_F64>();
		cameraRightPts = new ArrayList<Point3D_F64>();

		// transform points into second camera's reference frame
		pointPose = new ArrayList<Stereo2D3D>();
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
}
