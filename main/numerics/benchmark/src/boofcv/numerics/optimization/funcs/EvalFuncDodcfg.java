/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.numerics.optimization.funcs;

import boofcv.numerics.optimization.functions.FunctionNtoN;
import boofcv.numerics.optimization.functions.FunctionNtoS;

/**
 * Test function for large scale unconstrained minimization.
 *
 *<pre>
 *     This subroutine computes the function and gradient of the
 *     optimal design with composite materials problem.
 *
 *     MINPACK-2 Project. November 1993.
 *     Argonne National Laboratory and University of Minnesota.
 *     Brett M. Averick.
 * </pre>
 *
 * @author Peter Abeles
 */
public class EvalFuncDodcfg implements EvalFuncMinimization {

	int nx; // number of grid points in the first coordinate direction.
	int ny; // number of grid points in the second coordinate direction.
	double lambda;

	double hx,hy,hxhy,area;

	double t1,t2;

	double p25=0.25;
	double p5=0.5;
	double zero = 0;
	double one = 1.0;
	double two = 2.0;

	double mu1=one,mu2=two;

	public EvalFuncDodcfg(int nx, int ny , double lambda) {
		this.nx = nx;
		this.ny = ny;
		this.lambda = lambda;

		hx = 1/(double)(nx+1);
		hy = 1/(double)(ny+1);
		hxhy = hx*hy;
		area = p5*hxhy;

		// Compute the break points.

		t1 = Math.sqrt(two*lambda*mu1/mu2);
		t2 = Math.sqrt(two*lambda*mu2/mu1);
	}

	@Override
	public double getMinimum() {
		return -1e30;
	}

	@Override
	public FunctionNtoS getFunction() {
		return new Func();
	}

	@Override
	public FunctionNtoN getGradient() {
		return new Deriv();
	}

	@Override
	public double[] getInitial() {
		double x[] = new double[nx*ny];
		
		for( int j = 1; j <= ny; j++) {
			double temp = Math.min(j,ny-j+1)*hy;
			for( int i = 1; i <= nx; i++ ) {
				int k = nx*(j-1) + i;
				double a = Math.min(Math.min(i,nx-i+1)*hx,temp);
				x[fi(k)] = -a*a;
			}
		}

		return x;
	}

	@Override
	public double[] getOptimal() {
		return null;
	}

	public class Func implements FunctionNtoS
	{
		@Override
		public int getN() {
			return nx*ny;
		}

		@Override
		public double process(double[] input ) {
			return computeStuff(true,false,input,null);
		}
	}

	public class Deriv implements FunctionNtoN
	{
		@Override
		public int getN() {
			return nx*ny;
		}

		@Override
		public void process(double[] input, double[] output) {
			computeStuff(false,true,input,output);
		}
	}

