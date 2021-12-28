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

package boofcv.demonstrations.fiducial;

import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.distort.AdjustmentType;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.LensDistortionOps;
import boofcv.alg.distort.brown.LensDistortionBrown;
import boofcv.alg.fiducial.square.DetectFiducialSquareBinary;
import boofcv.alg.fiducial.square.FoundFiducial;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.factory.shape.ConfigPolygonDetector;
import boofcv.factory.shape.FactoryShapeDetector;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.feature.VisualizeShapes;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.border.BorderType;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import org.ddogleg.struct.DogArray;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Visualizes {@link DetectFiducialSquareBinary}
 *
 * @author Peter Abeles
 */
public class VisualizeSquareBinaryFiducial {

	final static int gridWidth = 4;
	final static double borderWidth = 0.25;

	public void process( String nameImage, @Nullable String nameIntrinsic ) {

		CameraPinholeBrown intrinsic = nameIntrinsic == null ? null : (CameraPinholeBrown)CalibrationIO.load(nameIntrinsic);
		GrayF32 input = Objects.requireNonNull(UtilImageIO.loadImage(nameImage, GrayF32.class));
		var undistorted = new GrayF32(input.width, input.height);

		InputToBinary<GrayF32> inputToBinary = FactoryThresholdBinary.globalOtsu(0, 255, 1.0, true, GrayF32.class);
		Detector detector = new Detector(gridWidth, borderWidth, inputToBinary);
		detector.setLengthSide(0.1);

		if (intrinsic != null) {
			CameraPinholeBrown paramUndist = new CameraPinholeBrown();
			ImageDistort<GrayF32, GrayF32> undistorter = LensDistortionOps.changeCameraModel(
					AdjustmentType.EXPAND, BorderType.EXTENDED, intrinsic, new CameraPinhole(intrinsic), paramUndist,
					ImageType.single(GrayF32.class));
			detector.configure(new LensDistortionBrown(paramUndist),
					paramUndist.width, paramUndist.height, false);
			undistorter.apply(input, undistorted);
			detector.process(undistorted);
		} else {
			detector.process(input);
		}

		System.out.println("Total Found: " + detector.squares.size());
		DogArray<FoundFiducial> fiducials = detector.getFound();

		int N = Math.min(20, detector.squares.size());
		ListDisplayPanel squares = new ListDisplayPanel();
		for (int i = 0; i < N; i++) {
			squares.addImage(VisualizeBinaryData.renderBinary(detector.squares.get(i), false, null), " " + i);
			squares.addImage(ConvertBufferedImage.convertTo(detector.squaresGray.get(i), null), " " + i);
		}

		BufferedImage output = new BufferedImage(input.width, input.height, BufferedImage.TYPE_INT_RGB);
		ConvertBufferedImage.convertTo(input, output);
		Graphics2D g2 = output.createGraphics();
		g2.setColor(Color.RED);
		g2.setStroke(new BasicStroke(2));
		for (int i = 0; i < N; i++) {
			VisualizeShapes.drawArrowSubPixel(fiducials.get(i).distortedPixels, 3, 1, g2);
		}

		ShowImages.showWindow(output, "Binary", true);
		ShowImages.showWindow(squares, "Candidates", true);
	}

	public static class Detector extends DetectFiducialSquareBinary<GrayF32> {

		public List<GrayU8> squares = new ArrayList<>();
		public List<GrayF32> squaresGray = new ArrayList<>();

		protected Detector( int gridWidth, double borderWidth, InputToBinary<GrayF32> inputToBinary ) {
			super(gridWidth, borderWidth, 0.65, inputToBinary, FactoryShapeDetector.polygon(new ConfigPolygonDetector(false, 4, 4), GrayF32.class)
					, GrayF32.class);
		}

		@Override
		protected boolean processSquare( GrayF32 square, Result result, double a, double b ) {
			if (super.processSquare(square, result, a, b)) {
				squares.add(super.getBinaryInner().clone());
				squaresGray.add(super.getGrayNoBorder().clone());
				return true;
			}

			return false;
		}
	}

	public static void main( String[] args ) {

		SwingUtilities.invokeLater(() -> {
			VisualizeSquareBinaryFiducial app = new VisualizeSquareBinaryFiducial();
			app.process(UtilIO.pathExample("fiducial/binary/image0001.jpg"), null);
		});
//		app.process(directory+"/image0000.jpg",directory+"/intrinsic.yaml");
	}
}
