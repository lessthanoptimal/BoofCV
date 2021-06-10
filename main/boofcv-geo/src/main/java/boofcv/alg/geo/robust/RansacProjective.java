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

import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.ElevateViewInfo;
import org.ddogleg.fitting.modelset.ModelManager;
import org.ddogleg.fitting.modelset.ransac.Ransac;

/**
 * RANSAC for dealing with projective geometry. Shape of input images is provided and that allows for proper
 * normalization / scaling of input data.
 *
 * @author Peter Abeles
 */
public class RansacProjective<Model, Point> extends Ransac<Model, Point>
		implements ModelMatcherViews<Model, Point, ElevateViewInfo> {
	private final DistanceFromModelViews<Model, Point, ElevateViewInfo> modelDistance;
	private final ModelGeneratorViews<Model, Point, ElevateViewInfo> modelGenerator;

	public RansacProjective( long randSeed,
							 ModelManager<Model> modelManager,
							 ModelGeneratorViews<Model, Point, ElevateViewInfo> modelGenerator,
							 DistanceFromModelViews<Model, Point, ElevateViewInfo> modelDistance, int maxIterations, double thresholdFit ) {
		super(randSeed, maxIterations, thresholdFit, modelManager, modelDistance.getPointType());
		setModel(() -> modelGenerator, () -> modelDistance);
		this.modelDistance = modelDistance;
		this.modelGenerator = modelGenerator;
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
