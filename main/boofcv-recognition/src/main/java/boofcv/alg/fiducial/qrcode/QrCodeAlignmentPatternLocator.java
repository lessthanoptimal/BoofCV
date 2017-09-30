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

import boofcv.abst.geo.Estimate1ofEpipolar;
import boofcv.alg.distort.PointTransformHomography_F32;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_F32;
import georegression.struct.shapes.Polygon2D_F64;
import org.ddogleg.struct.FastQueue;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.FMatrixRMaj;
import org.ejml.dense.row.misc.UnrolledInverseFromMinor_FDRM;
import org.ejml.ops.ConvertMatrixData;

import java.util.ArrayList;

/**
 * Searches the image for alignment patterns. First it computes a transform that removes perspective distortion
 * using previously detected position patterns. Then it searches inside the image for the position patterns. If
 * available, adjacent position patterns are used to adjust the search so that locations distant from position
 * patterns can be compensated for.
 *
 * @author Peter Abeles
 */
public class QrCodeAlignmentPatternLocator<T extends ImageGray<T>> {
	QrCodePatternLocations patternLocations = new QrCodePatternLocations();

	InterpolatePixelS<T> interpolate;

	// grid for quick look up of alignment patterns to adjust search
	FastQueue<QrCode.Alignment> lookup = new FastQueue<>(QrCode.Alignment.class,true);

	// Transform from undistorted to image coordinates
	// Grid is the QR coordinate system where a grid element is a qr's square module
	PointTransformHomography_F32 gridToImage = new PointTransformHomography_F32();
	PointTransformHomography_F32 imageToGrid = new PointTransformHomography_F32();

	// used for homography calculation
	Estimate1ofEpipolar computeHomography = FactoryMultiView.computeHomography(true);
	ArrayList<AssociatedPair> associatedPairs = new ArrayList<>();
	DMatrixRMaj H = new DMatrixRMaj(3,3);
	FMatrixRMaj H32 = new FMatrixRMaj(3,3);
	FMatrixRMaj H_inv = new FMatrixRMaj(3,3);

	// pixel value storage used when localizing
	float arrayX[] = new float[11];
	float arrayY[] = new float[11];

	// more work space
	Point2D_F32 p32 = new Point2D_F32();

	public QrCodeAlignmentPatternLocator( Class<T> imageType ) {

		// use nearest neighbor to avoid shifting the location
		interpolate = FactoryInterpolation.nearestNeighborPixelS(imageType);

		for (int i = 0; i < 5; i++) {
			associatedPairs.add( new AssociatedPair() );
		}
	}

	/**
	 * Uses the previously detected position patterns to seed the search for the alignment patterns
	 */
	public boolean process(T image , QrCode qr ) {
		if( qr.version <= 1 )
			return true;

		interpolate.setImage(image);

		if( !computeHomography(qr))
			return false;

		initializePatterns(qr);

		return localizePositionPatterns(patternLocations.alignment[qr.version]);
	}

	private boolean localizePositionPatterns(int[] alignmentLocations) {
		int size = alignmentLocations.length;

		for (int row = 0; row < size; row++) {
			for (int col = 0; col < size; col++) {
				QrCode.Alignment a = lookup.get(row*size+col);
				if( a == null )
					continue;

				// adjustment from previously found alignment patterns
				double adjY=0,adjX=0;

				if( row > 0) {
					QrCode.Alignment p = lookup.get((row - 1) * size + col);
					adjY = p.moduleY+0.5-p.moduleFound.y;
				}
				if( col > 0 ) {
					QrCode.Alignment p = lookup.get((row - 1) * size + col);
					adjX = p.moduleX+0.5-p.moduleFound.x;
				}

				if( !localize(a,(float)(a.moduleX+0.5+adjX),(float)(a.moduleY+0.5+adjY)) )
					return false;
			}
		}
		return true;
	}

