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

package boofcv.alg.feature.detect.line;


import boofcv.alg.InputSanityCheck;
import boofcv.alg.feature.detect.line.gridline.Edgel;
import boofcv.struct.feature.MatrixOfList;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.geometry.UtilLine2D_F32;
import georegression.metric.ClosestPoint2D_F32;
import georegression.struct.line.LineParametric2D_F32;
import georegression.struct.line.LinePolar2D_F32;
import georegression.struct.line.LineSegment2D_F32;
import georegression.struct.point.Point2D_F32;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.struct.FastQueue;

import java.util.List;

/**
 * <p>
 * Line segment feature detector. The image is broken up into several regions of constant size.
 * Inside each region pixels that have been flaged as belonging to edges are connected into lines using RANSAC or
 * another {@link ModelMatcher}.  The output is a list of detected lines in a grid of lists.  This algorithm
 * is inspired by [1], but has several differences in how the image is processed and how the output is produced.
 * </p>
 *
 * <p>
 * The image is segmented into square regions of homogeneous size.  Inside each region pixels which have been
 * flagged as belonging to an edge are identified.  Flagged edge pixels are referred to as "edgels" and have
 * the image gradient at that point stored in their data structure.  Gradient information is used
 * to prune incompatible points from each other.  RANSAC or similar algorithms are used to estimate and detect
 * lines inside each region.  Ones a line has been identified it is removed from the list of candidate points and
 * more lines are searched for.
 * </p>
 *
 * <p>
 * [1] J.C. Clarke, S. Carlsson, and A. Zisserman. "Detecting and tracking linear features efficiently"
 * In. BMVC. British Machine Vision Association, 1996. 4,5,7.
 * </p>
 *
 * @author Peter Abeles
 */
public abstract class GridRansacLineDetector<D extends ImageGray> {

	// size of a region's width/height in pixels
	protected int regionSize;
	// the minimum number of points which must be fit to be accepted
	protected int minInlierSize;

	// list of detected edge pixels in a region
	protected FastQueue<Edgel> edgels = new FastQueue<>(30, Edgel.class, true);
	// The maximum number of lines which can be detected in a region
	protected int maxDetectLines;

	// extracts lines
	private ModelMatcher<LinePolar2D_F32,Edgel> robustMatcher;

	// list of lines found in each
	private MatrixOfList<LineSegment2D_F32> foundLines = new MatrixOfList<>(1, 1);

	/**
	 * Specifies major configuration parameters.
	 *
	 * @param regionSize Length of each side in a square region.  Try 40.
	 * @param maxDetectLines Maximum number of lines which can be detected in a region.  Try 10.
	 * @param robustMatcher Robust model matcher for line detection.
	 */
	public GridRansacLineDetector(int regionSize, int maxDetectLines ,
								  ModelMatcher<LinePolar2D_F32, Edgel> robustMatcher)
	{
		this.regionSize = regionSize;
		this.maxDetectLines = maxDetectLines;
		this.robustMatcher = robustMatcher;

		this.minInlierSize = 2*regionSize/3;
	}

	/**
	 * Detects line segments through the image inside of grids.
	 *
	 * @param derivX Image derivative along x-axis. Not modified.
	 * @param derivY Image derivative along x-axis. Not modified.
	 * @param binaryEdges True values indicate that a pixel is an edge pixel. Not modified.
	 */
	public void process( D derivX , D derivY , GrayU8 binaryEdges )
	{
		InputSanityCheck.checkSameShape(derivX,derivY,binaryEdges);

		int w = derivX.width-regionSize+1;
		int h = derivY.height-regionSize+1;

		foundLines.reshape(derivX.width / regionSize, derivX.height / regionSize);
		foundLines.reset();

		// avoid partial regions/other image edge conditions by being at least the region's radius away
		for( int y = 0; y < h; y += regionSize) {
			int gridY = y/regionSize;
			// index of the top left pixel in the region being considered
			// possible over optimization
			int index = binaryEdges.startIndex + y*binaryEdges.stride;
			for( int x = 0; x < w; x+= regionSize , index += regionSize) {
				int gridX = x/regionSize;
				// detects edgels inside the region
				detectEdgels(index,x,y,derivX,derivY,binaryEdges);

				// find lines inside the region using RANSAC
				findLinesInRegion(foundLines.get(gridX,gridY));
			}
		}
	}

	/**
	 * Returns all the found line segments contained in a grid.
	 *
	 * @return Grid of detected lines.
	 */
	public MatrixOfList<LineSegment2D_F32> getFoundLines() {
		return foundLines;
	}

	/**
	 * Computes edgel information for pixels which have been flagged inside a region
	 * @param index0
	 * @param x0 offset of region top left corner
	 * @param y0 offset of region top left corner
	 * @param derivX contains image derivative x-axis
	 * @param derivY contains image derivative y-axis
	 * @param binaryEdges Mark indicting which pixels are edges along a line
	 */
	protected abstract void detectEdgels( int index0 , int x0 , int y0 , D derivX , D derivY , GrayU8 binaryEdges);

	/**
	 * Searches for lines inside inside the region..
	 *
	 * @param gridLines Where the found lines are stored.
	 */
	private void findLinesInRegion( List<LineSegment2D_F32> gridLines ) {

		List<Edgel> list = edgels.copyIntoList(null);

		int iterations = 0;

		// exit if not enough points or max iterations exceeded
		while( iterations++ < maxDetectLines) {
			if( !robustMatcher.process(list) )
				break;

			// remove the found edges from the main list
			List<Edgel> matchSet = robustMatcher.getMatchSet();

			// make sure the match set is large enough
			if( matchSet.size() < minInlierSize )
				break;

			for( Edgel e : matchSet ) {
				list.remove(e);
			}

			gridLines.add(convertToLineSegment(matchSet, robustMatcher.getModelParameters()));
		}
	}

	/**
	 * Lines are found in polar form and this coverts them into line segments by finding
	 * the extreme points of points on the line.
	 *
	 * @param matchSet Set of points belonging to the line.
	 * @param model Detected line.
	 * @return Line segement.
	 */
	private LineSegment2D_F32 convertToLineSegment(List<Edgel> matchSet, LinePolar2D_F32 model) {
		float minT = Float.MAX_VALUE;
		float maxT = -Float.MAX_VALUE;

		LineParametric2D_F32 line = UtilLine2D_F32.convert(model,(LineParametric2D_F32)null);

		Point2D_F32 p = new Point2D_F32();
		for( Edgel e : matchSet ) {
			p.set(e.x,e.y);
			float t = ClosestPoint2D_F32.closestPointT(line,e);
			if( minT > t )
				minT = t;
			if( maxT < t )
				maxT = t;
		}

		LineSegment2D_F32 segment = new LineSegment2D_F32();

		segment.a.x = line.p.x + line.slope.x * minT;
		segment.a.y = line.p.y + line.slope.y * minT;
		segment.b.x = line.p.x + line.slope.x * maxT;
		segment.b.y = line.p.y + line.slope.y * maxT;

		return segment;
	}


}
