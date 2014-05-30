/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.feature.shapes.FitData;
import boofcv.alg.feature.shapes.ShapeFittingOps;
import boofcv.alg.filter.binary.Contour;
import boofcv.struct.PointIndex_I32;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.EllipseRotated_F64;
import processing.core.PImage;

import java.util.ArrayList;
import java.util.List;

/**
 * Storage for a the contours from a binary image and the labeled blobs inside the image.
 *
 * @author Peter Abeles
 */
public class SimpleContour {
	Contour contour;

	public SimpleContour(Contour contour) {
		this.contour = contour;
	}

	/**
	 * Fits a polygon to the specified contour.
	 *
	 * @see ShapeFittingOps#fitPolygon(java.util.List, boolean, double, double, int)
	 *
	 * @param external true for the external contour or false for all the internal contours
	 * @param toleranceDist Maximum distance away each point in the sequence can be from a line, in pixels.  Try 2.
	 * @param toleranceAngle Tolerance for fitting angles, in radians. Try 0.1
	 * @return List of polygons described by their vertexes
	 */
	public List<List<Point2D_I32>> fitPolygon( boolean external , double toleranceDist, double toleranceAngle ) {
		List<List<Point2D_I32>> polygons = new ArrayList<List<Point2D_I32>>();

		int numIterations = 20;

		if( external ) {
			List<PointIndex_I32> output = ShapeFittingOps.
					fitPolygon(contour.external, true, toleranceDist, toleranceAngle, numIterations);

			List<Point2D_I32> poly = new ArrayList<Point2D_I32>();
			for( PointIndex_I32 p : output ) {
				poly.add( new Point2D_I32(p.x,p.y));
			}
			polygons.add(poly);
		} else {
			for( List<Point2D_I32> i : contour.internal ) {
				List<PointIndex_I32> output = ShapeFittingOps.
						fitPolygon(i, true, toleranceDist, toleranceAngle, numIterations);

				List<Point2D_I32> poly = new ArrayList<Point2D_I32>();
				for (PointIndex_I32 p : output) {
					poly.add(new Point2D_I32(p.x, p.y));
				}
				polygons.add(poly);
			}
		}

		return polygons;
	}

	/**
	 * Fits ellipse(s) to the specified contour
	 *
	 * @see boofcv.alg.feature.shapes.ShapeFittingOps#fitEllipse_I32
	 *
	 * @param external true for the external contour or false for all the internal contours
	 * @return List of found ellipses
	 */
	public List<EllipseRotated_F64> fitEllipses( boolean external ) {
		List<EllipseRotated_F64> ellipses = new ArrayList<EllipseRotated_F64>();

		if( external ) {
			FitData<EllipseRotated_F64> found = ShapeFittingOps.fitEllipse_I32(contour.external,0,false,null);
			ellipses.add(found.shape);
		} else {
			for( List<Point2D_I32> i : contour.internal ) {
				FitData<EllipseRotated_F64> found = ShapeFittingOps.fitEllipse_I32(i,0,false,null);
				ellipses.add(found.shape);
			}
		}

		return ellipses;
	}

	public void visualize( PImage image , int colorExternal , int colorInternal )  {
		for( Point2D_I32 p : contour.external ) {
			int index = p.y * image.width + p.x;
			image.pixels[index] = colorExternal;
		}

		for( List<Point2D_I32> i : contour.internal ) {
			for( Point2D_I32 p : i ) {
				int index = p.y * image.width + p.x;
				image.pixels[index] = colorInternal;
			}
		}
	}

	public Contour getContour() {
		return contour;
	}
}
