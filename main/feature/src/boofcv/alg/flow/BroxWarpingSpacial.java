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

package boofcv.alg.flow;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.abst.filter.derivative.ImageHessian;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.misc.PixelMath;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import boofcv.struct.pyramid.ImagePyramid;

import java.util.Arrays;

/**
 * <p>
 * Dense optical flow which adheres to a brightness constancy assumption, a gradient constancy
 * assumption, and a discontinuity-preserving spatio-temporal smoothness constraint.  Based on the
 * work of Brox [2] with implementation details taken from [1].
 * </p>
 *
 * <ol>
 * <li>
 * Javier Sánchez Pérez, Nelson Monzón López, and Agustín Salgado de la Nuez, Robust Optical Flow Estimation,
 * Image Processing On Line, 3 (2013), pp. 252–270. http://dx.doi.org/10.5201/ipol.2013.21</li>
 * <li>
 * Thomas Brox, Andr´es Bruhn, Nils Papenberg, and Joachim Weickert. High accuracy optical
 * ﬂow estimation based on a theory for warping. In T. Pajdla and J. Matas, editors, European
 * Conference on Computer Vision (ECCV), volume 3024 of Lecture Notes in Computer Science,
 * pages 25–36, Prague, Czech Republic, May 2004. Springer.
 * </li>
 * </ol>
 *
 * @author Peter Abeles
 */
public class BroxWarpingSpacial<T extends ImageGray> extends DenseFlowPyramidBase<T> {

	// regularization term
	private static final double EPSILON = 0.001;

	// brightness error weighting factor
	protected float alpha;
	// gradient error weighting factor
	protected float gamma;

	// relaxation parameter for SOR  0 < w < 2.  Recommended default is 1.9
	private float SOR_RELAXATION;

	// number of iterations for inner and outer loops
	private int numOuter;
	private int numInner;
	// maximum number of iterations for SOR
	private int maxIterationsSor;
	// convergence tolerance for SOR
	private float convergeTolerance;

	// derivative of first image
	private GrayF32 deriv1X = new GrayF32(1,1);
	private GrayF32 deriv1Y = new GrayF32(1,1);

	// derivatives of second image
	private GrayF32 deriv2X = new GrayF32(1,1);
	private GrayF32 deriv2Y = new GrayF32(1,1);
	private GrayF32 deriv2XX = new GrayF32(1,1);
	private GrayF32 deriv2YY = new GrayF32(1,1);
	private GrayF32 deriv2XY = new GrayF32(1,1);

	private ImageGradient<GrayF32, GrayF32> gradient = FactoryDerivative.three(GrayF32.class, GrayF32.class);
	private ImageHessian<GrayF32> hessian = FactoryDerivative.hessianThree(GrayF32.class);

	// flow estimation at the start of the iteration
	protected GrayF32 flowU = new GrayF32(1,1); // flow along x-axis
	protected GrayF32 flowV = new GrayF32(1,1); // flow along y-axis

	// storage for the warped flow
	protected GrayF32 warpImage2 = new GrayF32(1,1);
	protected GrayF32 warpDeriv2X = new GrayF32(1,1);
	protected GrayF32 warpDeriv2Y = new GrayF32(1,1);
	protected GrayF32 warpDeriv2XX = new GrayF32(1,1);
	protected GrayF32 warpDeriv2YY = new GrayF32(1,1);
	protected GrayF32 warpDeriv2XY = new GrayF32(1,1);

	// derivative of flow
	protected GrayF32 derivFlowUX = new GrayF32(1,1);
	protected GrayF32 derivFlowUY = new GrayF32(1,1);
	protected GrayF32 derivFlowVX = new GrayF32(1,1);
	protected GrayF32 derivFlowVY = new GrayF32(1,1);

	protected GrayF32 psiSmooth = new GrayF32(1,1);
	protected GrayF32 psiData = new GrayF32(1,1);
	protected GrayF32 psiGradient = new GrayF32(1,1);

	// divergence for u,v,d
	protected GrayF32 divU = new GrayF32(1,1);
	protected GrayF32 divV = new GrayF32(1,1);
	protected GrayF32 divD = new GrayF32(1,1);

	// motion increments for optical flow
	protected GrayF32 du = new GrayF32(1,1);
	protected GrayF32 dv = new GrayF32(1,1);

