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

package boofcv.alg.fiducial.calib.ecocheck;

import boofcv.BoofVerbose;
import boofcv.abst.fiducial.calib.ConfigChessboardX;
import boofcv.alg.feature.detect.chess.ChessboardCorner;
import boofcv.alg.feature.detect.chess.DetectChessboardCornersXPyramid;
import boofcv.alg.fiducial.calib.chess.ChessboardCornerClusterFinder;
import boofcv.alg.fiducial.calib.chess.ChessboardCornerClusterToGrid;
import boofcv.alg.fiducial.calib.chess.ChessboardCornerClusterToGrid.GridElement;
import boofcv.alg.fiducial.calib.chess.ChessboardCornerClusterToGrid.GridInfo;
import boofcv.alg.fiducial.calib.chess.ChessboardCornerGraph;
import boofcv.alg.fiducial.qrcode.PackedBits8;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.interpolate.InterpolationType;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.GridCoordinate;
import boofcv.struct.border.BorderType;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.struct.line.LineSegment2D_F64;
import georegression.struct.point.Point2D_F64;
import lombok.Getter;
import org.ddogleg.struct.*;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static boofcv.alg.fiducial.calib.ecocheck.ECoCheckUtils.rotateObserved;

/**
 * <p>Detects chessboard patterns with marker and grid coordinate information encoded inside of the inner white squares.
 * Multiple unique markers can be detected and damaged/partial targets will work. If no binary patterns are found
 * in a chessboard, then an "anonymous" pattern is returned. Anonymous chessboards are useful when trying to track
 * distant targets. Which corners were decoded next to a binary pattern is recorded as those corners are extremely
 * unlikely to be a false positive.
 * </p>
 *
 * Processing steps: 1) x-corner detector. 2) find clusters of corners. 3) clusters into grids. 4) detect encoded binary
 * data inside of grids. 5) align coordinate systems
 *
 * @author Peter Abeles
 */
public class ECoCheckDetector<T extends ImageGray<T>> implements VerbosePrint {
	// TODO Add back in the ability to encoded every N squares

	/** Number of times a side is sampled when determining binarization threshold */
	final int NUM_SAMPLES_SIDE = 5; // number of samples along each side

	/**
	 * Number of points along a side it will sample when trying to determine if a border is white. Disable by settings
	 * to zero.
	 */
	public int whiteBorderSampleCount = 5;

	/** If more than this number of points fail the test consider it a failure */
	public double maxWhiteBorderFailFraction = 0.3;

	/** Common utilities for decoding ECoCheck patterns */
	@Getter protected ECoCheckUtils utils;

	/** Chessboard corner detector */
	@Getter protected DetectChessboardCornersXPyramid<T> detector;
	/** Cluster Finder */
	@Getter protected ChessboardCornerClusterFinder<T> clusterFinder;
	/** Cluster to grid */
	@Getter protected ChessboardCornerClusterToGrid clusterToGrid = new ChessboardCornerClusterToGrid();
	/** Used to sample the input image when "undistorting" the bit pattern */
	public InterpolatePixelS<T> interpolate;

	/** Found chessboard patterns */
	@Getter public final
	DogArray<ECoCheckFound> found = new DogArray<>(ECoCheckFound::new, ECoCheckFound::reset);

	// Binary cells in grid pattern for easy access
	FastArray<CellReading> gridBinaryCells = new FastArray<>(CellReading.class);
	// All decoded binary cells
	DogArray<CellReading> binaryCells = new DogArray<>(CellReading::new, CellReading::reset);

	// Image pixels that are read when decoding the bit pattern
	DogArray<Point2D_F64> samplePixels = new DogArray<>(Point2D_F64::new);
	// Storage for sampled pixel values
	DogArray_F32 sampleValues = new DogArray_F32();
	// The read in bits in a format the codec can understand
	PackedBits8 bits = new PackedBits8();
	// Storage the bits inside an image so that it can be rotated easily
	GrayU8 bitImage = new GrayU8(1, 1);
	// Stores intermediate results when rotating
	GrayU8 workImage = new GrayU8(1, 1);
	// Used to indicate which corners are around a binary pattern
	GrayU8 cornersAroundBinary = new GrayU8(1, 1);

