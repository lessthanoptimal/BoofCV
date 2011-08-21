/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.feature;

import gecv.abst.detect.interest.InterestPointDetector;
import gecv.struct.image.ImageBase;
import jgrl.struct.affine.Affine2D_F32;
import jgrl.struct.point.Point2D_F32;
import jgrl.struct.point.Point2D_I32;
import jgrl.transform.affine.AffinePointOps;

import java.util.ArrayList;
import java.util.List;


/**
 * Evaluates information extracted at automatically selected points inside the image.  Points
 * are selected using a feature detector.  Once the points have been selected inside the initial image they are
 * transformed to their location in the distorted image.
 *
 * @author Peter Abeles
 */
public abstract class StabilityEvaluatorPoint<T extends ImageBase>
		implements StabilityEvaluator<T>
{
	protected int borderSize;
	private InterestPointDetector<T> detector;

	private List<Point2D_I32> initialPoints = new ArrayList<Point2D_I32>();
	private List<Point2D_I32> transformPoints = new ArrayList<Point2D_I32>();
	private List<Integer> transformIndexes = new ArrayList<Integer>();

	protected StabilityEvaluatorPoint(int borderSize, InterestPointDetector<T> detector) {
		this.borderSize = borderSize;
		this.detector = detector;
	}

	@Override
	public void extractInitial(StabilityAlgorithm alg, T image) {
		initialPoints.clear();
		detector.detect(image);

		float w = image.width-borderSize;
		float h = image.height-borderSize;

		for( int i = 0; i < detector.getNumberOfFeatures(); i++ ) {
			Point2D_I32 pt = detector.getLocation(i);
			// make sure its not too close to the image border
			if( pt.x >= borderSize && pt.y >= borderSize && pt.x < w && pt.y < h) {
				initialPoints.add(pt.copy());
			}
		}
		extractInitial(alg,image,initialPoints);
	}

	@Override
	public double[] evaluateImage(StabilityAlgorithm alg, T image, Affine2D_F32 initToImage ) {
		if( initToImage == null ) {
			return evaluateImage(alg,image,initToImage,initialPoints,null);
		} else {
			transform(initToImage,image);

			return evaluateImage(alg,image,initToImage,transformPoints,transformIndexes);
		}
	}

	/**
	 * rotate points in first image into current image
	 * filter points outside of image or too close to the border
	 */
	private void transform( Affine2D_F32 initToImage , T image )
	{
		transformPoints.clear();
		transformIndexes.clear();

		Point2D_F32 a = new Point2D_F32();
		Point2D_F32 b = new Point2D_F32();

		float w = image.width-borderSize;
		float h = image.height-borderSize;

		for( int index = 0; index < initialPoints.size(); index++ ) {
			Point2D_I32 i = initialPoints.get(index);
			a.x = i.x;
			a.y = i.y;
			AffinePointOps.transform(initToImage,a,b);
			if( b.x >= borderSize && b.y >= borderSize && b.x < w && b.y < h) {
				transformPoints.add( new Point2D_I32((int)b.x,(int)b.y));
				transformIndexes.add(index);
			}
		}

	}

	public abstract void extractInitial(StabilityAlgorithm alg,
										T image, List<Point2D_I32> points );

	public abstract double[] evaluateImage(StabilityAlgorithm alg,
										   T image, Affine2D_F32 initToImage,
										   List<Point2D_I32> points ,
										   List<Integer> indexes );

}
