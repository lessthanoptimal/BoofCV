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

package boofcv.alg.fiducial.calib.chessbits;

import boofcv.abst.fiducial.calib.ConfigChessboardX;
import boofcv.alg.feature.detect.chess.DetectChessboardCornersXPyramid;
import boofcv.alg.fiducial.calib.chess.ChessboardCornerClusterFinder;
import boofcv.alg.fiducial.calib.chess.ChessboardCornerClusterToGrid;
import boofcv.alg.fiducial.calib.chess.ChessboardCornerClusterToGrid.GridElement;
import boofcv.alg.fiducial.calib.chess.ChessboardCornerGraph;
import boofcv.alg.fiducial.qrcode.PackedBits8;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.GridCoordinate;
import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.FastAccess;
import org.ddogleg.struct.FastArray;
import org.jetbrains.annotations.Nullable;

/**
 * @author Peter Abeles
 */
public class ChessboardReedSolomonDetector<T extends ImageGray<T>> {
	// TODO Create a custom cluster finder
	// TODO only keep clusters with 1 encoded pattern inside
	// TODO concensus approach to deciding coordinates
	// TODo specify max grid coordinates
	// TODO Handle the case where two or more chessboard patterns get merged together but can be distinguished based on
	//      encoded cells
	// TODO Weigh bit sample points based on distance from center?
	// TODO Add back in the ability to encoded every N squares

	protected ChessBitsUtils utils;

	protected DetectChessboardCornersXPyramid<T> detector;
	protected ChessboardCornerClusterFinder<T> clusterFinder;
	protected ChessboardCornerClusterToGrid clusterToGrid = new ChessboardCornerClusterToGrid();

	FastArray<CellReading> gridCells = new FastArray<>(CellReading.class);
	DogArray<CellReading> cells = new DogArray<>(CellReading::new, CellReading::reset);

	/** Used to sample the input image when "undistorting" the bit pattern */
	public InterpolatePixelS<T> interpolate;

	/** Found chessboard patterns */
	public DogArray<ChessboardBitPattern> found = new DogArray<>(ChessboardBitPattern::new, ChessboardBitPattern::reset);

	// Image pixels that are read when decoding the bit pattern
	DogArray<Point2D_F64> samplePixels = new DogArray<>(Point2D_F64::new);
	// The read in bits in a format the codec can understand
	PackedBits8 bits = new PackedBits8();
	// Storage the bits inside an image so that it can be rotated easily
	GrayU8 bitImage = new GrayU8(1, 1);
	GrayU8 workImage = new GrayU8(1, 1);

	// Found transform from found to a marker coordinate system
	DogArray<Transform> transforms = new DogArray<>(Transform::new, Transform::reset);

	// Storage for a decided binary pattern
	CellValue decoded = new CellValue();

	public ChessboardReedSolomonDetector( ChessBitsUtils utils,
										  ConfigChessboardX config, Class<T> imageType ) {

		this.utils = utils;

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

		interpolate = FactoryInterpolation.nearestNeighborPixelS(imageType);

		bitImage.reshape(utils.codec.gridBitLength, utils.codec.gridBitLength);
	}

	/**
	 * Processes the image and searches for all chessboard patterns.
	 */
	public void process( T input ) {
		found.reset();
		interpolate.setImage(input);
		detector.process(input);
		clusterFinder.process(input, detector.getCorners().toList(), detector.getNumberOfLevels());
		DogArray<ChessboardCornerGraph> clusters = clusterFinder.getOutputClusters();

		for (int clusterIdx = 0; clusterIdx < clusters.size; clusterIdx++) {
			// Find the chessboard pattern inside the cluster
			if (!clusterToGrid.clusterToSparse(clusters.get(clusterIdx))) {
				continue;
			}
			clusterToGrid.sparseToDense();

			int rows = clusterToGrid.getSparseRows();
			int cols = clusterToGrid.getSparseCols();

			// Go through and find all the white squares that could contain data. Attempt to decode
			decodeBinaryPatterns(rows, cols);

			// If no data patterns are found, extract the largest grid and return that
			if (cells.isEmpty()) {
				// TODO create empty result from largest grid
				continue;
			}

			// TODO split up chessboard if multiple likely hypotheses exist
			// Select the coordinate system that best matches the decoded cells
			tallyMarkerVotes();
			Transform t = selectBestTransform();

			// Construct the resulting observed chessboard by applying the transform
			createCorrectedTarget(t, found.grow());

			// TODO mark which corners had decded patterns next to them
		}
	}

