/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.qrcode;

import boofcv.abst.filter.binary.BinaryContourFinder;
import boofcv.alg.distort.RemovePerspectiveDistortion;
import boofcv.alg.fiducial.calib.squares.SquareGraph;
import boofcv.alg.fiducial.calib.squares.SquareNode;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.shapes.polygon.DetectPolygonBinaryGrayRefine;
import boofcv.alg.shapes.polygon.DetectPolygonFromContour;
import boofcv.core.image.border.BorderType;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.misc.MovingAverage;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.geometry.UtilLine2D_F64;
import georegression.geometry.UtilPoint2D_F64;
import georegression.struct.line.LineParametric2D_F64;
import georegression.struct.line.LineSegment2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import org.ddogleg.nn.FactoryNearestNeighbor;
import org.ddogleg.nn.NearestNeighbor;
import org.ddogleg.nn.NnData;
import org.ddogleg.struct.FastQueue;

import java.util.List;

/**
 * Detects position patterns for a QR code inside an image and forms a graph of ones which can potentially
 * be connected together. Squares are detected in the image and position patterns are found based on their appearance.
 *
 *
 * @author Peter Abeles
 */
public class QrCodePositionPatternDetector<T extends ImageGray<T>> {

	InterpolatePixelS<T> interpolate;

	// maximum QR code version that it can detect
	int maxVersionQR;

	// Detects squares inside the image
	DetectPolygonBinaryGrayRefine<T> squareDetector;

	FastQueue<PositionPatternNode> positionPatterns = new FastQueue<>(PositionPatternNode.class,true);
	SquareGraph graph = new SquareGraph();

	// Nearst Neighbor Search related variables
	private NearestNeighbor<PositionPatternNode> search = FactoryNearestNeighbor.kdtree();
	private FastQueue<double[]> searchPoints;
	private FastQueue<NnData<PositionPatternNode>> searchResults = new FastQueue(NnData.class,true);

	// Computes a mapping to remove perspective distortion
	private RemovePerspectiveDistortion<?> removePerspective = new RemovePerspectiveDistortion(70,70);

	// Workspace for checking to see if two squares should be connected
	protected LineSegment2D_F64 lineA = new LineSegment2D_F64();
	protected LineSegment2D_F64 lineB = new LineSegment2D_F64();
	protected LineSegment2D_F64 connectLine = new LineSegment2D_F64();
	protected Point2D_F64 intersection = new Point2D_F64();

	// runtime profiling
	protected MovingAverage milliGraph = new MovingAverage(0.8);
	protected boolean profiler = false;

	// storage for nearest neighbor
	double point[] = new double[2];

	/**
	 * Configures the detector
	 *
	 * @param squareDetector Square detector
	 * @param maxVersionQR Maximum QR code version it can detect.
	 */
	public QrCodePositionPatternDetector(DetectPolygonBinaryGrayRefine<T> squareDetector , int maxVersionQR) {

		this.squareDetector = squareDetector;
		this.maxVersionQR = maxVersionQR;

		squareDetector.getDetector().setConvex(true);
		squareDetector.getDetector().setOutputClockwise(false);
		squareDetector.getDetector().setNumberOfSides(4,4);

		// set up nearest neighbor search for 2-DOF
		search.init(2);
		searchPoints = new FastQueue<double[]>(double[].class,true) {
			@Override
			protected double[] createInstance() {
				return new double[2];
			}
		};

		interpolate = FactoryInterpolation.bilinearPixelS(squareDetector.getInputType(), BorderType.EXTENDED);
	}

	public void resetRuntimeProfiling() {
		squareDetector.resetRuntimeProfiling();
		milliGraph.reset();
	}

	public void setProfilerState( boolean active ) {
		profiler = active;
	}

	/**
	 * Detects position patterns inside the image and forms a graph.
	 * @param gray Gray scale input image
	 * @param binary Thresholed version of gray image.
	 */
	public void process(T gray, GrayU8 binary ) {
		configureContourDetector(gray);
		recycleData();
		positionPatterns.reset();
		interpolate.setImage(gray);

		// detect squares
		squareDetector.process(gray,binary);

		long time0 = System.nanoTime();
		squaresToPositionList();

		long time1 = System.nanoTime();

		// Create graph of neighboring squares
		createPositionPatternGraph();
//		long time2 = System.nanoTime();  // doesn't take very long

		double milli = (time1-time0)*1e-6;

		milliGraph.update(milli);

		DetectPolygonFromContour<T> detectorPoly = squareDetector.getDetector();
		if( profiler ) {
			System.out.printf(" contour %5.1f shapes %5.1f adjust_bias %5.2f PosPat %6.2f",
					detectorPoly.getMilliContour(), detectorPoly.getMilliShapes(), squareDetector.getMilliAdjustBias(),
					milliGraph.getAverage());
		}
	}

