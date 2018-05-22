/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.geo.h;

import boofcv.abst.geo.Estimate1ofEpipolar;
import boofcv.alg.geo.h.HomographyDirectLinearTransform;
import boofcv.struct.geo.AssociatedPair;
import org.ejml.data.DMatrixRMaj;

import java.util.List;

/**
 * Wrapper around {@link HomographyDirectLinearTransform} for {@link boofcv.abst.geo.Estimate1ofEpipolar}.
 * 
 * @author Peter Abeles
 */
public class HomographyDLT_to_Epipolar implements Estimate1ofEpipolar
{
	HomographyDirectLinearTransform alg;

	public HomographyDLT_to_Epipolar(HomographyDirectLinearTransform alg) {
		this.alg = alg;
	}

	@Override
	public boolean process(List<AssociatedPair> points, DMatrixRMaj H) {
		return alg.process(points,H);
	}

	@Override
	public int getMinimumPoints() {
		return 4;
	}
}
