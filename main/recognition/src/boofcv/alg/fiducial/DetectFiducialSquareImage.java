/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial;

import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.distort.DistortImageOps;
import boofcv.alg.feature.associate.HammingTable16;
import boofcv.alg.feature.shapes.SplitMergeLineFitLoop;
import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO comment
 * @author Peter Abeles
 */
public class DetectFiducialSquareImage<T extends ImageSingleBand>
		extends BaseDetectFiducialSquare<T> {

	// Width of black border (units = pixels)
	private final static int w=16;
	private final static int squareLength=w*4; // this must be a multiple of 16
	// length of description in 16bit units
	private final static int DESC_LENGTH = squareLength*squareLength/16;

	// converts the input image into a binary one
	private InputToBinary<ImageFloat32> threshold = FactoryThresholdBinary.globalOtsu(0,256,true,ImageFloat32.class);
	private ImageUInt8 binary = new ImageUInt8(squareLength,squareLength);

	// list of all known targets
	private List<FiducialDef> targets = new ArrayList<FiducialDef>();

	// lookup table to make computing the match score very fast
	private HammingTable16 table = new HammingTable16();

	// description of the current target candidate
	private  short squareDef[] = new short[DESC_LENGTH];

	// storage for no border sub-image
	private ImageFloat32 grayNoBorder = new ImageFloat32();

	// if the hamming score is better than this it is considered to be a good match
	private int hammingThreshold;

	/**
	 * Configures the fiducial detector
	 *
	 * @param matchThreshold Considered a match if the hamming distance is less than this fraction of the maximum
	 */
	public DetectFiducialSquareImage(InputToBinary<T> thresholder,
									 SplitMergeLineFitLoop fitPolygon,
									 double minContourFraction, double matchThreshold, Class<T> inputType) {
		super(thresholder,fitPolygon, squareLength+squareLength, minContourFraction, inputType);

		hammingThreshold = (int)(squareLength*squareLength*matchThreshold);

		//noinspection ConstantConditions
		if( squareLength%16 != 0 )
			throw new RuntimeException("Square Length must be a multiple of 16");
	}

	/**
	 * Adds a new image to the detector.  Image must be gray-scale and is converted into
	 * a binary image using the specified threshold.  All input images are rescaled to be
	 * square and of the appropriate size.  Thus the original shape of the image doesn't
	 * matter.  Square shapes are highly recommended since that's what the target looks like.
	 *
	 * @param grayScale Grayscale input image
	 * @param threshold Threshold which will be used to convert it into a binary image
	 * @return The ID of the provided image
	 */
	public int addImage( T grayScale , double threshold ) {
		// scale the image to the desired size
		T scaled = GeneralizedImageOps.createSingleBand(getInputType(),squareLength,squareLength);
		DistortImageOps.scale(grayScale,scaled, TypeInterpolate.BILINEAR);

		// threshold it
		ImageUInt8 binary0 = binary;
		ImageUInt8 binary1 = new ImageUInt8(squareLength,squareLength);
		GThresholdImageOps.threshold(scaled,binary0,threshold,true);

		// describe it in 4 different orientations
		FiducialDef def = new FiducialDef();

		binaryToDef(binary0, def.desc[0]);
		ImageMiscOps.rotateCW(binary0, binary1);
		binaryToDef(binary1, def.desc[1]);
		ImageMiscOps.rotateCW(binary1,binary0);
		binaryToDef(binary0,def.desc[2]);
		ImageMiscOps.rotateCW(binary0,binary1);
		binaryToDef(binary1,def.desc[3]);

		int index = targets.size();
		targets.add( def );
		return index;
	}

	/**
	 * Converts a binary image into the compressed bit format
	 */
	protected static void binaryToDef( ImageUInt8 binary , short[] desc ) {
		for (int i = 0; i < binary.data.length; i+=16) {
			int value = 0;
			for (int j = 0; j < 16; j++) {
				value |= binary.data[i+j] << j;
			}
			desc[i/16] = (short)value;
		}
	}

	@Override
	protected boolean processSquare(ImageFloat32 gray, Result result) {

		int off = (gray.width-binary.width)/2;
		gray.subimage(off,off,gray.width-off,gray.width-off,grayNoBorder);

		threshold.process(grayNoBorder,binary);

		binaryToDef(binary,squareDef);

		for (int i = 0; i < targets.size(); i++) {
			FiducialDef def = targets.get(i);

			int bestOrientation = 0;
			int bestScore = Integer.MAX_VALUE;
			for (int j = 0; j < 4; j++) {
				int score = hamming(def.desc[j], squareDef);
				if( score < bestScore ) {
					bestScore = score;
					bestOrientation = j;
				}
			}

			// see if it meets the match threshold
			if( bestScore <= hammingThreshold ) {
				result.rotation = bestOrientation;
				result.which = i;
				return true;
			}
		}

		return false;
	}

	/**
	 * Computes the hamming score between two descriptions.  Larger the number better the fit
	 */
	protected int hamming(short[] a, short[] b) {
		int distance = 0;
		for (int i = 0; i < a.length; i++) {
			distance += table.lookup(a[i],b[i]);
		}
		return distance;
	}

	public List<FiducialDef> getTargets() {
		return targets;
	}

	/**
	 * description of an image in 4 different orientations
	 */
	protected static class FiducialDef
	{
		short[][] desc = new short[4][DESC_LENGTH];
	}
}