	// Found transform from found to a marker coordinate system
	DogArray<Transform> transforms = new DogArray<>(Transform::new, Transform::reset);

	// Workspace for anonymous chessboards
	GridInfo anonymousInfo = new GridInfo();

	// Storage for a decided binary pattern
	CellValue decoded = new CellValue();

	// Coordinate decoded from cellID
	GridCoordinate decodedCoordinate = new GridCoordinate();

	// Coordinate of the cell observed on the grid taking in account orientation
	GridCoordinate observedCoordinate = new GridCoordinate();

	// workspace for white border test
	LineSegment2D_F64 lineBlack = new LineSegment2D_F64();
	LineSegment2D_F64 lineWhite = new LineSegment2D_F64();
	Point2D_F64 pixel = new Point2D_F64();

	// Verbose print debugging
	@Nullable PrintStream verbose;
	// If true it will print profiling information to verbose out
	boolean runtimeProfiling;

	// Processing time for different components in milliseconds
	@Getter double timeCornerDetectorMS;
	@Getter double timeClusteringMS;
	@Getter double timeGridMS;
	@Getter double timeDecodingMS;
	@Getter double timeAllMS = 0;

	/**
	 * Specifies configuration for detector
	 */
	public ECoCheckDetector( ECoCheckUtils utils,
							 ConfigChessboardX config, Class<T> imageType ) {
		this.utils = utils;
		this.utils.checkFixate();

		detector = new DetectChessboardCornersXPyramid<>(ImageType.single(imageType));
		clusterFinder = new ChessboardCornerClusterFinder<>(imageType);

		detector.setPyramidTopSize(config.detPyramidTopSize);
		detector.getDetector().setNonmaxRadius(config.detNonMaxRadius);
		detector.getDetector().setNonmaxThresholdRatio((float)config.detNonMaxThresholdRatio);
		detector.getDetector().setRefinedXCornerThreshold(config.detRefinedXCornerThreshold);

		clusterFinder.setAmbiguousTol(config.connAmbiguousTol);
		clusterFinder.setDirectionTol(config.connDirectionTol);
		clusterFinder.setOrientationTol(config.connOrientationTol);
		clusterFinder.setMaxNeighbors(config.connMaxNeighbors);
		clusterFinder.setMaxNeighborDistance(config.connMaxNeighborDistance);
		clusterFinder.setThresholdEdgeIntensity(config.connEdgeThreshold);

		interpolate = FactoryInterpolation.createPixelS(0, 255,
				InterpolationType.NEAREST_NEIGHBOR, BorderType.EXTENDED, imageType);

		bitImage.reshape(utils.codec.gridBitLength, utils.codec.gridBitLength);
	}

	/**
	 * Processes the image and searches for all chessboard patterns.
	 */
	public void process( T input ) {
		// reset / initialize data structures
		timeCornerDetectorMS = 0;
		timeClusteringMS = 0;
		timeGridMS = 0;
		timeDecodingMS = 0;
		timeAllMS = 0;
		found.reset();
		interpolate.setImage(input);

		// Find the chessboard corners
		long time0 = System.nanoTime();
		detector.process(input);
		long time1 = System.nanoTime();
		timeCornerDetectorMS = (time1 - time0)*1e-6;

		// Find chessboard clusters
		clusterFinder.process(input, detector.getCorners().toList(), detector.getNumberOfLevels());
		DogArray<ChessboardCornerGraph> clusters = clusterFinder.getOutputClusters();
		long time2 = System.nanoTime();
		timeClusteringMS = (time2 - time1)*1e-6;

		// Convert the clusters into grids
		for (int clusterIdx = 0; clusterIdx < clusters.size; clusterIdx++) {
			// Find the chessboard pattern inside the cluster
			long timeGrid0 = System.nanoTime();
			if (!clusterToGrid.clusterToSparse(clusters.get(clusterIdx))) {
				continue;
			}

			// Convert it into a dense grid to make it easier to process
			clusterToGrid.sparseToDense();
			long timeGrid1 = System.nanoTime();
			timeGridMS += (timeGrid1 - timeGrid0)*1e-6;

			// TODO make sure the grid has the

			// Go through and find all the white squares that could contain data. Attempt to decode
			decodeBinaryPatterns();
			timeDecodingMS += (System.nanoTime() - timeGrid1)*1e-6;

			// If no data patterns are found, extract the largest grid and return that
			if (binaryCells.isEmpty()) {
				createAnonymousTarget();
				continue;
			}

			// Select the coordinate system that best matches the decoded cells
			tallyMarkerVotes();

			// Sort transform based on the number of votes. The ones with most votes should be first.
			if (transforms.size > 1) // don't know if line below will allocate memory or not
				Collections.sort(transforms.toList(), ( a, b ) -> Integer.compare(b.votes, a.votes));

			// Two targets could be so close to each other that their chessboards become joined together
			// Create targets from all hypothesises, unless there's a contradiction
			for (int transformIdx = 0; transformIdx < transforms.size; transformIdx++) {
				Transform t = transforms.get(transformIdx);
				if (!createCorrectedTarget(t, found.grow())) {
					// conflict was found, abort
					found.removeTail();
				}
			}
		}

		timeAllMS = (System.nanoTime() - time0)*1e-6;

		if (verbose != null && runtimeProfiling) {
			verbose.printf("time (ms): all=%.1f corners=%.1f cluster=%.1f grid=%.1f decode=%.1f\n",
					timeAllMS, timeCornerDetectorMS, timeClusteringMS, timeGridMS, timeDecodingMS);
		}
	}

