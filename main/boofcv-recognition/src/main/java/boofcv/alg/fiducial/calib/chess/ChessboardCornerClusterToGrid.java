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

package boofcv.alg.fiducial.calib.chess;

import boofcv.alg.fiducial.calib.chess.ChessboardCornerGraph.Node;
import boofcv.misc.BoofMiscOps;
import georegression.metric.UtilAngle;
import georegression.struct.shapes.Rectangle2D_I32;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.sorting.QuickSort_F64;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_B;
import org.ddogleg.struct.VerbosePrint;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.*;

/**
 * Given a chessboard corner cluster find the grid which it matches. A grid is an ordering of corners in a specific
 * order and such that their edges are consistent and form a 4-neighborhood. The grid will be in "standard order".
 * Depending on the chessboard pattern there might be multiple multiple configurations that are in standard order
 * or a unique ordering.
 *
 * On a grid a "Corner" is defined as a corner point which has a black square attached to it which is not attached
 * to any more corner points. These are useful in that they allow orientation to be uniquely defined under certain
 * conditions. If the "allowNoCorner" flag is true then grid with no corners are allowed.
 *
 * The corner point at grid(0,0) will be set to a valid corner point with a "corner" that's closest to the (0,0) pixel
 * coordinate. If there are no corner points then the corner which is closest to (0,0) pixel is selected.
 *
 * Edges will be ordered in CCW direction and the index of an edge which connects two adjacent corners to
 * each other follows the (i+2)%4 relationship. If multiple corners can be (0,0) then the one closest to
 * the top left corner will be selected.
 *
 * The largest rectangular grid is returned. The idea being that it's not uncommon for there to be noise
 * which adds an extra stray element. This will remove those stray elements.
 *
 * @author Peter Abeles
 */
public class ChessboardCornerClusterToGrid implements VerbosePrint {
	/** If true then it will find the largest rectangular grid and return this. This will often remove stray noise */
	public boolean findLargestGrid = true;
	/** If false then a hole in the middle of the grid will be considered a failure */
	public boolean allowHoles = false;

	// used to put edge into CW order
	QuickSort_F64 sorter = new QuickSort_F64();
	double[] directions = new double[4];
	int[] order = new int[4];
	Node[] tmpEdges = new Node[4];

	// Indicates which corners have been added to the sparse grd
	DogArray_B marked = new DogArray_B();
	Queue<Node> open = new ArrayDeque<>(); // FIFO queue

	// Workspace for isCornerValidOrigin
	List<Node> edgeList = new ArrayList<>();

	// Storage for finding corners in a grid
	List<Node> cornerList = new ArrayList<>();

	// See documentation above. if true then the requirement that the (0,0) grid element be a corner is removed.
	@Getter @Setter boolean requireCornerSquares = false;

	// Used to optionally print extra debugging information
	@Nullable PrintStream verbose;

	// optional check on the shape
	@Setter @Nullable CheckShape checkShape;

	/** Storage for elements in the sparse grid. Elements are not ordered. */
	@Getter DogArray<GridElement> sparseGrid = new DogArray<>(GridElement::new, GridElement::reset);
	/** Dimension of the sparse/dense grids */
	@Getter protected int sparseCols, sparseRows;
	/** Dense grid. Empty elements are set to null */
	GridElement[] denseGrid = new GridElement[0];

	// Region of the dense grid that should be copied into the output
	Rectangle2D_I32 region = new Rectangle2D_I32();

	/**
	 * Converts the graph into a sparse grid. This ensures that chessboard constraints are logically consistent in
	 * the grid
	 *
	 * @return true if no errors were detected
	 */
	public boolean clusterToSparse( ChessboardCornerGraph cluster ) {
		sparseGrid.reset();

		// Get the edges in a consistent order
		if (!orderEdges(cluster))
			return false;

		// Find the grid which defines a chessboard pattern
		if (!createSparseGrid(cluster.corners))
			return false;

		return true;
	}

	/**
	 * After the sparse grid has been found this function can then be called to extract a complete target. It will
	 * find the largest rectangular region with all the elements contained.
	 *
	 * @param info (Output) Found rectangular grid
	 * @return true if no errors were detected
	 */
	public boolean sparseToGrid( GridInfo info ) {
		info.reset();

		sparseToDense();

		if (!findLargestRectangle(region)) {
			return false;
		}

		if (!copyDenseRectangle(region.y0, region.y1, region.x0, region.x1, info)) {
			return false;
		}

		// Put grid elements into a specific order
		// select a valid corner to be (0,0). If there are multiple options select the one which is
		int corner = selectCorner(info);
		if (corner == -1) {
			if (verbose != null) verbose.println("Failed to find valid corner.");
			return false;
		}
		// rotate the grid until the select corner is at (0,0)
		for (int i = 0; i < corner; i++) {
			rotateCCW(info);
		}

		return true;
	}