	/**
	 * Localizizes the alignment pattern crudely by searching for the black box in the center by looking
	 * for its edges in the gray scale image
	 *
	 * @return true if success or false if it doesn't resemble an alignment pattern
	 */
	protected boolean localize(QrCode.Alignment pattern , float guessX , float guessY)
	{
		// sample along the middle. Try to not sample the outside edges which could confuse it
		for (int i = 0; i < arrayY.length; i++) {
			float x = guessX - 1.25f + i*2.5f/10.0f;
			float y = guessY - 1.25f + i*2.5f/10.0f;
			arrayX[i] = interpolate.get(x,guessY);
			arrayY[i] = interpolate.get(guessX,y);
		}

		int downX = greatestDown(arrayX);
		int upX = greatestUp(arrayX,downX);

		if( downX == -1 || upX == -1)
			return false;

		int downY = greatestDown(arrayY);
		int upY = greatestUp(arrayY,downY);

		if( downY == -1 || upY == -1)
			return false;

		pattern.moduleFound.x = guessX - 1.25f + (downX+upX)*2.5f/20.0f;
		pattern.moduleFound.y = guessY - 1.25f + (downY+upY)*2.5f/20.0f;

		gridToImage.compute((float)pattern.moduleFound.x,(float)pattern.moduleFound.y,p32);
		pattern.pixel.set(p32.x,p32.y);

		return true;
	}

	/**
	 * Searches for the greatest down slope in the list
	 */
	static int greatestDown( float array[] ) {
		int best = -1;
		float bestScore = 0;

		for (int i = 1; i < array.length; i++) {
			float diff = array[i]-array[i-1];
			if( diff < bestScore) {
				bestScore = diff;
				best = i;
			}
		}
		return best;
	}

	static int greatestUp( float array[] , int start) {
		int best = -1;
		float bestScore = 0;

		for (int i = start; i < array.length; i++) {
			float diff = array[i]-array[i-1];
			if( diff > bestScore) {
				bestScore = diff;
				best = i;
			}
		}
		return best;
	}

	/**
	 * Creates a list of alignment patterns to look for and their grid coordinates
	 */
	private void initializePatterns(QrCode qr) {
		int where[] = patternLocations.alignment[qr.version];
		qr.alignment.reset();
		for (int row = 0; row < where.length; row++) {
			for (int col = 0; col < where.length; col++) {
				boolean skip = false;
				if( row == 0 & col == 0 )
					skip = true;
				else if( row == where.length-1 & col == 0)
					skip = true;
				else if( row == where.length-1 & col == where.length-1)
					skip = true;

				if( skip ) {
					lookup.add(null);
				} else {
					QrCode.Alignment a = qr.alignment.grow();
					a.moduleX = col;
					a.moduleY = row;
					lookup.add(a);
				}
			}
		}
	}

	/**
	 * Computes an approximate homography the initialize the search. Avoid corner features since they
	 * are more likely to be damaged. Outside features are also likely to be damaged fewer of them are sampled
	 *
	 * @param qr The QR code with detected position patterns
	 */
	private boolean computeHomography( QrCode qr ) {
		int gridSize = patternLocations.size[qr.version];

		// features from corner
		associatedPairs.get(0).p1.set(7,0);
		associatedPairs.get(1).p1.set(7,7);
		associatedPairs.get(2).p1.set(0,7);

		// features from right
		associatedPairs.get(3).p1.set(gridSize-7,0);
		associatedPairs.get(4).p1.set(gridSize-7,7);

		// features from bottom
		associatedPairs.get(5).p1.set(0,gridSize-7);
		associatedPairs.get(6).p1.set(7,gridSize-7);

		// apply the actual pixel coordinates
		set(0,qr.ppCorner,1);
		set(1,qr.ppCorner,2);
		set(2,qr.ppCorner,3);

		set(3,qr.ppRight,0);
		set(4,qr.ppRight,3);

		set(5,qr.ppDown,0);
		set(6,qr.ppDown,1);

		// Compute the homography
		if( !computeHomography.process(associatedPairs, H) )
			return false;

		ConvertMatrixData.convert(H,H32);
		UnrolledInverseFromMinor_FDRM.inv(H32,H_inv);

		gridToImage.set(H32);
		imageToGrid.set(H_inv);

		return true;
	}

	private void set(int pairIndex , Polygon2D_F64 square , int squareIndex ) {
		associatedPairs.get(pairIndex).p2.set(square.get(squareIndex));
	}
}
