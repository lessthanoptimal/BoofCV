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
import boofcv.alg.fiducial.calib.squares.SquareGraph;
import boofcv.alg.fiducial.calib.squares.SquareNode;
import boofcv.alg.fiducial.qrcode.PositionPatternNode;
import boofcv.alg.fiducial.qrcode.SquareLocatorPatternDetectorBase;
import boofcv.alg.shapes.polygon.DetectPolygonBinaryGrayRefine;
import boofcv.alg.shapes.polygon.DetectPolygonFromContour;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.ImageGray;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import lombok.Getter;
import org.ddogleg.nn.FactoryNearestNeighbor;
import org.ddogleg.nn.NearestNeighbor;
import org.ddogleg.nn.NnData;
import org.ddogleg.struct.DogArray;
import org.ejml.data.DMatrixRMaj;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Searches for Aztec finder patterns inside an image and returns a list of candidates. Finder patterns are found
 * by looking at the external contour of block quadrilaterals and looking for quadrilaterals that have a similar
 * center pixel. If one, two, or three match then that's consider a match for compact or full-range Aztec codes.
 *
 * @author Peter Abeles
 */
public class AztecFinderPatternDetector<T extends ImageGray<T>> extends SquareLocatorPatternDetectorBase<T> {

	/** At least this fraction of points is required to match a template when examining a potential pyramid */
	public double minimumTemplateMatch = 0.8;

	/** Two pyramid layers are considered to be close if they are within this fraction of side length of each other */
	public double distanceTolerance = 0.15;

	// Layers that describe a pyramid
	private DogArray<Layer> layers = new DogArray<>(Layer::new, Layer::reset);

	/** Found candidate pyramids/locator patterns. Recycled every search */
	private @Getter DogArray<AztecPyramid> found = new DogArray<>(AztecPyramid::new, AztecPyramid::reset);

	// used to search for neighbors that which are candidates for connecting
	private NearestNeighbor<Layer> nn = (NearestNeighbor)FactoryNearestNeighbor.kdtree(new SquareNode.KdTreeSquareNode());
	private NearestNeighbor.Search<Layer> search = nn.createSearch();
	private DogArray<NnData<Layer>> searchResults = new DogArray<>(NnData::new);

	// workspace for homography calculation
	Estimate1ofEpipolar computeH = FactoryMultiView.homographyTLS();
	DMatrixRMaj gridToImage = new DMatrixRMaj(3, 3);
	DogArray<AssociatedPair> pairs = new DogArray<>(AssociatedPair::new, AssociatedPair::zero);
	Point2D_F64 gridPoint = new Point2D_F64();
	Point2D_F64 pixel = new Point2D_F64();

	/**
	 * Configures the detector
	 *
	 * @param squareDetector Square detector
	 */
	public AztecFinderPatternDetector( DetectPolygonBinaryGrayRefine<T> squareDetector ) {
		super(squareDetector);
		maxContourFraction = 2.0;
	}

	@Override protected void findLocatorPatternsFromSquares() {
		layers.reset();
		found.reset();
		squaresToLayerList();
		findLayersInsideOfLayers();
		createPyramids();
	}

	/**
	 * Takes the detected squares and turns it into a list of {@link PositionPatternNode}.
	 */
	void squaresToLayerList() {
		List<DetectPolygonFromContour.Info> infoList = squareDetector.getPolygonInfo();
		for (int i = 0; i < infoList.size(); i++) {
			DetectPolygonFromContour.Info info = infoList.get(i);

			// See if the appearance matches a finder pattern
			double grayThreshold = (info.edgeInside + info.edgeOutside)/2;

			int diameter = computeLayerDiameter(info.polygon, (float)grayThreshold);
			if (diameter <= 0)
				continue;

			// refine the edge estimate
			squareDetector.refine(info);

			// Save the results
			Layer pp = this.layers.grow();
			pp.square = info.polygon;
			pp.threshold = grayThreshold;
			pp.diameter = diameter;

			SquareGraph.computeNodeInfo(pp);
		}
	}

	/**
	 * Add layers which are contained inside other layers as children.
	 */
	void findLayersInsideOfLayers() {
		// Initialize search
		nn.setPoints(layers.toList(), false);

		// Go through all layers
		for (int layerIdx = 0; layerIdx < layers.size; layerIdx++) {
			Layer a = layers.get(layerIdx);

			// Find all layers with a center close to the center of 'a'
			search.findNearest(a, a.largestSide/2.0, 10, searchResults);
			for (int searchIdx = 0; searchIdx < searchResults.size; searchIdx++) {
				NnData<Layer> result = searchResults.get(searchIdx);
				Layer b = result.point;

				// Don't compare against itself
				if (a == b)
					continue;

				// Only add children to 'a'
				if (b.largestSide > a.largestSide)
					continue;

				// See if their centers are close to each other
				// use the max of the two to make it symmetric so that order doesn't matter
				double maxSide = Math.max(a.largestSide, b.largestSide);
				double distance = a.center.distance(b.center);
				if (distance > maxSide*distanceTolerance)
					continue;

				b.child = true;
				a.children.add(b);

				if (verbose != null) verbose.printf("%s child of %s\n", format(a), format(b));
			}
		}
	}

