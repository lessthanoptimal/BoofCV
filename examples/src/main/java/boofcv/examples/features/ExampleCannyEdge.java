/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.examples.features;

import boofcv.alg.feature.detect.edge.CannyEdge;
import boofcv.alg.feature.detect.edge.EdgeContour;
import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.Contour;
import boofcv.factory.feature.detect.edge.FactoryEdgeDetectors;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.ConnectRule;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Demonstration of the Canny edge detection algorithm.  In this implementation the output can be a binary image and/or
 * a graph describing each contour.
 *
 * @author Peter Abeles
 */
public class ExampleCannyEdge {

	public static void main( String args[] ) {
		BufferedImage image = UtilImageIO.loadImage(UtilIO.pathExample("simple_objects.jpg"));

		GrayU8 gray = ConvertBufferedImage.convertFrom(image,(GrayU8)null);
		GrayU8 edgeImage = gray.createSameShape();

		// Create a canny edge detector which will dynamically compute the threshold based on maximum edge intensity
		// It has also been configured to save the trace as a graph.  This is the graph created while performing
		// hysteresis thresholding.
		CannyEdge<GrayU8,GrayS16> canny = FactoryEdgeDetectors.canny(2,true, true, GrayU8.class, GrayS16.class);

		// The edge image is actually an optional parameter.  If you don't need it just pass in null
		canny.process(gray,0.1f,0.3f,edgeImage);

		// First get the contour created by canny
		List<EdgeContour> edgeContours = canny.getContours();
		// The 'edgeContours' is a tree graph that can be difficult to process.  An alternative is to extract
		// the contours from the binary image, which will produce a single loop for each connected cluster of pixels.
		// Note that you are only interested in external contours.
		List<Contour> contours = BinaryImageOps.contour(edgeImage, ConnectRule.EIGHT, null);

		// display the results
		BufferedImage visualBinary = VisualizeBinaryData.renderBinary(edgeImage, false, null);
		BufferedImage visualCannyContour = VisualizeBinaryData.renderContours(edgeContours,null,
				gray.width,gray.height,null);
		BufferedImage visualEdgeContour = new BufferedImage(gray.width, gray.height,BufferedImage.TYPE_INT_RGB);
		VisualizeBinaryData.render(contours, (int[]) null, visualEdgeContour);

		ListDisplayPanel panel = new ListDisplayPanel();
		panel.addImage(visualBinary,"Binary Edges from Canny");
		panel.addImage(visualCannyContour, "Canny Trace Graph");
		panel.addImage(visualEdgeContour,"Contour from Canny Binary");
		ShowImages.showWindow(panel,"Canny Edge", true);
	}
}
