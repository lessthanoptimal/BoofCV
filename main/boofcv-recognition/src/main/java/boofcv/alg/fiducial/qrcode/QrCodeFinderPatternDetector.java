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

package boofcv.alg.fiducial.qrcode;

import boofcv.alg.distort.PointTransformHomography_F32;
import boofcv.alg.distort.RemovePerspectiveDistortion;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.shapes.polygon.DetectPolygonBinaryGrayRefine;
import boofcv.alg.shapes.polygon.DetectPolygonFromContour;
import boofcv.core.image.border.BorderType;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.metric.Intersection2D_F64;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import org.ddogleg.nn.FactoryNearestNeighbor;
import org.ddogleg.nn.NearestNeighbor;
import org.ddogleg.nn.NnData;
import org.ddogleg.struct.FastQueue;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
// TODO
public class QrCodeFinderPatternDetector<T extends ImageGray<T>> {

	InterpolatePixelS<T> interpolate;

	// maximum QR code version that it can detect
	int maxVersionQR;

	// Detects squares inside the image
	DetectPolygonBinaryGrayRefine<T> squareDetector;

	FastQueue<PositionSquare> squares = new FastQueue<>(PositionSquare.class,true);
	List<PositionSquare> positionSquares = new ArrayList<>();

	// Nearst Neighbor Search related variables
	private NearestNeighbor<PositionSquare> search = FactoryNearestNeighbor.kdtree();
	private FastQueue<double[]> searchPoints;
	private FastQueue<NnData<PositionSquare>> searchResults = new FastQueue(NnData.class,true);

	// Computes a mapping to remove perspective distortion
	private RemovePerspectiveDistortion<?> removePerspective = new RemovePerspectiveDistortion(70,70);

