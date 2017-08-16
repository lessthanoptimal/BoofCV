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

package boofcv.demonstrations.feature.detect.line;


import boofcv.abst.feature.detect.line.DetectLineHoughPolar;
import boofcv.alg.filter.blur.GBlurImageOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.detect.line.ConfigHoughPolar;
import boofcv.factory.feature.detect.line.FactoryDetectLineAlgs;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.feature.ImageLinePanel;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import georegression.struct.line.LineParametric2D_F32;
import georegression.struct.point.Point2D_F64;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Computes the Hough Polar transform and displays some of its steps and the detected lines
 *
 * @author Peter Abeles
 */
public class VisualizeHoughPolar<I extends ImageGray<I>, D extends ImageGray<D>> {

	Class<I> imageType;
	Class<D> derivType;

	public VisualizeHoughPolar(Class<I> imageType, Class<D> derivType) {
		this.imageType = imageType;
		this.derivType = derivType;
	}

	public void process( BufferedImage image ) {
		I input = GeneralizedImageOps.createSingleBand(imageType, image.getWidth(), image.getHeight());
		I blur = GeneralizedImageOps.createSingleBand(imageType, image.getWidth(), image.getHeight());

		ConvertBufferedImage.convertFromSingle(image, input, imageType);
		GBlurImageOps.gaussian(input, blur, -1, 2, null);

		DetectLineHoughPolar<I,D> alg =  FactoryDetectLineAlgs.houghPolar(
				new ConfigHoughPolar(5, 10, 2, Math.PI / 180, 25, 10), imageType, derivType);

		List<LineParametric2D_F32> lines = alg.detect(blur);

		ImageLinePanel gui = new ImageLinePanel();
		gui.setBackground(image);
		gui.setLines(lines);
		gui.setPreferredSize(new Dimension(image.getWidth(),image.getHeight()));

		BufferedImage renderedTran = VisualizeImageData.grayMagnitude(alg.getTransform().getTransform(),null,-1);
		BufferedImage renderedBinary = VisualizeBinaryData.renderBinary(alg.getBinary(), false, null);

		// Draw the location of lines onto the magnitude image
		Graphics2D g2 = renderedTran.createGraphics();
		g2.setColor(Color.RED);
		Point2D_F64 location = new Point2D_F64();
		for( LineParametric2D_F32 l : lines ) {
			alg.getTransform().lineToCoordinate(l,location);
			int r = 6;
			int w = r*2 + 1;
			int x = (int)(location.x+0.5);
			int y = (int)(location.y+0.5);
//			System.out.println(x+" "+y+"  "+renderedTran.getWidth()+" "+renderedTran.getHeight());

			g2.drawOval(x-r,y-r,w,w);
		}

		ShowImages.showWindow(renderedBinary,"Detected Edges");
		ShowImages.showWindow(renderedTran,"Parameter Space");
		ShowImages.showWindow(gui,"Detected Lines");
	}

	public static void main( String args[] ) {
		VisualizeHoughPolar<GrayF32,GrayF32> app =
				new VisualizeHoughPolar<>(GrayF32.class, GrayF32.class);

		app.process(UtilImageIO.loadImage(UtilIO.pathExample("simple_objects.jpg")));
//		app.process(UtilImageIO.loadImage(UtilIO.pathExample("lines_indoors.jpg"));
//		app.process(UtilImageIO.loadImage(UtilIO.pathExample("outdoors01.jpg"));
	}
}
