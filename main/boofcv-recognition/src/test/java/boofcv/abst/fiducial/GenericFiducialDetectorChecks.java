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

package boofcv.abst.fiducial;

import boofcv.BoofTesting;
import boofcv.abst.distort.FDistort;
import boofcv.alg.distort.brown.LensDistortionBrown;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.WorldToCameraToPixel;
import boofcv.core.image.GConvertImage;
import boofcv.simulation.SimulatePlanarWorld;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.metric.Intersection2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import georegression.struct.shapes.Polygon2D_F64;
import org.ejml.UtilEjml;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static georegression.struct.se.SpecialEuclideanOps_F64.eulerXyz;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class GenericFiducialDetectorChecks extends BoofStandardJUnit {

	// tolerance for difference between pixel location and geometric center from reprojection
	protected double pixelAndProjectedTol = 3.0;

	// true if it supports the getBounds() function
	protected boolean supportsBounds = true;

	protected List<ImageType> types = new ArrayList<>();

	Se3_F64 markerToWorld = eulerXyz(-0.2,0,1.2,0.1,Math.PI,0,null);

	protected double stabilityShrink = 0.2;
	protected double tolAccuracyT = 0.015;
	protected double tolAccuracyTheta = 0.002;

	/**
	 * Renders everything in gray scale first then converts it
	 * @param intrinsic camera model
	 * @param imageType image type for output
	 * @return rendered image
	 */
	public ImageBase renderImage(CameraPinholeBrown intrinsic  , ImageType imageType) {
		return renderImage(intrinsic, markerToWorld, imageType);
	}
	public ImageBase renderImage(CameraPinholeBrown intrinsic  , Se3_F64 markerToWorld, ImageType imageType) {

		SimulatePlanarWorld simulator = new SimulatePlanarWorld();
		simulator.setCamera(intrinsic);
		simulator.setBackground(255);

		simulator.addSurface(markerToWorld,1.5,renderFiducial());

		GrayF32 rendered = simulator.render();

		if( rendered.getImageType().isSameType(imageType)) {
			return rendered;
		}

		switch( imageType.getFamily() ) {
			case GRAY: {
				ImageBase output = imageType.createImage(rendered.width,rendered.height);
				GConvertImage.convert(rendered,output);
				return output;
			}
			default: break;
		}

		throw new RuntimeException("Currently only gray scale images supported");
	}

	public CameraPinholeBrown loadDistortion(boolean distorted ) {
		if( distorted ) {
			return new CameraPinholeBrown(250,250,0,250,250,500,500).
					fsetRadial(-0.1,-0.0005);
		} else {
			return new CameraPinholeBrown(250,250,0,250,250,500,500);
		}
	}

	public abstract<T extends ImageBase<T>> FiducialDetector<T> createDetector( ImageType<T> imageType );

	public abstract GrayF32 renderFiducial();

	/**
	 * Tests several items:
	 *
	 * 1) Does it properly handle setIntrinsic() being called multiple times
	 * 2) Can it handle no distortion
	 */
	@Test void checkHandleNewIntrinsic() {
		for( ImageType type : types ) {

			// distorted camera model
			CameraPinholeBrown instrinsic = loadDistortion(true);
			LensDistortionBrown distortion = new LensDistortionBrown(instrinsic);

			// render a distorted image
			ImageBase image = renderImage(instrinsic,type);

			// give it an undistored model
			FiducialDetector detector = createDetector(type);
			assertFalse(detector.is3D());
			detector.setLensDistortion(new LensDistortionBrown(loadDistortion(false)),
					image.width,image.height);
			assertTrue(detector.is3D());
			detect(detector,image);

			// it might not be able to detect the target
			if( detector.totalFound() >= 1 ) {
				checkBounds(detector);
			}

			// Give it the correct model and this time it should work
			detector.setLensDistortion(distortion,image.width,image.height);
			assertTrue(detector.is3D());
			detect(detector,image);
			checkBounds(detector);

			assertTrue(detector.totalFound()>=1);

			// Now remove the distortion model
			detector.setLensDistortion(null,image.width,image.height);
			assertFalse(detector.is3D());
			detect(detector,image);
			if( detector.totalFound() >= 1 ) {
				checkBounds(detector);
			}
		}
	}

	/**
	 * Give it an undistorted image. See if it can detect the target. Now give it an distorted image with
	 * lens parameters and see if it produces the same solution
	 */
	@Test void checkPoseWithAndWithOutDistortion() {
		LensDistortionBrown lensDistorted = new LensDistortionBrown(loadDistortion(true));
		LensDistortionBrown lensUndistorted = new LensDistortionBrown(loadDistortion(false));

		for( ImageType type : types ) {

			// render an undistorted image
			ImageBase imageUn = renderImage(loadDistortion(false),type);

//			ShowImages.showWindow(imageUn,"adsasdf");
//			BoofMiscOps.sleep(10000);

			FiducialDetector detector = createDetector(type);
			detector.setLensDistortion(lensUndistorted,imageUn.width,imageUn.height);
			detect(detector,imageUn);

			assertTrue(detector.totalFound()>=1);
			Results results = extractResults(detector);

			// feed it a distorted with and give the detector the undistortion model
			ImageBase imageD = renderImage(loadDistortion(true),type);
			detector.setLensDistortion(lensDistorted,imageD.width,imageD.height);
			detect(detector,imageD);

			// see if the results are the same
			assertEquals(results.id.length,detector.totalFound());
			for (int i = 0; i < detector.totalFound(); i++) {
				assertEquals(results.id[i],detector.getId(i));
				Se3_F64 pose = new Se3_F64();
				detector.getFiducialToCamera(i, pose);

				// make the error relative to the translation
				double t = pose.getT().norm();
				assertEquals(0,pose.getT().distance(results.pose.get(i).T),t*0.01);
				assertTrue(MatrixFeatures_DDRM.isIdentical(pose.getR(),results.pose.get(i).R,0.01));
			}
		}
	}

	@Test void checkPoseAccuracy() {

		// this has to do with the marker coordinate system to image coordinate system
		// marker is +y up   +x right +z up
		// image is  +y down +x right +z out
		Se3_F64 adjustment = SpecialEuclideanOps_F64.eulerXyz(0,0,0,0,0,Math.PI,null);

		for( boolean distorted : new boolean[]{false,true}) {
//			System.out.println("distorted = "+distorted);
			LensDistortionBrown lensDistorted = new LensDistortionBrown(loadDistortion(distorted));

			for (ImageType type : types) {
				FiducialDetector detector = createDetector(type);

				ImageBase imageD = renderImage(loadDistortion(distorted), type);
				detector.setLensDistortion(lensDistorted, imageD.width, imageD.height);
				detect(detector,imageD);

//				ShowImages.showBlocking(imageD,"Distorted", 2_000);

				assertEquals(1, detector.totalFound());

				Se3_F64 pose = new Se3_F64();
				detector.getFiducialToCamera(0, pose);

				pose.T.scale(markerToWorld.T.norm()/pose.T.norm());
				Se3_F64 diff = adjustment.concat(markerToWorld.concat(pose.invert(null),null),null);
				double theta = ConvertRotation3D_F64.matrixToRodrigues(diff.R, null).theta;
//				System.out.println("norm = "+diff.T.norm()+"  theta = "+theta);

				// threshold selected through manual trial and error
				assertEquals(0,diff.T.norm(), tolAccuracyT);
				assertEquals(0,theta, tolAccuracyTheta);
			}
		}
	}

	/**
	 * Makes sure the input is not modified
	 */
	@Test void modifyInput() {
		CameraPinholeBrown intrinsic = loadDistortion(true);
		LensDistortionBrown lensDistorted = new LensDistortionBrown(intrinsic);
		for( ImageType type : types ) {

			ImageBase image = renderImage(intrinsic,type);
			ImageBase orig = image.clone();
			FiducialDetector detector = createDetector(type);

			detector.setLensDistortion(lensDistorted,image.width,image.height);

			detect(detector,image);

			BoofTesting.assertEquals(image,orig,0);
		}
	}

	@Test void checkMultipleRuns() {
		CameraPinholeBrown intrinsic = loadDistortion(true);
		LensDistortionBrown lensDistorted = new LensDistortionBrown(intrinsic);
		for( ImageType type : types ) {

			ImageBase image = renderImage(intrinsic,type);
			FiducialDetector detector = createDetector(type);

			detector.setLensDistortion(lensDistorted,image.width,image.height);

			detect(detector,image);

			assertTrue(detector.totalFound()>= 1);

			Results results = extractResults(detector);

			// run it again
			detect(detector,image);

			// see if it produced exactly the same results
			assertEquals(results.id.length,detector.totalFound());
			for (int i = 0; i < detector.totalFound(); i++) {
				assertEquals(results.id[i],detector.getId(i));

				Point2D_F64 centerPixel = new Point2D_F64();
				detector.getCenter(i,centerPixel);
				assertEquals(0.0, results.centerPixel.get(i).distance(centerPixel), 1e-8);

				Se3_F64 pose = new Se3_F64();
				detector.getFiducialToCamera(i, pose);
				assertEquals(0,pose.getT().distance(results.pose.get(i).T),1e-8);
				assertTrue(MatrixFeatures_DDRM.isIdentical(pose.getR(),results.pose.get(i).R,1e-8));
			}
		}
	}

	@Test void checkSubImage() {
		CameraPinholeBrown intrinsic = loadDistortion(true);
		LensDistortionBrown lensDistorted = new LensDistortionBrown(intrinsic);
		for( ImageType type : types ) {

			ImageBase image = renderImage(intrinsic,type);
			FiducialDetector detector = createDetector(type);

			detector.setLensDistortion(lensDistorted,image.width,image.height);

			detect(detector,image);

			assertTrue(detector.totalFound()>= 1);

			long[] foundID = new long[ detector.totalFound() ];
			List<Se3_F64> foundPose = new ArrayList<>();

			for (int i = 0; i < detector.totalFound(); i++) {
				foundID[i] = detector.getId(i);
				Se3_F64 pose = new Se3_F64();
				detector.getFiducialToCamera(i, pose);
				foundPose.add(pose);
			}

			// run it again with a sub-image
			detect(detector,BoofTesting.createSubImageOf(image));

			// see if it produced exactly the same results
			assertEquals(foundID.length,detector.totalFound());
			for (int i = 0; i < detector.totalFound(); i++) {
				assertEquals(foundID[i],detector.getId(i));
				Se3_F64 pose = new Se3_F64();
				detector.getFiducialToCamera(i, pose);
				assertEquals(0,pose.getT().distance(foundPose.get(i).T),1e-8);
				assertTrue(MatrixFeatures_DDRM.isIdentical(pose.getR(),foundPose.get(i).R,1e-8));
			}
		}
	}

	/**
	 * See if the stability estimation is reasonable. First detect targets in the full sized image. Then shrink it
	 * by 15% and see if the instability increases. The instability should always increase for smaller objects with
	 * the same orientation since the geometry is worse.
	 */
	@Test void checkStability() {
		// has to be undistorted otherwise rescaling the image won't work
		CameraPinholeBrown intrinsic = loadDistortion(false);
		LensDistortionBrown lensDistorted = new LensDistortionBrown(intrinsic);
		for( ImageType type : types ) {

			ImageBase image = renderImage(intrinsic,type);
			FiducialDetector detector = createDetector(type);

			detector.setLensDistortion(lensDistorted,image.width,image.height);

//			ShowImages.showBlocking(image,"First Detect", 3_000);

			detect(detector,image);
			assertTrue(detector.totalFound() >= 1);

			long[] foundIds      = new long[ detector.totalFound() ];
			double[] location    = new double[ detector.totalFound() ];
			double[] orientation = new double[ detector.totalFound() ];

			FiducialStability results = new FiducialStability();
			for (int i = 0; i < detector.totalFound(); i++) {
				detector.computeStability(i,0.2,results);

				foundIds[i] = detector.getId(i);
				location[i] = results.location;
				orientation[i] = results.orientation;
			}

			// by shrinking the image a small pixel error should result
			// in a larger pose error, hence more unstable
			ImageBase shrunk = image.createSameShape();
			new FDistort(image,shrunk).affine(stabilityShrink,0,0,stabilityShrink,image.width/4,image.height/4).apply();

//			ShowImages.showBlocking(shrunk,"Shrunk", 2_000);

			detect(detector,shrunk);

			assertEquals(detector.totalFound(), foundIds.length);

			for (int i = 0; i < detector.totalFound(); i++) {
				detector.computeStability(i,0.2,results);

				long id = detector.getId(i);

				boolean matched = false;
				for (int j = 0; j < foundIds.length; j++) {
					if( foundIds[j] == id ) {
						matched = true;
						assertTrue(location[j] < results.location);
						assertTrue(orientation[j] < results.orientation);
						break;
					}
				}
				assertTrue(matched);
			}
		}
	}

	@Test void checkCenter() {
		// It's not specified if the center should be undistorted or distorted. Just make it easier by
		// using undistorted
		CameraPinholeBrown intrinsic = loadDistortion(false);
		LensDistortionBrown lensDistorted = new LensDistortionBrown(intrinsic);
		for( ImageType type : types ) {
			ImageBase image = renderImage(intrinsic,type);
			FiducialDetector detector = createDetector(type);
			detector.setLensDistortion(lensDistorted,image.width,image.height);

//			ShowImages.showWindow(image,"asdfasdf");
//			BoofMiscOps.sleep(10_000);

			detect(detector,image);

			assertTrue(detector.totalFound() >= 1);
			assertTrue(detector.is3D());

			for (int i = 0; i < detector.totalFound(); i++) {
				Se3_F64 fidToCam = new Se3_F64();
				Point2D_F64 found = new Point2D_F64();
				detector.getFiducialToCamera(i, fidToCam);
				detector.getCenter(i, found);

				Point2D_F64 rendered = new Point2D_F64();
				WorldToCameraToPixel worldToPixel = PerspectiveOps.createWorldToPixel(lensDistorted, fidToCam);
				worldToPixel.transform(new Point3D_F64(0,0,0),rendered);

				// see if the reprojected is near the pixel location
				assertEquals( 0.0, rendered.distance(found), pixelAndProjectedTol);
			}
		}
	}

	/**
	 * Makes sure that if it hasn't been provided intrinsic it can't support pose
	 */
	@Test void is3dWithNoIntrinsic() {
		FiducialDetector detector = createDetector(types.get(0));

		assertFalse( detector.is3D() );
	}

	public void checkBounds(FiducialDetector detector) {
		if( !supportsBounds )
			return;

		Polygon2D_F64 queue = new Polygon2D_F64();
		for (int i = 0; i < detector.totalFound(); i++) {

			// make sure it handles null correctly
			Polygon2D_F64 listA = detector.getBounds(i,null);
			Polygon2D_F64 listB = detector.getBounds(i,queue);

			assertSame(listB, queue);
			assertEquals(listA.size(),listB.size());

			Polygon2D_F64 polygon = new Polygon2D_F64(listA.size());
			for (int j = 0; j < listA.size(); j++) {
				Point2D_F64 pa = listA.get(j);
				Point2D_F64 pb = listB.get(j);

				assertEquals(pa.x, pb.x);
				assertEquals(pa.y, pb.y);

				// very simple sanity check on the results
				assertFalse(UtilEjml.isUncountable(pa.x));
				assertFalse(UtilEjml.isUncountable(pa.y));

				polygon.get(j).setTo(pa);
			}

			// in almost all cases the center should be inside
			Point2D_F64 center = new Point2D_F64();
			detector.getCenter(i,center);
			Intersection2D_F64.containsConvex(polygon,center);
		}
	}

	private Results extractResults( FiducialDetector detector ) {
		Results out = new Results(detector.totalFound());

		for (int i = 0; i < detector.totalFound(); i++) {
			Se3_F64 pose = new Se3_F64();
			Point2D_F64 centerPixel = new Point2D_F64();
			detector.getFiducialToCamera(i, pose);
			detector.getCenter(i, centerPixel);

			out.id[i] = detector.getId(i);
			out.pose.add(pose);
			out.centerPixel.add(centerPixel);
		}

		return out;
	}

	/**
	 * Function for calling detect. Primarily used to enable the FiducialTracker tests to recycle code
	 */
	protected void detect( FiducialDetector detector , ImageBase image ) {
		detector.detect(image);
	}

	private static class Results {
		public long[] id;
		public List<Se3_F64> pose = new ArrayList<>();
		public List<Point2D_F64> centerPixel = new ArrayList<>();

		public Results( int N ) {
			id = new long[ N ];
		}
	}
}