	/**
	 * Creates a grid where a sparse data structure is used to define it. The shape and which elements
	 * are filled in are not known initially. This is used as an intermediate step to building the dense grid
	 */
	boolean createSparseGrid( DogArray<Node> corners ) {
		marked.resize(corners.size);
		marked.fill(false);

		sparseGrid.resize(corners.size);
		for (int i = 0; i < sparseGrid.size; i++) {
			sparseGrid.get(i).reset();
		}

		open.clear();

		GridElement g = sparseGrid.get(0);
		Node n = corners.get(0);
		g.node = n;
		g.row = g.col = 0;
		marked.set(0, true);
		open.add(n);

		int minCol = Integer.MAX_VALUE;
		int minRow = Integer.MAX_VALUE;
		sparseCols = -1;
		sparseRows = -1;

		while (!open.isEmpty()) {
			n = open.remove();
			g = sparseGrid.get(n.index);

			for (int idx = 0; idx < 4; idx++) {
				Node e = n.edges[idx];
				if (e == null)
					continue;

				GridElement ge = sparseGrid.get(e.index);
				int row = g.row, col = g.col;

				switch (idx) {
					case 0 -> col += 1;
					case 1 -> row += 1;
					case 2 -> col -= 1;
					case 3 -> row -= 1;
				}

				if (!ge.isAssigned()) {
					ge.node = e;
					ge.row = row;
					ge.col = col;
					if (row < minRow) minRow = row;
					if (col < minCol) minCol = col;
					if (row > sparseRows) sparseRows = row;
					if (col > sparseCols) sparseCols = col;
				} else if (ge.row != row || ge.col != col) {
					if (verbose != null)
						verbose.println("Contradiction in graph found.");
					return false;
				}

				if (!marked.get(e.index)) {
					open.add(e);
					marked.set(e.index, true);
				}
			}
		}

		// make sure all cols and rows are >= 0
		if (minCol < 0 || minRow < 0) {
			if (minRow < 0)
				sparseRows += -minRow;
			if (minCol < 0)
				sparseCols += -minCol;
			for (int i = 0; i < sparseGrid.size; i++) {
				GridElement e = sparseGrid.get(i);
				if (!e.isAssigned())
					throw new RuntimeException("BUG! grid element not assigned");
				e.col -= minCol;
				e.row -= minRow;
			}
		}
		sparseRows += 1;
		sparseCols += 1;

		return true;
	}

	/**
	 * Converts the sparse into a dense grid
	 */
	public void sparseToDense() {
		int N = sparseCols*sparseRows;
		if (denseGrid.length < N)
			denseGrid = new GridElement[N];
		Arrays.fill(denseGrid, 0, N, null);

		for (int i = 0; i < sparseGrid.size; i++) {
			GridElement g = sparseGrid.get(i);
			denseGrid[g.row*sparseCols + g.col] = g;
		}
	}

	/**
	 * Finds the largest complete rectangle with no holes in it.
	 */
	boolean findLargestRectangle( Rectangle2D_I32 rectangle ) {
		int row0 = 0;
		int row1 = sparseRows;
		int col0 = 0;
		int col1 = sparseCols;

		int[] rowZeros = new int[sparseRows];
		int[] colZeros = new int[sparseCols];

		for (int i = 0; i < sparseRows; i++) {
			rowZeros[i] = countZeros(i, i + 1, 0, sparseCols, 0, 1);
		}
		for (int i = 0; i < sparseCols; i++) {
			colZeros[i] = countZeros(0, sparseRows, i, i + 1, 1, 0);
		}

		boolean success = false;
		while (row0 < row1 && col0 < col1) {
			int rz = Math.max(rowZeros[row0], rowZeros[row1 - 1]);
			int cz = Math.max(colZeros[col0], colZeros[col1 - 1]);

			if (rz == 0 && cz == 0) {
				success = true;
				break;
			}

			// prune the outside edge with the most zeros
			if (rz > cz) {
				if (rowZeros[row0] > rowZeros[row1 - 1]) {
					for (int i = col0; i < col1; i++) {
						if (grid(row0, i) == null)
							colZeros[i] -= 1;
					}
					row0 += 1;
				} else {
					for (int i = col0; i < col1; i++) {
						if (grid(row1 - 1, i) == null)
							colZeros[i] -= 1;
					}
					row1 -= 1;
				}
			} else {
				if (colZeros[col0] > colZeros[col1 - 1]) {
					for (int i = row0; i < row1; i++) {
						if (grid(i, col0) == null)
							rowZeros[i] -= 1;
					}
					col0 += 1;
				} else {
					for (int i = row0; i < row1; i++) {
						if (grid(i, col1 - 1) == null)
							rowZeros[i] -= 1;
					}
					col1 -= 1;
				}
			}
		}

		if (!success)
			return false;

		// Save the results
		rectangle.x0 = col0;
		rectangle.x1 = col1;
		rectangle.y0 = row0;
		rectangle.y1 = row1;

		return success;
	}

