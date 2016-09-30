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
import boofcv.alg.fiducial.square.DetectFiducialSquareBinary;
import boofcv.alg.fiducial.square.FoundFiducial;
import boofcv.core.image.border.BorderType;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.factory.shape.ConfigPolygonDetector;
import boofcv.factory.shape.FactoryShapeDetector;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.feature.VisualizeShapes;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import org.ddogleg.struct.FastQueue;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class VisualizeSquareBinaryFiducial {

	final static int gridWidth = 4;
	final static double borderWidth = 0.25;

	public void process( String nameImage , String nameIntrinsic ) {

		CameraPinholeRadial intrinsic = UtilIO.loadXML(nameIntrinsic);
		GrayF32 input = UtilImageIO.loadImage(nameImage, GrayF32.class);
		GrayF32 undistorted = new GrayF32(input.width,input.height);

		CameraPinholeRadial paramUndist = new CameraPinholeRadial();
		ImageDistort<GrayF32,GrayF32> undistorter = LensDistortionOps.imageRemoveDistortion(
				AdjustmentType.EXPAND, BorderType.EXTENDED, intrinsic, paramUndist,
				ImageType.single(GrayF32.class));

		InputToBinary<GrayF32> inputToBinary = FactoryThresholdBinary.globalOtsu(0,255, true,GrayF32.class);
		Detector detector = new Detector(gridWidth,borderWidth,inputToBinary);
		detector.configure(paramUndist,false);
		detector.setLengthSide(0.1);

		undistorter.apply(input,undistorted);
		detector.process(undistorted);

		System.out.println("Total Found: "+detector.squares.size());
		FastQueue<FoundFiducial> fiducials = detector.getFound();

		int N = Math.min(20,detector.squares.size());
		ListDisplayPanel squares = new ListDisplayPanel();
		for (int i = 0; i < N; i++) {
			squares.addImage(VisualizeBinaryData.renderBinary(detector.squares.get(i),false, null)," "+i);
			squares.addImage(ConvertBufferedImage.convertTo(detector.squaresGray.get(i),null)," "+i);
		}

		BufferedImage output = new BufferedImage(input.width,input.height,BufferedImage.TYPE_INT_RGB);
		ConvertBufferedImage.convertTo(input,output);
		Graphics2D g2 = output.createGraphics();
		g2.setColor(Color.RED);
		g2.setStroke(new BasicStroke(2));
		for (int i = 0; i < N; i++) {
			VisualizeShapes.draw(fiducials.get(i).location,g2);
		}

		ShowImages.showWindow(output,"Binary",true);
		ShowImages.showWindow(squares,"Candidates",true);
	}

	public static class Detector extends DetectFiducialSquareBinary<GrayF32> {

		public List<GrayU8> squares = new ArrayList<GrayU8>();
		public List<GrayF32> squaresGray = new ArrayList<GrayF32>();

		protected Detector( int gridWidth, double borderWidth , InputToBinary<GrayF32> inputToBinary ) {
			super(gridWidth,borderWidth,0.65,inputToBinary,FactoryShapeDetector.polygon(new ConfigPolygonDetector(false, 4,4), GrayF32.class)
					,GrayF32.class);
		}

		@Override
		protected boolean processSquare(GrayF32 square, Result result, double a , double b) {
			if( super.processSquare(square,result,a,b)) {
				squares.add(super.getBinaryInner().clone());
				squaresGray.add(super.getGrayNoBorder().clone());
				return true;
			}

			return false;
		}
	}

	public static void main(String[] args) {

		String directory = UtilIO.pathExample("fiducial/binary/");

		VisualizeSquareBinaryFiducial app = new VisualizeSquareBinaryFiducial();

		app.process(directory+"/image0000.jpg",directory+"/intrinsic.xml");
	}
}
