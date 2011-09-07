/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.feature.detect.edge;

import boofcv.abst.feature.detect.edge.DetectEdgeContour;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.feature.detect.edge.FactoryDetectEdgeContour;
import boofcv.gui.SelectAlgorithmPanel;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.point.Point2D_I32;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Random;

/**
 * Displays contours selected using different algorithms.
 *
 * @author Peter Abeles
 */
public class ShowEdgeContourApp<T extends ImageBase, D extends ImageBase>
		extends SelectAlgorithmPanel {

//	static String fileName = "evaluation/data/outdoors01.jpg";
//	static String fileName = "evaluation/data/sunflowers.png";
	static String fileName = "evaluation/data/particles01.jpg";
//	static String fileName = "evaluation/data/scale/beach02.jpg";
//	static String fileName = "evaluation/data/shapes01.png";

	ImagePanel panel;

	BufferedImage input;
	Class<T> imageType;
	Class<D> derivType;

	public ShowEdgeContourApp(BufferedImage input, Class<T> imageType, Class<D> derivType) {
		this.input = input;
		this.imageType = imageType;
		this.derivType = derivType;
		panel = new ImagePanel(input);

		addAlgorithm("Original Image", null);
		addAlgorithm("Canny", FactoryDetectEdgeContour.canny(5,40,imageType,derivType));
		addAlgorithm("Binary Simple", FactoryDetectEdgeContour.<T>binarySimple());

		add(panel, BorderLayout.CENTER);
	}

	@Override
	public void setActiveAlgorithm(String name, Object cookie) {
		if( cookie == null ) {
			panel.setBufferedImage(input);
			panel.repaint();
			return;
		}

		DetectEdgeContour<T> contour = (DetectEdgeContour<T>)cookie;

		T workImage = ConvertBufferedImage.convertFrom(input,null,imageType);

		contour.process(workImage);
		List<List<Point2D_I32>> found = contour.getContours();


		BufferedImage temp = new BufferedImage(input.getWidth(),input.getHeight(),input.getType());
		Graphics2D g2 = (Graphics2D)temp.getGraphics();

//		g2.drawImage(input,0,0,null);

		Random rand = new Random(234);
		for( List<Point2D_I32> l : found ) {
			int c = rand.nextInt(0xFFFFFF);
			for( Point2D_I32 p : l ) {
				temp.setRGB(p.x,p.y,c);
			}
		}

		panel.setBufferedImage(temp);
		panel.repaint();
	}

	public static void main( String args[] ) {
		BufferedImage input = UtilImageIO.loadImage(fileName);

		ShowEdgeContourApp<ImageFloat32,ImageFloat32> app =
				new ShowEdgeContourApp<ImageFloat32, ImageFloat32>(input,ImageFloat32.class, ImageFloat32.class);
//		ShowFeatureOrientationApp<ImageUInt8, ImageSInt16> app =
//				new ShowFeatureOrientationApp<ImageUInt8,ImageSInt16>(input,ImageUInt8.class, ImageSInt16.class);

		ShowImages.showWindow(app,"Contours");
//		input = UtilImageIO.loadImage(fileName);
//		doStuff(input, ImageUInt8.class, ImageSInt16.class);
		System.out.println("Done");
	}
}
