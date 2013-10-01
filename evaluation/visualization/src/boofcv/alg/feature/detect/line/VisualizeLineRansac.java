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


import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.feature.detect.edge.GGradientToEdgeFeatures;
import boofcv.alg.feature.detect.line.gridline.Edgel;
import boofcv.alg.feature.detect.line.gridline.GridLineModelDistance;
import boofcv.alg.feature.detect.line.gridline.GridLineModelFitter;
import boofcv.alg.feature.detect.line.gridline.ImplGridRansacLineDetector_F32;
import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.feature.ImageLinePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.feature.MatrixOfList;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt8;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import georegression.fitting.line.ModelManagerLinePolar2D_F32;
import georegression.struct.line.LinePolar2D_F32;
import georegression.struct.line.LineSegment2D_F32;
import org.ddogleg.fitting.modelset.ModelManager;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.fitting.modelset.ransac.Ransac;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Computes the Hough Polar transform and displays some of its steps and the detected lines
 *
 * @author Peter Abeles
 */
public class VisualizeLineRansac<I extends ImageSingleBand, D extends ImageSingleBand> {

	Class<I> imageType;
	Class<D> derivType;

	public VisualizeLineRansac(Class<I> imageType, Class<D> derivType) {
		this.imageType = imageType;
		this.derivType = derivType;
	}

	public void process( BufferedImage image ) {
		int regionSize = 40;

		I input = GeneralizedImageOps.createSingleBand(imageType, image.getWidth(), image.getHeight());
		D derivX = GeneralizedImageOps.createSingleBand(derivType, image.getWidth(), image.getHeight());
		D derivY = GeneralizedImageOps.createSingleBand(derivType, image.getWidth(), image.getHeight());
		ImageFloat32 edgeIntensity =  new ImageFloat32(input.width,input.height);
		ImageFloat32 suppressed =  new ImageFloat32(input.width,input.height);
		ImageFloat32 orientation =  new ImageFloat32(input.width,input.height);
		ImageSInt8 direction = new ImageSInt8(input.width,input.height);
		ImageUInt8 detected = new ImageUInt8(input.width,input.height);

		ModelManager<LinePolar2D_F32> manager = new ModelManagerLinePolar2D_F32();
		GridLineModelDistance distance = new GridLineModelDistance((float)(Math.PI*0.75));
		GridLineModelFitter fitter = new GridLineModelFitter((float)(Math.PI*0.75));

		ModelMatcher<LinePolar2D_F32, Edgel> matcher =
				new Ransac<LinePolar2D_F32,Edgel>(123123,manager,fitter,distance,25,1);

		ImageGradient<I,D> gradient = FactoryDerivative.sobel(imageType, derivType);

		System.out.println("Image width "+input.width+" height "+input.height);

		ConvertBufferedImage.convertFromSingle(image, input, imageType);
		gradient.process(input, derivX, derivY);
		GGradientToEdgeFeatures.intensityAbs(derivX, derivY, edgeIntensity);

		// non-max suppression on the lines
//		GGradientToEdgeFeatures.direction(derivX,derivY,orientation);
//		GradientToEdgeFeatures.discretizeDirection4(orientation,direction);
//		GradientToEdgeFeatures.nonMaxSuppression4(edgeIntensity,direction,suppressed);

		GThresholdImageOps.threshold(edgeIntensity,detected,30,false);

		GridRansacLineDetector<ImageFloat32> alg = new ImplGridRansacLineDetector_F32(40,10,matcher);

		alg.process((ImageFloat32) derivX, (ImageFloat32) derivY, detected);

		MatrixOfList<LineSegment2D_F32> gridLine = alg.getFoundLines();

		ConnectLinesGrid connect = new ConnectLinesGrid(Math.PI*0.01,1,8);
//		connect.process(gridLine);
//		LineImageOps.pruneClutteredGrids(gridLine,3);
		List<LineSegment2D_F32> found = gridLine.createSingleList();
		System.out.println("size = "+found.size());
		LineImageOps.mergeSimilar(found,(float)(Math.PI*0.03),5f);
//		LineImageOps.pruneSmall(found,40);
		System.out.println("after size = "+found.size());

		ImageLinePanel gui = new ImageLinePanel();
		gui.setBackground(image);
		gui.setLineSegments(found);
		gui.setPreferredSize(new Dimension(image.getWidth(),image.getHeight()));

		BufferedImage renderedBinary = VisualizeBinaryData.renderBinary(detected, null);

		ShowImages.showWindow(renderedBinary,"Detected Edges");
		ShowImages.showWindow(gui,"Detected Lines");
	}

	public static void main( String args[] ) {
		VisualizeLineRansac<ImageFloat32,ImageFloat32> app =
				new VisualizeLineRansac<ImageFloat32,ImageFloat32>(ImageFloat32.class,ImageFloat32.class);

//		app.process(UtilImageIO.loadImage("../data/evaluation/simple_objects.jpg"));
//		app.process(UtilImageIO.loadImage("../data/evaluation/shapes01.png"));
		app.process(UtilImageIO.loadImage("../data/evaluation/lines_indoors.jpg"));
//		app.process(UtilImageIO.loadImage("../data/evaluation/outdoors01.jpg"));
	}
}
