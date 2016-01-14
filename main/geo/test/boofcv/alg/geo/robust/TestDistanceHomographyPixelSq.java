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

package boofcv.alg.geo.robust;


import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.struct.geo.AssociatedPair;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.homography.UtilHomography;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.transform.homography.HomographyPointOps_F64;
import org.ddogleg.fitting.modelset.DistanceFromModel;
import org.ejml.data.DenseMatrix64F;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class TestDistanceHomographyPixelSq extends StandardDistanceTest<Homography2D_F64, AssociatedPair> {

	Random rand = new Random(234);

	DenseMatrix64F K = new DenseMatrix64F(3,3,true,200,1,120,0,250,150,0,0,1);

	DenseMatrix64F H,H_pixel;

	@Override
	public DistanceFromModel<Homography2D_F64, AssociatedPair> create() {
		DistanceHomographyPixelSq alg = new DistanceHomographyPixelSq();
		alg.setIntrinsic(K.get(0,0),K.get(1,1),K.get(0,1));
		return alg;
	}

	@Override
	public Homography2D_F64 createRandomModel() {

		double rotX = rand.nextGaussian();
		double rotY = rand.nextGaussian();
		double rotZ = rand.nextGaussian();


		DenseMatrix64F R = ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,rotX,rotY,rotZ, null);
		Vector3D_F64 T = new Vector3D_F64(0.2,-0.5,3);
		Vector3D_F64 N = new Vector3D_F64(-0.5,1,3);

		// compute the Homography in normalized image coordinates and pixel coordinates
		H = MultiViewOps.createHomography(R, T, 1.0, N);
		H_pixel = MultiViewOps.createHomography(R,T,1.0,N,K);

		Homography2D_F64 h = new Homography2D_F64();
		UtilHomography.convert(H, h);

		return h;
	}

	@Override
	public AssociatedPair createRandomData() {
		Point2D_F64 p1 = new Point2D_F64(rand.nextGaussian(),rand.nextGaussian());
		Point2D_F64 p2 = new Point2D_F64(rand.nextGaussian(),rand.nextGaussian());

		return new AssociatedPair(p1,p2,false);
	}

	@Override
	public double distance(Homography2D_F64 h, AssociatedPair associatedPair) {

		Point2D_F64 result = new Point2D_F64();

		Point2D_F64 pixel1 = new Point2D_F64();
		Point2D_F64 pixel2 = new Point2D_F64();

		// convert points into pixel coordinates
		PerspectiveOps.convertNormToPixel(K,associatedPair.p1,pixel1);
		PerspectiveOps.convertNormToPixel(K,associatedPair.p2,pixel2);

		// compute error in pixels, which is what it should be in
		Homography2D_F64 h_pixel = new Homography2D_F64();
		UtilHomography.convert(H_pixel,h_pixel);

		HomographyPointOps_F64.transform(h_pixel, pixel1, result);
		return result.distance2(pixel2);
	}
}
