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


	// Detects squares inside the image
	DetectPolygonBinaryGrayRefine<T> squareDetector;

	FastQueue<FinderSquare> squares = new FastQueue<>(FinderSquare.class,true);
	List<FinderSquare> finderSquares = new ArrayList<>();

	// Nearst Neighbor Search related variables
	private NearestNeighbor<FinderSquare> search = FactoryNearestNeighbor.kdtree();
	private FastQueue<double[]> searchPoints;
	private FastQueue<NnData<FinderSquare>> searchResults = new FastQueue(NnData.class,true);

	// Computes a mapping to remove perspective distortion
	private RemovePerspectiveDistortion<?> removePerspective = new RemovePerspectiveDistortion(120,120);

	public QrCodeFinderPatternDetector(DetectPolygonBinaryGrayRefine<T> squareDetector) {

		// verify and configure polygon detector
		if( squareDetector.getMinimumSides() != 4 || squareDetector.getMaximumSides() != 4 )
			throw new IllegalArgumentException("Must detect 4 and only 4 sided polygons");
		squareDetector.getDetector().setOutputClockwise(true);
		this.squareDetector = squareDetector;

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
		squresToFinderList();

		// Create graph of neighboring squares
		createFinderSquareGraph();

		// connect finder squares together into a finder pattern
		identifyTripleSquares();


		// Refine the squares found in complete finder patterns

	}

	private void squresToFinderList() {
		List<DetectPolygonFromContour.Info> squares = squareDetector.getPolygonInfo();
		for (int i = 0; i < squares.size(); i++) {
			DetectPolygonFromContour.Info info = squares.get(i);

			// squares with no internal contour cannot possibly be a finder pattern
			if( !info.hasInternal )
				continue;

			// See if the appearance matches a finder pattern
			double grayThreshold = (info.edgeInside+info.edgeOutside)/2;;
			if( !checkFinderSquarePattern(info,(float)grayThreshold))
				continue;

			FinderSquare fs = this.squares.grow();
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

	private void createFinderSquareGraph() {
		// Add items to NN search
		searchPoints.resize(finderSquares.size());
		for (int i = 0; i < finderSquares.size(); i++) {
			FinderSquare f = finderSquares.get(i);
			double[] p = searchPoints.get(i);
			p[0] = f.center.x;
			p[1] = f.center.y;
		}

		double point[] = new double[2]; // TODO remove
		for (int i = 0; i < finderSquares.size(); i++) {
			FinderSquare f = finderSquares.get(i);


			// TODO Set search radius based on the largest possible QR code version it can find>
			double searchRadius = f.largestSide*0.6;
			searchRadius*=searchRadius;

			point[0] = f.center.x;
			point[1] = f.center.y;

			// See if at this location it's the largest square
			search.findNearest(point,searchRadius,-1,searchResults);

			if( searchResults.size > 1) {
				for (int j = 0; j < searchResults.size; j++) {
					NnData<FinderSquare> r = searchResults.get(i);

					if( r.data == f ) continue; // skip over if it's the square that initiated the search


					// TODO Create graph
				}
			}
		}
	}


	private boolean checkFinderSquarePattern( DetectPolygonFromContour.Info info , float grayThreshold ) {
		Polygon2D_F64 sq = info.polygon;

		// create a mapping assuming perspective distortion
		// NOTE: Order doesn't matter here as long as the square is CW or CCW
		if( !removePerspective.createTransform(sq.get(0),sq.get(1),sq.get(2),sq.get(3)) )
			return false;

		// with Perspective removed to Image coordinates.
		PointTransformHomography_F32 p2i = removePerspective.getTransform();

		Point2D_F32 imagePixel = new Point2D_F32();
		float lineX[] = new float[12];
		float lineY[] = new float[12];
		for (int i = 0; i < 12; i++) {
			float location = 10*i+5; // middle point in a cell of a 120x120 image
			p2i.compute(location,60,imagePixel);
			lineX[i] = interpolate.get(imagePixel.x,imagePixel.y);
			p2i.compute(60,location,imagePixel);
			lineY[i] = interpolate.get(imagePixel.x,imagePixel.y);
		}

		// see if the change in intensity matched the expected pattern
		if( !finderSquareIntensityCheck(lineX,grayThreshold))
			return false;

		return finderSquareIntensityCheck(lineY,grayThreshold);
	}

	private boolean finderSquareIntensityCheck( float values[] , float threshold ) {
		if( values[0] > threshold || values[1] > threshold )
			return false;
		if( values[2] < threshold || values[3] < threshold )
			return false;
		if( values[4] > threshold || values[5] > threshold ||
				values[6] > threshold || values[7] > threshold )
			return false;
		if( values[8] < threshold || values[9] < threshold )
			return false;
		if( values[10] > threshold || values[11] > threshold )
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

	public static class FinderSquare {
		Polygon2D_F64 square;
		Point2D_F64 center = new Point2D_F64();
		boolean candidate;
		// length of the largest side
		double largestSide;

		// threshold for binary classification.
		double grayThreshold;

		public void reset()
		{
			square = null;
			center.set(-1,-1);
			candidate = true;
			grayThreshold = -1;
		}
	}
}
