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

package boofcv.abst.geo;

import boofcv.struct.geo.AssociatedPair;
import org.ddogleg.fitting.modelset.ModelFitter;
import org.ejml.data.DenseMatrix64F;

/**
 * <p>
 * Refines a Fundamental, Essential, or Homography matrix such that it is a better fit to the provided
 * observations.  Input and output matrices are assumed to follow the constraint conventions defined in
 * {@link boofcv.alg.geo.MultiViewOps#constraint(org.ejml.data.DenseMatrix64F,
 * georegression.struct.point.Point2D_F64, georegression.struct.point.Point2D_F64) constraint Fundamental}
 * and {@link boofcv.alg.geo.MultiViewOps#constraintHomography(org.ejml.data.DenseMatrix64F,
 * georegression.struct.point.Point2D_F64, georegression.struct.point.Point2D_F64) constraint Homography}.
 * </p>
 *
 * @author Peter Abeles
 */
public interface RefineEpipolar extends ModelFitter<DenseMatrix64F,AssociatedPair> {
}