	/**
	 * Examines all white cells in the found chessboard grid and attempts to decide the binary patterns.
	 */
	private void decodeBinaryPatterns() {
		// Number of rows and columns the cluster algorithm found. These are corners and not squares
		int rows = clusterToGrid.getSparseRows();
		int cols = clusterToGrid.getSparseCols();

		// Initialize data structures
		cornersAroundBinary.reshape(cols, rows);
		ImageMiscOps.fill(cornersAroundBinary, 0);

		gridBinaryCells.clear();
		gridBinaryCells.resize(rows*cols);
		binaryCells.reset();

		if (verbose != null) {
			verbose.printf("corner grid: shape=( %d %d ) size=%d\n", rows, cols, clusterToGrid.getSparseGrid().size);
		}

		// Go through "squares" in the corner grid
		for (int row = 1; row < rows; row++) {
			for (int col = 1; col < cols; col++) {
				// corners from top-left around the circle
				GridElement a = clusterToGrid.getDense(row - 1, col - 1);
				GridElement b = clusterToGrid.getDense(row - 1, col);
				GridElement c = clusterToGrid.getDense(row, col);
				GridElement d = clusterToGrid.getDense(row, col - 1);

				if (a == null || b == null || c == null || d == null)
					continue;

				// Grid orientation is currently unknown. We have to assume that any square could be black or white
				// See if this square could have an encoded value
				if (!clusterToGrid.isWhiteSquareOrientation(a.node, c.node))
					continue;

				// Compute homography from pixels to data-region coordinates
				if (!utils.computeGridToImage(a.node.corner, b.node.corner, c.node.corner, d.node.corner))
					continue;

				// This is needed as a backup if isWhiteSquare() is incorrect, which it can be for heavy fisheye/
				// Checksum and ECC should catch most errors, but we are concerned about outlier performance.
				// Verify that the border surrounding data bits is mostly white
				if (!isBorderWhite(a.node.corner, b.node.corner, c.node.corner, d.node.corner)) {
					continue;
				}

				// Which pixels need to be sampled
				utils.selectPixelsToSample(samplePixels);
				samplePixelGray(samplePixels.toList(), sampleValues);

				// Select a threshold that maximized the variance
				float threshold = utils.otsuThreshold(sampleValues);

				// Sample points and compute bit values
				if (!graySamplesToBits(sampleValues, utils.bitSampleCount, threshold))
					continue;

				boolean success = false;

				// Try all 4 possible orientations until something works
				for (int orientation = 0; orientation < 4; orientation++) {
					convertBitImageToBitArray();
					if (!decodeAndSanityCheck()) {
						// rotate so it can try and see if another orientation works
						ImageMiscOps.rotateCCW(bitImage, workImage);
						bitImage.setTo(workImage);
						continue;
					}

					success = true;

					// mark corners as having a binary code next to them
					cornersAroundBinary.unsafe_set(col - 1, row - 1, 1);
					cornersAroundBinary.unsafe_set(col - 1, row, 1);
					cornersAroundBinary.unsafe_set(col, row - 1, 1);
					cornersAroundBinary.unsafe_set(col, row, 1);

					// Save the decoded results into a sparse grid
					CellReading cell = gridBinaryCells.data[row*cols + col] = binaryCells.grow();
					cell.row = row - 1;
					cell.col = col - 1;
					cell.orientation = orientation;
					cell.markerID = decoded.markerID;
					cell.cellID = decoded.cellID;

					if (verbose != null) {
						utils.cellIdToCornerCoordinate(cell.markerID, cell.cellID, decodedCoordinate);
						verbose.printf("marker=%d id=%d ori=%d code_grid=( %d %d ) obs_grid=( %d %d ) tl=( %.1f %.1f ) tr=( %.1f %.1f )\n",
								decoded.markerID, decoded.cellID, orientation,
								decodedCoordinate.row, decodedCoordinate.col,
								row - 1, col - 1, a.node.corner.x, a.node.corner.y,
								b.node.corner.x, b.node.corner.y);
					}

					break;
				}

				if (verbose != null && !success)
					verbose.printf("Failed to decode. obs_grid=(%d %d) tl=( %.1f %.1f ) thresh=%.1f\n",
							row - 1, col - 1, a.node.corner.x, a.node.corner.y, threshold);
			}
		}
	}

