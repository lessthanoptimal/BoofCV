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

package boofcv.abst.geo;

import boofcv.struct.geo.AssociatedTriple;
import org.ejml.data.DMatrixRMaj;

import java.util.List;

/**
 * Refines the camera matrices from three views. This is the same as refining the
 * trifocal tensor.
 *
 * @author Peter Abeles
 */
public interface RefineThreeViewProjective {
	/**
	 * Refines the camera matrices from three views. The first view is assumed to be P1 = [I|0]. It's
	 * recommended that observations already be normalized so that they have a mean of 0 and standard
	 * deviation of 1, or similar.
	 *
	 * @param observations (Input) Pixel observations from each view
	 * @param P2 (Input) Initial estimate camera matrix from view 2
	 * @param P3 (Input) Initial estimate camera matrix from view 3
	 * @param refinedP2 (Output) Refined estimate of camera matrix from view 2
	 * @param refinedP3 (Output) Refined estimate of camera matrix from view 3
	 * @return true if successful or false if it failed
	 */
	boolean process( List<AssociatedTriple> observations, DMatrixRMaj P2, DMatrixRMaj P3,
					 DMatrixRMaj refinedP2, DMatrixRMaj refinedP3 );
}
