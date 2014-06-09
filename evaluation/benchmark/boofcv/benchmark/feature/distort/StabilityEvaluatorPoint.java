/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.benchmark.feature.distort;

import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.alg.distort.impl.DistortSupport;
import boofcv.benchmark.feature.BenchmarkAlgorithm;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.affine.Affine2D_F32;
import georegression.struct.affine.Affine2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.transform.affine.AffinePointOps_F64;

import java.util.ArrayList;
import java.util.List;


/**
 * Evaluates information extracted at automatically selected points inside the image.  Points
 * are selected using a feature detector.  Once the points have been selected inside the initial image they are
 * transformed to their location in the distorted image.
 *
 * @author Peter Abeles
 */
public abstract class StabilityEvaluatorPoint<T extends ImageSingleBand>
		implements StabilityEvaluator<T>
{
	protected int borderSize;
	private InterestPointDetector<T> detector;

	private List<Point2D_F64> initialPoints = new ArrayList<Point2D_F64>();
	private List<Point2D_F64> transformPoints = new ArrayList<Point2D_F64>();
	private List<Integer> transformIndexes = new ArrayList<Integer>();

	protected StabilityEvaluatorPoint(int borderSize, InterestPointDetector<T> detector) {
		this.borderSize = borderSize;
		this.detector = detector;
	}

	@Override
	public void extractInitial(BenchmarkAlgorithm alg, T image) {
		initialPoints.clear();
		detector.detect(image);

		float w = image.width-borderSize;
		float h = image.height-borderSize;

		for( int i = 0; i < detector.getNumberOfFeatures(); i++ ) {
			Point2D_F64 pt = detector.getLocation(i);
			// make sure its not too close to the image border
			if( pt.x >= borderSize && pt.y >= borderSize && pt.x < w && pt.y < h) {
				initialPoints.add(pt.copy());
			}
		}
		extractInitial(alg,image,initialPoints);
	}

	@Override
	public double[] evaluateImage(BenchmarkAlgorithm alg, T image, DistortParam param ) {

		transformPoints(param.scale, param.rotation, image.width, image.height);

		return evaluateImage(alg,image,param.scale,param.rotation,transformPoints,transformIndexes);
	}

	/**
	 * rotate points in first image into current image
	 * filter points outside of image or too close to the border
	 */
	private void transformPoints(double scale, double theta, int imageWidth, int imageHeight)
	{
		Affine2D_F64 initToImage = createTransform(scale,theta,imageWidth,imageHeight);

		transformPoints.clear();
		transformIndexes.clear();

		Point2D_F64 b = new Point2D_F64();

		float w = imageWidth-borderSize;
		float h = imageHeight-borderSize;

		for( int index = 0; index < initialPoints.size(); index++ ) {
			Point2D_F64 a = initialPoints.get(index);
			AffinePointOps_F64.transform(initToImage, a, b);
			if( b.x >= borderSize && b.y >= borderSize && b.x < w && b.y < h) {
				transformPoints.add( b.copy() );
				transformIndexes.add(index);
			}
		}

	}

	public static Affine2D_F64 createTransform( double scale , double theta , int imageWidth , int imageHeight ) {
		// these create a transform from the dst to source image
		Affine2D_F32 a = createScale((float)scale,imageWidth,imageHeight).invert(null);
		Affine2D_F32 b = DistortSupport.transformRotate(imageWidth/2,imageHeight/2,imageWidth/2,imageHeight/2,(float)theta).getModel();

		// need to invert to the transform to be from src to dst image
		Affine2D_F32 tran = a.concat(b,null).invert(null);

		Affine2D_F64 ret = new Affine2D_F64();
		ret.a11 = tran.a11;
		ret.a12 = tran.a12;
		ret.a21 = tran.a21;
		ret.a22 = tran.a22;
		ret.tx = tran.tx;
		ret.ty = tran.ty;
		return ret;
	}

	/**
	 * Scale the image about the center pixel.
	 */
	public static Affine2D_F32 createScale( float scale , int w , int h )
	{
		int sw = (int)(w*scale);
		int sh = (int)(h*scale);

		int offX = (w-sw)/2;
		int offY = (h-sh)/2;

		return new Affine2D_F32(scale,0,0,scale,offX,offY);
	}

	public abstract void extractInitial(BenchmarkAlgorithm alg, T image, List<Point2D_F64> points );

	/**
	 *
	 * @param scale change in scale from initial image to current image
	 * @param theta rotation from initial image to current image.
	 * @param points Location of interest points in current frame.
	 * @param indexes Original index of each interest point in current frame.
	 */
	public abstract double[] evaluateImage(BenchmarkAlgorithm alg,
										   T image, double scale , double theta,
										   List<Point2D_F64> points ,
										   List<Integer> indexes );

}
