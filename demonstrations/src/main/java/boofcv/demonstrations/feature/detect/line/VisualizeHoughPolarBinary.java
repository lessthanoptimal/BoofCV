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


import boofcv.abst.feature.detect.line.DetectLineHoughPolarBinary;
import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.alg.misc.PixelMath;
import boofcv.factory.feature.detect.line.FactoryDetectLineAlgs;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
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
public class VisualizeHoughPolarBinary
	extends VisualizeHoughPolarCommon
{
	//--------------------------
	final Object lockAlg = new Object();
	DetectLineHoughPolarBinary alg;
	//--------------------------
	GrayU8 binary = new GrayU8(1,1);


	public VisualizeHoughPolarBinary(List<PathLabel> examples) {
		super(examples, ImageType.single(GrayU8.class));

		createAlg();
	}

	@Override
	protected void createAlg() {
		synchronized (lockAlg) {
			alg = FactoryDetectLineAlgs.houghPolarBinary(config);
		}
	}

	@Override
	protected void handleInputChange(int source, InputMethod method, int width, int height) {
		synchronized (lockAlg) {
			alg.setInputSize(width, height);
			binary.reshape(width,height);

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
		long time0 = System.nanoTime();
		ThresholdImageOps.threshold((GrayU8)_input,binary,125,true);
		long time1;
		synchronized (lockAlg) {
			alg.detect(binary);
			imagePanel.setLines(alg.getFoundLines(),_input.width,_input.height);
			time1 = System.nanoTime();

			imagePanel.input = buffered;

			if (logIntensity) {
				PixelMath.log(alg.getTransform().getTransform(), transformLog);
				renderedTran = VisualizeImageData.grayMagnitude(transformLog, renderedTran, -1);
			} else {
				renderedTran = VisualizeImageData.grayMagnitude(alg.getTransform().getTransform(), renderedTran, -1);
			}

			VisualizeBinaryData.renderBinary(binary, false, renderedBinary);
		}

		BoofSwingUtil.invokeNowOrLater(()->{
			imagePanel.handleViewChange();
			controlPanel.setProcessingTimeMS((time1-time0)*1e-6);
			imagePanel.repaint();
		});
	}

	public static void main( String args[] ) {
		List<PathLabel> examples = new ArrayList<>();

		examples.add(new PathLabel("Drawn Lines",UtilIO.pathExample("shapes/black_lines_01.jpg")));

		SwingUtilities.invokeLater(()->{
			VisualizeHoughPolarBinary app = new VisualizeHoughPolarBinary(examples);

			app.openExample(examples.get(0));
			app.waitUntilInputSizeIsKnown();
			app.display("Hough Polar");
		});
	}
}
