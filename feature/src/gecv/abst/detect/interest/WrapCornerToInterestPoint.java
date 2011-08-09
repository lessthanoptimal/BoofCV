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

package gecv.abst.detect.interest;

import gecv.abst.detect.corner.GeneralFeatureDetector;
import gecv.abst.filter.derivative.ImageGradient;
import gecv.abst.filter.derivative.ImageHessian;
import gecv.core.image.ImageGenerator;
import gecv.struct.QueueCorner;
import gecv.struct.image.ImageBase;
import jgrl.struct.point.Point2D_I16;
import jgrl.struct.point.Point2D_I32;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper around {@link gecv.abst.detect.corner.GeneralFeatureDetector} to make it compatible with {@link InterestPointDetector}.
 *
 * @author Peter Abeles
 */
public class WrapCornerToInterestPoint< T extends ImageBase, D extends ImageBase> implements InterestPointDetector<T>
{

	ImageGenerator<D> derivativeGenerator;
	GeneralFeatureDetector<T,D> detector;
	ImageGradient<T,D> gradient;
	ImageHessian<D> hessian;

	// true if the data 
	boolean declaredDerivatives = false;
	D derivX;
	D derivY;
	D derivXX;
	D derivYY;
	D derivXY;

	List<Point2D_I32> foundPoints;

	public WrapCornerToInterestPoint(GeneralFeatureDetector<T, D> detector,
									 ImageGradient<T,D> gradient ,
									 ImageHessian<D> hessian ,
									 ImageGenerator<D> derivativeGenerator ) {
		this.detector = detector;
		this.gradient = gradient;
		this.hessian = hessian;
		this.derivativeGenerator = derivativeGenerator;
	}

	@Override
	public void detect(T input) {

		if( !declaredDerivatives ) {
			declaredDerivatives = true;
			if( detector.getRequiresGradient() || detector.getRequiresHessian() ) {
				derivX = derivativeGenerator.createInstance(input.width,input.height);
				derivY = derivativeGenerator.createInstance(input.width,input.height);
			}
			if( detector.getRequiresHessian() ) {
				derivXX = derivativeGenerator.createInstance(input.width,input.height);
				derivYY = derivativeGenerator.createInstance(input.width,input.height);
				derivXY = derivativeGenerator.createInstance(input.width,input.height);
			}
		}

		if( detector.getRequiresGradient() || detector.getRequiresHessian() )
			gradient.process(input,derivX,derivY);
		if( detector.getRequiresHessian() )
			hessian.process(derivX,derivY,derivXX,derivYY,derivXY);

		detector.process(input,derivX,derivY,derivXX,derivYY,derivXY);

		QueueCorner corners = detector.getFeatures();

		foundPoints = new ArrayList<Point2D_I32>();
		for( int i = 0; i < corners.num; i++ ) {
			Point2D_I16 p = corners.get(i);
			foundPoints.add( new Point2D_I32(p.x,p.y));
		}
	}

	@Override
	public int getNumberOfFeatures() {
		return foundPoints.size();
	}

	@Override
	public Point2D_I32 getLocation(int featureIndex) {
		return foundPoints.get(featureIndex);
	}

	@Override
	public double getScale(int featureIndex) {
		throw new IllegalArgumentException("Not supported");
	}

	@Override
	public double getOrientation(int featureIndex) {
		throw new IllegalArgumentException("Not supported");
	}

	@Override
	public boolean hasScale() {
		return false;
	}

	@Override
	public boolean hasOrientation() {
		return false;
	}
}
