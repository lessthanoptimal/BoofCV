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

package boofcv.alg.fiducial.square;

import boofcv.abst.distort.FDistort;
import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.descriptor.DescriptorDistance;
import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.alg.filter.misc.AverageDownSampleOps;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.alg.misc.PixelMath;
import boofcv.alg.shapes.polygon.BinaryPolygonDetector;
import boofcv.core.image.ConvertImage;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Fiducial which uses images to describe arbitrary binary patterns.  When useing this fiducial it's up to the user to
 * select good images which will provide unique orientation and are easily distinguished against other patterns and
 * noise insensitive.
 * </p>
 * <center>
 * <img src="doc-files/square_image.png"/>
 * </center>
 * <p>
 * The above image visually shows the fiducials internal coordinate system.  The center of the fiducial is the origin
 * of the coordinate system, e.g. all sides are width/2 distance away from the origin.  +x is to the right, +y is up
 * , and +z out of the paper towards the viewer.
 * </p>
 * <p>
 * A good pattern will have thick lines or thick shapes.  When detecting the image it's not uncommon for the distortion
 * removal to be off by one or two pixels.  So think lines are be completely out of synch.  The image should also
 * be chosen so that there is to rotational ambiguity.  A perfect circle in the center is an example of a bad fiducial
 * in which orientation can't be uniquely determined.
 * </p>
 * @author Peter Abeles
 */
public class DetectFiducialSquareImage<T extends ImageGray>
		extends BaseDetectFiducialSquare<T> {

	// Width of black border (units = pixels)
	private final static int w=16;
	private final static int squareLength=w*4; // this must be a multiple of 16
	// length of description in 16bit units
	private final static int DESC_LENGTH = squareLength*squareLength/16;

	// converts the input image into a binary one
	private GrayU8 binary = new GrayU8(squareLength,squareLength);

	// list of all known targets
	private List<FiducialDef> targets = new ArrayList<>();

	// description of the current target candidate
	private  short squareDef[] = new short[DESC_LENGTH];

	// storage for no border sub-image
	private GrayF32 grayNoBorder = new GrayF32();

	// if the hamming score is better than this it is considered to be a good match
	private int hammingThreshold;

	/**
	 * Configures the fiducial detector
	 *
	 * @param matchThreshold Considered a match if the hamming distance is less than this fraction of the maximum
	 */
	public DetectFiducialSquareImage(InputToBinary<T> inputToBinary,
									 BinaryPolygonDetector<T> quadDetector,
									 double borderWidthFraction ,
									 double minimumBlackBorderFraction ,
									 double matchThreshold, Class<T> inputType) {
		super(inputToBinary,quadDetector,borderWidthFraction, minimumBlackBorderFraction,
				(int)Math.round(squareLength/(1-2.0*borderWidthFraction)), inputType);

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
	 * @param inputBinary Binary input image pattern.  0 = black, 1 = white.
	 * @param lengthSide How long one of the sides of the target is in world units.
	 * @return The ID of the provided image
	 */
	public int addPattern(GrayU8 inputBinary, double lengthSide) {
		if( inputBinary == null ) {
			throw new IllegalArgumentException("Input image is null.");
		} else if( lengthSide <= 0 ) {
			throw new IllegalArgumentException("Parameter lengthSide must be more than zero");
		} else if(ImageStatistics.max(inputBinary) > 1 )
			throw new IllegalArgumentException("A binary image is composed on 0 and 1 pixels.  This isn't binary!");

		// see if it needs to be resized
		if ( inputBinary.width != squareLength || inputBinary.height != squareLength ) {
			// need to create a new image and rescale it to better handle the resizing
			GrayF32 inputGray = new GrayF32(inputBinary.width,inputBinary.height);
			ConvertImage.convert(inputBinary,inputGray);
			PixelMath.multiply(inputGray,255,inputGray);

			GrayF32 scaled = new GrayF32(squareLength,squareLength);

			// See if it can use the better algorithm for scaling down the image
			if( inputBinary.width > squareLength && inputBinary.height > squareLength ) {
				AverageDownSampleOps.down(inputGray,scaled);
			} else {
				new FDistort(inputGray,scaled).scaleExt().apply();
			}
			GThresholdImageOps.threshold(scaled,binary,255/2.0,false);
		} else {
			binary.setTo(inputBinary);
		}

		// describe it in 4 different orientations
		FiducialDef def = new FiducialDef();
		def.lengthSide = lengthSide;

		// CCW rotation so that the index refers to how many CW rotation it takes to put it into the nominal pose
		binaryToDef(binary, def.desc[0]);
		ImageMiscOps.rotateCCW(binary);
		binaryToDef(binary, def.desc[1]);
		ImageMiscOps.rotateCCW(binary);
		binaryToDef(binary, def.desc[2]);
		ImageMiscOps.rotateCCW(binary);
		binaryToDef(binary, def.desc[3]);

		int index = targets.size();
		targets.add( def );
		return index;
	}

	/**
	 * Converts a binary image into the compressed bit format
	 */
	protected static void binaryToDef(GrayU8 binary , short[] desc ) {
		for (int i = 0; i < binary.data.length; i+=16) {
			int value = 0;
			for (int j = 0; j < 16; j++) {
				value |= binary.data[i+j] << j;
			}
			desc[i/16] = (short)value;
		}
	}

	@Override
	protected boolean processSquare(GrayF32 gray, Result result, double edgeInside, double edgeOutside) {

		int off = (gray.width-binary.width)/2;
		gray.subimage(off,off,off+binary.width,off+binary.width,grayNoBorder);

//		grayNoBorder.printInt();

		// compute a global threshold from the difference between the outside and inside perimeter pixel values
		float threshold = (float)((edgeInside+edgeOutside)/2.0);
		GThresholdImageOps.threshold(grayNoBorder,binary,threshold,false);

//		binary.printBinary();
		binaryToDef(binary, squareDef);

		boolean matched = false;
		int bestScore = hammingThreshold+1;
		for (int i = 0; i < targets.size(); i++) {
			FiducialDef def = targets.get(i);

			for (int j = 0; j < 4; j++) {
				int score = hamming(def.desc[j], squareDef);
				if( score < bestScore ) {
					bestScore = score;
					result.rotation = j;
					result.which = i;
					result.lengthSide = def.lengthSide;
					matched = true;
				}
			}
		}

		return matched;
	}

	/**
	 * Computes the hamming score between two descriptions.  Larger the number better the fit
	 */
	protected int hamming(short[] a, short[] b) {
		int distance = 0;
		for (int i = 0; i < a.length; i++) {
			distance += DescriptorDistance.hamming((a[i]&0xFFFF) ^ (b[i]&0xFFFF));
		}
		return distance;
	}

	public List<FiducialDef> getTargets() {
		return targets;
	}

	/**
	 * description of an image in 4 different orientations
	 */
	public static class FiducialDef
	{
		public short[][] desc = new short[4][DESC_LENGTH];
		public double lengthSide;
	}
}