	/**
	 * Examines all white cells in the found chessboard grid and attempts to decide the binary patterns.
	 */
	private void decodeBinaryPatterns( int rows, int cols ) {
		gridCells.clear();
		gridCells.resize(rows*cols);
		cells.reset();

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

				// See if this square could have an ecoded value
				if (!clusterToGrid.isWhiteSquare(a.node, c.node))
					continue;

				// TODO use homography to select sample points so that perspective distortion is handled correctly
				// Convert image pixels into bit values
				float threshold = determineThreshold(a.node, b.node, c.node, d.node);
				sampleBitsGray(a.node, b.node, c.node, d.node, threshold);

//				bitImage.printBinary();

				// true if it could be decoded
				boolean success = false;

				// Try all 4 possible orientations until something works
				for (int orientation = 0; orientation < 4; orientation++) {
					convertBitImageToBitArray();
					if (!utils.codec.decode(bits, decoded)) {
						ImageMiscOps.rotateCCW(bitImage, workImage);
						bitImage.setTo(workImage);
						continue;
					}

					success = true;

					// Save the decoded results into a sparse grid
					CellReading cell = gridCells.data[row*cols + col] = cells.grow();
					cell.row = row - 1;
					cell.col = col - 1;
					cell.orientation = orientation;
					cell.markerID = decoded.markerID;
					cell.cellID = decoded.cellID;
					System.out.println("   (below) cellID="+cell.cellID);
					break;
				}

				System.out.println("white square grid=(" + (row - 1) + "," + (col - 1) + ") tl=" + a.node + " decoded=" + success);

//				if (!success)
//					System.out.println("Failed to decode white square: row=" + (row-1) + " col=" + (col-1));
			}
		}
	}

	void convertBitImageToBitArray() {
		bits.resize(bitImage.width*bitImage.height);
		for (int y = 0, i = 0; y < bitImage.height; y++) {
			for (int x = 0; x < bitImage.width; x++, i++) {
				bits.set(i, bitImage.data[i]);
			}
		}
	}

	/**
	 * Sample along the line in known white/black regions
	 */
	private float determineThreshold( Point2D_F64 a, Point2D_F64 b, Point2D_F64 c, Point2D_F64 d ) {
		float sum = 0.0f;

		sum += sampleThresholdSide(a, b);
		sum += sampleThresholdSide(b, c);
		sum += sampleThresholdSide(c, d);
		sum += sampleThresholdSide(d, a);

		return sum/4.0f;
	}

	/**
	 * Samples points offset along the line. The extremes of the line are avoided since those will naturally be
	 * blurry in a chessboard pattern
	 *
	 * @return Average value of tangent points along the side
	 */
	private float sampleThresholdSide( Point2D_F64 a, Point2D_F64 b ) {
		final int numSamples = 5; // number of samples along each side

		double border = utils.dataBorderFraction;
		double length = 1.0 - 2.0*border;

		// this specifies how far away from the line it will sample in the tangent direction
		// We use the border as guide and sample in the middle of it
		float nx = (float)(0.5*(b.x - a.x)*utils.dataBorderFraction);
		float ny = (float)(0.5*(b.y - a.y)*utils.dataBorderFraction);

		float sumWhite = 0.0f;
		float sumBlack = 0.0f;

		for (int i = 0; i < numSamples; i++) {
			double f = border + length*i/(double)(numSamples - 1);
			float cx = (float)(a.x + (b.x - a.x)*f);
			float cy = (float)(a.y + (b.y - a.y)*f);

			sumWhite += interpolate.get(cx - ny, cy + nx);
			sumBlack += interpolate.get(cx + ny, cy - nx);
		}

		return (sumWhite + sumBlack)/(2.0f*numSamples);
	}

	/**
	 * Reads the gray values of data bits inside the square. Votes using the gray threshold. Decides on the bit values
	 *
	 * @return true if nothing went wrong
	 */
	private boolean sampleBitsGray( Point2D_F64 a, Point2D_F64 b, Point2D_F64 c, Point2D_F64 d, float threshold ) {
		// compute homography
		if (!utils.computeGridToImage(a, b, c, d))
			return false;

		// Which pixels need to be sampled
		utils.selectPixelsToSample(samplePixels);

		int blockSize = utils.bitSampleCount;
		int majority = blockSize/2;

		for (int i = 0, bit = 0; i < samplePixels.size; i += blockSize, bit++) {
			// Each pixel in the bit's block gets a vote for it's value
			int vote = 0;
			for (int blockIdx = 0; blockIdx < blockSize; blockIdx++) {
				Point2D_F64 pixel = samplePixels.get(i + blockIdx);
				float value = interpolate.get((float)pixel.x, (float)pixel.y);
				if (value <= threshold) {
					vote++;
				}
//				System.out.println("   sample="+pixel+"  value="+value);
			}

//			System.out.println("center="+samplePixels.get(i + blockSize-1)+" vote="+vote);

			bitImage.data[bit] = (byte)(vote > majority ? 1 : 0);
		}

		return true;
	}

	/**
	 * Find the adjustment from the observed coordinate system to the one encoded and compute the number of matches
	 */
	void tallyMarkerVotes() {
		// Coordinate decoded from cellID
		var decodedCoordinate = new GridCoordinate();

		// Coordinate of the cell observed on the grid taking in account orientation
		var observedCoordinate = new GridCoordinate();

		// Shape of the observed grid
		int numRows = clusterToGrid.getSparseRows();
		int numCols = clusterToGrid.getSparseCols();

		for (int cellIdx = 0; cellIdx < cells.size; cellIdx++) {
			CellReading cell = cells.get(cellIdx);

			// Figure out the encoded coordinate the cell
			utils.cellToCoordinate(cell.markerID, cell.cellID, decodedCoordinate);

			// rotate the arbitrary observed coordinate system to match the decoded
			rotateObserved(numRows, numCols, cell.row, cell.col, cell.orientation, observedCoordinate);

			// Figure out the offset. This should be the same for all encoded cells from the same target
			int offsetRow = decodedCoordinate.row - observedCoordinate.row;
			int offsetCol = decodedCoordinate.col - observedCoordinate.col;

			System.out.println("cell: id="+cell.cellID+" ori="+cell.orientation+" coor="+decodedCoordinate+" obs=("+cell.row+","+cell.col+") adj="+observedCoordinate);
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

	Transform selectBestTransform() {
		Transform best = transforms.get(0);
		for (int i = 1; i < transforms.size; i++) {
			Transform t = transforms.get(i);
			if (t.votes > best.votes)
				best = t;
		}
		return best;
	}

	/**
	 * Rotates the observed coordinate system so that it aligns with the decoded coordinate system
	 */
	static void rotateObserved( int numRows, int numCols, int row, int col, int orientation, GridCoordinate found ) {
		switch (orientation) {
			case 0 -> found.setTo(row, col); // top-left
			case 1 -> found.setTo(numCols - col - 1, row); // top-right
			case 2 -> found.setTo(numRows - row - 1, numCols - col - 1); // bottom-right
			case 3 -> found.setTo(col, numRows - row - 1);  // bottom-left
			default -> throw new IllegalStateException("Unknown orientation");
		}
	}

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
	 * @param transform (Input) correction to corner grd coordinate system
	 * @param target (Output) Description of target
	 */
	void createCorrectedTarget( Transform transform, ChessboardBitPattern target ) {
		target.marker = transform.marker;

		// Save the shape of the grid in squares
		target.squareRows = utils.markers.get(target.marker).rows;
		target.squareCols = utils.markers.get(target.marker).cols;

		var correctedCoordinate = new GridCoordinate();

		// Get shape of the corner grid.
		int cornerRows = target.squareRows - 1;
		int cornerCols = target.squareCols - 1;

		// Go through all the found corners, correct the corner grid coordinate, check if valid, then add to the
		// found target
		FastAccess<GridElement> sparseGrid = clusterToGrid.getSparseGrid();
		for (int i = 0; i < sparseGrid.size; i++) {
			GridElement e = sparseGrid.get(i);
			rotateObserved(cornerRows, cornerCols, e.row, e.col, transform.orientation, correctedCoordinate);
			if (correctedCoordinate.row < 0 || correctedCoordinate.col < 0 ||
					correctedCoordinate.row >= cornerRows || correctedCoordinate.col >= cornerCols) {
				continue;
			}

			int cornerIndex = correctedCoordinate.row*cornerRows + correctedCoordinate.col;
			target.corners.grow().setTo(e.node.x, e.node.y, cornerIndex);

			// TODO denote which corners were corners around a decoded square
		}

		for (int i = 0; i < target.corners.size; i++) {
			PointIndex2D_F64 c = target.corners.get(i);
			System.out.println("corner="+c);
		}
	}

	/**
	 * Records a decoded cell and the location in the observed grid it was decoded at
	 */
	private static class CellReading {
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
	private static class Transform {
		/** Adjustment to grid coordinates after applying the rotation */
		public int offsetRow, offsetCol;
		/** Rotational difference between observed and encoded coordinate system */
		public int orientation;
		/** Marker ID */
		public int marker;
		/** Number of encoded squares that agree with this transform */
		public int votes;

		public void reset() {
			offsetRow = 0;
			offsetCol = 0;
			orientation = 0;
			votes = 0;
			marker = -1;
		}
	}
}
