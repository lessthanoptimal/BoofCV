/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.abst.filter.derivative;

import boofcv.abst.filter.FilterImageInterface;
import boofcv.core.image.border.BorderType;
import boofcv.struct.image.ImageBase;


/**
 * Computes the hessian numerically using filters for computing X and Y derivatives.
 *
 * @author Peter Abeles
 */
public class ImageHessian_Filter<D extends ImageBase >
		implements ImageHessian<D> {

	// default border types.
	// These have been selected to maximize visual appearance while sacrificing some theoretical properties
	private BorderType borderDeriv = BorderType.EXTENDED;

	// filters for computing image derivatives
	private FilterImageInterface<D, D> derivX;
	private FilterImageInterface<D, D> derivY;

	Class<D> derivType;

	public ImageHessian_Filter( FilterImageInterface<D, D> derivX ,
								FilterImageInterface<D, D> derivY ,
								Class<D> derivType ) {
		this.derivX = derivX;
		this.derivY = derivY;
		this.derivType = derivType;
	}

	@Override
	public void process(D inputDerivX, D inputDerivY, D derivXX, D derivYY, D derivXY)
	{
		derivX.process(inputDerivX,derivXX);
		derivY.process(inputDerivY,derivYY);
		// todo this is probably incorrect.
		// It is the equivalent of
		// (deriv Blur X) (deriv blurY)(blur X) -> (deriv X)(deriv Y)(blur X)(blur X)(blur Y)
		// there is an extra blur in there
		derivY.process(inputDerivX,derivXY);
	}

	public BorderType getBorderDeriv() {
		return borderDeriv;
	}

	public void setBorderDeriv(BorderType borderDeriv) {
		this.borderDeriv = borderDeriv;
	}

	@Override
	public void setBorderType(BorderType type) {
		this.borderDeriv = type;
	}

	@Override
	public BorderType getBorderType() {
		return borderDeriv;
	}

	@Override
	public int getBorder() {
		int ret = Math.max(derivX.getHorizontalBorder(),derivY.getHorizontalBorder());
		ret = Math.max(ret,derivX.getVerticalBorder());
		ret = Math.max(ret,derivY.getVerticalBorder());
		return ret;
	}

	@Override
	public Class<D> getDerivType() {
		return derivType;
	}

}