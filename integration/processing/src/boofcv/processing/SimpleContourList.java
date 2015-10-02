/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.processing;

import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.EllipseRotated_F64;
import processing.core.PConstants;
import processing.core.PImage;

import java.util.ArrayList;
import java.util.List;

/**
 * List of {@link boofcv.processing.SimpleContour}
 *
 * @author Peter Abeles
 */
public class SimpleContourList {
	List<SimpleContour> contour = new ArrayList<SimpleContour>();
	// input image width and height
	int width,height;

	public SimpleContourList(List<SimpleContour> contour, int width, int height) {
		this.contour = contour;
		this.width = width;
		this.height = height;
	}

	/**
	 * Fits a polygon to the specified contour.
	 *
	 * @see boofcv.alg.shapes.ShapeFittingOps#fitPolygon(java.util.List, boolean, double, double, int)
	 *
	 * @param external true for the external contour or false for all the internal contours
	 * @param splitFraction A line will be split if a point is more than this fraction of its
	 *                     length away from the line. Try 0.05
	 * @param minimumSplitPixels A line will always be split if a point is more than this number of pixels away. try 5.0
	 * @return List of polygons described by their vertexes
	 */
	public List<List<Point2D_I32>> fitPolygons( boolean external , double splitFraction, double minimumSplitPixels ) {
		List<List<Point2D_I32>> polygons = new ArrayList<List<Point2D_I32>>();

		for (int i = 0; i < contour.size(); i++) {
			polygons.addAll(contour.get(i).fitPolygon(external,splitFraction,minimumSplitPixels));
		}

		return polygons;
	}

	/**
	 * Fits ellipse(s) to the specified contour
	 *
	 * @see boofcv.alg.shapes.ShapeFittingOps#fitEllipse_I32
	 *
	 * @param external true for the external contour or false for all the internal contours
	 * @return List of found ellipses
	 */
	public List<EllipseRotated_F64> fitEllipses( boolean external ) {
		List<EllipseRotated_F64> ellipses = new ArrayList<EllipseRotated_F64>();

		for (int i = 0; i < contour.size(); i++) {
			ellipses.addAll(contour.get(i).fitEllipses(external));
		}

		return ellipses;
	}

	public int size(){
		return contour.size();
	}

	public List<SimpleContour> getList() {
		return contour;
	}

	public PImage visualize() {
		PImage out = new PImage(width, height, PConstants.RGB);

		for( SimpleContour sc : contour ) {
			sc.visualize(out,0xFFFF0000,0xFF00FF00);
		}

		return out;
	}
}