	/**
	 * Configures the contour detector based on the image size. Setting a maximum contour and turning off recording
	 * of inner contours and improve speed and reduce the memory foot print significantly.
	 */
	private void configureContourDetector(T gray) {
		// determine the maximum possible size of a position pattern
		// contour size is maximum when viewed head one. Assume the smallest qrcode is 3x this width
		// 4 side in a square
		int maxContourSize = Math.min(gray.width,gray.height)*4/3;
		BinaryContourFinder contourFinder = squareDetector.getDetector().getContourFinder();
		contourFinder.setMaxContour(maxContourSize);
		contourFinder.setSaveInnerContour(false);
	}

	protected void recycleData() {
		for (int i = 0; i < positionPatterns.size(); i++) {
			SquareNode n = positionPatterns.get(i);
			for (int j = 0; j < n.edges.length; j++) {
				if (n.edges[j] != null) {
					graph.detachEdge(n.edges[j]);
				}
			}
		}
		positionPatterns.reset();
	}

	/**
	 * Takes the detected squares and turns it into a list of {@link PositionPatternNode}.
	 */
	private void squaresToPositionList() {
		this.positionPatterns.reset();
		List<DetectPolygonFromContour.Info> infoList = squareDetector.getPolygonInfo();
		for (int i = 0; i < infoList.size(); i++) {
			DetectPolygonFromContour.Info info = infoList.get(i);

			// The test below has been commented out because the new external only contour
			// detector discards all information related to internal contours
			// squares with no internal contour cannot possibly be a finder pattern
//			if( !info.hasInternal() )
//				continue;

			// See if the appearance matches a finder pattern
			double grayThreshold = (info.edgeInside+info.edgeOutside)/2;
			if( !checkPositionPatternAppearance(info.polygon,(float)grayThreshold))
				continue;

			// refine the edge estimate
			squareDetector.refine(info);

			PositionPatternNode pp = this.positionPatterns.grow();
			pp.reset();
			pp.square = info.polygon;
			pp.grayThreshold = grayThreshold;

			graph.computeNodeInfo(pp);
		}
	}

	/**
	 * Connects together position patterns. For each square, finds all of its neighbors based on center distance.
	 * Then considers them for connections
	 */
	private void createPositionPatternGraph() {
		// Add items to NN search
		searchPoints.resize(positionPatterns.size());
		for (int i = 0; i < positionPatterns.size(); i++) {
			PositionPatternNode f = positionPatterns.get(i);
			double[] p = searchPoints.get(i);
			p[0] = f.center.x;
			p[1] = f.center.y;
		}
		search.setPoints(searchPoints.toList(),positionPatterns.toList());

		for (int i = 0; i < positionPatterns.size(); i++) {
			PositionPatternNode f = positionPatterns.get(i);

			// The QR code version specifies the number of "modules"/blocks across the marker is
			// A position pattern is 7 blocks. A version 1 qr code is 21 blocks. Each version past one increments
			// by 4 blocks. The search is relative to the center of each position pattern, hence the - 7
			double maximumQrCodeWidth = f.largestSide*(17+4*maxVersionQR-7.0)/7.0;
			double searchRadius = 1.2*maximumQrCodeWidth; // search 1/2 the width + some fudge factor
			searchRadius*=searchRadius;

			point[0] = f.center.x;
			point[1] = f.center.y;

			// Connect all the finder patterns which are near by each other together in a graph
			search.findNearest(point,searchRadius,Integer.MAX_VALUE,searchResults);

			if( searchResults.size > 1) {
				for (int j = 0; j < searchResults.size; j++) {
					NnData<PositionPatternNode> r = searchResults.get(j);

					if( r.data == f ) continue; // skip over if it's the square that initiated the search

					considerConnect(f,r.data);
				}
			}
		}
	}

