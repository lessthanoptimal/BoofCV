/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial;

import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.distort.AdjustmentType;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.LensDistortionOps;
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
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.ImageUInt8;
import org.ddogleg.struct.FastQueue;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class VisualizeSquareBinaryFiducial {

	public void process( String nameImage , String nameIntrinsic ) {

		IntrinsicParameters intrinsic = UtilIO.loadXML(nameIntrinsic);
		ImageFloat32 input = UtilImageIO.loadImage(nameImage, ImageFloat32.class);
		ImageFloat32 undistorted = new ImageFloat32(input.width,input.height);

		IntrinsicParameters paramUndist = new IntrinsicParameters();
		ImageDistort<ImageFloat32,ImageFloat32> undistorter = LensDistortionOps.imageRemoveDistortion(
				AdjustmentType.EXPAND, BorderType.EXTENDED, intrinsic, paramUndist,
				ImageType.single(ImageFloat32.class));

		InputToBinary<ImageFloat32> inputToBinary = FactoryThresholdBinary.globalOtsu(0,256, true,ImageFloat32.class);
		Detector detector = new Detector(inputToBinary);
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

		ShowImages.showWindow(output,"Binary");
		ShowImages.showWindow(squares,"Candidates");
	}

	public static class Detector extends DetectFiducialSquareBinary<ImageFloat32> {

		public List<ImageUInt8> squares = new ArrayList<ImageUInt8>();
		public List<ImageFloat32> squaresGray = new ArrayList<ImageFloat32>();

		protected Detector( InputToBinary<ImageFloat32> inputToBinary ) {
			super(inputToBinary,FactoryShapeDetector.polygon(new ConfigPolygonDetector(4,false), ImageFloat32.class)
					,ImageFloat32.class);
		}

		@Override
		protected boolean processSquare(ImageFloat32 square, Result result) {
			if( super.processSquare(square,result)) {
				squares.add(super.binary.clone());
				squaresGray.add(super.grayNoBorder.clone());
				return true;
			}

			return false;
		}
	}

	public static void main(String[] args) {

		String directory = "../data/applet/fiducial/binary/";

		VisualizeSquareBinaryFiducial app = new VisualizeSquareBinaryFiducial();

		app.process(directory+"/image0000.jpg",directory+"/intrinsic.xml");
	}
}