	/**
	 * Configures flow estimation
	 *
	 * @param config Configuration parameters
	 * @param interp Interpolation for image flow between image layers and warping.  Overrides selection in config.
	 */
	public BroxWarpingSpacial(ConfigBroxWarping config, InterpolatePixelS<GrayF32> interp)
	{
		super(config.pyrScale,config.pyrSigma,config.pyrMaxLayers,interp);
		this.alpha = config.alpha;
		this.gamma = config.gamma;
		this.SOR_RELAXATION = config.SOR_RELAXATION;
		this.numOuter = config.numOuter;
		this.numInner = config.numInner;
		this.maxIterationsSor = config.maxIterationsSor;
		this.convergeTolerance = config.convergeToleranceSor;
	}

	/**
	 * Computes dense optical flow from the provided image pyramid.  Image gradient for each layer should be
	 * computed directly from the layer images.
	 *
	 * @param image1 Pyramid of first image
	 * @param image2 Pyramid of second image
	 */
	public void process(ImagePyramid<GrayF32> image1 , ImagePyramid<GrayF32> image2 )
	{
		// Process the pyramid from low resolution to high resolution
		boolean first = true;
		for( int i = image1.getNumLayers()-1; i >= 0; i-- ) {
			GrayF32 layer1 = image1.getLayer(i);
			GrayF32 layer2 = image2.getLayer(i);

			resizeForLayer(layer1.width,layer2.height);

			// compute image derivatives
			gradient.process(layer1,deriv1X,deriv1Y);
			gradient.process(layer2,deriv2X,deriv2Y);
			hessian.process(deriv2X,deriv2Y,deriv2XX,deriv2YY,deriv2XY);

			if( !first ) {
				// interpolate initial flow from previous layer
				interpolateFlowScale(layer1.width, layer1.height);
			} else {
				// for the very first layer there is no information on flow so set everything to 0
				first = false;

				flowU.reshape(layer1.width, layer1.height);
				flowV.reshape(layer1.width, layer1.height);

				ImageMiscOps.fill(flowU,0);
				ImageMiscOps.fill(flowV,0);
			}

			// compute flow for this layer
			processLayer(layer1,layer2,deriv1X,deriv1Y,deriv2X,deriv2Y,deriv2XX,deriv2YY,deriv2XY);
		}
	}

	/**
	 * Resize images for the current layer being processed
	 */
	protected void resizeForLayer( int width , int height ) {
		deriv1X.reshape(width,height);
		deriv1Y.reshape(width,height);
		deriv2X.reshape(width,height);
		deriv2Y.reshape(width,height);
		deriv2XX.reshape(width,height);
		deriv2YY.reshape(width,height);
		deriv2XY.reshape(width,height);

		warpImage2.reshape(width,height);
		warpDeriv2X.reshape(width,height);
		warpDeriv2Y.reshape(width,height);
		warpDeriv2XX.reshape(width,height);
		warpDeriv2YY.reshape(width,height);
		warpDeriv2XY.reshape(width,height);

		derivFlowUX.reshape(width,height);
		derivFlowUY.reshape(width,height);
		derivFlowVX.reshape(width,height);
		derivFlowVY.reshape(width,height);

		psiData.reshape(width,height);
		psiGradient.reshape(width,height);
		psiSmooth.reshape(width,height);

		divU.reshape(width,height);
		divV.reshape(width,height);
		divD.reshape(width,height);

		du.reshape(width,height);
		dv.reshape(width,height);
	}

	/**
	 * Provides an initial estimate for the flow by interpolating values from the previous layer.
	 */
	protected void interpolateFlowScale(int widthNew, int heightNew) {
		// warping isn't done until later so use those images as temporary storage
		GrayF32 enlargedU = warpDeriv2X;
		GrayF32 enlargedV = warpDeriv2Y;

		// use the previous low resolution flow estimate to initialize the new image
		interpolateFlowScale(flowU, enlargedU);
		interpolateFlowScale(flowV, enlargedV);

		flowU.reshape(widthNew, heightNew);
		flowV.reshape(widthNew, heightNew);

		// save the initial flow values
		flowU.setTo(enlargedU);
		flowV.setTo(enlargedV);
	}