	/**
	 * Copies a portion of the dense grid into GridInfo
	 *
	 * @return true if no errors were found
	 */
	boolean copyDenseRectangle( int row0, int row1, int col0, int col1, GridInfo info ) {
		info.nodes.clear();
		info.rows = row1 - row0;
		info.cols = col1 - col0;

		for (int row = row0; row < row1; row++) {
			for (int col = col0; col < col1; col++) {
				GridElement g = grid(row, col);
				if (g == null) {
					if (verbose != null)
						verbose.println("Failed due to hole inside of grid");
					return false;
				}
				info.nodes.add(g.node);
			}
		}

		return true;
	}

	int countZeros( int row0, int row1, int col0, int col1, int stepRow, int stepCol ) {
		int total = 0;
		while (row0 != row1 && col0 != col1) {
			if (grid(row0, col0) == null)
				total++;

			row0 += stepRow;
			col0 += stepCol;
		}
		return total;
	}

	final GridElement grid( int row, int col ) {
		return denseGrid[row*sparseCols + col];
	}

	/**
	 * Selects a corner to be the grid's origin. 0 = top-left, 1 = top-right, 2 = bottom-right, 3 = bottom-left.
	 *
	 * Looks at each grid and see if it can be valid. Out of the valid list
	 */
	int selectCorner( GridInfo info ) {

		info.lookupGridCorners(cornerList);

		int bestCorner = -1;
		double bestScore = Double.MAX_VALUE;
		boolean bestIsCornerSquare = false;

		for (int i = 0; i < cornerList.size(); i++) {
			Node n = cornerList.get(i);

			boolean corner = isCornerValidOrigin(n);

			// If there are no corner points which are valid corners, then any corner can be the origin if
			// allowNoCorner is true
			if (corner || (!requireCornerSquares && !bestIsCornerSquare)) {
				// sanity check the shape
				if (checkShape != null) {
					if (i%2 == 0) {
						if (!checkShape.isValidShape(info.rows, info.cols)) {
							continue;
						}
					} else {
						if (!checkShape.isValidShape(info.cols, info.rows)) {
							continue;
						}
					}
				}

				// If the distance is to (0,0) pixel is smaller or this is a corner square and the other best
				// is not a corner square
				double distance = n.corner.normSq();
				if (distance < bestScore || (!bestIsCornerSquare && corner)) {
					bestIsCornerSquare |= corner;
					bestScore = distance;
					bestCorner = i;
				}
			}
		}
		info.hasCornerSquare = bestIsCornerSquare;
		return bestCorner;
	}

	/**
	 * A corner can be an origin if the corner's orientation (a line between the two adjacent black squares) and
	 * the line splitting the direction to the two connecting nodes are the same.
	 */
	boolean isCornerValidOrigin( Node candidate ) {
		candidate.putEdgesIntoList(edgeList);
		if (edgeList.size() != 2) {
			throw new RuntimeException("BUG! Should be a corner and have two edges");
		}

		Node a = edgeList.get(0);
		Node b = edgeList.get(1);

		// Find the average angle from the two vectors defined by the two connected nodes
		double dirA = Math.atan2(a.getY() - candidate.getY(), a.getX() - candidate.getX());
		double dirB = Math.atan2(b.getY() - candidate.getY(), b.getX() - candidate.getX());

		double dirAB = UtilAngle.boundHalf(dirA + UtilAngle.distanceCCW(dirA, dirB)/2.0);

		// Find the acute angle between the corner's orientation and the vector
		double acute = UtilAngle.distHalf(dirAB, candidate.getOrientation());

		return acute < Math.PI/4.0;
	}

