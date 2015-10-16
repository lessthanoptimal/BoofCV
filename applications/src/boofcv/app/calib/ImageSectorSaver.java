/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.app.calib;

import boofcv.alg.distort.RemovePerspectiveDistortion;
import boofcv.alg.filter.derivative.DerivativeType;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.core.image.border.BorderType;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F64;

import java.util.Arrays;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class ImageSectorSaver {

	RemovePerspectiveDistortion<ImageFloat32> removePerspective =
			new RemovePerspectiveDistortion<ImageFloat32>(30,30, ImageType.single(ImageFloat32.class));

	ImageFloat32 derivX = new ImageFloat32(30,30);
	ImageFloat32 derivY = new ImageFloat32(30,30);

	ImageFloat32 bestImage;
	double bestScore;



	public ImageSectorSaver() {
		bestImage = new ImageFloat32(1,1);
	}

	public void clearHistory() {
		bestScore = 0;
	}

	public void process(ImageFloat32 image, List<Point2D_F64> sides) {
		removePerspective.apply(image,sides.get(0),sides.get(1),sides.get(2),sides.get(3));

		GImageDerivativeOps.gradient(DerivativeType.SOBEL,removePerspective.getOutput(),
				derivX,derivY, BorderType.EXTENDED);

		double total = 0;

		int N = derivX.getWidth()*derivX.getHeight();

		double[] foo = new double[N];
		for (int i = 0; i < N; i++) {
			double dx = derivX.data[i];
			double dy = derivY.data[i];

			total += foo[i] = dx*dx + dy*dy;
		}

		total /= N;

		Arrays.sort(foo);
		double focus2 = foo[(int)(N*0.97)];
		System.out.println("total focus "+total+"   or  "+focus2);



	}

	public void save() {

	}

}