	/**
	 * Decode the bits and sanity check the solution to see if it could be correct.
	 */
	private boolean decodeAndSanityCheck() {
		boolean success = false;
		if (utils.codec.decode(bits, decoded)) {
			success = true;
		}
		// If the marker is out of range it's invalid
		if (success && decoded.markerID >= utils.markers.size()) {
			success = false;

			// This is a rare event. Let's print it just in case something is wrong.
			if (verbose != null) {
				verbose.println("Success decoding a cell, but markerID was invalid!");
			}
		}

		// If the cellID is too larger it's invalid
		if (success) {
			int maxCellID = utils.countEncodedSquaresInMarker(decoded.markerID);
			if (decoded.cellID >= maxCellID) {
				if (verbose != null) verbose.println("Success decoding a cell, but cellID was invalid!");
				success = false;
			}
		}

		return success;
	}

	/**
	 * Converts the binary image into a dense bit array that's understood by the codec
	 */
	void convertBitImageToBitArray() {
		bits.resize(bitImage.width*bitImage.height);
		for (int y = 0, i = 0; y < bitImage.height; y++) {
			for (int x = 0; x < bitImage.width; x++, i++) {
				bits.set(utils.bitOrder.get(i), bitImage.data[i]);
			}
		}
	}

	/**
	 * Samples points offset along the line. The extremes of the line are avoided since those will naturally be
	 * blurry in a chessboard pattern
	 *
	 * @return Average value of tangent points along the side
	 */
	float sampleInnerWhite( Point2D_F64 a, Point2D_F64 b ) {
		double border = utils.dataBorderFraction;
		double length = 1.0 - 2.0*border;

		// this specifies how far away from the line it will sample in the tangent direction
		// We use the border as guide and sample in the middle of it
		float nx = (float)(0.5*(b.x - a.x)*utils.dataBorderFraction);
		float ny = (float)(0.5*(b.y - a.y)*utils.dataBorderFraction);

		float sumWhite = 0.0f;
		float sumBlack = 0.0f;

		for (int i = 0; i < NUM_SAMPLES_SIDE; i++) {
			double f = border + length*i/(double)(NUM_SAMPLES_SIDE - 1);
			float cx = (float)(a.x + (b.x - a.x)*f);
			float cy = (float)(a.y + (b.y - a.y)*f);

			sumWhite += interpolate.get(cx - ny, cy + nx);
			sumBlack += interpolate.get(cx + ny, cy - nx);
		}

		return (sumWhite + sumBlack)/(2.0f*NUM_SAMPLES_SIDE);
	}

	/**
	 * RReads the gray scale value at the provided pixel locations
	 *
	 * @param samplePoints (Input) Which pixels to sample.
	 * @param sampleValues (Output) Pixel values
	 */
	void samplePixelGray( List<Point2D_F64> samplePoints, DogArray_F32 sampleValues ) {
		sampleValues.resize(samplePoints.size());

		for (int i = 0; i < samplePoints.size(); i++) {
			Point2D_F64 pixel = samplePoints.get(i);
			sampleValues.data[i] = interpolate.get((float)pixel.x, (float)pixel.y);
		}
	}

