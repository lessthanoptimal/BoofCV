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

import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.PointToPixelTransform_F32;
import boofcv.alg.distort.PointTransformHomography_F32;
import boofcv.alg.feature.shapes.SplitMergeLineFitLoop;
import boofcv.alg.filter.binary.Contour;
import boofcv.alg.filter.binary.LinearContourLabelChang2004;
import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.alg.geo.h.HomographyLinear4;
import boofcv.core.image.border.BorderType;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.factory.distort.FactoryDistort;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.ConnectRule;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.distort.PixelTransform_F32;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.homography.UtilHomography;
import georegression.struct.se.Se3_F64;
import georegression.struct.shapes.Quadrilateral_F64;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;
import org.ejml.data.DenseMatrix64F;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
// TODO allow for different binary strategies to be used for speed reasons
// TODO create a tracking algorithm which uses previous frame information for speed + stability
public abstract class DetectFiducialSquarePattern {

	double squareWidth;
	double borderWidth;

	// minimum size of a shape's contour
	int minimumContour = 100;

	ImageUInt8 binary = new ImageUInt8(1,1);
	ImageUInt8 temp0 = new ImageUInt8(1,1);
	ImageUInt8 temp1 = new ImageUInt8(1,1);

	LinearContourLabelChang2004 contourFinder = new LinearContourLabelChang2004(ConnectRule.FOUR);
	ImageSInt32 labeled = new ImageSInt32(1,1);

	SplitMergeLineFitLoop fitPolygon;

	FastQueue<Quadrilateral_F64> candidates = new FastQueue<Quadrilateral_F64>(Quadrilateral_F64.class,true);

	protected ImageFloat32 square;

	HomographyLinear4 computeHomography = new HomographyLinear4(true);
	DenseMatrix64F H = new DenseMatrix64F(3,3);
	List<AssociatedPair> pairs = new ArrayList<AssociatedPair>();
	ImageDistort<ImageUInt8,ImageFloat32> removePerspective;
	PointTransformHomography_F32 transformHomography = new PointTransformHomography_F32();

	FitQuadrilaterialEM fitQuad = new FitQuadrilaterialEM();

	IntrinsicParameters intrinsic;

	protected DetectFiducialSquarePattern( SplitMergeLineFitLoop fitPolygon , int squarePixels ) {

		this.square = new ImageFloat32(squarePixels,squarePixels);

		this.fitPolygon = fitPolygon;

		for (int i = 0; i < 4; i++) {
			pairs.add( new AssociatedPair());
		}

		removePerspective = FactoryDistort.distort(FactoryInterpolation.bilinearPixelS(ImageUInt8.class),
				FactoryImageBorder.general(ImageUInt8.class, BorderType.EXTENDED),ImageFloat32.class);
		PixelTransform_F32 squareToInput= new PointToPixelTransform_F32(transformHomography);
		removePerspective.setModel(squareToInput);
	}

	/**
	 * Specifies the image's intrinsic parameters
	 * @param intrinsic Intrinsic parameters for the distortion free input image
	 */
	public void setIntrinsic(IntrinsicParameters intrinsic) {
		this.intrinsic = intrinsic;

		binary.reshape(intrinsic.width,intrinsic.height);
		temp0.reshape(intrinsic.width,intrinsic.height);
		temp1.reshape(intrinsic.width,intrinsic.height);
		labeled.reshape(intrinsic.width,intrinsic.height);
	}

	/**
	 *
	 * @param gray Input image with lens distortion removed
	 */
	public void process( ImageUInt8 gray ) {

		candidates.reset();

		// convert image into a binary image using adaptive thresholding
		ThresholdImageOps.threshold(gray,binary,200,true);
//		ThresholdImageOps.adaptiveSquare(gray,binary,3,-5,true,temp0,temp1);

//		binary.printNotZero();

		// TODO filter binary?

		// Find quadrilaterials that could be fiducials
		findCandidateShapes();

		// undistort the squares
		for (int i = 0; i < candidates.size; i++) {
			// compute the homography from the input image to an undistorted square image
			Quadrilateral_F64 q = candidates.get(i);

			pairs.get(0).set( 0              ,    0            , q.a.x , q.a.y);
			pairs.get(1).set( square.width-1 ,    0            , q.b.x , q.b.y );
			pairs.get(2).set( square.width-1 , square.height-1 , q.c.x , q.c.y );
			pairs.get(3).set( 0              , square.height-1 , q.d.x , q.d.y );

			computeHomography.process(pairs,H);
			// pass the found homography onto the image transform
			UtilHomography.convert(H,transformHomography.getModel());
			// remove the perspective distortion and process it
			removePerspective.apply(gray,square);
			processSquare(square,q);
		}
	}

	private void findCandidateShapes() {
		// find binary blobs
		contourFinder.process(binary,labeled);

		// find blobs where all 4 edges are lines
		FastQueue<Contour> blobs = contourFinder.getContours();
		for (int i = 0; i < blobs.size; i++) {
			Contour c = blobs.get(i);

			// can't be entirely black
			if( c.internal.isEmpty() )
				continue;

			System.out.println("Contour size "+c.external.size());

			// todo use internal contour?
			if( c.external.size() >= minimumContour) {
				fitPolygon.process(c.external);
				GrowQueue_I32 splits = fitPolygon.getSplits();
				System.out.println("  splits "+splits.size);
				if( splits.size <= 8 ) {
					Quadrilateral_F64 q = candidates.grow();
					fitQuad.fit(c.external,splits,q);
					// todo check quality of the fit here some how
					candidates.removeTail();

					// TODO filter extreme shapes here
				}
			}
		}
	}

	public void computeTargetToWorld( Quadrilateral_F64 quad , Se3_F64 targetToWorld ) {

	}

	public abstract void processSquare( ImageFloat32 square , Quadrilateral_F64 where );
}
