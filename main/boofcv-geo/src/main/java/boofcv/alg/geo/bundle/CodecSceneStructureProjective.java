/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.bundle;

import boofcv.abst.geo.bundle.BundleAdjustmentSchur;
import boofcv.abst.geo.bundle.SceneStructureCommon;
import boofcv.abst.geo.bundle.SceneStructureProjective;

/**
 * Encodes and decodes the values in a {@link SceneStructureProjective} using the following
 * parameterization:<br>
 * <pre>
 * [ (X Y Z)*M ][ P11 P12 P13 P14 P21 P22 ... ]
 * [ features  ][         projective          ]
 * </pre>
 *
 * @author Peter Abeles
 */
public class CodecSceneStructureProjective implements BundleAdjustmentSchur.Codec<SceneStructureProjective> {
	@Override
	public void decode( double[] input, SceneStructureProjective structure ) {
		int index = 0;

		for (int i = 0; i < structure.points.size; i++) {
			SceneStructureCommon.Point p = structure.points.data[i];
			p.coordinate[0] = input[index++];
			p.coordinate[1] = input[index++];
			p.coordinate[2] = input[index++];
			if (structure.isHomogenous())
				p.coordinate[3] = input[index++];
		}

		for (int viewIndex = 0; viewIndex < structure.views.size; viewIndex++) {
			SceneStructureProjective.View view = structure.views.data[viewIndex];
			// Decode the rigid body transform from world to view
			if (!view.known) {
				for (int i = 0; i < 12; i++) {
					view.worldToView.data[i] = input[index++];
				}
			}
		}

		for (int i = 0; i < structure.cameras.size; i++) {
			SceneStructureCommon.Camera camera = structure.cameras.data[i];
			if (!camera.known) {
				camera.model.setIntrinsic(input, index);
				index += camera.model.getIntrinsicCount();
			}
		}
	}

	@Override
	public void encode( SceneStructureProjective structure, double[] output ) {
		int index = 0;

		for (int i = 0; i < structure.points.size; i++) {
			SceneStructureCommon.Point p = structure.points.data[i];
			output[index++] = p.coordinate[0];
			output[index++] = p.coordinate[1];
			output[index++] = p.coordinate[2];
			if (structure.isHomogenous())
				output[index++] = p.coordinate[3];
		}

		for (int viewIndex = 0; viewIndex < structure.views.size; viewIndex++) {
			SceneStructureProjective.View view = structure.views.data[viewIndex];
			// Decode the rigid body transform from world to view
			if (!view.known) {
				for (int i = 0; i < 12; i++) {
					output[index++] = view.worldToView.data[i];
				}
			}
		}

		for (int i = 0; i < structure.cameras.size; i++) {
			SceneStructureCommon.Camera camera = structure.cameras.data[i];
			if (!camera.known) {
				camera.model.getIntrinsic(output, index);
				index += camera.model.getIntrinsicCount();
			}
		}
	}
}