	/**
	 * Connects the 'candidate' node to node 'n' if they meet several criteria.  See code for details.
	 */
	void considerConnect(SquareNode node0, SquareNode node1) {
		// Find the side on each line which intersects the line connecting the two centers
		lineA.a = node0.center;
		lineA.b = node1.center;

		int intersection0 = graph.findSideIntersect(node0,lineA,intersection,lineB);
		connectLine.a.set(intersection);
		int intersection1 = graph.findSideIntersect(node1,lineA,intersection,lineB);
		connectLine.b.set(intersection);

		if( intersection1 < 0 || intersection0 < 0 ) {
			return;
		}

		double side0 = node0.sideLengths[intersection0];
		double side1 = node1.sideLengths[intersection1];

		// it should intersect about in the middle of the line

		double sideLoc0 = connectLine.a.distance(node0.square.get(intersection0))/side0;
		double sideLoc1 = connectLine.b.distance(node1.square.get(intersection1))/side1;

		if( Math.abs(sideLoc0-0.5)>0.35 || Math.abs(sideLoc1-0.5)>0.35 )
			return;

		// see if connecting sides are of similar size
		if( Math.abs(side0-side1)/Math.max(side0,side1) > 0.25 ) {
			return;
		}

		// Checks to see if the two sides selected above are closest to being parallel to each other.
		// Perspective distortion will make the lines not parallel, but will still have a smaller
		// acute angle than the adjacent sides
		if( !graph.almostParallel(node0, intersection0, node1, intersection1)) {
			return;
		}

		double ratio = Math.max(node0.smallestSide/node1.largestSide ,
				node1.smallestSide/node0.largestSide);

//		System.out.println("ratio "+ratio);
		if( ratio > 1.3 )
			return;

		double angle = graph.acuteAngle(node0, intersection0, node1, intersection1);
		double score = lineA.getLength()*(1.0+angle/0.1);

		graph.checkConnect(node0,intersection0,node1,intersection1,score);
	}

	/**
	 * Determines if the found polygon looks like a position pattern. A horizontal and vertical line are sampled.
	 * At each sample point it is marked if it is above or below the binary threshold for this square. Location
	 * of sample points is found by "removing" perspective distortion.
	 */
	boolean checkPositionPatternAppearance( Polygon2D_F64 square , float grayThreshold ) {
		return( checkLine(square,grayThreshold,0) || checkLine(square,grayThreshold,1));
	}

	LineSegment2D_F64 segment = new LineSegment2D_F64();
	LineParametric2D_F64 parametric = new LineParametric2D_F64();
	float[] samples = new float[9*5+1];
	int length[] = new int[12]; // 9 is the max, but I'll let it go farther for no reason
	int type[] = new int[12];
	private boolean checkLine( Polygon2D_F64 square , float grayThreshold , int side )
	{
		// find the mid point between two parallel sides
		int c0 = side;
		int c1 = (side+1)%4;
		int c2 = (side+2)%4;
		int c3 = (side+3)%4;

		UtilPoint2D_F64.mean(square.get(c0),square.get(c1), segment.a);
		UtilPoint2D_F64.mean(square.get(c2),square.get(c3), segment.b);

		UtilLine2D_F64.convert(segment,parametric);

		// Scan along the line plus some extra
		int period = samples.length/9;
		double N = samples.length-2*period-1;

		for (int i = 0; i < samples.length; i++) {
			double location = (i-period)/N;

			float x = (float)(parametric.p.x + location*parametric.slope.x);
			float y = (float)(parametric.p.y + location*parametric.slope.y);

			samples[i] = interpolate.get(x,y);
		}

		// threshold and compute run length encoding

		int size = 0;
		boolean black = samples[0] < grayThreshold;
		type[0] = black ? 0 : 1;
		for (int i = 0; i < samples.length; i++) {
			boolean b = samples[i] < grayThreshold;

			if( black == b ) {
				length[size]++;
			} else {
				black = b;
				if( size < type.length-1 ) {
					size += 1;
					type[size] = black ? 0 : 1;
					length[size] = 1;
				} else {
					break;
				}
			}
		}
		size++;

		// if too simple or too complex reject
		if( size < 5 || size > 9)
			return false;
		// detect finder pattern inside RLE
		for (int i = 0; i+5 <= size; i++) {
			if( type[i] != 0)
				continue;

			int black0 = length[i];
			int black1 = length[i+2];
			int black2 = length[i+4];

			int white0 = length[i+1];
			int white1 = length[i+3];

			// the center black area can get exagerated easily
			if( black0 < 0.4*white0 || black0 > 3*white0 )
				continue;
			if( black2 < 0.4*white1 || black2 > 3*white1 )
				continue;

			int black02 = black0+black2;

			if( black1 >= black02 && black1 <= 2*black02 )
				return true;
		}
		return false;
	}

	/**
	 * Checks to see if the array of sampled intensity values follows the expected pattern for a position pattern.
	 * X.XXX.X where x = black and . = white.
	 */
	static boolean positionSquareIntensityCheck(float values[] , float threshold ) {
		if( values[0] > threshold || values[1] < threshold )
			return false;
		if( values[2] > threshold || values[3] > threshold || values[4] > threshold  )
			return false;
		if( values[5] < threshold || values[6] > threshold )
			return false;
		return true;
	}

	/**
	 * Returns a list of all the detected position pattern squares and the other PP that they are connected to.
	 * @return List of PP
	 */
	public FastQueue<PositionPatternNode> getPositionPatterns() {
		return positionPatterns;
	}

	public DetectPolygonBinaryGrayRefine<T> getSquareDetector() {
		return squareDetector;
	}

	public SquareGraph getGraph() {
		return graph;
	}
}