	/**
	 * Returns true of the candidate is the top-left corner in a white square.
	 *
	 * @param a Corner at top-left
	 * @param c Corner at bottom-right
	 */
	public boolean isWhiteSquareOrientation( Node a, Node c ) {
		// Find the average angle from the two vectors defined by the two connected nodes
		double dirAC = Math.atan2(c.getY() - a.getY(), c.getX() - a.getX());

		// If it's white then there will be a big difference between the orientation and c
		double acute = UtilAngle.distHalf(dirAC, a.getOrientation());

		// A "perfect" angle would be pi/2 with 0 being the worst
		return acute > Math.PI/4.0;
	}

	/**
	 * Put corners into a proper grid. Make sure its a rectangular grid or else return false. Rows and columns
	 * are selected to ensure right hand rule.
	 */
	boolean orderNodes( DogArray<Node> corners, GridInfo info ) {

		// Find a node with just two edges. This is a corner and will be the arbitrary origin in our graph
		Node seed = null;
		for (int i = 0; i < corners.size; i++) {
			Node n = corners.get(i);
			if (n.countEdges() == 2) {
				seed = n;
				break;
			}
		}
		if (seed == null) {
			if (verbose != null) verbose.println("Can't find a corner with just two edges. Aborting");
			return false;
		}

		// find one edge and mark that as the row direction
		int rowEdge = 0;
		while (seed.edges[rowEdge] == null)
			rowEdge = (rowEdge + 1)%4;
		int colEdge = (rowEdge + 1)%4;
		while (seed.edges[colEdge] == null)
			colEdge = (colEdge + 2)%4;

		// if it's left handed swap the row and column direction
		if (!isRightHanded(seed, rowEdge, colEdge)) {
			int tmp = rowEdge;
			rowEdge = colEdge;
			colEdge = tmp;
		}

		// add the corns to list in a row major order
		while (seed != null) {
			int before = info.nodes.size();
			Node n = seed;
			do {
				info.nodes.add(n);
				n = n.edges[colEdge];
			} while (n != null);

			seed = seed.edges[rowEdge];

			if (info.cols == -1) {
				info.cols = info.nodes.size();
			} else {
				int columnsInRow = info.nodes.size() - before;
				if (columnsInRow != info.cols) {
					if (verbose != null) verbose.println("Number of columns in each row is variable");
					return false;
				}
			}
		}
		info.rows = info.nodes.size()/info.cols;
		return true;
	}

	/**
	 * Checks to see if the rows and columns for a coordinate system which is right handed
	 *
	 * @param idxRow Index for moving up a row
	 * @param idxCol index for moving up a column
	 */
	static boolean isRightHanded( Node seed, int idxRow, int idxCol ) {
		Node r = seed.edges[idxRow];
		Node c = seed.edges[idxCol];

		double dirRow = Math.atan2(r.getY() - seed.getY(), r.getX() - seed.getX());
		double dirCol = Math.atan2(c.getY() - seed.getY(), c.getX() - seed.getX());

		return UtilAngle.distanceCW(dirRow, dirCol) < Math.PI;
	}

	/**
	 * Puts the edges in CCW order and aligns edge indexes into pairs.
	 */
	boolean orderEdges( ChessboardCornerGraph cluster ) {
		sortEdgesCCW(cluster.corners);
		return alignEdges(cluster.corners);
	}

	/**
	 * Enforces the rule that an edge in node A has an edge in node B that points back to A at index (i+2)%4.
	 */
	boolean alignEdges( DogArray<Node> corners ) {
		open.clear();
		open.add(corners.get(0));

		marked.resize(corners.size);
		marked.fill(false);

		marked.set(corners.get(0).index, true);

		while (!open.isEmpty()) {
			Node na = open.remove();

			// examine each neighbor and see the neighbor is correctly aligned
			for (int i = 0; i < 4; i++) {
				if (na.edges[i] == null) {
					continue;
				}
				// Compute which index should be an edge pointing back at 'na'
				int j = (i + 2)%4;

				Node nb = na.edges[i];

				// Sanity check. If it has been marked it should be correctly aligned
				if (marked.get(nb.index)) {
					if (nb.edges[j] != na) {
						if (verbose != null)
							verbose.println("BUG! node " + nb.index + " has been processed and edge " + j + " doesn't point to node " + na.index);
						return false;
					}
					continue;
				}

				// Rotate edges
				boolean failed = true;
				for (int attempt = 0; attempt < 4; attempt++) {
					if (nb.edges[j] != na) {
						nb.rotateEdgesDown();
					} else {
						failed = false;
						break;
					}
				}
				if (failed) {
					if (verbose != null) verbose.println("BUG! Can't align edges");
					return false;
				}
				marked.set(nb.index, true);
				open.add(nb);
			}
		}
		return true;
	}

