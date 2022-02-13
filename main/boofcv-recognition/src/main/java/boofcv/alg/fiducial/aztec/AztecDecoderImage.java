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

import boofcv.alg.fiducial.aztec.AztecCode.Structure;
import boofcv.alg.fiducial.qrcode.PackedBits8;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.interpolate.InterpolationType;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.border.BorderType;
import boofcv.struct.image.ImageGray;
import georegression.geometry.UtilPolygons2D_F64;
import georegression.struct.point.Point2D_F64;
import lombok.Getter;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.VerbosePrint;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Given locations of candidate finder patterns and the source image, decode all the markers inside the image and
 * reject false positives.
 *
 * Note: A fixed feature is a feature on the marker which is not dynamically computed. E.g. the pyramid.
 *
 * @author Peter Abeles
 */
public class AztecDecoderImage<T extends ImageGray<T>> implements VerbosePrint {
	/** Successfully decoded markers in the image */
	@Getter final List<AztecCode> success = new ArrayList<>();

	/** Candidate markers which could not be decoded */
	@Getter final List<AztecCode> failed = new ArrayList<>();

	/** Reads interpolated image pixel intensity values */
	@Getter protected InterpolatePixelS<T> interpolate;

	/**
	 * Should it consider a QR code which has been encoded with a transposed bit pattern?
	 *
	 * TODO update when config exists
	 * //	 * @see boofcv.factory.fiducial.ConfigQrCode#considerTransposed
	 */
	public boolean considerTransposed = true;

	@Nullable PrintStream verbose = null;

	// Specifies which bits are fixed and which are data. 1 = black, 0 = white, 2 = data
	final static int[] modeBitTypesFull = new int[]{
			1, 1, 2, 2, 2, 2, 2, 0, 2, 2, 2, 2, 2, 0,
			1, 1, 2, 2, 2, 2, 2, 0, 2, 2, 2, 2, 2, 1,
			0, 0, 2, 2, 2, 2, 2, 0, 2, 2, 2, 2, 2, 0,
			0, 0, 2, 2, 2, 2, 2, 0, 2, 2, 2, 2, 2, 1};
	final static int[] modeBitTypesComp = new int[]{
			1, 1, 2, 2, 2, 2, 2, 2, 2, 0,
			1, 1, 2, 2, 2, 2, 2, 2, 2, 1,
			0, 0, 2, 2, 2, 2, 2, 2, 2, 0,
			0, 0, 2, 2, 2, 2, 2, 2, 2, 1};

	final DogArray<AztecCode> allMarkers = new DogArray<>(AztecCode::new, AztecCode::reset);

	AztecDecoder decoderBits = new AztecDecoder();
	AztecMessageModeCodec codecMode = new AztecMessageModeCodec();

	// value of bits directly read in from the image. This will include fixed structures
	PackedBits8 imageBits = new PackedBits8();
	// bit values after fixed structures are removed. It will be just data
	PackedBits8 bits = new PackedBits8();

	GridToPixelHelper gridToPixel = new GridToPixelHelper();
	Point2D_F64 pixel = new Point2D_F64();

	public AztecDecoderImage( Class<T> imageType ) {
		interpolate = FactoryInterpolation.createPixelS(
				0, 255, InterpolationType.NEAREST_NEIGHBOR, BorderType.EXTENDED, imageType);
	}

	public void process( List<AztecPyramid> locatorPatterns, T gray ) {
		interpolate.setImage(gray);
		allMarkers.reset();
		success.clear();
		failed.clear();

		for (int i = 0; i < locatorPatterns.size(); i++) {
			AztecCode marker = allMarkers.grow();
			if (!decodeMode(locatorPatterns.get(i), marker)) {
				failed.add(marker);
				continue;
			}

			if (!decodeMessage(marker)) {
				failed.add(marker);
				continue;
			}

			success.add(marker);
		}
	}

