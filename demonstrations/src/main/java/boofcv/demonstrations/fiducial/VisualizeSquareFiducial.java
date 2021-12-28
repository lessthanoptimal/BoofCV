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
import boofcv.alg.fiducial.square.BaseDetectFiducialSquare;
import boofcv.alg.fiducial.square.FoundFiducial;
import boofcv.factory.distort.LensDistortionFactory;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.factory.filter.binary.ThresholdType;
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
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Quadrilateral_F64;
import org.ddogleg.struct.DogArray;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Visualizes results from detecting square fiducials.
 *
 * @author Peter Abeles
 */
public class VisualizeSquareFiducial {
	static ConfigThreshold configThreshold = ConfigThreshold.local(ThresholdType.LOCAL_MEAN, 13);

	static InputToBinary<GrayF32> inputToBinary = FactoryThresholdBinary.threshold(configThreshold, GrayF32.class);

	public void process( String nameImage, @Nullable String nameIntrinsic ) {

		CameraPinholeBrown intrinsic = nameIntrinsic == null ? null : (CameraPinholeBrown)CalibrationIO.load(nameIntrinsic);
		GrayF32 input = Objects.requireNonNull(UtilImageIO.loadImage(nameImage, GrayF32.class));
		GrayF32 undistorted = new GrayF32(input.width, input.height);

		Detector detector = new Detector();

		if (intrinsic != null) {
			var paramUndist = new CameraPinholeBrown();
			ImageDistort<GrayF32, GrayF32> undistorter = LensDistortionOps.changeCameraModel(
					AdjustmentType.EXPAND, BorderType.EXTENDED, intrinsic, new CameraPinhole(intrinsic), paramUndist,
					ImageType.single(GrayF32.class));

			detector.configure(new LensDistortionBrown(paramUndist),
					paramUndist.width, paramUndist.height, false);
			undistorter.apply(input, undistorted);
		} else {
			undistorted.setTo(input);
		}

		detector.process(undistorted);

		System.out.println("Total Found: " + detector.squares.size());
		DogArray<FoundFiducial> fiducials = detector.getFound();

		int N = Math.min(20, detector.squares.size());
		ListDisplayPanel squares = new ListDisplayPanel();
		for (int i = 0; i < N; i++) {
			squares.addImage(ConvertBufferedImage.convertTo(detector.squares.get(i), null), " " + i);
		}

		BufferedImage output = new BufferedImage(input.width, input.height, BufferedImage.TYPE_INT_RGB);
		VisualizeBinaryData.renderBinary(detector.getBinary(), false, output);
		Graphics2D g2 = output.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setColor(Color.RED);
		g2.setStroke(new BasicStroke(2));

		if (intrinsic != null) {
			Point2Transform2_F64 add_p_to_p = LensDistortionFactory.narrow(intrinsic).distort_F64(true, true);

			for (int i = 0; i < N; i++) {
				// add back in lens distortion
				Quadrilateral_F64 q = fiducials.get(i).distortedPixels;

				apply(add_p_to_p, q.a, q.a);
				apply(add_p_to_p, q.b, q.b);
				apply(add_p_to_p, q.c, q.c);
				apply(add_p_to_p, q.d, q.d);

				VisualizeShapes.draw(q, g2);
			}
		}

		BufferedImage outputGray = new BufferedImage(input.width, input.height, BufferedImage.TYPE_INT_RGB);
		ConvertBufferedImage.convertTo(undistorted, outputGray);
		g2 = outputGray.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		for (int i = 0; i < N; i++) {
			// add back in lens distortion
			Quadrilateral_F64 q = fiducials.get(i).distortedPixels;
//			g2.setStroke(new BasicStroke(2));
//			VisualizeBinaryData.render(detector.getSquareDetector().getUsedContours(),Color.BLUE,outputGray);
			VisualizeShapes.drawArrowSubPixel(q, 3, 1, g2);
		}

		ShowImages.showWindow(output, "Binary");
		ShowImages.showWindow(outputGray, "Gray");
		ShowImages.showWindow(squares, "Candidates");
	}

	private void apply( Point2Transform2_F64 dist, Point2D_F64 p, Point2D_F64 o ) {
		dist.compute(p.x, p.y, o);
	}

	public static class Detector extends BaseDetectFiducialSquare<GrayF32> {

		public List<GrayF32> squares = new ArrayList<>();

		protected Detector() {
			super(inputToBinary, FactoryShapeDetector.polygon(new ConfigPolygonDetector(false, 4, 4), GrayF32.class),
					true, 0.25, 0.65, 200, GrayF32.class);
		}

		@Override
		protected boolean processSquare( GrayF32 square, Result result, double a, double b ) {
			squares.add(square.clone());
			return true;
		}
	}

	public static void main( String[] args ) {

//		"/home/pja/projects/ValidationBoof/data/fiducials/square_border_binary/standard/distance_angle/image00008.png";
//		String directory = UtilIO.pathExample("fiducial/binary");
//		String directory = UtilIO.pathExample("fiducial/image");

		SwingUtilities.invokeLater(() -> {
			VisualizeSquareFiducial app = new VisualizeSquareFiducial();

			app.process(UtilIO.pathExample("fiducial/image/examples/image01.jpg"), null);
//		app.process(directory+"/image0001.jpg",directory+"/intrinsic.yaml");
//		app.process(directory+"/image0002.jpg",directory+"/intrinsic.yaml");
		});
	}
}
