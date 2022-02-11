/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.aztec;

import boofcv.abst.geo.Estimate1ofEpipolar;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.geo.AssociatedPair;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import org.ddogleg.struct.DogArray;
import org.ejml.data.DMatrixRMaj;

/**
 * Everything you need to go from a grid coordinate into pixels using a homography
 *
 * @author Peter Abeles
 */
public class GridToPixelHelper {
	// Used to compute the homography
	protected Estimate1ofEpipolar computeH = FactoryMultiView.homographyTLS();

	// Storage for the homography from grid to image coordinates
	protected DMatrixRMaj gridToImage = new DMatrixRMaj(3, 3);

	// internal workspace
	protected DogArray<AssociatedPair> pairs = new DogArray<>(AssociatedPair::new, AssociatedPair::zero);
	protected Point2D_F64 gridPoint = new Point2D_F64();

	/**
	 * Defines a grid coordinate system with the origin at corner[0] in the polygon
	 *
	 * @param polygon Polygon which defines the square grid in image pixels
	 * @param squaresWide Number of squares wide the grid coordinate is inside the square
	 */
	public void initOriginCorner0( Polygon2D_F64 polygon, int squaresWide ) {
		// Compute a homography from local grid coordinates around the square to image pixels
		pairs.resetResize(4);
		pairs.get(0).setTo(0, 0, polygon.get(0));
		pairs.get(1).setTo(squaresWide, 0, polygon.get(1));
		pairs.get(2).setTo(squaresWide, squaresWide, polygon.get(2));
		pairs.get(3).setTo(0, squaresWide, polygon.get(3));

		// Compute a homography to map a grid to pixel coordinate
		computeH.process(pairs.toList(), gridToImage);
	}

	/** Computes location of grid coordinate on the image in pixels */
	public void convert( double gridX, double gridY, Point2D_F64 pixel ) {
		gridPoint.setTo(gridX, gridY);
		GeometryMath_F64.mult(gridToImage, gridPoint, pixel);
	}
}