	/**
	 * Samples along the edge making sure the inside is brighter than the outside.
	 */
	boolean isBorderWhite( ChessboardCorner a, ChessboardCorner b, ChessboardCorner c, ChessboardCorner d ) {
		// See if test has been disabled
		if (whiteBorderSampleCount <= 0)
			return true;

		// Scan each side to see if they have the expected brightness pattern
		int failures = 0;
		failures += sampleWhiteSide(a, b, 0);
		failures += sampleWhiteSide(b, c, 1);
		failures += sampleWhiteSide(c, d, 2);
		failures += sampleWhiteSide(d, a, 3);

		boolean success = failures <= 4*whiteBorderSampleCount*maxWhiteBorderFailFraction;

		if (!success && verbose != null) {
			verbose.println("FAILED: white border test: " + failures + " / " + (4*whiteBorderSampleCount));
		}

		return success;
	}

	/**
	 * Samples the specified side to see if inside points are brighter than outside points. The corner's scale
	 * estimate is used to avoid the corners which tend to have lower contrast
	 *
	 * @return Number of points which failed the brightness check
	 */
	int sampleWhiteSide( ChessboardCorner a, ChessboardCorner b, int corner ) {
		// Use the homography to estimate the center of the white border along this side
		double r = utils.dataBorderFraction;

		// set the corners up correctly for each side
		switch (corner) {
			case 0 -> {
				utils.gridToPixel(r, r, lineWhite.a);
				utils.gridToPixel(1.0 - r, r, lineWhite.b);
				utils.gridToPixel(r, -r, lineBlack.a);
				utils.gridToPixel(1.0 - r, -r, lineBlack.b);
			}

			case 1 -> {
				utils.gridToPixel(1.0 - r, r, lineWhite.a);
				utils.gridToPixel(1.0 - r, 1.0 - r, lineWhite.b);
				utils.gridToPixel(1.0 + r, r, lineBlack.a);
				utils.gridToPixel(1.0 + r, 1.0 - r, lineBlack.b);
			}

			case 2 -> {
				utils.gridToPixel(1.0 - r, 1.0 - r, lineWhite.a);
				utils.gridToPixel(r, 1.0 - r, lineWhite.b);
				utils.gridToPixel(1.0 - r, 1.0 + r, lineBlack.a);
				utils.gridToPixel(r, 1.0 + r, lineBlack.b);
			}

			case 3 -> {
				utils.gridToPixel(r, 1.0 - r, lineWhite.a);
				utils.gridToPixel(r, r, lineWhite.b);
				utils.gridToPixel(-r, 1.0 - r, lineBlack.a);
				utils.gridToPixel(-r, r, lineBlack.b);
			}
		}

		// use blur to figure out how much padding is needed to avoid the blurred corner where black/white will
		// be ambiguous
		double blurPaddingA = Math.pow(2, a.levelMax);
		double blurPaddingB = Math.pow(2, b.levelMax);

		double slopeX = lineWhite.slopeX();
		double slopeY = lineWhite.slopeY();
		double n = Math.sqrt(slopeX*slopeX + slopeY*slopeY);

		double fractionStart, fractionEnd;

		fractionStart = blurPaddingA/n;
		fractionEnd = 1.0 - blurPaddingB/n;

		// if the blur is so great that the padding extends beyond the length just sample in the middle
		if (fractionEnd < fractionStart) {
			fractionStart = 0.45;
			fractionEnd = 0.55;
		}

		// Sample points along the line
		int failed = 0;
		for (int i = 0; i < whiteBorderSampleCount; i++) {
			double f = (fractionEnd - fractionStart)*i/(whiteBorderSampleCount - 1) + fractionStart;
			pixel.x = (lineBlack.b.x - lineBlack.a.x)*f + lineBlack.a.x;
			pixel.y = (lineBlack.b.y - lineBlack.a.y)*f + lineBlack.a.y;
			float blackValue = interpolate.get((float)pixel.x, (float)pixel.y);
			pixel.x = (lineWhite.b.x - lineWhite.a.x)*f + lineWhite.a.x;
			pixel.y = (lineWhite.b.y - lineWhite.a.y)*f + lineWhite.a.y;
			float whiteValue = interpolate.get((float)pixel.x, (float)pixel.y);

			// Just check to see if white is brighter than black. Adding a tolerance is problematic since you need
			// to estimate the tolerance
			if (whiteValue <= blackValue)
				failed++;
		}

		return failed;
	}

