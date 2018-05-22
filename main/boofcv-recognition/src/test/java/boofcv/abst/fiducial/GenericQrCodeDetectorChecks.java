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

import boofcv.alg.fiducial.qrcode.QrCode;
import boofcv.alg.fiducial.qrcode.QrCodeEncoder;
import boofcv.alg.fiducial.qrcode.QrCodeGeneratorImage;
import boofcv.alg.fiducial.qrcode.QrCodeMaskPattern;
import boofcv.core.image.ConvertImage;
import boofcv.gui.image.ShowImages;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofMiscOps;
import boofcv.simulation.SimulatePlanarWorld;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.se.Se3_F64;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Generic tests for qr codes under mild lens distortion
 */
public abstract class GenericQrCodeDetectorChecks {

	double simulatedTargetWidth = 0.3; // size of target in simulated world

	String message = "012340985";
	QrCode expected;

	boolean display = false;

	@Before
	public void setup() {
		expected = new QrCodeEncoder().setVersion(2).
				setError(QrCode.ErrorLevel.H).
				setMask(QrCodeMaskPattern.M010).
				addNumeric(message).fixate();
	}

	protected abstract QrCodeDetector<GrayF32> createDetector();

	/**
	 * See if a clear well defined qr code can be detected while rating
	 */
	@Test
	public void rotation() {
		QrCodeDetector<GrayF32> detector = createDetector();

		CameraPinholeRadial model = CalibrationIO.load(getClass().getResource("calib/pinhole_radial.yaml"));
		SimulatePlanarWorld simulator = new SimulatePlanarWorld();
		simulator.setCamera(model);

		simulator.resetScene();
		Se3_F64 markerToWorld = new Se3_F64();
		simulator.addTarget(markerToWorld, simulatedTargetWidth, generateMarker());

		markerToWorld.T.set(0, 0, 0.5);

		for (int i = 0; i < 30; i++) {
			double roll = 2*Math.PI*i/30.0;
			ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,0,0,roll,markerToWorld.R);
			renderAndCheck(detector, simulator);
		}
	}

	/**
	 * The marker is at a skewed angle and rotating
	 */
	@Test
	public void skewed() {
		QrCodeDetector<GrayF32> detector = createDetector();

		CameraPinholeRadial model = CalibrationIO.load(getClass().getResource("calib/pinhole_radial.yaml"));
		SimulatePlanarWorld simulator = new SimulatePlanarWorld();
		simulator.setCamera(model);

		simulator.resetScene();
		Se3_F64 markerToWorld = new Se3_F64();
		simulator.addTarget(markerToWorld, simulatedTargetWidth, generateMarker());

		markerToWorld.T.set(0, 0, 0.5);

		for (int i = 0; i < 30; i++) {
			double roll = 2*Math.PI*i/30.0;
			double pitch = Math.PI*0.3;
			ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,pitch,0,roll,markerToWorld.R);
			renderAndCheck(detector, simulator);
		}
	}

	/**
	 * The marker zooming in and out of the frame
	 */
	@Test
	public void scale() {
		QrCodeDetector<GrayF32> detector = createDetector();

		CameraPinholeRadial model = CalibrationIO.load(getClass().getResource("calib/pinhole_radial.yaml"));
		SimulatePlanarWorld simulator = new SimulatePlanarWorld();
		simulator.setCamera(model);

		simulator.resetScene();
		Se3_F64 markerToWorld = new Se3_F64();
		simulator.addTarget(markerToWorld, simulatedTargetWidth, generateMarker());

		markerToWorld.T.set(0, 0, 0.3);

		for (int i = 0; i < 30; i++) {
			renderAndCheck(detector, simulator);
			markerToWorld.T.z += 0.03;
		}
	}

	/**
	 * See if it can detect multiple markers in the image at the same time
	 */
	@Test
	public void multipleMarkers() {
		QrCodeDetector<GrayF32> detector = createDetector();

		CameraPinholeRadial model = CalibrationIO.load(getClass().getResource("calib/pinhole_radial.yaml"));
		SimulatePlanarWorld simulator = new SimulatePlanarWorld();
		simulator.setCamera(model);

		simulator.resetScene();
		Se3_F64 markerToWorld0 = new Se3_F64();
		Se3_F64 markerToWorld1 = new Se3_F64();
		simulator.addTarget(markerToWorld0, simulatedTargetWidth, generateMarker());
		simulator.addTarget(markerToWorld1, simulatedTargetWidth, generateMarker());

		markerToWorld0.T.set(0.2, 0, 0.6);
		markerToWorld1.T.set(-0.2, 0, 0.6);

		simulator.render();

		detector.process(simulator.getOutput());

		if( display ) {
			ShowImages.showWindow(simulator.getOutput(), "Foo", true);
			BoofMiscOps.sleep(10000);
		}

		List<QrCode> detections = detector.getDetections();
		assertEquals(2,detections.size());
	}

	private GrayF32 generateMarker() {
		QrCodeGeneratorImage generator = new QrCodeGeneratorImage(4);
//		generator.renderData = false;
		generator.render(expected);

		GrayU8 rendered = generator.getGray();

		return ConvertImage.convert(rendered,(GrayF32)null);
	}

	private void renderAndCheck(QrCodeDetector<GrayF32> detector, SimulatePlanarWorld simulator) {
		simulator.render();

		detector.process(simulator.getOutput());

		if( display ) {
			ShowImages.showWindow(simulator.getOutput(), "Foo", true);
			BoofMiscOps.sleep(200);
			UtilImageIO.saveImage(simulator.getOutput(),"qrcode_rendered.png");
		}

		List<QrCode> detections = detector.getDetections();
		assertEquals(1,detections.size());
		QrCode qr = detections.get(0);
		assertEquals(message,new String(qr.message));
	}

}
