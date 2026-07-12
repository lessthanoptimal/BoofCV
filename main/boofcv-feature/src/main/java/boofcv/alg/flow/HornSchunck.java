/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.InputSanityCheck;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.InterleavedF32;

/**
 * <p>
 * This is Horn-Schunck's well known work [1] for dense optical flow estimation. It is based off the following
 * equation Ex*u + Ey*v + Et = 0, where (u,v) is the estimated flow for a single pixel, and (Ex,Ey) is the pixel's
 * gradient and Et is the grave in intensity value. It is assumed that each pixel maintains a constant intensity
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
public abstract class HornSchunck<T extends ImageBase<T>, D extends ImageBase<D>> {

	// used to weight the error of image brightness and smoothness of velocity flow
	protected float alpha2;

	// Number of iterations
	protected int numIterations;

	// storage for the average flow. Interleaved: band 0 = x, band 1 = y
	protected InterleavedF32 averageFlow = new InterleavedF32(1, 1, 2);

	// If the output should be cleared each time a new image is processed or used as an initial estimate
	protected boolean resetOutput = true;

	// storage for derivatives
	protected D derivX;
	protected D derivY;
	protected D derivT;

	/**
	 * Constructor
	 *
	 * @param alpha Larger values place more importance on flow smoothness consistency over brightness consistency. Try 20
	 * @param numIterations Number of iterations. Try 1000
	 */
	protected HornSchunck( float alpha, int numIterations, ImageType<D> derivType ) {
		this.alpha2 = alpha*alpha;
		this.numIterations = numIterations;

		derivX = derivType.createImage(1, 1);
		derivY = derivType.createImage(1, 1);
		derivT = derivType.createImage(1, 1);
	}

	/**
	 * changes the maximum number of iterations
	 *
	 * @param numIterations maximum number of iterations
	 */
	public void setNumIterations( int numIterations ) {
		this.numIterations = numIterations;
	}

	/**
	 * Computes dense optical flow from the first image's gradient and the difference between
	 * the second and the first image.
	 *
	 * @param image1 First image
	 * @param image2 Second image
	 * @param output Found dense optical flow. Interleaved: band 0 = x, band 1 = y
	 */
	public void process( T image1, T image2, InterleavedF32 output ) {

		InputSanityCheck.checkSameShape(image1, image2);

		derivX.reshape(image1.width, image1.height);
		derivY.reshape(image1.width, image1.height);
		derivT.reshape(image1.width, image1.height);

		averageFlow.reshape(output.width, output.height);

		if (resetOutput)
			ImageMiscOps.fill(output, 0);

		computeDerivX(image1, image2, derivX);
		computeDerivY(image1, image2, derivY);
		computeDerivT(image1, image2, derivT);

		findFlow(derivX, derivY, derivT, output);
	}

	protected abstract void computeDerivX( T image1, T image2, D derivX );

	protected abstract void computeDerivY( T image1, T image2, D derivY );

	protected abstract void computeDerivT( T image1, T image2, D derivT );

	/**
	 * Inner function for computing optical flow
	 */
	protected abstract void findFlow( D derivX, D derivY, D derivT, InterleavedF32 output );

	/**
	 * Computes average flow using an 8-connect neighborhood for the inner image
	 */
	protected static void innerAverageFlow( InterleavedF32 flow, InterleavedF32 averageFlow ) {

		int endX = flow.width - 1;
		int endY = flow.height - 1;

		final int w = flow.width;
		final float[] f = flow.data;

		for (int y = 1; y < endY; y++) {
			int index = w*y + 1;
			for (int x = 1; x < endX; x++, index++) {
				// pixel indices scaled by 2 bands (x = band 0, y = band 1)
				int a = index*2;

				int i0 = (index - 1)*2;
				int i1 = (index + 1)*2;
				int i2 = (index - w)*2;
				int i3 = (index + w)*2;

				int i4 = (index - 1 - w)*2;
				int i5 = (index + 1 - w)*2;
				int i6 = (index - 1 + w)*2;
				int i7 = (index + 1 + w)*2;

				averageFlow.data[a] = 0.1666667f*(f[i0] + f[i1] + f[i2] + f[i3]) +
						0.08333333f*(f[i4] + f[i5] + f[i6] + f[i7]);
				averageFlow.data[a + 1] = 0.1666667f*(f[i0 + 1] + f[i1 + 1] + f[i2 + 1] + f[i3 + 1]) +
						0.08333333f*(f[i4 + 1] + f[i5 + 1] + f[i6 + 1] + f[i7 + 1]);
			}
		}
	}

	/**
	 * Computes average flow using an 8-connect neighborhood for the image border
	 */
	protected static void borderAverageFlow( InterleavedF32 flow, InterleavedF32 averageFlow ) {

		for (int y = 0; y < flow.height; y++) {
			computeBorder(flow, averageFlow, 0, y);
			computeBorder(flow, averageFlow, flow.width - 1, y);
		}

		for (int x = 1; x < flow.width - 1; x++) {
			computeBorder(flow, averageFlow, x, 0);
			computeBorder(flow, averageFlow, x, flow.height - 1);
		}
	}

	protected static void computeBorder( InterleavedF32 flow, InterleavedF32 averageFlow, int x, int y ) {
		int a = averageFlow.getIndex(x, y, 0);

		final float[] f = flow.data;

		int i0 = getExtend(flow, x - 1, y);
		int i1 = getExtend(flow, x + 1, y);
		int i2 = getExtend(flow, x, y - 1);
		int i3 = getExtend(flow, x, y + 1);

		int i4 = getExtend(flow, x - 1, y - 1);
		int i5 = getExtend(flow, x + 1, y - 1);
		int i6 = getExtend(flow, x - 1, y + 1);
		int i7 = getExtend(flow, x + 1, y + 1);

		averageFlow.data[a] = 0.1666667f*(f[i0] + f[i1] + f[i2] + f[i3]) +
				0.08333333f*(f[i4] + f[i5] + f[i6] + f[i7]);
		averageFlow.data[a + 1] = 0.1666667f*(f[i0 + 1] + f[i1 + 1] + f[i2 + 1] + f[i3 + 1]) +
				0.08333333f*(f[i4 + 1] + f[i5 + 1] + f[i6 + 1] + f[i7 + 1]);
	}

	/** Returns the band-0 array index of the clamped (extended border) pixel */
	protected static int getExtend( InterleavedF32 flow, int x, int y ) {
		if (x < 0) x = 0;
		else if (x >= flow.width) x = flow.width - 1;
		if (y < 0) y = 0;
		else if (y >= flow.height) y = flow.height - 1;

		return flow.getIndex(x, y, 0);
	}
}
