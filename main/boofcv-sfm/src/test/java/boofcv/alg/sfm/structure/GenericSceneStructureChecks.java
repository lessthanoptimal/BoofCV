/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.sfm.structure;

import boofcv.struct.calib.CameraPinhole;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static boofcv.alg.geo.PerspectiveOps.convertNormToPixel;

/**
 * @author Peter Abeles
 */
public class GenericSceneStructureChecks {
	CameraPinhole intrinsic = new CameraPinhole(400,400,0,450,450,900,900);

	Random rand = new Random(234);
	List<Point3D_F64> pointsWorld = new ArrayList<>();
	List<Se3_F64> listCameraToWorld = new ArrayList<>();

	public void createWorld( double radius , double centerZ ) {
		pointsWorld.clear();
		listCameraToWorld.clear();

		for (int i = 0; i < 300; i++) {
			pointsWorld.add( new Point3D_F64(rand.nextGaussian()*radius,rand.nextGaussian()*radius,rand.nextGaussian()*radius+centerZ));
		}

		for (int i = 0; i < 10; i++) {
			double theta = Math.PI*i/20.0;

			Se3_F64 cameraToWorld = new Se3_F64();

			cameraToWorld.T.x = Math.sin(theta)*centerZ;
			cameraToWorld.T.z = centerZ - Math.cos(theta)*centerZ;

			ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,0,theta,0,cameraToWorld.R);
			listCameraToWorld.add(cameraToWorld);
		}
	}

	/**
	 * Finds a set of points which is viewed by all the cameras listed
	 */
	public void findViewable( int[] cameras , List<Point3D_F64> viewable ) {

		Point3D_F64 c = new Point3D_F64();
		Point2D_F64 p = new Point2D_F64();
		for (int i = 0; i < pointsWorld.size(); i++) {
			Point3D_F64 w = pointsWorld.get(i);

			boolean viewedAll = true;

			for (int j = 0; j < cameras.length; j++) {
				Se3_F64 cameraToWorld =listCameraToWorld.get(cameras[j]);

				SePointOps_F64.transformReverse(cameraToWorld,w,c);
				if( c.z <= 0 ) {
					viewedAll = false;
					break;
				}
				convertNormToPixel(intrinsic,c.x/c.z,c.y/c.z,p);

				if( p.x < 0 || p.y < 0 || p.x >= intrinsic.width-1 || p.y >= intrinsic.height-1 ) {
					viewedAll = false;
					break;
				}
			}

			if( viewedAll ) {
				viewable.add(w);
			}
		}
	}

	public Se3_F64 cameraAtoB( int a , int b ) {
		Se3_F64 a_to_w = listCameraToWorld.get(a);
		Se3_F64 b_to_w = listCameraToWorld.get(b);

		return a_to_w.concat(b_to_w.invert(null),null);
	}

	public void renderObservations( int camera , boolean pixels ,
									List<Point3D_F64> worldPoints,
									List<Point2D_F64> observations ) {
		Se3_F64 cameraToWorld =listCameraToWorld.get(camera);

		Point3D_F64 c = new Point3D_F64();
		for (int i = 0; i < worldPoints.size(); i++) {
			SePointOps_F64.transformReverse(cameraToWorld,worldPoints.get(i),c);

			if( pixels ) {
				observations.add( convertNormToPixel(intrinsic,c.x/c.z,c.y/c.z,null) );
			} else {
				observations.add( new Point2D_F64(c.x/c.z,c.y/c.z));
			}
		}

	}
}