	/**
	 * Reads the gray values of data bits inside the square. Votes using the gray threshold. Decides on the bit values
	 *
	 * @param sampleValues (Input) Values of each pixel.. Flattened block array of points in the grid
	 * @param blockSize (Input) How many points are sampled per bit.
	 * @return true if nothing went wrong
	 */
	boolean graySamplesToBits( DogArray_F32 sampleValues, int blockSize, float threshold ) {
		// Sanity check
		BoofMiscOps.checkEq(bitImage.width*bitImage.height, sampleValues.size()/blockSize);

		int majority = blockSize/2;

		int nonWhiteBits = 0;
		for (int i = 0, bit = 0; i < sampleValues.size(); i += blockSize, bit++) {
			// Each pixel in the bit's block gets a vote for it's value
			int vote = 0;
			for (int blockIdx = 0; blockIdx < blockSize; blockIdx++) {
				float value = sampleValues.get(i + blockIdx);
				if (value <= threshold) {
					vote++;
				}
			}

			int value = vote > majority ? 1 : 0;
			nonWhiteBits += value;
			bitImage.data[bit] = (byte)value;
		}

		// If every bit is zero then it's a white square reject it
		return nonWhiteBits != 0;
	}

	/**
	 * Find the adjustment from the observed coordinate system to the one encoded and compute the number of matches
	 */
	void tallyMarkerVotes() {
		// Shape of the observed grid
		int numRows = clusterToGrid.getSparseRows();
		int numCols = clusterToGrid.getSparseCols();

		transforms.reset();
		for (int cellIdx = 0; cellIdx < binaryCells.size; cellIdx++) {
			CellReading cell = binaryCells.get(cellIdx);

			// Figure out the encoded coordinate the cell
			utils.cellIdToCornerCoordinate(cell.markerID, cell.cellID, decodedCoordinate);

			// rotate the arbitrary observed coordinate system to match the decoded
			rotateObserved(numRows, numCols, cell.row, cell.col, cell.orientation, observedCoordinate);

			// After correcting for orientation, the top left corner is in a different location
			ECoCheckUtils.adjustTopLeft(cell.orientation, observedCoordinate);

			// Figure out the offset. This should be the same for all encoded cells from the same target
			int offsetRow = decodedCoordinate.row - observedCoordinate.row;
			int offsetCol = decodedCoordinate.col - observedCoordinate.col;

			// Save the coordinate system transform
			Transform t = findMatching(offsetRow, offsetCol, cell.orientation, cell.markerID);
			if (t == null) {
				t = transforms.grow();
				t.offsetRow = offsetRow;
				t.offsetCol = offsetCol;
				t.marker = cell.markerID;
				t.orientation = cell.orientation;
			}

			// Increment the vote counter
			t.votes++;
		}
	}

	/**
	 * Finds an existing transform that matches the one just computed
	 */
	@Nullable Transform findMatching( int offsetRow, int offsetCol, int orientation, int marker ) {
		for (int i = 0; i < transforms.size; i++) {
			Transform t = transforms.get(i);
			if (t.marker != marker)
				continue;
			if (t.offsetRow != offsetRow || t.offsetCol != offsetCol || t.orientation != orientation)
				continue;
			return t;
		}
		return null;
	}