	private static String format( Layer a ) {
		return String.format("(%.1f %.1f, s=%d)", a.center.x, a.center.y, a.diameter);
	}

	/**
	 * Go from layers to output pyramid while rejecting false positives
	 */
	void createPyramids() {
		for (int layerIdx = 0; layerIdx < layers.size; layerIdx++) {
			Layer a = layers.get(layerIdx);

			// If a layer is inside another it can't be the outermost layer.
			// Reject if there are too many children and it's probably noise
			if (a.child || a.children.size() > 1)
				continue;

			// Reject if not the smallest layer and it has a child or the reverse
			if (a.children.isEmpty() == (a.diameter == 9))
				continue;

			AztecPyramid p = found.grow();
			copyToOutput(a, p.layers.grow());
			for (int i = 0; i < a.children.size(); i++) {
				copyToOutput(a.children.get(i), p.layers.grow());
			}
			p.alignCorners();
		}
	}

	void copyToOutput( Layer src, AztecPyramid.Layer dst ) {
		dst.square.setTo(src.square);
		dst.center.setTo(src.center);
		dst.threshold = src.threshold;
	}

	/**
	 * Decides if the polygon is part of a pyramid. If so the number of layers in the pyramid it belongs to
	 *
	 * @return Number of layers in the pyramid. 3 = width 5, 5 = width 7
	 */
	int computeLayerDiameter( Polygon2D_F64 polygon, float threshold ) {
		double score5 = scoreTemplate(polygon, threshold, 5);
		double score9 = scoreTemplate(polygon, threshold, 9);

		if (score5 < minimumTemplateMatch && score9 < minimumTemplateMatch)
			return 0;

		return score5 > score9 ? 5 : 9;
	}

	/**
	 * Samples the center of squares inside assuming it's a pyramid with the specified number of squares in the outer
	 * ring.
	 *
	 * @param polygon Corners of square region
	 * @param threshold B&W threshold value
	 * @param squaresWide Number of squares wide the black ring is.
	 * @return 0.0 to 1.0. 1.0 indicates a perfect fit
	 */
	double scoreTemplate( Polygon2D_F64 polygon, float threshold, int squaresWide ) {
		// Compute a homography from local grid coordinates around the square to image pixels
		pairs.resetResize(4);
		pairs.get(0).setTo(0, 0, polygon.get(0));
		pairs.get(1).setTo(squaresWide, 0, polygon.get(1));
		pairs.get(2).setTo(squaresWide, squaresWide, polygon.get(2));
		pairs.get(3).setTo(0, squaresWide, polygon.get(3));

		// Compute a homography to map a grid to pixel coordinate
		computeH.process(pairs.toList(), gridToImage);

		// Number of times an observation matches the template
		int numMatches = 0;

		for (int row = 1; row < squaresWide - 1; row++) {
			// rrow and rcol is distance from the border along their respective axis
			int rrow = Math.min(row, squaresWide - row - 1);
			gridPoint.y = row + 0.5; // sample it in the square's center

			for (int col = 1; col < squaresWide - 1; col++) {
				int rcol = Math.min(col, squaresWide - col - 1);
				gridPoint.x = col + 0.5; // sample it in the square's center

				// find the pixel coordinate for the specified grid coordinate
				GeometryMath_F64.mult(gridToImage, gridPoint, pixel);

				// Sample the image at this point
				float pixelValue = interpolate.get((float)pixel.x, (float)pixel.y);

				// distance from the edge determines if we expect a white or black region
				int r = Math.min(rrow, rcol);
				if (pixelValue > threshold == (r%2 == 1))
					numMatches++;
			}
		}

		// total number of times the template was tested
		int numSamples = (squaresWide - 2)*(squaresWide - 2);
		double fitFraction = numMatches/(double)numSamples;

		if (verbose != null) verbose.printf("poly_sore: p[0]=(%.1f %.1f) template: score=%.2f squares=%d pixels=%.1f\n",
				polygon.get(0).x, polygon.get(1).y, fitFraction, squaresWide, polygon.getSideLength(0));

		return fitFraction;
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> set ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
	}

	/**
	 * Candidate locator patterns. Layers are the number of transitions between dark and white
	 */
	public static class Layer extends SquareNode {
		// Number of squares wide in squares this pyramid layer is
		public int diameter;
		// threshold used to split black/white pixels
		public double threshold;
		// true if it's a child to another node
		public boolean child;
		// List of nodes which consider this layer to be a parent
		public List<Layer> children = new ArrayList<>();

		@Override
		public void reset() {
			super.reset();
			diameter = -1;
			threshold = 0;
			child = false;
			children.clear();
		}
	}
}
