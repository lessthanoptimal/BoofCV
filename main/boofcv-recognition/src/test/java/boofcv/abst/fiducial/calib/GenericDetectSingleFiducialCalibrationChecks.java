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

package boofcv.abst.fiducial.calib;

import boofcv.abst.geo.calibration.DetectSingleFiducialCalibration;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.ShowImages;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofMiscOps;
import boofcv.simulation.SimulatePlanarWorld;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.calib.CameraUniversalOmni;
import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.struct.image.GrayF32;
import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public abstract class GenericDetectSingleFiducialCalibrationChecks extends BoofStandardJUnit {

	List<ConfigGridDimen> targetConfigs = new ArrayList<>();

	double fisheyeMatchTol = 3.0; // how close a pixel needs to come to be considered a match

	double simulatedTargetWidth = 0.3; // size of target in simulated world

	boolean visualizeFailures = true;
	long visualizeTime = 20000;

	int failedToDetect;
	double fisheyeAllowedFails = 0;

	// list of posses for fisheye test
	protected List<Se3_F64> fisheye_poses = new ArrayList<>();

	protected GenericDetectSingleFiducialCalibrationChecks() {
		createFisheyePoses();
	}

	protected void createFisheyePoses() {
		Se3_F64 markerToWorld = new Se3_F64();
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0, Math.PI + 0.2, 0, markerToWorld.R);

		// up close exploding - center
		markerToWorld.T.setTo(0, 0, 0.12);
		fisheye_poses.add(markerToWorld.copy());

		// up close exploding - left
		markerToWorld.T.setTo(0.1, 0, 0.12);
		fisheye_poses.add(markerToWorld.copy());

		markerToWorld.T.setTo(0.25, 0, 0.2);
		fisheye_poses.add(markerToWorld.copy());

		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0, Math.PI - 0.2, 0, markerToWorld.getR());
		fisheye_poses.add(markerToWorld.copy());

		markerToWorld.T.setTo(0.3, 0, 0.2);
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0, Math.PI + .5, 0, markerToWorld.getR());
		fisheye_poses.add(markerToWorld.copy());

		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0, Math.PI + .5, 0.5, markerToWorld.getR());
		fisheye_poses.add(markerToWorld.copy());

		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0, Math.PI + .5, 0.7, markerToWorld.getR());
		fisheye_poses.add(markerToWorld.copy());
	}

	/**
	 * Renders an image of the calibration target.
	 *
	 * @param config (Optional)
	 * @param image Storage for rendered calibration target This should be just the calibration target
	 */
	public abstract void renderTarget( ConfigGridDimen config,
									   double targetWidth,
									   GrayF32 image,
									   List<Point2D_F64> points2D );

	public abstract DetectSingleFiducialCalibration createDetector( ConfigGridDimen layout );

	/**
	 * See if it can detect targets distorted by fisheye lens. Entire target is always seen
	 */
	@Test void fisheye_fullview() {
		CameraUniversalOmni model = CalibrationIO.load(getClass().getResource("fisheye.yaml"));
		SimulatePlanarWorld simulator = new SimulatePlanarWorld();
		simulator.setCamera(model);

		List<Point2D_F64> locations2D = new ArrayList<>();
		GrayF32 pattern = new GrayF32(1, 1);
		for (int i = 0; i < targetConfigs.size(); i++) {
			DetectSingleFiducialCalibration detector = createDetector(targetConfigs.get(i));
			renderTarget(targetConfigs.get(i), simulatedTargetWidth, pattern, locations2D);

			simulator.resetScene();
			Se3_F64 markerToWorld = new Se3_F64();
			simulator.addSurface(markerToWorld, simulatedTargetWidth, pattern);

			failedToDetect = 0;
			for (int j = 0; j < fisheye_poses.size(); j++) {
//				System.out.println("fisheye pose = "+j);
				markerToWorld.setTo(fisheye_poses.get(j));
				checkRenderedResults(detector, simulator, locations2D);
			}
		}
		assertTrue(failedToDetect <= fisheyeAllowedFails);
	}

	private void checkRenderedResults( DetectSingleFiducialCalibration detector,
									   SimulatePlanarWorld simulator,
									   List<Point2D_F64> locations2D ) {
//		System.out.println("checkRendered");
		simulator.render();

//		visualize(simulator, locations2D, null);
		if (!detector.process(simulator.getOutput())) {
			visualize(simulator, locations2D, null);
			failedToDetect++;
			return;
		}

		checkResults(simulator, detector.getDetectedPoints(), locations2D);
	}

	protected void checkResults( SimulatePlanarWorld simulator, CalibrationObservation found, List<Point2D_F64> locations2D ) {
		if (locations2D.size() != found.size()) {
			visualize(simulator, locations2D, found);
			fail("Number of detected points miss match");
		}

		Point2D_F64 truth = new Point2D_F64();

		for (int i = 0; i < locations2D.size(); i++) {
			Point2D_F64 p = locations2D.get(i);
			simulator.computePixel(0, p.x, p.y, truth);

			// TODO ensure that the order is correct. Kinda a pain since some targets have symmetry...
			double bestDistance = Double.MAX_VALUE;
			for (int j = 0; j < found.size(); j++) {
				double distance = found.get(j).p.distance(truth);
				if (distance < bestDistance) {
					bestDistance = distance;
				}
			}
			if (bestDistance > fisheyeMatchTol) {
				visualize(simulator, locations2D, found);
				fail("Didn't find a match: best distance " + bestDistance);
			}
		}
	}

	protected void visualize( SimulatePlanarWorld simulator, List<Point2D_F64> locations2D, CalibrationObservation found ) {
		if (!visualizeFailures)
			return;
		GrayF32 output = simulator.getOutput();
		BufferedImage buff = new BufferedImage(output.width, output.height, BufferedImage.TYPE_INT_RGB);
		ConvertBufferedImage.convertTo(simulator.getOutput(), buff, true);

		UtilImageIO.saveImage(buff, "failed.png");

		Graphics2D g2 = buff.createGraphics();
		Point2D_F64 p, f;
		if (found != null) {
			for (int j = 0; j < found.size(); j++) {
				f = found.get(j).p;
				VisualizeFeatures.drawPoint(g2, f.x, f.y, 2, Color.RED, false);
			}
			g2.setStroke(new BasicStroke(5));
			f = found.get(0).p;
			VisualizeFeatures.drawCircle(g2, f.x, f.y, 4);
		}
		Point2D_F64 truth = new Point2D_F64();

		g2.setColor(Color.GREEN);
		g2.setStroke(new BasicStroke(2));
		for (int j = 0; j < locations2D.size(); j++) {
			p = locations2D.get(j);
			simulator.computePixel(0, p.x, p.y, truth);
			VisualizeFeatures.drawCircle(g2, truth.x, truth.y, 4);
		}
		p = locations2D.get(0);
		g2.setStroke(new BasicStroke(5));
		simulator.computePixel(0, p.x, p.y, truth);
		VisualizeFeatures.drawCircle(g2, truth.x, truth.y, 4);

		ShowImages.showWindow(buff, "Foo", true);
		BoofMiscOps.sleep(visualizeTime);
	}

	/**
	 * Simulated scene using a pinhole camera model with radial distortion. Entire target is visible
	 */
	@Test void pinhole_radial_fullview() {

		CameraPinholeBrown model = CalibrationIO.load(getClass().getResource("pinhole_radial.yaml"));
		SimulatePlanarWorld simulator = new SimulatePlanarWorld();
		simulator.setCamera(model);

		List<Point2D_F64> locations2D = new ArrayList<>();
		GrayF32 pattern = new GrayF32(1, 1);
		for (int i = 0; i < targetConfigs.size(); i++) {
//			System.out.println("*---------- Configuration "+i);
			failedToDetect = 0;
			DetectSingleFiducialCalibration detector = createDetector(targetConfigs.get(i));
			renderTarget(targetConfigs.get(i), simulatedTargetWidth, pattern, locations2D);

			simulator.resetScene();
			Se3_F64 markerToWorld = new Se3_F64();
			ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0, Math.PI, 0, markerToWorld.R);
			simulator.addSurface(markerToWorld, simulatedTargetWidth, pattern);

			// up close exploding - center
			markerToWorld.T.setTo(0, 0, 0.5);
			checkRenderedResults(detector, simulator, locations2D);

			// farther away centered
			markerToWorld.T.setTo(0, 0, 1);
			checkRenderedResults(detector, simulator, locations2D);

			markerToWorld.T.setTo(-0.33, 0, 1);
			checkRenderedResults(detector, simulator, locations2D);

			ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0, Math.PI - 1, 0, markerToWorld.getR());
			checkRenderedResults(detector, simulator, locations2D);

			ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0, Math.PI - 1, 0.8, markerToWorld.getR());
			checkRenderedResults(detector, simulator, locations2D);

			markerToWorld.T.setTo(-0.33, 0.33, 1);
			ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0, Math.PI - 1, 0.8, markerToWorld.getR());
			checkRenderedResults(detector, simulator, locations2D);

			markerToWorld.T.setTo(0, -0.20, 1);
			ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0.8, Math.PI, 0.8, markerToWorld.getR());
			checkRenderedResults(detector, simulator, locations2D);

			ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0.8, Math.PI, 1.8, markerToWorld.getR());
			checkRenderedResults(detector, simulator, locations2D);

			markerToWorld.T.setTo(0, -0.15, 1);
			ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0.2, Math.PI, 2.4, markerToWorld.getR());
			checkRenderedResults(detector, simulator, locations2D);
		}
		assertEquals(0, failedToDetect);
	}

	protected GrayF32 renderEasy( ConfigGridDimen layout, List<Point2D_F64> locations2D ) {
		CameraPinholeBrown model = CalibrationIO.load(getClass().getResource("pinhole_radial.yaml"));

		if (locations2D == null)
			locations2D = new ArrayList<>();
		GrayF32 pattern = new GrayF32(1, 1);
		renderTarget(layout, simulatedTargetWidth, pattern, locations2D);

		SimulatePlanarWorld simulator = new SimulatePlanarWorld();
		simulator.setCamera(model);

		Se3_F64 markerToWorld = SpecialEuclideanOps_F64.eulerXyz(0, 0, 0.5, 0, Math.PI, 0, null);
		simulator.addSurface(markerToWorld, simulatedTargetWidth, pattern);
		simulator.render();

		return simulator.getOutput();
	}

	/**
	 * Nothing was detected. make sure it doesn't return null.
	 */
	@Test void checkDetectionsNonnull() {
		for (ConfigGridDimen layout : targetConfigs) {
			DetectSingleFiducialCalibration detector = createDetector(layout);

			detector.process(new GrayF32(300, 400));

			assertNotNull(detector.getDetectedPoints());
			assertEquals(0, detector.getDetectedPoints().size());
		}
	}

	/**
	 * First call something was detected, second call nothing was detected. it should return an empty list
	 */
	@Test void checkDetectionsResetOnFailure() {
		DetectSingleFiducialCalibration detector = createDetector(targetConfigs.get(0));

		GrayF32 original = renderEasy(targetConfigs.get(0), null);

		detector.process(original);
		assertTrue(detector.getDetectedPoints().size() > 0);

		detector.process(new GrayF32(300, 400));

		assertNotNull(detector.getDetectedPoints());
		assertEquals(detector.getDetectedPoints().size(), 0);
	}

	/**
	 * Makes sure origin in the target's physical center. This is done by seeing that most extreme
	 * points are all equally distant. Can't use the mean since the target might not evenly distributed.
	 *
	 * Should this really be a requirement?  There is some mathematical justification for it and make sense
	 * when using it as a fiducial.
	 */
	@Test void targetIsCentered() {
		List<Point2D_F64> layout = createDetector(targetConfigs.get(0)).getLayout();

		double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
		double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;

		for (Point2D_F64 p : layout) {
			if (p.x < minX)
				minX = p.x;
			if (p.x > maxX)
				maxX = p.x;
			if (p.y < minY)
				minY = p.y;
			if (p.y > maxY)
				maxY = p.y;
		}

		assertEquals(Math.abs(minX), Math.abs(maxX), 1e-8);
		assertEquals(Math.abs(minY), Math.abs(maxY), 1e-8);
	}

	/**
	 * Make sure new instances of calibration points are returned each time
	 */
	@Test void dataNotRecycled() {
		for (ConfigGridDimen layout : targetConfigs) {
			DetectSingleFiducialCalibration detector = createDetector(layout);

			GrayF32 original = renderEasy(layout, null);
			assertTrue(detector.process(original));
			CalibrationObservation found0 = detector.getDetectedPoints();

			assertTrue(detector.process(original));
			CalibrationObservation found1 = detector.getDetectedPoints();

			assertEquals(found0.size(), found1.size());
			assertNotSame(found0, found1);
			for (int i = 0; i < found0.size(); i++) {
				PointIndex2D_F64 p0 = found0.get(i);

				for (int j = 0; j < found1.size(); j++) {
					PointIndex2D_F64 p1 = found1.get(j);
					assertNotSame(p0, p1);
				}
			}
		}
	}

	/**
	 * Observations points should always be in increasing order
	 */
	@Test void checkPointIndexIncreasingOrder() {
		for (ConfigGridDimen layout : targetConfigs) {
			DetectSingleFiducialCalibration detector = createDetector(layout);

			GrayF32 original = renderEasy(layout, null);
			assertTrue(detector.process(original));
			CalibrationObservation found = detector.getDetectedPoints();

			assertEquals(detector.getLayout().size(), found.size());

			for (int i = 0; i < found.size(); i++) {
				assertEquals(i, found.get(i).index);
			}
		}
	}
}
