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

import boofcv.alg.distort.LensDistortionOps;
import boofcv.alg.shapes.SplitMergeLineFitLoop;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.feature.VisualizeShapes;
import boofcv.gui.fiducial.VisualizeFiducial;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.distort.PointTransform_F64;
import boofcv.struct.image.ImageFloat32;
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

	public void process( String nameImage , String nameIntrinsic ) {

		ImageFloat32 input = UtilImageIO.loadImage(nameImage,ImageFloat32.class);
		IntrinsicParameters intrinsic = UtilIO.loadXML(nameIntrinsic);

		Detector detector = new Detector();
		detector.configure(intrinsic);
		detector.process(input);

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
		g2.setColor(Color.RED);
		g2.setStroke(new BasicStroke(2));

		PointTransform_F64 add_p_to_p = LensDistortionOps.distortTransform(intrinsic).distort_F64(true, true);

		Se3_F64 targetToWorld = new Se3_F64();

		for (int i = 0; i < N; i++) {
			// add back in lens distortion
			Quadrilateral_F64 q = fiducials.get(i).location;

			detector.computeTargetToWorld(q,0.1,targetToWorld);
			VisualizeFiducial.drawCube(targetToWorld, intrinsic, 0.1, g2);

			apply(add_p_to_p,q.a,q.a);
			apply(add_p_to_p,q.b,q.b);
			apply(add_p_to_p,q.c,q.c);
			apply(add_p_to_p,q.d,q.d);

			VisualizeShapes.draw(q,g2);
		}

		BufferedImage outputGray = new BufferedImage(input.width,input.height,BufferedImage.TYPE_INT_RGB);
		ConvertBufferedImage.convertTo(input,outputGray);
		g2 = outputGray.createGraphics();
		g2.setColor(Color.RED);
		g2.setStroke(new BasicStroke(1));
		for (int i = 0; i < N; i++) {
			// add back in lens distortion
			Quadrilateral_F64 q = fiducials.get(i).location;
			VisualizeShapes.draw(q,g2);
		}

		ShowImages.showWindow(output,"Binary");
		ShowImages.showWindow(outputGray,"Gray");
		ShowImages.showWindow(squares,"Candidates");
	}

	private void apply( PointTransform_F64 dist , Point2D_F64 p , Point2D_F64 o ) {
		dist.compute(p.x,p.y,o);
	}

	public static class Detector extends BaseDetectFiducialSquare<ImageFloat32> {

		public List<ImageFloat32> squares = new ArrayList<ImageFloat32>();

		protected Detector() {
			super(FactoryThresholdBinary.adaptiveSquare(6, 0, true, ImageFloat32.class),
					new SplitMergeLineFitLoop(5,0.05,20), 200, 0.2, ImageFloat32.class);
		}

		@Override
		protected boolean processSquare(ImageFloat32 square, Result result) {
			squares.add(square.clone());
			return true;
		}
	}

	public static void main(String[] args) {

		String directory = "../data/applet/fiducial/binary";

		VisualizeSquareFiducial app = new VisualizeSquareFiducial();

//		app.process(directory+"/image0000.jpg",directory+"/intrinsic.xml");
//		app.process(directory+"/image0001.jpg",directory+"/intrinsic.xml");
		app.process(directory+"/image0002.jpg",directory+"/intrinsic.xml");
	}
}
