/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.ElevateViewInfo;
import org.ddogleg.fitting.modelset.ModelManager;
import org.ddogleg.fitting.modelset.lmeds.LeastMedianOfSquares;

/**
 * LeastMedianOfSquares for dealing with projective geometry. Shape of input images is provided and that allows for proper
 * normalization / scaling of input data.
 *
 * @author Peter Abeles
 */
public class LeastMedianOfSquaresProjective<Model, Point> extends LeastMedianOfSquares<Model, Point>
		implements ModelMatcherViews<Model, Point, ElevateViewInfo> {
	private final DistanceFromModelViews<Model, Point, ElevateViewInfo> modelDistance;
	private final ModelGeneratorViews<Model, Point, ElevateViewInfo> modelGenerator;

	public LeastMedianOfSquaresProjective( long randSeed, int totalCycles, double maxMedianError,
										   double inlierFraction,
										   ModelManager<Model> modelManager,
										   ModelGeneratorViews<Model, Point, ElevateViewInfo> generator,
										   DistanceFromModelViews<Model, Point, ElevateViewInfo> errorMetric ) {
		super(randSeed, totalCycles, maxMedianError, inlierFraction, modelManager, errorMetric.getPointType());
		setModel(() -> generator, () -> errorMetric);
		this.modelDistance = errorMetric;
		this.modelGenerator = generator;
		BoofMiscOps.checkTrue(modelDistance.getNumberOfViews() == modelGenerator.getNumberOfViews());
	}

	@Override
	public void setView( int view, ElevateViewInfo info ) {
		this.modelDistance.setView(view, info);
		this.modelGenerator.setView(view, info);
	}

	@Override
	public int getNumberOfViews() {
		return modelDistance.getNumberOfViews();
	}
}
