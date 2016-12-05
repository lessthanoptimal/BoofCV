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

package boofcv.alg.distort;

import boofcv.abst.distort.FDistort;
import boofcv.abst.geo.Estimate1ofEpipolar;
import boofcv.alg.interpolate.InterpolationType;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F64;
import org.ejml.data.DenseMatrix64F;

import java.util.ArrayList;

/**
 * Class which simplifies the removal of perspective distortion from a region inside an image.  Given the ordered
 * corners of a quadrilateral in the input image it applies a homography transform which reprojects the region
 * into the input image into a rectangular output image.
 *
 * @author Peter Abeles
 */
public class RemovePerspectiveDistortion<T extends ImageBase> {
	FDistort distort;

	// computes the homography
	Estimate1ofEpipolar computeHomography = FactoryMultiView.computeHomography(true);

//	RefineEpipolar refineHomography = FactoryMultiView.refineHomography(1e-8,20, EpipolarError.SIMPLE);

	// storage for computed homography
	DenseMatrix64F H = new DenseMatrix64F(3,3);
//	DenseMatrix64F Hrefined = new DenseMatrix64F(3,3);
	// transform which applies the homography
	PointTransformHomography_F32 homography = new PointTransformHomography_F32();

	// storage for associated points between the two images
	ArrayList<AssociatedPair> associatedPairs = new ArrayList<>();

	// input and output images
	T output;

	/**
	 * Constructor which specifies the characteristics of the undistorted image
	 *
	 * @param width Width of undistorted image
	 * @param height Height of undistorted image
	 * @param imageType Type of undistorted image
	 */
	public RemovePerspectiveDistortion( int width , int height , ImageType<T> imageType ) {
		output = imageType.createImage(width,height);
		distort = new FDistort(imageType);
		distort.output(output);

		distort.interp(InterpolationType.BILINEAR).transform(homography);

		for (int i = 0; i < 4; i++) {
			associatedPairs.add( new AssociatedPair());
		}

		associatedPairs.get(0).p1.set(0,0);
		associatedPairs.get(1).p1.set(output.width-1,0);
		associatedPairs.get(2).p1.set(output.width-1,output.height-1);
		associatedPairs.get(3).p1.set(0,output.height-1);
	}

	/**
	 * Applies distortion removal to the specified region in the input image.  The undistorted image is returned.
	 * @param input Input image
	 * @param corner0 Top left corner
	 * @param corner1 Top right corner
	 * @param corner2 Bottom right corner
	 * @param corner3 Bottom left corner
	 * @return true if successful or false if it failed
	 */
	public boolean apply( T input ,
					Point2D_F64 corner0 , Point2D_F64 corner1 ,
					Point2D_F64 corner2 , Point2D_F64 corner3 )
	{
		associatedPairs.get(0).p2.set(corner0);
		associatedPairs.get(1).p2.set(corner1);
		associatedPairs.get(2).p2.set(corner2);
		associatedPairs.get(3).p2.set(corner3);

		if( !computeHomography.process(associatedPairs, H) )
			return false;

//		if( !refineHomography.fitModel(associatedPairs,H,Hrefined) ) {
//			return false;
//		}
//		homography.set(Hrefined);

		homography.set(H);
		distort.input(input).apply();

		return true;
	}

	/**
	 * Returns the undistorted output image
	 */
	public T getOutput() {
		return output;
	}
}