	/**
	 * Computes the flow for a layer using Taylor series expansion and Successive Over-Relaxation linear solver.
	 * Flow estimates from previous layers are feed into this by setting initFlow and flow to their values.
	 */
	protected void processLayer(GrayF32 image1 , GrayF32 image2 ,
								GrayF32 deriv1X , GrayF32 deriv1Y,
								GrayF32 deriv2X , GrayF32 deriv2Y,
								GrayF32 deriv2XX , GrayF32 deriv2YY, GrayF32 deriv2XY) {

		int N = image1.width*image1.height;
		int stride = image1.stride;

		// outer Taylor expansion iterations
		for( int indexOuter = 0; indexOuter < numOuter; indexOuter++ ) {

			// warp the image and the first + second derivatives
			warpImageTaylor(image2, flowU, flowV, warpImage2);

			warpImageTaylor(deriv2X, flowU, flowV, warpDeriv2X);
			warpImageTaylor(deriv2Y, flowU, flowV, warpDeriv2Y);

			warpImageTaylor(deriv2XX, flowU, flowV, warpDeriv2XX);
			warpImageTaylor(deriv2YY, flowU, flowV, warpDeriv2YY);
			warpImageTaylor(deriv2XY, flowU, flowV, warpDeriv2XY);

			gradient.process(flowU,derivFlowUX,derivFlowUY);
			gradient.process(flowV,derivFlowVX,derivFlowVY);

			computePsiSmooth(derivFlowUX,derivFlowUY,derivFlowVX,derivFlowVY,psiSmooth);

			computeDivUVD(flowU, flowV,psiSmooth,divU,divV,divD);

			// initialize the motion increments to zero
			Arrays.fill(du.data,0,N,0);
			Arrays.fill(dv.data,0,N,0);

			for( int indexInner = 0; indexInner < numInner; indexInner++ ) {

				computePsiDataPsiGradient(image1, image2,
						deriv1X, deriv1Y,
						deriv2X, deriv2Y, deriv2XX, deriv2YY, deriv2XY,
						du, dv, psiData, psiGradient);

				float error;
				int iter = 0;

				do {
					// inner SOR iteration.
					error = 0;

					// inner portion
					for (int y = 1; y < image1.height - 1; y++) {
						int i = y * image1.width + 1;
						for (int x = 1; x < image1.width - 1; x++, i++) {
							error += iterationSor(image1, deriv1X, deriv1Y, i, i + 1, i - 1, i + stride, i - stride);
						}
					}

					// border regions require special treatment
					int y0 = 0;
					int y1 = image1.height-1;
					for (int x = 0; x < image1.width; x++ ) {
						error += iterationSor(image1, deriv1X, deriv1Y,
								s(x, y0), s(x + 1, y0), s(x - 1, y0), s(x, y0 - 1), s(x, y0 + 1));

						error += iterationSor(image1, deriv1X, deriv1Y,
								s(x, y1), s(x + 1, y1), s(x - 1, y1), s(x, y1 - 1), s(x, y1 + 1));
					}

					int x0 = 0;
					int x1 = image1.width-1;
					for (int y = 1; y < image1.height - 1; y++) {
						error += iterationSor(image1, deriv1X, deriv1Y,
								s(x0, y), s(x0 - 1, y), s(x0 + 1, y), s(x0, y - 1), s(x0, y + 1));
						error += iterationSor(image1, deriv1X, deriv1Y,
								s(x1, y), s(x1 - 1, y), s(x1 + 1, y), s(x1, y - 1), s(x1, y + 1));
					}
				} while (error > convergeTolerance * image1.width * image1.height && ++iter < maxIterationsSor);
			}

			// update the flow with the motion increments
			PixelMath.add(flowU,du, flowU);
			PixelMath.add(flowV,dv, flowV);
		}
	}

