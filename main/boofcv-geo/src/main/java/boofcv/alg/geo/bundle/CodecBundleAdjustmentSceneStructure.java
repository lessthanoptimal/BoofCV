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

package boofcv.alg.geo.bundle;

import boofcv.abst.geo.bundle.BundleAdjustmentSceneStructure;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.so.Rodrigues_F64;

/**
 * Encodes and decodes the values in a {@link BundleAdjustmentSceneStructure} using the following
 * parameterization:<br>
 * <pre>
 * [ (X Y Z)*M ][ (rodX rodY rodZ Tx Ty Tz)*N ][ intrinsic*O ]
 * [ features  ][           views             ][ camera      ]
 * </pre>
 * @author Peter Abeles
 */
public class CodecBundleAdjustmentSceneStructure {

	// local variable which stores the predicted location of the feature in the camera frame
	private Rodrigues_F64 rodrigues = new Rodrigues_F64();

	public void decode(double[] input , BundleAdjustmentSceneStructure structure ) {
		int index = 0;

		for (int i = 0; i < structure.points.length; i++) {
			BundleAdjustmentSceneStructure.Point p = structure.points[i];
			p.x = input[index++];
			p.y = input[index++];
			p.z = input[index++];
		}

		for( int viewIndex = 0; viewIndex < structure.views.length; viewIndex++ ) {
			BundleAdjustmentSceneStructure.View view = structure.views[viewIndex];
			// Decode the rigid body transform from world to view
			if( !view.known ) {
				double rodX = input[index++];
				double rodY = input[index++];
				double rodZ = input[index++];

				view.worldToView.T.x = input[index++];
				view.worldToView.T.y = input[index++];
				view.worldToView.T.z = input[index++];

				rodrigues.setParamVector(rodX,rodY,rodZ);

				ConvertRotation3D_F64.rodriguesToMatrix(rodrigues,view.worldToView.R);
			}
		}

		for (int i = 0; i < structure.cameras.length; i++) {
			BundleAdjustmentSceneStructure.Camera camera = structure.cameras[i];
			if( !camera.known ) {
				camera.model.setParameters(input,index);
				index += camera.model.getParameterCount();
			}
		}
	}

	public void encode(BundleAdjustmentSceneStructure structure , double[] output ) {
		int index = 0;

		for (int i = 0; i < structure.points.length; i++) {
			BundleAdjustmentSceneStructure.Point p = structure.points[i];
			output[index++] = p.x;
			output[index++] = p.y;
			output[index++] = p.z;
		}

		for( int viewIndex = 0; viewIndex < structure.views.length; viewIndex++ ) {
			BundleAdjustmentSceneStructure.View view = structure.views[viewIndex];
			// Decode the rigid body transform from world to view
			if( !view.known ) {
				ConvertRotation3D_F64.matrixToRodrigues(view.worldToView.R,rodrigues);
				rodrigues.unitAxisRotation.scale(rodrigues.theta);
				output[index++] = rodrigues.unitAxisRotation.x;
				output[index++] = rodrigues.unitAxisRotation.y;
				output[index++] = rodrigues.unitAxisRotation.z;

				output[index++] = view.worldToView.T.x;
				output[index++] = view.worldToView.T.y;
				output[index++] = view.worldToView.T.z;
			}
		}

		for (int i = 0; i < structure.cameras.length; i++) {
			BundleAdjustmentSceneStructure.Camera camera = structure.cameras[i];
			if( !camera.known ) {
				camera.model.getParameters(output,index);
				index += camera.model.getParameterCount();
			}
		}
	}
}
