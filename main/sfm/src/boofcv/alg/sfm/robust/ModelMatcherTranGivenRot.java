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

package boofcv.alg.sfm.robust;

import boofcv.struct.geo.Point2D3D;
import georegression.struct.point.Vector3D_F64;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.fitting.modelset.ransac.Ransac;
import org.ejml.data.DenseMatrix64F;

import java.util.List;

/**
 * @author Peter Abeles
 */
public class ModelMatcherTranGivenRot implements ModelMatcher<Vector3D_F64,Point2D3D> {

	Ransac<Vector3D_F64,Point2D3D> alg;
	DistanceTranGivenRotSq dist = new DistanceTranGivenRotSq();
	TranGivenRotGenerator gen = new TranGivenRotGenerator();

	public ModelMatcherTranGivenRot(long randSeed, int maxIterations,
									double thresholdFit) {
		alg = new Ransac<Vector3D_F64, Point2D3D>(randSeed, gen, dist,
				maxIterations, thresholdFit);
	}

	public void setRotation( DenseMatrix64F R ) {
		dist.setRotation(R);
		gen.setRotation(R);
	}

	@Override
	public boolean process(List<Point2D3D> dataSet) {
		return alg.process(dataSet);
	}

	@Override
	public Vector3D_F64 getModel() {
		return alg.getModel();
	}

	@Override
	public List<Point2D3D> getMatchSet() {
		return alg.getMatchSet();
	}

	@Override
	public int getInputIndex(int matchIndex) {
		return alg.getInputIndex(matchIndex);
	}

	@Override
	public double getError() {
		return alg.getError();
	}

	@Override
	public int getMinimumSize() {
		return alg.getMinimumSize();
	}
}