	/**
	 * Inner SOR iteration step
	 *
	 * @param i Index of target pixel at (x,y)
	 * @param ipx (x+1,y)
	 * @param imx (x-1,y)
	 * @param ipy (x,y+1)
	 * @param imy (x,y-1)
	 */
	private float iterationSor(GrayF32 image1, GrayF32 deriv1X, GrayF32 deriv1Y,
							   int i, int ipx, int imx, int ipy, int imy) {
		float w = SOR_RELAXATION;

		// these variables could be precomputed once.  See equation 11
		float psid = psiData.data[i];
		float psig = gamma*psiGradient.data[i];

		float di = warpImage2.data[i] - image1.data[i];
		float dx2 = warpDeriv2X.data[i];
		float dy2 = warpDeriv2Y.data[i];
		float dxx2 = warpDeriv2XX.data[i];
		float dyy2 = warpDeriv2YY.data[i];
		float dxy2 = warpDeriv2XY.data[i];

		float Au = -psid*di*dx2 + alpha*divU.data[i]
				- psig*((dx2 - deriv1X.data[i])*warpDeriv2XX.data[i] + (dy2 - deriv1Y.data[i])*warpDeriv2XY.data[i]);

		float Av = -psid*di*dy2 + alpha*divV.data[i]
				- psig*((dx2 - deriv1X.data[i])*warpDeriv2XY.data[i] + (dy2 - deriv1Y.data[i])*warpDeriv2YY.data[i]);

		float Du = psid*dx2*dx2 + psig*(dxx2*dxx2 + dxy2*dxy2) + alpha*divD.data[i];
		float Dv = psid*dy2*dy2 + psig*(dyy2*dyy2 + dxy2*dxy2) + alpha*divD.data[i];
		float D = psid*dx2*dy2 + psig*(dxx2 + dyy2)*dxy2;

		// update the change in flow
		float psi_index = psiSmooth.data[i];
		float coef0 = 0.5f*(psiSmooth.data[ipx] + psi_index);
		float coef1 = 0.5f*(psiSmooth.data[imx] + psi_index);
		float coef2 = 0.5f*(psiSmooth.data[ipy] + psi_index);
		float coef3 = 0.5f*(psiSmooth.data[imy] + psi_index);

		float div_du = coef0 * du.data[ipx] + coef1 * du.data[imx] + coef2 * du.data[ipy] + coef3 * du.data[imy];
		float div_dv = coef0 * dv.data[ipx] + coef1 * dv.data[imx] + coef2 * dv.data[ipy] + coef3 * dv.data[imy] ;

		final float dui = du.data[i];
		final float dvi = dv.data[i];

		du.data[i] = (1f-w)*dui + w*(Au - D*dvi + alpha*div_du)/Du;
		dv.data[i] = (1f-w)*dvi + w*(Av - D*du.data[i] + alpha*div_dv)/Dv;

		return (du.data[i] - dui) * (du.data[i] - dui) + (dv.data[i] - dvi) * (dv.data[i] - dvi);
	}

	/**
	 * Equation 5.  Psi_s
	 */
	private void computePsiSmooth(GrayF32 ux , GrayF32 uy , GrayF32 vx , GrayF32 vy ,
								  GrayF32 psiSmooth ) {
		int N = derivFlowUX.width * derivFlowUX.height;

		for( int i = 0; i < N; i++ ) {
			float vux = ux.data[i];
			float vuy = uy.data[i];
			float vvx = vx.data[i];
			float vvy = vy.data[i];

			float mu = vux*vux + vuy*vuy;
			float mv = vvx*vvx + vvy*vvy;

			psiSmooth.data[i] = (float)(1.0/(2.0*Math.sqrt(mu + mv + EPSILON*EPSILON)));
		}
	}

	/**
	 * Compute Psi-data using equation 6 and approximation in equation 5
	 */
	protected void computePsiDataPsiGradient(GrayF32 image1, GrayF32 image2,
											 GrayF32 deriv1x, GrayF32 deriv1y,
											 GrayF32 deriv2x, GrayF32 deriv2y,
											 GrayF32 deriv2xx, GrayF32 deriv2yy, GrayF32 deriv2xy,
											 GrayF32 du, GrayF32 dv,
											 GrayF32 psiData, GrayF32 psiGradient ) {
		int N = image1.width * image1.height;

		for( int i = 0; i < N; i++ ) {

			float du_ = du.data[i];
			float dv_ = dv.data[i];

			// compute Psi-data
			float taylor2 = image2.data[i] + deriv2x.data[i]*du_ + deriv2y.data[i]*dv_;
			float v = taylor2 - image1.data[i];

			psiData.data[i] = (float)(1.0/(2.0*Math.sqrt(v*v + EPSILON*EPSILON)));

			// compute Psi-gradient
			float dIx = deriv2x.data[i] + deriv2xx.data[i]*du_ + deriv2xy.data[i]*dv_ - deriv1x.data[i];
			float dIy = deriv2y.data[i] + deriv2xy.data[i]*du_ + deriv2yy.data[i]*dv_ - deriv1y.data[i];
			float dI2 = dIx*dIx +  dIy*dIy;

			psiGradient.data[i] = (float)(1.0/(2.0*Math.sqrt(dI2 + EPSILON*EPSILON)));
		}
	}