	/**
	 * Sorts edges so that they point towards nodes in an increasing counter clockwise direction
	 */
	void sortEdgesCCW( DogArray<Node> corners ) {
		for (int nodeIdx = 0; nodeIdx < corners.size; nodeIdx++) {
			Node na = corners.get(nodeIdx);

			// reference node to do angles relative to.
			double ref = Double.NaN;
			int count = 0;
			for (int i = 0; i < 4; i++) {
				order[i] = i;
				tmpEdges[i] = na.edges[i];
				if (na.edges[i] == null) {
					directions[i] = Double.MAX_VALUE;
				} else {
					Node nb = na.edges[i];
					double angleB = Math.atan2(nb.getY() - na.getY(), nb.getX() - na.getX());
					if (Double.isNaN(ref)) {
						ref = angleB;
						directions[i] = 0;
					} else {
						directions[i] = UtilAngle.distanceCCW(ref, angleB);
					}
					count++;
				}
			}

			sorter.sort(directions, 0, 4, order);
			for (int i = 0; i < 4; i++) {
				na.edges[i] = tmpEdges[order[i]];
			}
			if (count == 2) {
				// If there are only two then we define the order to be defined by the one which minimizes
				// CCW direction
				if (directions[order[1]] > Math.PI) {
					na.edges[0] = tmpEdges[order[1]];
					na.edges[1] = tmpEdges[order[0]];
				} else {
					na.edges[0] = tmpEdges[order[0]];
					na.edges[1] = tmpEdges[order[1]];
				}
			} else if (count == 3) {
				// Edges need to point along the 4 possible directions, in the case of 3 edges, there might
				// need to be a gap at a different location than at the end
				int selected = -1;
				double largestAngle = 0;
				for (int i = 0, j = 2; i < 3; j = i, i++) {
					double ccw = UtilAngle.distanceCCW(directions[order[j]], directions[order[i]]);
					if (ccw > largestAngle) {
						largestAngle = ccw;
						selected = j;
					}
				}

				for (int i = 2; i > selected; i--) {
					na.edges[i + 1] = na.edges[i];
				}
				na.edges[selected + 1] = null;
			}
		}
	}

	/**
	 * Rotates the grid in the CCW direction
	 */
	public void rotateCCW( GridInfo grid ) {
		cornerList.clear();
		for (int col = 0; col < grid.cols; col++) {
			for (int row = 0; row < grid.rows; row++) {
				cornerList.add(grid.get(row, grid.cols - col - 1));
			}
		}
		int tmp = grid.rows;
		grid.rows = grid.cols;
		grid.cols = tmp;

		grid.nodes.clear();
		grid.nodes.addAll(cornerList);
	}

	/**
	 * Used to access an element in the sparse grid
	 */
	public @Nullable GridElement getDense( int row, int col ) {
		return denseGrid[row*sparseCols + col];
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		verbose = BoofMiscOps.addPrefix(this, out);
	}

	@SuppressWarnings({"NullAway.Init"})
	public static class GridElement {
		public Node node;
		public int row, col;
		public int rowLength, colLength;
		public boolean marked;

		public boolean isAssigned() {
			return row != Integer.MAX_VALUE;
		}

		@SuppressWarnings({"NullAway"})
		public void reset() {
			node = null;
			rowLength = colLength = -1;
			row = Integer.MAX_VALUE;
			col = Integer.MAX_VALUE;
			marked = false;
		}
	}

	public static class GridInfo {
		public List<Node> nodes = new ArrayList<>();
		public int rows, cols;

		/**
		 * Indicates if there are no "corner" corner points. See class JavaDoc.
		 */
		public boolean hasCornerSquare;

		public void reset() {
			rows = cols = -1;

			hasCornerSquare = true;
			nodes.clear();
		}

		public Node get( int row, int col ) {
			return nodes.get(row*cols + col);
		}

		public void lookupGridCorners( List<Node> corners ) {
			corners.clear();
			corners.add(this.nodes.get(0));
			corners.add(this.nodes.get(cols - 1));
			corners.add(this.nodes.get(rows*cols - 1));
			corners.add(this.nodes.get((rows - 1)*cols));

			for (int i = 3; i >= 0; i--) {
				if (corners.get(i).countEdges() != 2)
					corners.remove(i);
			}
		}
	}

	public interface CheckShape {
		boolean isValidShape( int rows, int cols );
	}
}
