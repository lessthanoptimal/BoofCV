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


import boofcv.abst.feature.detect.line.DetectEdgeLinesToLines;
import boofcv.abst.feature.detect.line.DetectLineHoughPolarEdge;
import boofcv.alg.filter.blur.GBlurImageOps;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.misc.PixelMath;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.detect.line.FactoryDetectLineAlgs;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Computes the Hough Polar transform and displays some of its steps and the detected lines
 *
 * @author Peter Abeles
 */
public class VisualizeHoughPolarEdge<I extends ImageGray<I>, D extends ImageGray<D>>
	extends VisualizeHoughPolarCommon
{

	Class<I> imageType;
	Class<D> derivType;

	//--------------------------
	DetectLineHoughPolarEdge<D> alg;
	DetectEdgeLinesToLines<I,D> lineDetector; // use a high level line detector since it already has boilerplate code in it
	//--------------------------

	I blur;

	public VisualizeHoughPolarEdge(List<PathLabel> examples, Class<I> imageType) {
		super(examples,ImageType.single(imageType));
		this.imageType = imageType;
		this.derivType = GImageDerivativeOps.getDerivativeType(imageType);

		blur = GeneralizedImageOps.createSingleBand(imageType,1,1);
		createAlg();
	}

	@Override
	protected void createAlg() {
		synchronized (lockAlg) {
			alg = FactoryDetectLineAlgs.houghPolarEdge(config, derivType);
			super.alg = this.alg;
			lineDetector = new DetectEdgeLinesToLines<>(alg, imageType, derivType);
		}
	}

	@Override
	protected void handleInputChange(int source, InputMethod method, int width, int height) {
		blur.reshape(width,height);

		synchronized (lockAlg) {
			alg.setInputSize(width, height);

			int tranWidth = alg.getTransform().getTransform().width;
			int tranHeight = alg.getTransform().getTransform().height;
			renderedTran = ConvertBufferedImage.checkDeclare(tranWidth, tranHeight, renderedTran, BufferedImage.TYPE_INT_RGB);
			renderedBinary = ConvertBufferedImage.checkDeclare(width, height, renderedBinary, BufferedImage.TYPE_INT_RGB);
		}

		BoofSwingUtil.invokeNowOrLater(()-> {
			imagePanel.setPreferredSize(new Dimension(width, height));
			controlPanel.setImageSize(width,height);
		});
	}

	@Override
	public synchronized void processImage(int sourceID, long frameID, BufferedImage buffered, ImageBase _input)
	{
		I input = (I)_input;

		long time0 = System.nanoTime();
		if( blurRadius > 0 ) {
			GBlurImageOps.gaussian(input, blur, -1, blurRadius, null);
		} else {
			blur.setTo(input);
		}
		long time1;
		synchronized (lockAlg) {
			imagePanel.setLines(lineDetector.detect(blur),input.width,input.height);
			time1 = System.nanoTime();

			imagePanel.input = buffered;

			if (logIntensity) {
				PixelMath.log(alg.getTransform().getTransform(), transformLog);
				renderedTran = VisualizeImageData.grayMagnitude(transformLog, renderedTran, -1);
			} else {
				renderedTran = VisualizeImageData.grayMagnitude(alg.getTransform().getTransform(), renderedTran, -1);
			}

			VisualizeBinaryData.renderBinary(alg.getBinary(), false, renderedBinary);
		}

		BoofSwingUtil.invokeNowOrLater(()->{
			imagePanel.handleViewChange();
			controlPanel.setProcessingTimeMS((time1-time0)*1e-6);
			imagePanel.repaint();
		});
	}

	public static void main( String args[] ) {
		java.util.List<PathLabel> examples = new ArrayList<>();

		examples.add(new PathLabel("Simple Objects",UtilIO.pathExample("simple_objects.jpg")));
		examples.add(new PathLabel("Indoors",UtilIO.pathExample("lines_indoors.jpg")));
		examples.add(new PathLabel("Outdoors", UtilIO.pathExample("outdoors01.jpg")));
		examples.add(new PathLabel("Drawn Lines",UtilIO.pathExample("shapes/black_lines_01.jpg")));
		examples.add(new PathLabel("Indoors Video",UtilIO.pathExample("lines_indoors.mjpeg")));

		SwingUtilities.invokeLater(()->{
			VisualizeHoughPolarEdge<GrayF32, GrayF32> app = new VisualizeHoughPolarEdge(examples,GrayF32.class);

			app.openExample(examples.get(0));
			app.waitUntilInputSizeIsKnown();
			app.display("Hough Polar");
		});
	}
}
