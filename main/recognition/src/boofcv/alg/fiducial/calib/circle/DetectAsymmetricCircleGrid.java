/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.calib.circle;

import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.fiducial.calib.circle.EllipseClustersIntoAsymmetricGrid.Grid;
import boofcv.alg.shapes.ellipse.BinaryEllipseDetector;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.EllipseRotated_F64;
import org.ddogleg.struct.FastQueue;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Detects asymmetric grids of circles.  The grid is composed of two regular grids which are offset by half a period.
 * See image below for an example.  Rows and columns are counted by counting every row even if they are offset
 * from each other.</p>
 *
 * <p>
 * For each circle there is one control point.  The control point is first found by detecting all the ellipses, which
 * is what a circle appears to be under perspective distortion.  The center the ellipse might not match the physical
 * center of the circle.  The intersection of lines does not change under perspective distortion.  The outer common
 * tangent lines between neighboring ellipses are found.  Then the intersection of two such lines is found.  This
 * intersection will be the physical center of the circle.
 * </p>
 *
 * <center>
 * <img src="doc-files/asymcirclegrid.jpg"/>
 * </center>
 * Example of a 8 by 5 grid; row, column.
 *
 * @author Peter Abeles
 */
public class DetectAsymmetricCircleGrid<T extends ImageGray> {

	private BinaryEllipseDetector<T> ellipseDetector;
	private InputToBinary<T> inputToBinary;

	private GrayU8 binary = new GrayU8(1,1);

	// description of the calibration target
	private int numRows, numCols;

	// converts ellipses into clusters
	private EllipsesIntoClusters clustering;
	private EllipseClustersIntoAsymmetricGrid grider;

	private List<Grid> validGrids = new ArrayList<>();

	public DetectAsymmetricCircleGrid( int numRows , int numCols , EllipsesIntoClusters clustering,
									   BinaryEllipseDetector<T> ellipseDetector,
									   InputToBinary<T> inputToBinary) {
		this.numRows = numRows;
		this.numCols = numCols;

		this.clustering = clustering;
		this.grider = new EllipseClustersIntoAsymmetricGrid();
	}

	public void process(T gray) {
		this.binary.reshape(gray.width,gray.height);

		inputToBinary.process(gray, binary);

		ellipseDetector.process(gray, binary);
		List<EllipseRotated_F64> found = ellipseDetector.getFoundEllipses().toList();

		List<List<EllipsesIntoClusters.Node>> clusters = new ArrayList<>();

		clustering.process(found, clusters);

		pruneIncorrectSize(clusters, numRows*numCols);

		grider.process(found, clusters);

		FastQueue<Grid> grids = grider.getGrids();

		pruneIncorrectShape(grids,numRows,numCols);

		validGrids.clear();
		for (int i = 0; i < grids.size(); i++) {
			Grid g = grids.get(i);
			putGridIntoCanonical(g,numRows,numCols);
			validGrids.add( g );
		}
	}

	/**
	 * Puts the grid into a canonical orientation
	 */
	static void putGridIntoCanonical(Grid g , int numRows , int numCols ) {
		if( g.columns != numCols ) {
			rotateGridCCW(g);
			rotateGridCCW(g);
		}

		if( g.get(0,0) == null ) {
			flipHorizontal(g);
		}
	}

	static void rotateGridCCW( Grid g ) {
		// TODO implement
	}

	static void flipHorizontal( Grid g ) {
		// TODO implement
	}

	/**
	 * Remove grids which cannot possible match the expected shape
	 */
	static void pruneIncorrectShape(FastQueue<Grid> grids , int numRows, int numCols ) {
		// prune clusters which can't be a member calibration target
		for (int i = grids.size()-1; i >= 0; i--) {
			Grid g = grids.get(i);
			if ((g.rows != numRows || g.columns != numCols) && (g.rows != numCols || g.columns != numRows)) {
				grids.remove(i);
			}
		}
	}

	/**
	 * Prune clusters which do not have the expected number of elements
	 */
	static void pruneIncorrectSize(List<List<EllipsesIntoClusters.Node>> clusters, int N) {
		// prune clusters which can't be a member calibration target
		for (int i = clusters.size()-1; i >= 0; i--) {
			if( clusters.get(i).size() != N ) {
				clusters.remove(i);
			}
		}
	}

	void putIntoCanonical( FastQueue<Grid> grids ) {
		validGrids.clear();

		for (int i = 0; i < grids.size; i++) {
			Grid grid = grids.get(i);
			if( grid.columns == numCols ) {
				if( grid.rows == numRows ) {
					// TODO could it be flipped?
					validGrids.add(grid);
				}
			} else if( grid.columns == numRows && grid.rows == numCols ) {
				// TODO rotate to put into canonical

				validGrids.add( grid );
			}
		}

	}

	public List<Point2D_F64> getCalibrationPoints() {
		return null;
	}

	public GrayU8 getBinary() {
		return binary;
	}

	public int getColumns() {
		return numCols;
	}

	public int getRows() {
		return numRows;
	}
}
