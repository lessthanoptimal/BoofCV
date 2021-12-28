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

package boofcv.examples.features;

import boofcv.alg.feature.detect.edge.CannyEdge;
import boofcv.alg.feature.detect.edge.EdgeContour;
import boofcv.alg.feature.detect.edge.EdgeSegment;
import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.Contour;
import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.alg.shapes.ShapeFittingOps;
import boofcv.factory.feature.detect.edge.FactoryEdgeDetectors;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.feature.VisualizeShapes;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.ConnectRule;
import boofcv.struct.PointIndex_I32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_I32;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Random;

/**
 * Demonstration of how to convert a point sequence describing an objects outline/contour into a sequence of line
 * segments. Useful when analysing shapes such as squares and triangles or when trying to simply the low level
 * pixel output.
 *
 * @author Peter Abeles
 */
public class ExampleFitPolygon {

	// Used to bias it towards more or fewer sides. larger number = fewer sides
	static double cornerPenalty = 0.25;
	// The fewest number of pixels a side can have
	static int minSide = 10;

	static ListDisplayPanel gui = new ListDisplayPanel();

	/**
	 * Fits polygons to found contours around binary blobs.
	 */
	public static void fitBinaryImage( GrayF32 input ) {

		GrayU8 binary = new GrayU8(input.width, input.height);
		BufferedImage polygon = new BufferedImage(input.width, input.height, BufferedImage.TYPE_INT_RGB);

		// the mean pixel value is often a reasonable threshold when creating a binary image
		double mean = ImageStatistics.mean(input);

		// create a binary image by thresholding
		ThresholdImageOps.threshold(input, binary, (float)mean, true);

		// reduce noise with some filtering
		GrayU8 filtered = BinaryImageOps.erode8(binary, 1, null);
		filtered = BinaryImageOps.dilate8(filtered, 1, null);

		// Find internal and external contour around each shape
		List<Contour> contours = BinaryImageOps.contour(filtered, ConnectRule.EIGHT, null);

		// Fit a polygon to each shape and draw the results
		Graphics2D g2 = polygon.createGraphics();
		g2.setStroke(new BasicStroke(2));

		for (Contour c : contours) {
			// Fit the polygon to the found external contour. Note loop = true
			List<PointIndex_I32> vertexes = ShapeFittingOps.fitPolygon(c.external, true, minSide, cornerPenalty);

			g2.setColor(Color.RED);
			VisualizeShapes.drawPolygon(vertexes, true, g2);

			// handle internal contours now
			g2.setColor(Color.BLUE);
			for (List<Point2D_I32> internal : c.internal) {
				vertexes = ShapeFittingOps.fitPolygon(internal, true, minSide, cornerPenalty);
				VisualizeShapes.drawPolygon(vertexes, true, g2);
			}
		}

		gui.addImage(polygon, "Binary Blob Contours");
	}

	/**
	 * Fits a sequence of line-segments into a sequence of points found using the Canny edge detector. In this case
	 * the points are not connected in a loop. The canny detector produces a more complex tree and the fitted
	 * points can be a bit noisy compared to the others.
	 */
	public static void fitCannyEdges( GrayF32 input ) {

		BufferedImage displayImage = new BufferedImage(input.width, input.height, BufferedImage.TYPE_INT_RGB);

		// Finds edges inside the image
		CannyEdge<GrayF32, GrayF32> canny =
				FactoryEdgeDetectors.canny(2, true, true, GrayF32.class, GrayF32.class);

		canny.process(input, 0.1f, 0.3f, null);
		List<EdgeContour> contours = canny.getContours();

		Graphics2D g2 = displayImage.createGraphics();
		g2.setStroke(new BasicStroke(2));

		// used to select colors for each line
		Random rand = new Random(234);

		for (EdgeContour e : contours) {
			g2.setColor(new Color(rand.nextInt()));

			for (EdgeSegment s : e.segments) {
				// fit line segments to the point sequence. Note that loop is false
				List<PointIndex_I32> vertexes = ShapeFittingOps.fitPolygon(s.points, false, minSide, cornerPenalty);

				VisualizeShapes.drawPolygon(vertexes, false, g2);
			}
		}

		gui.addImage(displayImage, "Canny Trace");
	}

	/**
	 * Detects contours inside the binary image generated by canny. Only the external contour is relevant. Often
	 * easier to deal with than working with Canny edges directly.
	 */
	public static void fitCannyBinary( GrayF32 input ) {

		BufferedImage displayImage = new BufferedImage(input.width, input.height, BufferedImage.TYPE_INT_RGB);
		GrayU8 binary = new GrayU8(input.width, input.height);

		// Finds edges inside the image
		CannyEdge<GrayF32, GrayF32> canny =
				FactoryEdgeDetectors.canny(2, false, true, GrayF32.class, GrayF32.class);

		canny.process(input, 0.1f, 0.3f, binary);

		// Only external contours are relevant
		List<Contour> contours = BinaryImageOps.contourExternal(binary, ConnectRule.EIGHT);

		Graphics2D g2 = displayImage.createGraphics();
		g2.setStroke(new BasicStroke(2));

		// used to select colors for each line
		Random rand = new Random(234);

		for (Contour c : contours) {
			List<PointIndex_I32> vertexes = ShapeFittingOps.fitPolygon(c.external, true, minSide, cornerPenalty);

			g2.setColor(new Color(rand.nextInt()));
			VisualizeShapes.drawPolygon(vertexes, true, g2);
		}

		gui.addImage(displayImage, "Canny Contour");
	}

	public static void main( String[] args ) {
		// load and convert the image into a usable format
		BufferedImage image = UtilImageIO.loadImageNotNull(UtilIO.pathExample("shapes/shapes02.png"));
		GrayF32 input = ConvertBufferedImage.convertFromSingle(image, null, GrayF32.class);

		fitCannyEdges(input);
		fitCannyBinary(input);
		fitBinaryImage(input);
		gui.addImage(image, "Original");

		ShowImages.showWindow(gui, "Polygon from Contour", true);
	}
}
