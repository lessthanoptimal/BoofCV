/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.feature.detect.edge.GGradientToEdgeFeatures;
import boofcv.alg.feature.detect.line.GridRansacLineDetector;
import boofcv.alg.feature.detect.line.LineImageOps;
import boofcv.alg.feature.detect.line.gridline.Edgel;
import boofcv.alg.feature.detect.line.gridline.GridLineModelDistance;
import boofcv.alg.feature.detect.line.gridline.GridLineModelFitter;
import boofcv.alg.feature.detect.line.gridline.ImplGridRansacLineDetector_F32;
import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.feature.ImageLinePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.feature.MatrixOfList;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.fitting.line.ModelManagerLinePolar2D_F32;
import georegression.struct.line.LinePolar2D_F32;
import georegression.struct.line.LineSegment2D_F32;
import org.ddogleg.fitting.modelset.ModelManager;
import org.ddogleg.fitting.modelset.ModelMatcherPost;
import org.ddogleg.fitting.modelset.ransac.Ransac;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Objects;

/**
 * Computes the Hough Polar transform and displays some of its steps and the detected lines
 *
 * @author Peter Abeles
 */
public class VisualizeLineRansac<I extends ImageGray<I>, D extends ImageGray<D>> {

	Class<I> imageType;
	Class<D> derivType;

	public VisualizeLineRansac( Class<I> imageType, Class<D> derivType ) {
		this.imageType = imageType;
		this.derivType = derivType;
	}

	public void process( @Nullable BufferedImage image ) {
		Objects.requireNonNull(image);
//		int regionSize = 40;

		I input = GeneralizedImageOps.createSingleBand(imageType, image.getWidth(), image.getHeight());
		D derivX = GeneralizedImageOps.createSingleBand(derivType, image.getWidth(), image.getHeight());
		D derivY = GeneralizedImageOps.createSingleBand(derivType, image.getWidth(), image.getHeight());
		GrayF32 edgeIntensity = new GrayF32(input.width, input.height);
//		GrayF32 suppressed =  new GrayF32(input.width,input.height);
//		GrayF32 orientation =  new GrayF32(input.width,input.height);
//		GrayS8 direction = new GrayS8(input.width,input.height);
		GrayU8 detected = new GrayU8(input.width, input.height);

		ModelManager<LinePolar2D_F32> manager = new ModelManagerLinePolar2D_F32();

		ModelMatcherPost<LinePolar2D_F32, Edgel> matcher = new Ransac<>(123123, 25, 1, manager, Edgel.class);
		matcher.setModel(
				()->new GridLineModelFitter((float)(Math.PI*0.75)),
				()->new GridLineModelDistance((float)(Math.PI*0.75)));

		ImageGradient<I, D> gradient = FactoryDerivative.sobel(imageType, derivType);

		System.out.println("Image width " + input.width + " height " + input.height);

		ConvertBufferedImage.convertFromSingle(image, input, imageType);
		gradient.process(input, derivX, derivY);
		GGradientToEdgeFeatures.intensityAbs(derivX, derivY, edgeIntensity);

		// non-max suppression on the lines
//		GGradientToEdgeFeatures.direction(derivX,derivY,orientation);
//		GradientToEdgeFeatures.discretizeDirection4(orientation,direction);
//		GradientToEdgeFeatures.nonMaxSuppression4(edgeIntensity,direction,suppressed);

		GThresholdImageOps.threshold(edgeIntensity, detected, 30, false);

		GridRansacLineDetector<GrayF32> alg = new ImplGridRansacLineDetector_F32(40, 10, matcher);

		alg.process((GrayF32)derivX, (GrayF32)derivY, detected);

		MatrixOfList<LineSegment2D_F32> gridLine = alg.getFoundLines();

//		ConnectLinesGrid connect = new ConnectLinesGrid(Math.PI*0.01,1,8);
//		connect.process(gridLine);
//		LineImageOps.pruneClutteredGrids(gridLine,3);
		List<LineSegment2D_F32> found = gridLine.createSingleList();
		System.out.println("size = " + found.size());
		LineImageOps.mergeSimilar(found, (float)(Math.PI*0.03), 5f);
//		LineImageOps.pruneSmall(found,40);
		System.out.println("after size = " + found.size());

		ImageLinePanel gui = new ImageLinePanel();
		gui.setImage(image);
		gui.setLineSegments(found);
		gui.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));

		BufferedImage renderedBinary = VisualizeBinaryData.renderBinary(detected, false, null);

		ShowImages.showWindow(renderedBinary, "Detected Edges");
		ShowImages.showWindow(gui, "Detected Lines");
	}

	public static void main( String[] args ) {
		VisualizeLineRansac<GrayF32, GrayF32> app =
				new VisualizeLineRansac<>(GrayF32.class, GrayF32.class);

//		app.process(UtilImageIO.loadImage(UtilIO.pathExample("simple_objects.jpg"));
//		app.process(UtilImageIO.loadImage(UtilIO.pathExample("shapes/shapes01.png"));
		app.process(UtilImageIO.loadImage(UtilIO.pathExample("lines_indoors.jpg")));
//		app.process(UtilImageIO.loadImage(UtilIO.pathExample("outdoors01.jpg"));
	}
}
