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

package boofcv.demonstrations.fiducial;

import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.distort.AdjustmentType;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.LensDistortionOps;
import boofcv.alg.fiducial.square.BaseDetectFiducialSquare;
import boofcv.alg.fiducial.square.FoundFiducial;
import boofcv.core.image.border.BorderType;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.factory.filter.binary.ThresholdType;
import boofcv.factory.shape.ConfigPolygonDetector;
import boofcv.factory.shape.FactoryShapeDetector;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.feature.VisualizeShapes;
import boofcv.gui.fiducial.VisualizeFiducial;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.calib.PinholeRadial;
import boofcv.struct.distort.PointTransform_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.shapes.Quadrilateral_F64;
import org.ddogleg.struct.FastQueue;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class VisualizeSquareFiducial {

	static ConfigThreshold configThreshold = ConfigThreshold.local(ThresholdType.LOCAL_SQUARE,6);

	static InputToBinary<GrayF32> inputToBinary = FactoryThresholdBinary.threshold(configThreshold, GrayF32.class);

	public void process( String nameImage , String nameIntrinsic ) {

		PinholeRadial intrinsic = nameIntrinsic == null ? null : (PinholeRadial)UtilIO.loadXML(nameIntrinsic);
		GrayF32 input = UtilImageIO.loadImage(nameImage, GrayF32.class);
		GrayF32 undistorted = new GrayF32(input.width,input.height);

		Detector detector = new Detector();

		if( intrinsic != null ) {
			PinholeRadial paramUndist = new PinholeRadial();
			ImageDistort<GrayF32, GrayF32> undistorter = LensDistortionOps.imageRemoveDistortion(
					AdjustmentType.EXPAND, BorderType.EXTENDED, intrinsic, paramUndist,
					ImageType.single(GrayF32.class));

			detector.configure(paramUndist,false);
			undistorter.apply(input,undistorted);
		} else {
			undistorted.setTo(input);
		}

		detector.process(undistorted);

		System.out.println("Total Found: "+detector.squares.size());
		FastQueue<FoundFiducial> fiducials = detector.getFound();

		int N = Math.min(20,detector.squares.size());
		ListDisplayPanel squares = new ListDisplayPanel();
		for (int i = 0; i < N; i++) {
			squares.addImage(ConvertBufferedImage.convertTo(detector.squares.get(i),null)," "+i);
		}

		BufferedImage output = new BufferedImage(input.width,input.height,BufferedImage.TYPE_INT_RGB);
		VisualizeBinaryData.renderBinary(detector.getBinary(), false, output);
		Graphics2D g2 = output.createGraphics();
		g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setColor(Color.RED);
		g2.setStroke(new BasicStroke(2));

		if( intrinsic != null ) {
			PointTransform_F64 add_p_to_p = LensDistortionOps.transformPoint(intrinsic).distort_F64(true, true);
			Se3_F64 targetToWorld = new Se3_F64();

			for (int i = 0; i < N; i++) {
				// add back in lens distortion
				Quadrilateral_F64 q = fiducials.get(i).location;

				detector.computeTargetToWorld(q, 0.1, targetToWorld);
				VisualizeFiducial.drawCube(targetToWorld, intrinsic, 0.1, 3, g2);

				apply(add_p_to_p, q.a, q.a);
				apply(add_p_to_p, q.b, q.b);
				apply(add_p_to_p, q.c, q.c);
				apply(add_p_to_p, q.d, q.d);

				VisualizeShapes.draw(q, g2);
			}
		}

		BufferedImage outputGray = new BufferedImage(input.width,input.height,BufferedImage.TYPE_INT_RGB);
		ConvertBufferedImage.convertTo(undistorted,outputGray);
		g2 = outputGray.createGraphics();
		g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		for (int i = 0; i < N; i++) {
			// add back in lens distortion
			Quadrilateral_F64 q = fiducials.get(i).location;
//			g2.setStroke(new BasicStroke(2));
//			VisualizeBinaryData.renderExternal(detector.getSquareDetector().getUsedContours(),Color.BLUE,outputGray);
			VisualizeShapes.drawArrowSubPixel(q,3,g2);
		}

		ShowImages.showWindow(output,"Binary");
		ShowImages.showWindow(outputGray,"Gray");
		ShowImages.showWindow(squares,"Candidates");
	}

	private void apply( PointTransform_F64 dist , Point2D_F64 p , Point2D_F64 o ) {
		dist.compute(p.x,p.y,o);
	}

	public static class Detector extends BaseDetectFiducialSquare<GrayF32> {

		public List<GrayF32> squares = new ArrayList<GrayF32>();

		protected Detector() {
			super(inputToBinary,FactoryShapeDetector.polygon(new ConfigPolygonDetector(false, 4,4), GrayF32.class),
					0.25,0.65,200,GrayF32.class);
		}

		@Override
		protected boolean processSquare(GrayF32 square, Result result, double a, double b) {
			squares.add(square.clone());
			return true;
		}
	}

	public static void main(String[] args) {

		String directory = UtilIO.pathExample("fiducial/binary");
//		String directory = UtilIO.pathExample("fiducial/image");

		VisualizeSquareFiducial app = new VisualizeSquareFiducial();

		app.process(directory+"/image0000.jpg",directory+"/intrinsic.xml");
		app.process(directory+"/image0001.jpg",directory+"/intrinsic.xml");
		app.process(directory+"/image0002.jpg",directory+"/intrinsic.xml");
	}
}