	public QrCodeFinderPatternDetector(DetectPolygonBinaryGrayRefine<T> squareDetector , int maxVersionQR) {

		// verify and configure polygon detector
		if( squareDetector.getMinimumSides() != 4 || squareDetector.getMaximumSides() != 4 )
			throw new IllegalArgumentException("Must detect 4 and only 4 sided polygons");
		if( squareDetector.getDetector().isOutputClockwise() )
			throw new IllegalArgumentException("Must be CCW");
		this.squareDetector = squareDetector;
		this.maxVersionQR = maxVersionQR;

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

	public void process(T gray, GrayU8 binary ) {

		squares.reset();
		interpolate.setImage(gray);

		// detect squares
		squareDetector.process(gray,binary);
		squresToPositionList();

		// Create graph of neighboring squares
		createPositionPatternGraph();

		// connect finder squares together into a finder pattern
		identifyTripleSquares();


		// Refine the squares found in complete finder patterns

	}

	private void squresToPositionList() {
		List<DetectPolygonFromContour.Info> squares = squareDetector.getPolygonInfo();
		for (int i = 0; i < squares.size(); i++) {
			DetectPolygonFromContour.Info info = squares.get(i);

			// squares with no internal contour cannot possibly be a finder pattern
			if( !info.hasInternal )
				continue;

			// See if the appearance matches a finder pattern
			double grayThreshold = (info.edgeInside+info.edgeOutside)/2;
			if( !checkPositionPatternApperance(info,(float)grayThreshold))
				continue;

			PositionSquare fs = this.squares.grow();
			fs.reset();
			fs.square = info.polygon;
			fs.grayThreshold = grayThreshold;

			// Under perspective distortion the geometric center is the intersection of the lines formed by
			// opposing corners.
			if( null == Intersection2D_F64.intersection(
					fs.square.get(0),fs.square.get(2),fs.square.get(1),fs.square.get(3),
					fs.center) ) {
				// This should be impossible
				throw new RuntimeException("BAD");
			}

			// Find the length of the largest side on the square
			double largest = 0;
			for (int j = 0; j < 4; j++) {
				double l = fs.square.getSideLength(j);
				if( l > largest ) {
					largest = l;
				}
			}
			fs.largestSide = largest;
		}
	}

	private void createPositionPatternGraph() {
		// Add items to NN search
		searchPoints.resize(positionSquares.size());
		for (int i = 0; i < positionSquares.size(); i++) {
			PositionSquare f = positionSquares.get(i);
			double[] p = searchPoints.get(i);
			p[0] = f.center.x;
			p[1] = f.center.y;
		}

		double point[] = new double[2]; // TODO remove
		for (int i = 0; i < positionSquares.size(); i++) {
			PositionSquare f = positionSquares.get(i);

			// The QR code version specifies the number of "modules"/blocks across the marker is
			// A position pattern is 7 blocks. A version 1 qr code is 21 blocks. Each version past one increments
			// by 4 blocks. The search is relative to the center of each position pattern, hence the - 7
			double maximumQrCodeWidth = f.largestSide*(21+4*(maxVersionQR-1)-7.0)/7.0;
			double searchRadius = 1.2*maximumQrCodeWidth/2.0; // search 1/2 the width + some fudge factor
			searchRadius*=searchRadius;

			point[0] = f.center.x;
			point[1] = f.center.y;

			// Connect all the finder patterns which are near by each other together in a graph
			search.findNearest(point,searchRadius,-1,searchResults);

			if( searchResults.size > 1) {
				for (int j = 0; j < searchResults.size; j++) {
					NnData<PositionSquare> r = searchResults.get(i);

					if( r.data == f ) continue; // skip over if it's the square that initiated the search

					// TODO See if sides approximately lie along the same line
					// TODO maybe have one edge for each side?  Resolve ambiguity by selecting the closest one
					if( !f.edge.contains(r.data)) {
						f.edge.add( r.data );
						r.data.edge.add(f);
					}
				}
			}
		}
	}


	private boolean checkPositionPatternApperance(DetectPolygonFromContour.Info info , float grayThreshold ) {
		Polygon2D_F64 sq = info.polygon;

		// create a mapping assuming perspective distortion
		// NOTE: Order doesn't matter here as long as the square is CW or CCW
		if( !removePerspective.createTransform(sq.get(0),sq.get(1),sq.get(2),sq.get(3)) )
			return false;

		// with Perspective removed to Image coordinates.
		PointTransformHomography_F32 p2i = removePerspective.getTransform();

		// a single scan line across the position pattern. Sample inside each square once
		// X.XXX.X
		Point2D_F32 imagePixel = new Point2D_F32();
		float lineX[] = new float[7];
		float lineY[] = new float[7];
		for (int i = 0; i < 7; i++) {
			float location = 10*i;
			p2i.compute(location,35,imagePixel);
			lineX[i] = interpolate.get(imagePixel.x,imagePixel.y);
			p2i.compute(35,location,imagePixel);
			lineY[i] = interpolate.get(imagePixel.x,imagePixel.y);
		}

		// see if the change in intensity matched the expected pattern
		if( !finderSquareIntensityCheck(lineX,grayThreshold))
			return false;

		return finderSquareIntensityCheck(lineY,grayThreshold);
	}

	private boolean finderSquareIntensityCheck( float values[] , float threshold ) {
		if( values[0] < threshold || values[1] > threshold )
			return false;
		if( values[2] < threshold || values[3] < threshold || values[4] < threshold  )
			return false;
		if( values[5] > threshold || values[6] < threshold )
			return false;
		return true;
	}


	private void identifyTripleSquares() {
		// See if two squares have parallel sides

		// Determine orientation and check for white on outside and timing pattern on inside
	}

	private void connectSquaresIntoFinderPattern() {
		// sides need to be approximately parallel

		// need to be the expected distance apart
	}

	private void refineSquares() {

	}

	/**
	 * Information for position patterns. These are squares. One outer shape that is 1 block think, inner white
	 * space 1 block think, then the stone which is 3 blocks think. Total of 7 blocks.
	 */
	public static class PositionSquare {
		Polygon2D_F64 square;
		Point2D_F64 center = new Point2D_F64();
		boolean candidate;
		// length of the largest side
		double largestSide;

		// threshold for binary classification.
		double grayThreshold;

		List<PositionSquare> edge = new ArrayList<>();

		public void reset()
		{
			square = null;
			center.set(-1,-1);
			candidate = true;
			grayThreshold = -1;
			edge.clear();
		}
	}
}
