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
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.ImageGray;
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
 * @author Peter Abeles
 */
public class AztecDecoderImage<T extends ImageGray<T>> implements VerbosePrint {
	/** Found and successfully decoded markers in the image */
	@Getter final List<AztecCode> found = new ArrayList<>();

	/** Markers that it failed to decode */
	@Getter final List<AztecCode> failed = new ArrayList<>();

	/** used to subsample the input image */
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

	public void process( List<AztecPyramid> locatorPatterns, T gray ) {
		allMarkers.reset();
		found.clear();
		failed.clear();

		for (int i = 0; i < locatorPatterns.size(); i++) {
			AztecCode marker = allMarkers.grow();
			if (!decodeMode(locatorPatterns.get(i), marker)) {
				failed.add(marker);
				continue;
			}

			if (!decodeData(marker)) {
				failed.add(marker);
				continue;
			}

			found.add(marker);
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
		readModeBits(locator);

		// Determine the orientation
		int orientation = selectOrientation(type);
		if (orientation < 0)
			return false;

		// TODO correct orientation of pyramid

		// Read data bits given known orientation
		extractModeDataBits(orientation, type);

		// Apply error correction and extract the mode
		code.structure = type;
		return codecMode.decodeMode(bits, code);
	}

	void readModeBits( AztecPyramid locator ) {
		AztecPyramid.Layer layer = locator.layers.get(0);
		int locatorGridWith = locator.layers.size == 2 ? 9 : 5;
		gridToPixel.initOriginCorner0(layer.square, locatorGridWith);

		float threshold = (float)layer.threshold;

		int modeGridWidth = locatorGridWith + 5;
		int offset = modeGridWidth/2 - locatorGridWith/2;

		// read top, right, bottom, left in a circle around the center
		imageBits.resize(0);
		readBitsRow(-offset, -offset, 1, 0, modeGridWidth, threshold);
		readBitsRow(offset, -offset - 1, 0, -1, modeGridWidth - 2, threshold);
		readBitsRow(offset, offset, -1, 0, modeGridWidth, threshold);
		readBitsRow(-offset, offset + 1, 0, 1, modeGridWidth - 2, threshold);
	}

	/**
	 * Reads bits along a line. The line is specified in grid coordinates starting at (x0, y0) in direction of
	 * (dx, dy) for total bits.
	 */
	void readBitsRow( int x0, int y0, int dx, int dy, int total, float threshold ) {
		// first write to a single integer for a small speed boost
		int readBits = 0;
		for (int i = 0; i < total; i++) {
			// read from the center of a square
			double x = x0 + i*dx + 0.5;
			double y = y0 + i*dy + 0.5;
			gridToPixel.convert(x, y, pixel);
			float value = interpolate.get((float)pixel.x, (float)pixel.y);
			if (value < threshold)
				readBits |= 1 << i;
		}
		imageBits.append(readBits, total, true);
	}

	/** Select best orientation looking at the orientation patterns */
	int selectOrientation( Structure type ) {
		int[] modeBitTypes = getModeBitType(type);

		int width = modeBitTypes.length/4;
		int bestOrientation = -1;
		int bestErrors = Integer.MAX_VALUE;
		for (int ori = 0; ori < 4; ori++) {
			int errors = fixedFeatureModeErrors(ori*width, modeBitTypes);
			if (errors < bestErrors) {
				bestErrors = errors;
				bestOrientation = ori;
			}
		}

		return bestOrientation;
	}

	void extractModeDataBits( int orientation, Structure type ) {
		int offset = orientation*(type == Structure.COMPACT ? 10 : 15);
		int[] modeBitTypes = getModeBitType(type);

		bits.resize(0);
		for (int i = 0; i < modeBitTypes.length; i++) {
			if (modeBitTypes[i] != 2)
				continue;

			int index = (i + offset)%modeBitTypes.length;
			bits.append(imageBits.get(index), 1, false);
		}
	}

	private static int[] getModeBitType( Structure type ) {
		return type == Structure.COMPACT ? modeBitTypesComp : modeBitTypesFull;
	}

	/** See how many if the fixed features do not match obsrevations */
	int fixedFeatureModeErrors( int offset, int[] modeBitTypes ) {
		BoofMiscOps.checkEq(modeBitTypes.length, imageBits.size);

		int errors = 0;
		for (int i = 0; i < modeBitTypes.length; i++) {
			int type = modeBitTypes[i];

			// Data bit. Skip for now
			if (type == 2) {
				continue;
			}

			// Fixed Bit
			int index = (i + offset)%modeBitTypes.length;
			if (imageBits.get(index) != type)
				errors++;
		}
		return errors;
	}

	protected boolean decodeData( AztecCode code ) {
		return true;
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
	}
}
