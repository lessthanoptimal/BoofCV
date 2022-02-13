/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.fiducial.aztec.AztecCode;
import boofcv.alg.fiducial.aztec.AztecEncoder;
import boofcv.alg.fiducial.aztec.AztecGenerator;
import boofcv.core.image.ConvertImage;
import boofcv.gui.image.ShowImages;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofMiscOps;
import boofcv.simulation.SimulatePlanarWorld;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.se.Se3_F64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Generic tests for qr codes under mild lens distortion
 */
public abstract class GenericAztecCodeDetectorChecks extends BoofStandardJUnit {

	double simulatedTargetWidth = 0.3; // size of target in simulated world

	String message = "DEADBEEF";
	AztecCode expected;

	boolean display = false;

	@BeforeEach
	public void setup() {
		expected = new AztecEncoder().addUpper(message).fixate();
	}

	protected abstract AztecCodeDetector<GrayF32> createDetector();

	/**
	 * See if a clear well defined qr code can be detected while rating
	 */
	@Test void rotation() {
		AztecCodeDetector<GrayF32> detector = createDetector();

		CameraPinholeBrown model = CalibrationIO.load(getClass().getResource("calib/pinhole_radial.yaml"));
		SimulatePlanarWorld simulator = new SimulatePlanarWorld();
		simulator.setCamera(model);

		simulator.resetScene();
		Se3_F64 markerToWorld = new Se3_F64();
		simulator.addSurface(markerToWorld, simulatedTargetWidth, generateMarker());

		markerToWorld.T.setTo(0, 0, 0.5);

		for (int i = 0; i < 30; i++) {
			double roll = 2*Math.PI*i/30.0;
			ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0, Math.PI, roll, markerToWorld.R);
			renderAndCheck(detector, simulator);
		}
	}

	/**
	 * The marker is at a skewed angle and rotating
	 */
	@Test void skewed() {
		AztecCodeDetector<GrayF32> detector = createDetector();

		CameraPinholeBrown model = CalibrationIO.load(getClass().getResource("calib/pinhole_radial.yaml"));
		SimulatePlanarWorld simulator = new SimulatePlanarWorld();
		simulator.setCamera(model);

		simulator.resetScene();
		Se3_F64 markerToWorld = new Se3_F64();
		simulator.addSurface(markerToWorld, simulatedTargetWidth, generateMarker());

		markerToWorld.T.setTo(0, 0, 0.5);

		for (int i = 0; i < 30; i++) {
			double roll = 2*Math.PI*i/30.0;
			double pitch = Math.PI*0.3;
			ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, pitch, Math.PI, roll, markerToWorld.R);
			renderAndCheck(detector, simulator);
		}
	}

	/**
	 * The marker zooming in and out of the frame
	 */
	@Test void scale() {
		AztecCodeDetector<GrayF32> detector = createDetector();

		CameraPinholeBrown model = CalibrationIO.load(getClass().getResource("calib/pinhole_radial.yaml"));
		SimulatePlanarWorld simulator = new SimulatePlanarWorld();
		simulator.setCamera(model);

		simulator.resetScene();
		Se3_F64 markerToWorld = new Se3_F64();
		simulator.addSurface(markerToWorld, simulatedTargetWidth, generateMarker());

		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0, Math.PI, 0, markerToWorld.R);
		markerToWorld.T.setTo(0, 0, 0.3);

		for (int i = 0; i < 30; i++) {
			renderAndCheck(detector, simulator);
			markerToWorld.T.z += 0.03;
		}
	}

	/**
	 * See if it can detect multiple fiducials in the image at the same time
	 */
	@Test void multipleMarkers() {
		AztecCodeDetector<GrayF32> detector = createDetector();

		CameraPinholeBrown model = CalibrationIO.load(getClass().getResource("calib/pinhole_radial.yaml"));
		SimulatePlanarWorld simulator = new SimulatePlanarWorld();
		simulator.setCamera(model);

		simulator.resetScene();
		Se3_F64 markerToWorld0 = new Se3_F64();
		Se3_F64 markerToWorld1 = new Se3_F64();
		simulator.addSurface(markerToWorld0, simulatedTargetWidth, generateMarker());
		simulator.addSurface(markerToWorld1, simulatedTargetWidth, generateMarker());

		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0, Math.PI, 0, markerToWorld0.R);
		markerToWorld0.T.setTo(0.2, 0, 0.6);
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0, Math.PI, 0, markerToWorld1.R);
		markerToWorld1.T.setTo(-0.2, 0, 0.6);

		simulator.render();

		detector.process(simulator.getOutput());

		if (display) {
			ShowImages.showWindow(simulator.getOutput(), ShowImages.Colorization.MAGNITUDE, "Foo", true);
			BoofMiscOps.sleep(10000);
		}

		List<AztecCode> detections = detector.getDetections();
		assertEquals(2, detections.size());
	}

	private GrayF32 generateMarker() {
		GrayU8 gray = AztecGenerator.renderImage(4,1,expected);
		return ConvertImage.convert(gray, (GrayF32)null);
	}

	private void renderAndCheck( AztecCodeDetector<GrayF32> detector, SimulatePlanarWorld simulator ) {
		simulator.render();

		detector.process(simulator.getOutput());

		if (display) {
			UtilImageIO.saveImage(simulator.getOutput(), "aztec_failed.png");
			ShowImages.showBlocking(simulator.getOutput(), "Failure", 10_000, true);
		}

		List<AztecCode> detections = detector.getDetections();
		assertEquals(1, detections.size());
		AztecCode marker = detections.get(0);
		assertEquals(message, marker.message);
	}
}
