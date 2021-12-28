/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.intensity.impl;

import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayS32;
import pabeles.concurrency.GrowArray;

import javax.annotation.Generated;

//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;

/**
 * <p>
 * Implementation of {@link ImplSsdCornerBase} for {@link GrayS16}.
 * </p>
 *
 *
 * <p>DO NOT MODIFY. Automatically generated code created by GenerateImplSsdCorner</p>
 *
 * @author Peter Abeles
 */
@Generated("boofcv.alg.feature.detect.intensity.impl.GenerateImplSsdCorner")
public class ImplSsdCorner_S16 extends ImplSsdCornerBox<GrayS16, GrayS32> {

	private GrowArray<WorkSpace> workspaces = new GrowArray<>(() -> new WorkSpace(0));
	private CornerIntensity_S32 intensity;

	public ImplSsdCorner_S16( int windowRadius, CornerIntensity_S32 intensity ) {
		super(windowRadius, GrayS16.class, GrayS32.class);
		this.intensity = intensity;
	}

	@Override
	protected void setImageShape( int imageWidth, int imageHeight ) {
		super.setImageShape(imageWidth, imageHeight);
		workspaces = new GrowArray<>(() -> new WorkSpace(imageWidth));
	}

	/**
	 * Compute the derivative sum along the x-axis while taking advantage of duplicate
	 * calculations for each window.
	 */
	@Override
	protected void horizontal() {
		short[] dataX = derivX.data;
		short[] dataY = derivY.data;

		int[] hXX = horizXX.data;
		int[] hXY = horizXY.data;
		int[] hYY = horizYY.data;

		final int imgHeight = derivX.getHeight();
		final int imgWidth = derivX.getWidth();

		int windowWidth = radius*2 + 1;

		int radp1 = radius + 1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0,imgHeight,row->{
		for (int row = 0; row < imgHeight; row++) {

			int pix = row*imgWidth;
			int end = pix + windowWidth;

			int totalXX = 0;
			int totalXY = 0;
			int totalYY = 0;

			int indexX = derivX.startIndex + row*derivX.stride;
			int indexY = derivY.startIndex + row*derivY.stride;

			for (; pix < end; pix++) {
				short dx = dataX[indexX++];
				short dy = dataY[indexY++];

				totalXX += dx*dx;
				totalXY += dx*dy;
				totalYY += dy*dy;
			}

			hXX[pix - radp1] = totalXX;
			hXY[pix - radp1] = totalXY;
			hYY[pix - radp1] = totalYY;

			end = row*imgWidth + imgWidth;
			for (; pix < end; pix++, indexX++, indexY++) {

				short dx = dataX[indexX - windowWidth];
				short dy = dataY[indexY - windowWidth];

				// saving these multiplications in an array to avoid recalculating them made
				// the algorithm about 50% slower
				totalXX -= dx*dx;
				totalXY -= dx*dy;
				totalYY -= dy*dy;

				dx = dataX[indexX];
				dy = dataY[indexY];

				totalXX += dx*dx;
				totalXY += dx*dy;
				totalYY += dy*dy;

				hXX[pix - radius] = totalXX;
				hXY[pix - radius] = totalXY;
				hYY[pix - radius] = totalYY;
			}
		}
		//CONCURRENT_ABOVE });
	}

	/**
	 * Compute the derivative sum along the y-axis while taking advantage of duplicate
	 * calculations for each window and avoiding cache misses. Then compute the eigen values
	 */
	@Override
	protected void vertical( GrayF32 intensity ) {
		int[] hXX = horizXX.data;
		int[] hXY = horizXY.data;
		int[] hYY = horizYY.data;
		final float[] inten = intensity.data;

		final int imgHeight = horizXX.getHeight();
		final int imgWidth = horizXX.getWidth();

		final int kernelWidth = radius*2 + 1;

		final int startX = radius;
		final int endX = imgWidth - radius;

		final int backStep = kernelWidth*imgWidth;

		WorkSpace work = workspaces.grow(); //CONCURRENT_REMOVE_LINE
		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(radius,imgHeight-radius,workspaces,(work,y0,y1)->{
		int y0 = radius, y1 = imgHeight - radius;
		final int[] tempXX = work.xx;
		final int[] tempXY = work.yy;
		final int[] tempYY = work.zz;
		for (int x = startX; x < endX; x++) {
			// defines the A matrix, from which the eigenvalues are computed
			int srcIndex = x + (y0 - radius)*imgWidth;
			int destIndex = imgWidth*y0 + x;
			int totalXX = 0, totalXY = 0, totalYY = 0;

			int indexEnd = srcIndex + imgWidth*kernelWidth;
			for (; srcIndex < indexEnd; srcIndex += imgWidth) {
				totalXX += hXX[srcIndex];
				totalXY += hXY[srcIndex];
				totalYY += hYY[srcIndex];
			}

			tempXX[x] = totalXX;
			tempXY[x] = totalXY;
			tempYY[x] = totalYY;

			// compute the eigen values
			inten[destIndex] = this.intensity.compute(totalXX, totalXY, totalYY);
			destIndex += imgWidth;
		}

		// change the order it is processed in to reduce cache misses
		for (int y = y0 + 1; y < y1; y++) {
			int srcIndex = (y + radius)*imgWidth + startX;
			int destIndex = y*imgWidth + startX;

			for (int x = startX; x < endX; x++, srcIndex++, destIndex++) {
				int totalXX = tempXX[x] - hXX[srcIndex - backStep];
				tempXX[x] = totalXX += hXX[srcIndex];
				int totalXY = tempXY[x] - hXY[srcIndex - backStep];
				tempXY[x] = totalXY += hXY[srcIndex];
				int totalYY = tempYY[x] - hYY[srcIndex - backStep];
				tempYY[x] = totalYY += hYY[srcIndex];

				inten[destIndex] = this.intensity.compute(totalXX, totalXY, totalYY);
			}
		}
		//CONCURRENT_INLINE });
	}

	private static class WorkSpace {
		public final int[] xx;
		public final int[] yy;
		public final int[] zz;

		public WorkSpace( int size ) {
			xx = new int[size];
			yy = new int[size];
			zz = new int[size];
		}
	}
}