	/**
	 * Computes the divergence for u,v, and d. Equation 8 and Equation 10.
	 */
	private void computeDivUVD(GrayF32 u , GrayF32 v , GrayF32 psi ,
							   GrayF32 divU , GrayF32 divV , GrayF32 divD ) {

		final int stride = psi.stride;

		// compute the inside pixel
		for (int y = 1; y < psi.height-1; y++) {

			// index of the current pixel
			int index = y*stride + 1;

			for (int x = 1; x < psi.width-1; x++ , index++) {

				float psi_index = psi.data[index];

				float coef0 = 0.5f*(psi.data[index+1] + psi_index);
				float coef1 = 0.5f*(psi.data[index-1] + psi_index);
				float coef2 = 0.5f*(psi.data[index+stride] + psi_index);
				float coef3 = 0.5f*(psi.data[index-stride] + psi_index);

				float u_index = u.data[index];

				divU.data[index] = coef0*(u.data[index+1] - u_index) + coef1*(u.data[index-1] - u_index) +
								   coef2*(u.data[index+stride] - u_index) + coef3*(u.data[index-stride] - u_index);

				float v_index = v.data[index];

				divV.data[index] = coef0*(v.data[index+1] - v_index) + coef1*(v.data[index-1] - v_index) +
								   coef2*(v.data[index+stride] - v_index) + coef3*(v.data[index-stride] - v_index);

				divD.data[index] = coef0 + coef1 + coef2 + coef3;
			}
		}

		// handle the image borders
		for( int x = 0; x < psi.width; x++ ) {
			computeDivUVD_safe(x,0,u,v,psi,divU,divV,divD);
			computeDivUVD_safe(x,psi.height-1,u,v,psi,divU,divV,divD);
		}
		for( int y = 1; y < psi.height-1; y++ ) {
			computeDivUVD_safe(0,y,u,v,psi,divU,divV,divD);
			computeDivUVD_safe(psi.width-1,y,u,v,psi,divU,divV,divD);
		}
	}

	protected void computeDivUVD_safe(int x , int y ,
									  GrayF32 u , GrayF32 v , GrayF32 psi ,
									  GrayF32 divU , GrayF32 divV , GrayF32 divD ) {

		int index = u.getIndex(x, y);
		int index_px = s(x + 1, y);
		int index_mx = s(x - 1, y);
		int index_py = s(x, y + 1);
		int index_my = s(x, y - 1);

		float psi_index = psi.data[index];

		float coef0 = 0.5f*(psi.data[index_px] + psi_index);
		float coef1 = 0.5f*(psi.data[index_mx] + psi_index);
		float coef2 = 0.5f*(psi.data[index_py] + psi_index);
		float coef3 = 0.5f*(psi.data[index_my] + psi_index);

		float u_index = u.data[index];

		divU.data[index] = coef0*(u.data[index_px] - u_index) + coef1*(u.data[index_mx] - u_index) +
				coef2*(u.data[index_py] - u_index) + coef3*(u.data[index_my] - u_index);

		float v_index = v.data[index];

		divV.data[index] = coef0*(v.data[index_px] - v_index) + coef1*(v.data[index_mx] - v_index) +
				coef2*(v.data[index_py] - v_index) + coef3*(v.data[index_my] - v_index);

		divD.data[index] = coef0 + coef1 + coef2 + coef3;
	}

	protected int s( int x , int y ) {
		if( x < 0 ) x = 0;
		else if( x >= warpImage2.width ) x = warpImage2.width-1;
		if( y < 0 ) y = 0;
		else if( y >= warpImage2.height ) y = warpImage2.height-1;

		return warpImage2.getIndex(x,y);
	}

	public GrayF32 getFlowX() {
		return flowU;
	}

	public GrayF32 getFlowY() {
		return flowV;
	}
}
