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

package boofcv.abst.fiducial;

import boofcv.abst.distort.FDistort;
import boofcv.alg.distort.LensDistortionNarrowFOV;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.WorldToCameraToPixel;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofTesting;
import georegression.metric.Intersection2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.shapes.Polygon2D_F64;
import org.ejml.UtilEjml;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public abstract class GenericFiducialDetectorChecks {

	// tolerance for difference between pixel location and geometric center from reprojection
	protected double pixelAndProjectedTol = 3.0;

	// true if it supports the getBounds() function
	protected boolean supportsBounds = true;

	protected List<ImageType> types = new ArrayList<>();

	public abstract ImageBase loadImage(ImageType imageType);

	public abstract LensDistortionNarrowFOV loadDistortion(boolean distorted );

	public abstract FiducialDetector createDetector( ImageType imageType );

	/**
	 * Tests several items:
	 *
	 * 1) Does it properly handle setIntrinsic() being called multiple times
	 * 2) Can it handle no distortion
	 */
	@Test
	public void checkHandleNewIntrinsic() {
		for( ImageType type : types ) {

			ImageBase image = loadImage(type);
			FiducialDetector detector = createDetector(type);

			detector.setLensDistortion(loadDistortion(true),image.width,image.height);

			detector.detect(image);

			assertTrue(detector.totalFound()>= 1);
			checkBounds(detector);

			Results results = extractResults(detector);

			// run it again with a changed intrinsic that's incorrect
			detector.setLensDistortion(loadDistortion(false),image.width,image.height);
			detector.detect(image);
			checkBounds(detector);

			// results should have changed
			if( results.id.length == detector.totalFound()) {
				assertEquals(results.id.length, detector.totalFound());
				for (int i = 0; i < detector.totalFound(); i++) {
					assertEquals(results.id[i],detector.getId(i));
					Se3_F64 pose = new Se3_F64();
					Point2D_F64 pixel = new Point2D_F64();
					detector.getFiducialToCamera(i, pose);
					detector.getCenter(i, pixel);
					assertTrue(pose.getT().distance(results.pose.get(i).T) > 1e-4);
					assertFalse(MatrixFeatures_DDRM.isIdentical(pose.getR(), results.pose.get(i).R, 1e-4));
					// pixel location is based on the observed location, thus changing the intrinsics should not
					// affect it
					assertTrue(results.pixel.get(i).distance(pixel) <= 2.0 );
				}
			} else {
				// clearly changed
			}

			// then reproduce original
			detector.setLensDistortion(loadDistortion(true),image.width,image.height);
			detector.detect(image);

			// see if it produced exactly the same results
			assertEquals(results.id.length,detector.totalFound());
			for (int i = 0; i < detector.totalFound(); i++) {
				assertEquals(results.id[i],detector.getId(i));
				Se3_F64 pose = new Se3_F64();
				detector.getFiducialToCamera(i, pose);
				assertEquals(0,pose.getT().distance(results.pose.get(i).T),1e-8);
				assertTrue(MatrixFeatures_DDRM.isIdentical(pose.getR(),results.pose.get(i).R,1e-8));
			}
		}
	}

	/**
	 * Provide an intrinsic model then remove it
	 */
	@Test
	public void checkRemoveIntrinsic() {
		for( ImageType type : types ) {
			ImageBase image = loadImage(type);

			// detect with no intrinsics
			FiducialDetector detector = createDetector(type);
			detector.detect(image);

			assertFalse(detector.is3D());
			assertTrue(detector.totalFound() >= 1);
			Results expected = extractResults(detector);
			checkBounds(detector);

			// detect with intrinsics
			detector.setLensDistortion(loadDistortion(true),image.width,image.height);
			assertTrue(detector.is3D());
			assertTrue(detector.totalFound() >= 1);

			// detect without intrinsics again
			detector.setLensDistortion(null,0,0);
			assertFalse(detector.is3D());
			assertTrue(detector.totalFound() >= 1);
			Results found = extractResults(detector);

			// compare results
			assertEquals(expected.id.length, found.id.length);
			for (int i = 0; i < expected.id.length; i++) {
				assertEquals(expected.id[i],found.id[i]);
				assertTrue(expected.pose.get(i).T.distance(found.pose.get(i).T) <= 1e-4);
				assertTrue(MatrixFeatures_DDRM.isIdentical(expected.pose.get(i).getR(), found.pose.get(i).R, 1e-4));
				assertTrue(found.pixel.get(i).distance(expected.pixel.get(i)) <= 1e-4 );
			}
		}
	}

	/**
	 * Makes sure the input is not modified
	 */
	@Test
	public void modifyInput() {
		for( ImageType type : types ) {

			ImageBase image = loadImage(type);
			ImageBase orig = image.clone();
			FiducialDetector detector = createDetector(type);

			detector.setLensDistortion(loadDistortion(true),image.width,image.height);

			detector.detect(image);

			BoofTesting.assertEquals(image,orig,0);
		}
	}

	@Test
	public void checkMultipleRuns() {

		for( ImageType type : types ) {

			ImageBase image = loadImage(type);
			FiducialDetector detector = createDetector(type);

			detector.setLensDistortion(loadDistortion(true),image.width,image.height);

			detector.detect(image);

			assertTrue(detector.totalFound()>= 1);

			Results results = extractResults(detector);

			// run it again
			detector.detect(image);

			// see if it produced exactly the same results
			assertEquals(results.id.length,detector.totalFound());
			for (int i = 0; i < detector.totalFound(); i++) {
				assertEquals(results.id[i],detector.getId(i));
				Se3_F64 pose = new Se3_F64();
				detector.getFiducialToCamera(i, pose);
				assertEquals(0,pose.getT().distance(results.pose.get(i).T),1e-8);
				assertTrue(MatrixFeatures_DDRM.isIdentical(pose.getR(),results.pose.get(i).R,1e-8));
			}
		}
	}

	@Test
	public void checkSubImage() {

		for( ImageType type : types ) {

			ImageBase image = loadImage(type);
			FiducialDetector detector = createDetector(type);

			detector.setLensDistortion(loadDistortion(true),image.width,image.height);

			detector.detect(image);

			assertTrue(detector.totalFound()>= 1);

			long foundID[] = new long[ detector.totalFound() ];
			List<Se3_F64> foundPose = new ArrayList<>();

			for (int i = 0; i < detector.totalFound(); i++) {
				foundID[i] = detector.getId(i);
				Se3_F64 pose = new Se3_F64();
				detector.getFiducialToCamera(i, pose);
				foundPose.add(pose);
			}

			// run it again with a sub-image
			detector.detect(BoofTesting.createSubImageOf(image));

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
	 * See if the stability estimation is reasonable.  First detect targets in the full sized image.  Then shrink it
	 * by 15% and see if the instability increases.  The instability should always increase for smaller objects with
	 * the same orientation since the geometry is worse.
	 */
	@Test
	public void checkStability() {

		for( ImageType type : types ) {

			ImageBase image = loadImage(type);
			FiducialDetector detector = createDetector(type);

			detector.setLensDistortion(loadDistortion(true),image.width,image.height);

			detector.detect(image);
			assertTrue(detector.totalFound() >= 1);

			long foundIds[] = new long[ detector.totalFound() ];
			double location[] = new double[ detector.totalFound() ];
			double orientation[] = new double[ detector.totalFound() ];

			FiducialStability results = new FiducialStability();
			for (int i = 0; i < detector.totalFound(); i++) {
				detector.computeStability(i,0.2,results);

				foundIds[i] = detector.getId(i);
				location[i] = results.location;
				orientation[i] = results.orientation;
			}

			ImageBase shrunk = image.createSameShape();
			new FDistort(image,shrunk).affine(0.8,0,0,0.8,0,0).apply();

			detector.detect(shrunk);


			assertTrue(detector.totalFound() == foundIds.length);

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

	@Test
	public void checkImageLocation() {
		for( ImageType type : types ) {

			ImageBase image = loadImage(type);
			FiducialDetector detector = createDetector(type);

			// It's not specified if the center should be undistorted or distorted. Just make it easier by
			// using undistorted
			LensDistortionNarrowFOV distortion = loadDistortion(false);
			detector.setLensDistortion(distortion,image.width,image.height);

			detector.detect(image);

			assertTrue(detector.totalFound() >= 1);
			assertTrue(detector.is3D());

			for (int i = 0; i < detector.totalFound(); i++) {
				Se3_F64 fidToCam = new Se3_F64();
				Point2D_F64 found = new Point2D_F64();
				detector.getFiducialToCamera(i, fidToCam);
				detector.getCenter(i, found);

				Point2D_F64 rendered = new Point2D_F64();
				WorldToCameraToPixel worldToPixel = PerspectiveOps.createWorldToPixel(distortion, fidToCam);
				worldToPixel.transform(new Point3D_F64(0,0,0),rendered);

				// see if the reprojected is near the pixel location
				assertTrue( rendered.distance(found) <= pixelAndProjectedTol);
			}
		}
	}

	/**
	 * Makes sure that if it hasn't been provided intrinsic it can't support pose
	 */
	@Test
	public void is3dWithNoIntrinsic() {
		FiducialDetector detector = createDetector(types.get(0));

		assertFalse( detector.is3D() );
	}

	/**
	 * Make sure lens distortion is removed if it was set previously and then removed
	 */
	@Test // TODO remove test?  This should be a non-issue now
	public void clearLensDistortion() {
		for( ImageType type : types ) {

			ImageBase image = loadImage(type);
			FiducialDetector detector = createDetector(type);

			// save the results
			detector.setLensDistortion(loadDistortion(false),image.width,image.height);
			detector.detect(image);
			assertTrue(detector.totalFound() >= 1);

			Results before = extractResults(detector);

			// run with lens distortion
			detector.setLensDistortion(loadDistortion(true),image.width,image.height);
			detector.detect(image);

			// remove lens distortion
			detector.setLensDistortion(loadDistortion(false),image.width,image.height);
			detector.detect(image);

			Results after = extractResults(detector);

			// see if it's the same
			for (int i = 0; i < after.id.length; i++) {
				assertEquals(before.id[i], after.id[i]);
				assertEquals(0,before.pose.get(i).T.distance(after.pose.get(i).T),1e-8);
				assertTrue(MatrixFeatures_DDRM.isIdentical(before.pose.get(i).R,after.pose.get(i).R,1e-8));
				assertEquals(0,before.pixel.get(i).distance(after.pixel.get(i)),1e-8);
			}
		}
	}

	public void checkBounds(FiducialDetector detector) {
		if( !supportsBounds )
			return;

		Polygon2D_F64 queue = new Polygon2D_F64();
		for (int i = 0; i < detector.totalFound(); i++) {

			// make sure it handles null correctly
			Polygon2D_F64 listA = detector.getBounds(i,null);
			Polygon2D_F64 listB = detector.getBounds(i,queue);

			assertTrue(listB == queue);
			assertEquals(listA.size(),listB.size());

			Polygon2D_F64 polygon = new Polygon2D_F64(listA.size());
			for (int j = 0; j < listA.size(); j++) {
				Point2D_F64 pa = listA.get(j);
				Point2D_F64 pb = listB.get(j);

				assertTrue(pa.x == pb.x);
				assertTrue(pa.y == pb.y);

				// very simple sanity check on the results
				assertFalse(UtilEjml.isUncountable(pa.x));
				assertFalse(UtilEjml.isUncountable(pa.y));

				polygon.get(j).set(pa);
			}

			// in almost all cases the center should be inside
			Point2D_F64 center = new Point2D_F64();
			detector.getCenter(i,center);
			Intersection2D_F64.containConvex(polygon,center);
		}
	}

	private Results extractResults( FiducialDetector detector ) {
		Results out = new Results(detector.totalFound());

		for (int i = 0; i < detector.totalFound(); i++) {
			Se3_F64 pose = new Se3_F64();
			Point2D_F64 pixel = new Point2D_F64();
			detector.getFiducialToCamera(i, pose);
			detector.getCenter(i, pixel);

			out.id[i] = detector.getId(i);
			out.pose.add(pose);
			out.pixel.add(pixel);
		}

		return out;
	}

	private static class Results {
		public long id[];
		public List<Se3_F64> pose = new ArrayList<>();
		public List<Point2D_F64> pixel = new ArrayList<>();

		public Results( int N ) {
			id = new long[ N ];
			pose = new ArrayList<>();
			pixel = new ArrayList<>();
		}
	}
}
