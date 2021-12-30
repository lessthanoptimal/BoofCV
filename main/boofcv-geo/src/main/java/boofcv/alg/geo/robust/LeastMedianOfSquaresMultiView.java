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

package boofcv.alg.geo.robust;

import boofcv.alg.geo.DistanceFromModelMultiView;
import boofcv.struct.calib.CameraPinhole;
import org.ddogleg.fitting.modelset.ModelGenerator;
import org.ddogleg.fitting.modelset.ModelManager;
import org.ddogleg.fitting.modelset.lmeds.LeastMedianOfSquares;
import org.ddogleg.fitting.modelset.ransac.Ransac;

/**
 * Extension of {@link Ransac} for two calibrated camera views. Input point will be in normalized image coordinates
 *
 * @author Peter Abeles
 */
public class LeastMedianOfSquaresMultiView<Model, Point> extends LeastMedianOfSquares<Model, Point>
		implements ModelMatcherMultiview<Model, Point> {

	private DistanceFromModelMultiView<Model, Point> modelDistance;

	public LeastMedianOfSquaresMultiView( long randSeed, int totalCycles, double maxMedianError,
										  double inlierFraction, ModelManager<Model> modelManager,
										  ModelGenerator<Model, Point> generator,
										  DistanceFromModelMultiView<Model, Point> errorMetric ) {
		super(randSeed, totalCycles, maxMedianError, inlierFraction, modelManager, errorMetric.getPointType());
		setModel(() -> generator, () -> errorMetric);
		this.modelDistance = errorMetric;
	}

	public LeastMedianOfSquaresMultiView( long randSeed, int totalCycles, ModelManager<Model> modelManager,
										  ModelGenerator<Model, Point> generator,
										  DistanceFromModelMultiView<Model, Point> errorMetric ) {
		super(randSeed, totalCycles, modelManager, errorMetric.getPointType());
		setModel(() -> generator, () -> errorMetric);
		this.modelDistance = errorMetric;
	}

	@Override
	public void setIntrinsic( int view, CameraPinhole intrinsic ) {
		this.modelDistance.setIntrinsic(view, intrinsic);
	}

	@Override
	public int getNumberOfViews() {
		return modelDistance.getNumberOfViews();
	}
}
