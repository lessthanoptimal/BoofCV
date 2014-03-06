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

package boofcv.alg.flow;

import boofcv.struct.flow.ImageFlow;
import boofcv.struct.image.ImageBase;

/**
 * <p>
 * This is Horn-Schunk's well known work [1] for dense optical flow estimation.  It is based off the following
 * equation Ex*u + Ey*v + Et = 0, where (u,v) is the estimated flow for a single pixel, and (Ex,Ey) is the pixel's
 * gradient and Et is the grave in intensity value.  It is assumed that each pixel maintains a constant intensity
 * and that changes in flow are smooth. This implementation is faithful to the original
 * work and does not make any effort to improve its performance using more modern techniques.
 * </p>
 *
 * <p>
 * [1] Horn, Berthold K., and Brian G. Schunck. "Determining optical flow."
 * 1981 Technical Symposium East. International Society for Optics and Photonics, 1981.
 * </p>
 *
 * @author Peter Abeles
 */
public abstract class DenseOpticalFlowHornSchunck<T extends ImageBase> {

	// used to weight the error of image brightness and smoothness of velocity flow
	protected float alpha2;

	// Number of iterations
	protected int numIterations;

	// storage for the average flow
	protected ImageFlow averageFlow = new ImageFlow(1,1);

	/**
	 * Constructor
	 *
	 * @param alpha Weighting used adjust the importance of brightness and smoothness.  Try ?
	 */
	public DenseOpticalFlowHornSchunck(float alpha, int numIterations) {
		this.alpha2 = alpha*alpha;
		this.numIterations = numIterations;
	}

	/**
	 * changes the maximum number of iterations
	 * @param numIterations maximum number of iterations
	 */
	public void setNumIterations(int numIterations) {
		this.numIterations = numIterations;
	}

	/**
	 * Computes dense optical flow from the first image's gradient and the difference between
	 * the second and the first image.
	 *
	 * @param derivX First image's gradient x-axis
	 * @param derivY First image's gradient y-axis
	 * @param derivT Second image minus the first image
	 * @param output Found dense optical flow
	 */
	public abstract void process( T derivX , T derivY , T derivT , ImageFlow output);

	/**
	 * Computes average flow using an 8-connect neighborhood for the inner image
	 */
	protected static void innerAverageFlow( ImageFlow flow , ImageFlow averageFlow ) {

		int endX = flow.width-1;
		int endY = flow.height-1;

		for( int y = 1; y < endY; y++ ) {
			int index = flow.width*y + 1;
			for( int x = 1; x < endX; x++ , index++) {
				ImageFlow.D average = averageFlow.data[index];

				ImageFlow.D f0 = flow.data[index-1];
				ImageFlow.D f1 = flow.data[index+1];
				ImageFlow.D f2 = flow.data[index-flow.width];
				ImageFlow.D f3 = flow.data[index+flow.width];

				ImageFlow.D f4 = flow.data[index-1-flow.width];
				ImageFlow.D f5 = flow.data[index+1-flow.width];
				ImageFlow.D f6 = flow.data[index-1+flow.width];
				ImageFlow.D f7 = flow.data[index+1+flow.width];

				average.x = 0.1666667f*(f0.x + f1.x + f2.x + f3.x) + 0.08333333f*(f4.x + f5.x + f6.x + f7.x);
				average.y = 0.1666667f*(f0.y + f1.y + f2.y + f3.y) + 0.08333333f*(f4.y + f5.y + f6.y + f7.y);
			}
		}
	}

	/**
	 * Computes average flow using an 8-connect neighborhood for the image border
	 */
	protected static void borderAverageFlow( ImageFlow flow , ImageFlow averageFlow) {

		for( int y = 0; y < flow.height; y++ ) {
			computeBorder(flow,averageFlow, 0, y);
			computeBorder(flow,averageFlow, flow.width-1, y);
		}

		for( int x = 1; x < flow.width-1; x++ ) {
			computeBorder(flow,averageFlow, x, 0);
			computeBorder(flow,averageFlow, x, flow.height-1);
		}
	}

	protected static void computeBorder(ImageFlow flow, ImageFlow averageFlow, int x, int y) {
		ImageFlow.D average = averageFlow.get(x,y);

		ImageFlow.D f0 = getExtend(flow, x-1,y);
		ImageFlow.D f1 = getExtend(flow, x+1,y);
		ImageFlow.D f2 = getExtend(flow, x,y-1);
		ImageFlow.D f3 = getExtend(flow, x,y+1);

		ImageFlow.D f4 = getExtend(flow, x-1,y-1);
		ImageFlow.D f5 = getExtend(flow, x+1,y-1);
		ImageFlow.D f6 = getExtend(flow, x-1,y+1);
		ImageFlow.D f7 = getExtend(flow, x+1,y+1);

		average.x = 0.1666667f*(f0.x + f1.x + f2.x + f3.x) + 0.08333333f*(f4.x + f5.x + f6.x + f7.x);
		average.y = 0.1666667f*(f0.y + f1.y + f2.y + f3.y) + 0.08333333f*(f4.y + f5.y + f6.y + f7.y);
	}

	protected static ImageFlow.D getExtend(ImageFlow flow, int x, int y) {
		if( x < 0 ) x = 0;
		else if( x >= flow.width ) x = flow.width-1;
		if( y < 0 ) y = 0;
		else if( y >= flow.height ) y = flow.height-1;

		return flow.unsafe_get(x,y);
	}
}
