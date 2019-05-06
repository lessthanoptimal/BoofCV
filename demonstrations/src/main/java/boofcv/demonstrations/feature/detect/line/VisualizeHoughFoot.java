/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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


import boofcv.abst.feature.detect.line.DetectLineHoughFoot;
import boofcv.alg.filter.blur.GBlurImageOps;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.detect.line.ConfigHoughFoot;
import boofcv.factory.feature.detect.line.FactoryDetectLineAlgs;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.feature.ImageLinePanel;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ScaleOptions;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.struct.line.LineParametric2D_F32;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Computes the Hough foot of norm transform and displays some of its steps and the detected lines
 *
 * @author Peter Abeles
 */
public class VisualizeHoughFoot<I extends ImageGray<I>, D extends ImageGray<D>>
		extends DemonstrationBase
{

	Class<I> imageType;
	Class<D> derivType;

	I blur;

	ListDisplayPanel gui = new ListDisplayPanel("Lines","Edges","Parameter Space");
	ImageLinePanel linePanel = new ImageLinePanel();

	DetectLineHoughFoot<I,D> alg;

	BufferedImage renderedTran;
	BufferedImage renderedBinary;

	public VisualizeHoughFoot(List<PathLabel> examples, Class<I> imageType) {
		super(true,true,examples, ImageType.single(imageType));
		this.imageType = imageType;
		this.derivType = GImageDerivativeOps.getDerivativeType(imageType);

		alg =  FactoryDetectLineAlgs.houghFoot(
				new ConfigHoughFoot(6, 12, 5, 25, 10), imageType, derivType);
		blur = GeneralizedImageOps.createSingleBand(imageType,1,1);

		add(BorderLayout.CENTER,gui);
	}

	@Override
	protected void handleInputChange(int source, InputMethod method, int width, int height) {
		blur.reshape(width,height);

		alg.setInputSize(width,height);
		renderedTran = ConvertBufferedImage.checkDeclare(width,height,renderedTran,BufferedImage.TYPE_INT_RGB);
		renderedBinary = ConvertBufferedImage.checkDeclare(width,height,renderedBinary,BufferedImage.TYPE_INT_RGB);

		BoofSwingUtil.invokeNowOrLater(()->{
			gui.getBodyPanel().setPreferredSize(new Dimension(width,height));
			gui.setItem(0,linePanel);
			gui.setItem(1,new ImagePanel(renderedBinary, ScaleOptions.DOWN));
			gui.setItem(2,new ImagePanel(renderedTran, ScaleOptions.DOWN));
		});
	}

	@Override
	public void processImage(int sourceID, long frameID, BufferedImage buffered, ImageBase _input)
	{
		I input = (I)_input;

		GBlurImageOps.gaussian(input, blur, -1, 2, null);

		linePanel.setImage(buffered);
		linePanel.setLines(alg.detect(blur));

		VisualizeImageData.grayMagnitude(alg.getTransform().getTransform(), renderedTran,-1);
		VisualizeBinaryData.renderBinary(alg.getBinary(), false, renderedBinary);

		// Draw the location of lines onto the magnitude image
		Graphics2D g2 = renderedTran.createGraphics();
		g2.setColor(Color.RED);
		for( LineParametric2D_F32 l : alg.getTransform().getLines().toList() ) {
			int r = 6;
			int w = r*2 + 1;
			int x = (int)(l.p.x+0.5);
			int y = (int)(l.p.y+0.5);
//			System.out.println(x+" "+y+"  "+renderedTran.getWidth()+" "+renderedTran.getHeight());

			g2.drawOval(x-r,y-r,w,w);
		}

		gui.repaint();
	}

	public static void main( String args[] ) {
		java.util.List<PathLabel> examples = new ArrayList<>();

		examples.add(new PathLabel("Simple Objects",UtilIO.pathExample("simple_objects.jpg")));
		examples.add(new PathLabel("Indoors",UtilIO.pathExample("lines_indoors.jpg")));
		examples.add(new PathLabel("Outdoors", UtilIO.pathExample("outdoors01.jpg")));
		examples.add(new PathLabel("Indoors Video",UtilIO.pathExample("lines_indoors.mjpeg")));

		SwingUtilities.invokeLater(()->{
			VisualizeHoughFoot<GrayF32, GrayF32> app = new VisualizeHoughFoot(examples,GrayF32.class);

			app.openExample(examples.get(0));
			app.waitUntilInputSizeIsKnown();
			app.display("Hough Foot");
		});
	}
}