	/**
	 * Reads the image and decodes the marker's mode
	 *
	 * @param locator Locator pattern
	 * @param code Storage for decoded marker
	 * @return true if successful or false if it failed
	 */
	protected boolean decodeMode( AztecPyramid locator, AztecCode code ) {
		code.locator.setTo(locator);
		Structure type = locator.layers.size == 1 ? Structure.COMPACT : Structure.FULL;

		// Read the pixel values once
		readModeBitsFromImage(locator);

		// Determine the orientation
		int orientation = selectOrientation(type);
		if (orientation < 0)
			return false;

		// Read data bits given known orientation
		extractModeDataBits(orientation, type);

		// Rotate the locator pattern so that it's in the canonical position. corner[0] is top left
		for (int i = 0; i < orientation; i++) {
			for (int layerIdx = 0; layerIdx < code.locator.layers.size; layerIdx++) {
				UtilPolygons2D_F64.shiftUp(code.locator.layers.get(layerIdx).square);
			}
		}

		// Apply error correction and extract the mode
		code.structure = type;
		return codecMode.decodeMode(bits, code);
	}

	/**
	 * Read image pixel intensity values and use polygon threshold to find the bit values around the pyramid.
	 */
	void readModeBitsFromImage( AztecPyramid locator ) {
		AztecPyramid.Layer layer = locator.layers.get(0);
		int modeGridWidth = locator.layers.size == 2 ? 15 : 11;
		gridToPixel.initOriginCenter(layer.square, modeGridWidth - 6);

		float threshold = (float)layer.threshold;

		// radius in squares. Remenber coordinate system above is defined as the center of innermost square
		// So we will be sampling at the center of each square because of an implicit 0.5 square offset
		int radius = modeGridWidth/2;

		// read top, right, bottom, left in a circle around the center
		imageBits.resize(0);
		readBitsRow(-radius, -radius, 1, 0, modeGridWidth - 1, threshold);
		readBitsRow(radius, -radius, 0, 1, modeGridWidth - 1, threshold);
		readBitsRow(radius, radius, -1, 0, modeGridWidth - 1, threshold);
		readBitsRow(-radius, radius, 0, -1, modeGridWidth - 1, threshold);
	}

	/**
	 * Reads bits along a line. The line is specified in grid coordinates starting at (x0, y0) in direction of
	 * (dx, dy) for total bits.
	 */
	void readBitsRow( int x0, int y0, int dx, int dy, int total, float threshold ) {
		// first write to a single integer for a small speed boost
		int readBits = 0;
		for (int i = 0; i < total; i++) {
			double x = x0 + i*dx;
			double y = y0 + i*dy;
			gridToPixel.convert(x, y, pixel);
			float value = interpolate.get((float)pixel.x, (float)pixel.y);
			if (value < threshold)
				readBits |= 1 << i;
		}
		imageBits.append(readBits, total, true);
	}

	/**
	 * Select best orientation looking at the orientation patterns. Return number
	 * indicates which side is really the starting point
	 */
	int selectOrientation( Structure type ) {
		int[] modeBitTypes = getModeBitType(type);

		int width = modeBitTypes.length/4;
		int bestOrientation = -1;
		int bestErrors = Integer.MAX_VALUE;
		for (int ori = 0; ori < 4; ori++) {
			int errors = fixedFeatureReadErrors(ori*width, modeBitTypes);
			if (errors < bestErrors) {
				bestErrors = errors;
				bestOrientation = ori;
			}
		}

		return bestOrientation;
	}

	/** Extracts data from the previously read in mode bits, skipping fixed bits */
	void extractModeDataBits( int orientation, Structure type ) {
		int[] modeBitTypes = getModeBitType(type);
		int offset = orientation*modeBitTypes.length/4;

		bits.resize(0);
		for (int i = 0; i < modeBitTypes.length; i++) {
			if (modeBitTypes[i] != 2)
				continue;

			int index = (i + offset)%modeBitTypes.length;
			bits.append(imageBits.get(index), 1, false);
		}
	}

	static int[] getModeBitType( Structure type ) {
		return type == Structure.COMPACT ? modeBitTypesComp : modeBitTypesFull;
	}

	/** See how many of the fixed features do not match observations */
	int fixedFeatureReadErrors( int offset, int[] modeBitTypes ) {
		BoofMiscOps.checkEq(modeBitTypes.length, imageBits.size);

		int errors = 0;
		for (int i = 0; i < modeBitTypes.length; i++) {
			int type = modeBitTypes[i];

			// Data bit. Skip for now
			if (type == 2)
				continue;

			// Fixed Bit. See if it has the expected value
			int index = (i + offset)%modeBitTypes.length;
			if (imageBits.get(index) != type)
				errors++;
		}
		return errors;
	}

	protected boolean decodeMessage( AztecCode code ) {
		return true;
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
	}
}
