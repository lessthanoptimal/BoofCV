/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.fiducial.BinaryFiducialGridSize;
import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.shapes.polygon.BinaryPolygonDetector;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;

import java.util.Arrays;


/**
 * <p>
 * Fiducial which encores a 21-bit number (0 to 2097151) using a predetermined pattern.  The inner region is broken up
 * into 5 by 5 grid of 25-squares which are either white or black.  The lower left corner is always back and
 * while all the other corners are always white.  This allows orientation to be uniquely determined.
 * <p/>
 * The above image visually shows the fiducials internal coordinate system.  The center of the fiducial is the origin
 * of the coordinate system, e.g. all sides are width/2 distance away from the origin.  +x is to the right, +y is up
 * , and +z out of the paper towards the viewer.  The black orientation corner is pointed out in the image.
 * The fiducial's width refers to the width of each side along the black border NOT the internal encoded image.
 * </p>
 *
 * @author Peter Abeles, Nathan Pahucki
 */
public class DetectFiducialSquareBinary<T extends ImageSingleBand>
		extends BaseDetectFiducialSquare<T> {

    // helper data structures for computing the value of each grid point
   	int[] counts, classified, tmp;
    private BinaryFiducialGridSize gridSize;

    // converts the input image into a binary one
   	private InputToBinary<ImageFloat32> threshold = FactoryThresholdBinary.globalOtsu(0, 255, true, ImageFloat32.class);
   	private ImageUInt8 binaryInner = new ImageUInt8(1,1);
   	// storage for no border sub-image
   	private ImageFloat32 grayNoBorder = new ImageFloat32();

   	// size of a square
   	protected final static int w=11;
   	protected final static int N=w*w;

   	// length of a side on the fiducials in world units
   	private double lengthSide = 1;

   	// ambiguity threshold. 0 to 1.  0 = very strict and 1 = anything goes
   	// Sets how strict a square must be black or white for it to be accepted.
   	// TODO: This should be private, but not changing to avoid breaking any subclasses.
    protected double ambiguityThreshold = 0.4;

    /**
     * Configures the fiducial detector, using the default 4x4 grid.
     *
     * @param inputType Type of image it's processing
     */
    public DetectFiducialSquareBinary(final InputToBinary<T> inputToBinary,
                                         final BinaryPolygonDetector<T> quadDetector, Class<T> inputType) {
        this(BinaryFiducialGridSize.FOUR_BY_FOUR, inputToBinary,quadDetector, inputType);
    }


    /**
     * Configures the fiducial detector, specifying the grid type.
     *
     * @param inputType Type of image it's processing
     */
    public DetectFiducialSquareBinary(final BinaryFiducialGridSize gridSize, final InputToBinary<T> inputToBinary,
                                         final BinaryPolygonDetector<T> quadDetector, Class<T> inputType) {
        // Borders should be 2 * w, on each side which means w * 4 for the border,
        // and w * gridSize.getWidth() for the inner part.
        super(inputToBinary,quadDetector, (w * gridSize.getWidth() + 4 * w) ,inputType);
        this.gridSize = gridSize;
        binaryInner.reshape(w * gridSize.getWidth(),w * gridSize.getWidth());
        counts = new int[gridSize.getNumberOfElements()];
        classified = new int[gridSize.getNumberOfElements()];
        tmp = new int[gridSize.getNumberOfElements()];
    }

    @Override
    protected boolean processSquare(ImageFloat32 gray, Result result) {
        int off = (gray.width - binaryInner.width) / 2;
        gray.subimage(off, off, gray.width - off, gray.width - off, grayNoBorder);

        // convert input image into binary number
        findBitCounts(grayNoBorder);

        if (thresholdBinaryNumber())
            return false;


        // adjust the orientation until the black corner is in the lower left
        if (rotateUntilInLowerCorner(result))
            return false;

        result.which = extractNumeral();
        result.lengthSide = lengthSide;

        //printClassified();
        return true;
    }

    /**
     * Extract the numerical value it encodes
     * @return the int value of the numeral.
     */
    protected int extractNumeral() {
        //
        int val = 0;
        final int topLeft = gridSize.getNumberOfElements() - gridSize.getWidth();
        int shift = 0;

        // -2 because the top and bottom rows have 2 unusable bits (the first and last)
        for(int i = 1; i < gridSize.getWidth() - 1; i++) {
            final int idx = topLeft + i;
            val |= classified[idx] << shift;
            //System.out.println("val |= classified[" + idx + "] << " + shift + ";");
            shift++;
        }

        // Don't do the first or last row, handled above and below - special cases
        for(int ii = 1; ii < gridSize.getWidth() - 1; ii++) {
            for(int i = 0; i < gridSize.getWidth(); i++) {
                final int idx = gridSize.getNumberOfElements() - (gridSize.getWidth() * (ii + 1)) + i;
                val |= classified[idx] << shift;
                //  System.out.println("val |= classified[" + idx + "] << " + shift + ";");
                shift++;
            }
        }

        // The last row
        for(int i = 1; i < gridSize.getWidth() - 1; i++) {
            val |= classified[i] << shift;
            //System.out.println("val |= classified[" + i + "] << " + shift + ";");
            shift++;
        }

        return val;
    }


    /**
     * Rotate the pattern until the black corner is in the lower right.  Sanity check to make
     * sure there is only one black corner
     */
    private boolean rotateUntilInLowerCorner(Result result) {
        // sanity check corners.  There should only be one exactly one black
        final int topLeft = gridSize.getNumberOfElements() - gridSize.getWidth();
        final int topRight = gridSize.getNumberOfElements() - 1;
        final int bottomLeft = 0;
        final int bottomRight = gridSize.getWidth() - 1;


        if (classified[bottomLeft] + classified[bottomRight] + classified[topRight] + classified[topLeft] != 1)
            return true;

        // Rotate until the black corner is in the lower left hand corner on the image.
        // remember that origin is the top left corner
        result.rotation = 0;
        while (classified[topLeft] != 1) {
            result.rotation++;
            rotateClockWise();
        }
        return false;
    }

    protected void rotateClockWise() {
        // Swap the four corners
        for (int ii = 0; ii < gridSize.getWidth(); ii++) {
            for (int i = 0; i < gridSize.getWidth(); i++) {
                final int fromIdx = ii * gridSize.getWidth() + i;
                final int toIdx = (gridSize.getNumberOfElements() - (gridSize.getWidth() * (i + 1))) + ii;
                tmp[fromIdx] = classified[toIdx];
            }
        }

        System.arraycopy(tmp, 0, classified, 0, gridSize.getNumberOfElements());
    }


    /**
     * Sees how many pixels were positive and negative in each square region.  Then decides if they
     * should be 0 or 1 or unknown
     */
    protected boolean thresholdBinaryNumber() {

        int lower = (int) (N * (ambiguityThreshold / 2.0));
        int upper = (int) (N * (1 - ambiguityThreshold / 2.0));

        for (int i = 0; i < gridSize.getNumberOfElements(); i++) {
            if (counts[i] < lower) {
                classified[i] = 0;
            } else if (counts[i] > upper) {
                classified[i] = 1;
            } else {
                // it's ambiguous so just fail
                return true;
            }
        }
        return false;
    }

    protected void findBitCounts(ImageFloat32 gray) {
        // compute binary image using an adaptive algorithm to handle shadows
        threshold.process(gray, binaryInner);

        Arrays.fill(counts, 0);
        for (int row = 0; row < gridSize.getWidth(); row++) {
            int y0 = row * binaryInner.width / gridSize.getWidth();
            int y1 = (row + 1) * binaryInner.width / gridSize.getWidth();
            for (int col = 0; col < gridSize.getWidth(); col++) {
                int x0 = col * binaryInner.width / gridSize.getWidth();
                int x1 = (col + 1) * binaryInner.width / gridSize.getWidth();

                int total = 0;
                for (int i = y0; i < y1; i++) {
                    int index = i * binaryInner.width + x0;
                    for (int j = x0; j < x1; j++) {
                        total += binaryInner.data[index++];
                    }
                }

                counts[row * gridSize.getWidth() + col] = total;
            }
        }
    }

    public void setLengthSide(final double lengthSide) {
        this.lengthSide = lengthSide;
    }

    public BinaryFiducialGridSize getGridSize() { return gridSize; }

    /**
   	 * parameters which specifies how tolerant it is of a square being ambiguous black or white.
   	 * @param ambiguityThreshold 0 to 1, insclusive
   	 */
   	public void setAmbiguityThreshold(double ambiguityThreshold) {
   		if( ambiguityThreshold < 0 || ambiguityThreshold > 1 )
   			throw new IllegalArgumentException("Must be from 0 to 1, inclusive");
   		this.ambiguityThreshold = ambiguityThreshold;
   	}

	// For troubleshooting.
	public ImageFloat32 getGrayNoBorder() { return grayNoBorder; }

	// This is only works well as a visiual representation if the output font is mono spaced.
    public void printClassified() {
        System.out.println();
        System.out.println("██████");
        for (int row = 0; row < gridSize.getWidth(); row++) {
            System.out.print("█");
            for (int col = 0; col < gridSize.getWidth(); col++) {
                System.out.print(classified[row * gridSize.getWidth() + col] == 1 ? "█︎" : "◻");
            }
            System.out.print("█");
            System.out.println();
        }
        System.out.println("██████");
    }

}