	public double computeStuff( boolean feval , boolean geval, double x[], double fgrad[] ) {
//c     Evaluate the function if task = 'F', the gradient if task = 'G',
//c     or both if task = 'FG'.
		double f = 0;
		
		if (geval) {
			for( int k = 1; k <= nx*ny; k++ ) {
				fgrad[k-1] = zero;  
			}
		}

//c     Computation of the function and the gradient over the lower
//c     triangular elements.
		for (int j = 0; j <= ny; j++ ) {
			for( int i = 0; i <= nx; i++ ) {
				int k = nx*(j-1) + i;
				double v = zero;
				double vr = zero;
				double vt = zero;
				if (j >= 1 && i >= 1) v = x[fi(k)];
				if (i < nx && j > 0) vr = x[fi(k+1)];
				if (i > 0 && j < ny) vt = x[fi(k+nx)];
				double dvdx = (vr-v)/hx;
				double dvdy = (vt-v)/hy;
				double gradv = dvdx*dvdx + dvdy*dvdy;
				if (feval) {

					double dpsi = dodcps(gradv,mu1,mu2,t1,t2,0,lambda);
					f = f + dpsi;
//					System.out.printf("%3d  %3d  f = %8.3e  psi = %8.3e  gradv = %8.3e\n",j,i,f,dpsi,gradv);
				}
				if (geval) {
					double dpsip = dodcps(gradv,mu1,mu2,t1,t2,1,lambda);
					if (i >= 1 && j >= 1)
						fgrad[fi(k)] -= two*(dvdx/hx+dvdy/hy)*dpsip;
					if (i < nx && j > 0)
						fgrad[fi(k+1)] += two*(dvdx/hx)*dpsip;
					if (i > 0 && j < ny)
						fgrad[fi(k+nx)] += two*(dvdy/hy)*dpsip;
				}
			}
		}

//c     Computation of the function and the gradient over the upper
//c     triangular elements.

      for(int j = 1; j <= ny + 1; j++ ) {
         for(int i = 1; i <= nx + 1; i++ ) {
            int k = nx*(j-1) + i;
            double vb = zero;
			 double vl = zero;
			 double v = zero;
            if (i <= nx && j > 1) vb = x[fi(k-nx)];
            if (i > 1 && j <= ny) vl = x[fi(k-1)];
            if (i <= nx && j <= ny) v = x[fi(k)];
            double dvdx = (v-vl)/hx;
			 double dvdy = (v-vb)/hy;
			 double gradv = dvdx*dvdx + dvdy*dvdy;
			 if (feval) {
				 double dpsi = dodcps(gradv,mu1,mu2,t1,t2,0,lambda);
				 f = f + dpsi;
			 }
			 if (geval) {
				 double dpsip = dodcps(gradv,mu1,mu2,t1,t2,1,lambda);
				 if (i <= nx && j > 1)
					 fgrad[fi(k-nx)] -= two*(dvdy/hy)*dpsip;
				 if (i > 1 && j <= ny)
					 fgrad[fi(k-1)] -= two*(dvdx/hx)*dpsip;
				 if (i <= nx && j <= ny)
					 fgrad[fi(k)] += two*(dvdx/hx+dvdy/hy)*dpsip;
			 }
		 }
	  }

//c     Scale the function.

		if (feval) f = area*f;

//c     Integrate v over the domain.

		if (feval) {
			double temp = zero;
			for( int k = 1; k <= nx*ny; k++ ) {
				temp = temp + x[fi(k)];
			}
			f = f + hxhy*temp;
		}
		if (geval) {
			for( int k = 1; k <= nx*ny; k++ ) {
				fgrad[fi(k)] = area*fgrad[fi(k)] + hxhy;
			}
		}
		
		return f;
	}

	/**
	 *     This subroutine computes the function psi(t) and the scaled
	 *     functions psi'(t)/t and psi''(t)/t for the optimal design
	 *     with composite materials problem.
	 * @param t
	 * @param mu1
	 * @param mu2
	 * @param t1
	 * @param t2
	 * @param option
	 * @param lambda
	 * @return
	 */
	public double dodcps(double t,double mu1,double mu2,
						 double t1,double t2,
						 int option,double lambda)
	{
		double sqrtt;
		double result = Double.NaN;

		sqrtt = Math.sqrt(t);

		if (option == 0) {
			if (sqrtt <= t1)
				result = p5*mu2*t;
			else if (sqrtt > t1 && sqrtt < t2)
				result = mu2*t1*sqrtt - lambda*mu1;
			else if (sqrtt >= t2)
				result = p5*mu1*t + lambda*(mu2-mu1);
		} else if (option == 1) {
			if (sqrtt <= t1)
				result = p5*mu2;
			else if (sqrtt > t1 && sqrtt < t2)
				result = p5*mu2*t1/sqrtt;
			else if (sqrtt >= t2)
				result = p5*mu1;
		} else if (option == 2) {
			if (sqrtt <= t1)
				result = zero;
			else if (sqrtt > t1 && sqrtt < t2)
				result = -p25*mu2*t1/(sqrtt*t);
			else if (sqrtt >= t2)
				result = zero;
		}
		if( Double.isNaN(result)) {
			throw new RuntimeException("Bad");
		}

		return result;
	}
	
	private int fi( int i ) {
		return i-1;
	}
}
