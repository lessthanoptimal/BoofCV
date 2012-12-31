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

package boofcv.factory.filter.kernel;

import boofcv.alg.filter.kernel.SteerableCoefficients;

import static java.lang.Math.*;


/**
 * <p>
 * Coefficients for common steerable bases.
 * </p>
 *
 * <p>
 * William T. Freeman and Edward H. Adelson, "The Design and Use of Steerable Filters", IEEE Trans. Patt. Anal.
 * and Machine Intell., Vol. 13, No. 9, Sept. 1991
 * </p>
 *
 * @author Peter Abeles
 */
public class FactorySteerCoefficients {

	/**
	 * Coefficients for even or odd parity polynomials.
	 *
	 * @param order order of the polynomial.
	 * @return Steering coeficient.
	 */
	public static SteerableCoefficients polynomial( int order ) {
		if( order == 1 )
			return new PolyOrder1();
		else if( order == 2 )
			return new PolyOrder2();
		else if( order == 3 )
			return new PolyOrder3();
		else if( order == 4 )
			return new PolyOrder4();
		else
			throw new IllegalArgumentException("Only supports orders 1 to 4");
	}

	/**
	 * Coefficients for even or odd parity separable polynomials.
	 *
	 * @param order order of the polynomial.
	 * @return Steering coeficient.
	 */
	public static SteerableCoefficients separable( int order ) {
		return new Separable(order);
	}

	// forumulas for steering even or odd parity polynomials
	public static class PolyOrder1 implements SteerableCoefficients
	{
		@Override
		public double compute(double angle, int basis) {
			if( basis == 0 ) {
				return Math.cos(angle);
			} else {
				return Math.sin(angle);
			}
		}
	}

	public static class PolyOrder2 implements SteerableCoefficients
	{
		@Override
		public double compute(double angle, int basis) {
			angle -= basis*Math.PI/3.0;

			return (1.0/3.0)*(1.0 + 2.0*Math.cos(2*angle));
		}
	}

	public static class PolyOrder3 implements SteerableCoefficients
	{
		@Override
		public double compute(double angle, int basis) {
			angle -= basis*Math.PI/4.0;

			return (1.0/4.0)*(2.0*Math.cos(angle)+2.0*Math.cos(3.0*angle));
		}
	}

	public static class PolyOrder4 implements SteerableCoefficients
	{
		@Override
		public double compute(double angle, int basis) {
			angle -= basis*Math.PI/5.0;

			return (1.0/5.0)*(1+2.0*Math.cos(2.0*angle)+2.0*Math.cos(4.0*angle));
		}
	}

	public static class Separable implements SteerableCoefficients
	{
		final int order;

		public Separable(int order) {
			this.order = order;
		}

		@Override
		public double compute(double angle, int basis) {
			int powerC = order-basis;

			int middleIndex = Math.min(basis,order-basis);

			float middle = 1;
			if( middleIndex > 0 ) {
				middle = order;
				int inc = order;
				for( int i = 1; i < middleIndex; i++ ) {
					inc -= 2;
					middle += inc;
				}
			}
			middle *= Math.pow(-1,basis);

//			if( order == 2 ) {
//				if( basis == 0 )
//					return cos(angle);
//				else if( basis == 1 )
//					return sin(angle);
//				else
//					return sin(angle);
//			}

//			System.out.println("order "+order+" basis "+basis+" middle "+middle);
			return middle*pow(cos(angle),powerC)*pow(sin(angle),basis);
		}
	}
}