	/**
	 * Applies the transform to all found corners and creates a target ready to be returned. If a corner is impossible
	 * it's assumed to be a false positive and not added.
	 *
	 * @param transform (Input) correction to corner grd coordinate system
	 * @param target (Output) Description of target
	 * @return true if no faults found and it was successful
	 */
	boolean createCorrectedTarget( Transform transform, ECoCheckFound target ) {
		if (verbose != null)
			verbose.printf("transform: votes=%d marker=%d ori=%d offset={ row=%d col=%d }\n",
					transform.votes, transform.marker, transform.orientation, transform.offsetRow, transform.offsetCol);

		target.markerID = transform.marker;

		// Save the shape of the grid in squares
		target.squareRows = utils.markers.get(target.markerID).rows;
		target.squareCols = utils.markers.get(target.markerID).cols;
		for (int i = 0; i < binaryCells.size; i++) {
			target.decodedCells.add(binaryCells.get(i).cellID);
		}

		// Recycle the variable but give it a better name
		final GridCoordinate correctedCoordinate = observedCoordinate;

		// Get shape of the corner grid.
		int cornerRows = target.squareRows - 1;
		int cornerCols = target.squareCols - 1;

		// Go through all the found corners, correct the corner grid coordinate, check if valid, then add to the
		// found target
		FastAccess<GridElement> sparseGrid = clusterToGrid.getSparseGrid();
		for (int i = 0; i < sparseGrid.size; i++) {
			GridElement e = sparseGrid.get(i);

			// Change coordinate system
			rotateObserved(cornerRows, cornerCols, e.row, e.col, transform.orientation, correctedCoordinate);

			// Change origin
			correctedCoordinate.row += transform.offsetRow;
			correctedCoordinate.col += transform.offsetCol;

			// Make sure it's inside
			if (correctedCoordinate.row < 0 || correctedCoordinate.col < 0 ||
					correctedCoordinate.row >= cornerRows || correctedCoordinate.col >= cornerCols) {
				continue;
			}

			// If the row has been marked that means another target already claimed this corner and a false positive
			// is highly likely
			if (e.marked)
				return false;
			e.marked = true;

			// The ID is the index from a row-major matrix
			int cornerID = correctedCoordinate.row*cornerCols + correctedCoordinate.col;

			// Save the pixel coordinate it was observed at
			target.addCorner(e.node.corner, cornerID);

			// Note if this corner touches a binary pattern
			target.touchBinary.add(cornersAroundBinary.get(e.col, e.row) != 0);
		}

		return true;
	}

	/**
	 * No binary pattern was found inside the so we don't know which target it is, but this information still might
	 * be useful
	 */
	void createAnonymousTarget() {
		if (!clusterToGrid.sparseToGrid(anonymousInfo))
			return;

		ECoCheckFound target = found.grow();
		target.squareRows = anonymousInfo.rows + 1;
		target.squareCols = anonymousInfo.cols + 1;

		for (int cornerID = 0; cornerID < anonymousInfo.nodes.size(); cornerID++) {
			ChessboardCornerGraph.Node n = anonymousInfo.nodes.get(cornerID);
			target.addCorner(n.corner, cornerID);
		}
	}

	/**
	 * Type of input image it processes
	 */
	public ImageType<T> getImageType() {
		return detector.getImageType();
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
		BoofMiscOps.verboseChildren(out, configuration, clusterFinder, clusterToGrid);

		if (configuration == null)
			return;

		runtimeProfiling = configuration.contains(BoofVerbose.RUNTIME);
	}

	/**
	 * Records a decoded cell and the location in the observed grid it was decoded at
	 */
	static class CellReading {
		/** Location of cell in the observed coordinate system */
		public int row, col;
		/** Number of times it needed to be rotated before decoded */
		public int orientation;
		/** Decoded marker */
		public int markerID;
		/** Decoded cell */
		public int cellID;

		public void reset() {
			row = 0;
			col = 0;
			orientation = -1;
			markerID = -1;
			cellID = -1;
		}
	}

	/**
	 * Transform from the arbitrary grid coordinate system into
	 */
	static class Transform {
		/** Adjustment to grid coordinates after applying the rotation */
		public int offsetRow, offsetCol;
		/** Rotational difference between observed and encoded coordinate system */
		public int orientation;
		/** Marker ID */
		public int marker;
		/** Number of encoded squares that agree with this transform */
		public int votes;

		public void setTo( int offsetRow, int offsetCol, int orientation, int marker, int votes ) {
			this.offsetRow = offsetRow;
			this.offsetCol = offsetCol;
			this.orientation = orientation;
			this.marker = marker;
			this.votes = votes;
		}

		public void reset() {
			offsetRow = 0;
			offsetCol = 0;
			orientation = 0;
			votes = 0;
			marker = -1;
		}
	}
}
