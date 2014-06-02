/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.line;


import boofcv.abst.feature.detect.line.DetectLineHoughFoot;
import boofcv.alg.filter.blur.GBlurImageOps;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.detect.line.ConfigHoughFoot;
import boofcv.factory.feature.detect.line.FactoryDetectLineAlgs;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.feature.ImageLinePanel;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Computes the Hough foot of norm transform and displays some of its steps and the detected lines
 *
 * @author Peter Abeles
 */
public class VisualizeHoughFoot<I extends ImageSingleBand, D extends ImageSingleBand> {

	Class<I> imageType;
	Class<D> derivType;

	public VisualizeHoughFoot(Class<I> imageType, Class<D> derivType) {
		this.imageType = imageType;
		this.derivType = derivType;
	}

	public void process( BufferedImage image ) {
		I input = GeneralizedImageOps.createSingleBand(imageType, image.getWidth(), image.getHeight());
		I blur = GeneralizedImageOps.createSingleBand(imageType, image.getWidth(), image.getHeight());

		ConvertBufferedImage.convertFromSingle(image, input, imageType);
		GBlurImageOps.gaussian(input, blur, -1, 2, null);

		DetectLineHoughFoot<I,D> alg =  FactoryDetectLineAlgs.houghFoot(
				new ConfigHoughFoot(6, 12, 5, 25, 10), imageType, derivType);

		ImageLinePanel gui = new ImageLinePanel();
		gui.setBackground(image);
		gui.setLines(alg.detect(blur));
		gui.setPreferredSize(new Dimension(image.getWidth(),image.getHeight()));

		BufferedImage renderedTran = VisualizeImageData.grayMagnitude(alg.getTransform().getTransform(),null,-1);
		BufferedImage renderedBinary = VisualizeBinaryData.renderBinary(alg.getBinary(), null);

		ShowImages.showWindow(renderedBinary,"Detected Edges");
		ShowImages.showWindow(renderedTran,"Parameter Space");
		ShowImages.showWindow(gui,"Detected Lines");
	}

	public static void main( String args[] ) {
		VisualizeHoughFoot<ImageFloat32,ImageFloat32> app =
				new VisualizeHoughFoot<ImageFloat32,ImageFloat32>(ImageFloat32.class,ImageFloat32.class);

//		app.process(UtilImageIO.loadImage("../data/evaluation/simple_objects.jpg"));
		app.process(UtilImageIO.loadImage("../data/evaluation/lines_indoors.jpg"));
	}
}
