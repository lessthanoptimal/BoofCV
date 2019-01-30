/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.geo.bundle.BundleAdjustmentSchur_DSCC;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.geo.bundle.jacobians.JacobianSo3;
import boofcv.alg.geo.bundle.jacobians.JacobianSo3Rodrigues;

/**
 * Encodes and decodes the values in a {@link SceneStructureMetric} using the following
 * parameterization:<br>
 * <pre>
 *     RT = (rodX rodY rodZ Tx Ty Tz)
 * [ (X Y Z)*M ][  RT*len(rigid)  ][  RT*len(views) ][ intrinsic*O ]
 * [ features  ][    rigid        ][    views       ][ camera      ]
 * </pre>
 *
 * Default encoding for rotation matrix is {@link JacobianSo3Rodrigues}
 *
 * @author Peter Abeles
 */
public class CodecSceneStructureMetric implements BundleAdjustmentSchur_DSCC.Codec<SceneStructureMetric>
{
	/**
	 * Specifies encoding/decoding of rotation for bundle adjustment. Default is {@link JacobianSo3Rodrigues}
	 */
	public JacobianSo3 rotation = new JacobianSo3Rodrigues();

	public CodecSceneStructureMetric() {
	}

	public CodecSceneStructureMetric(JacobianSo3 rotation) {
		this.rotation = rotation;
	}

	@Override
	public void decode(double[] input , SceneStructureMetric structure ) {
		int index = 0;

		for (int i = 0; i < structure.points.length; i++) {
			SceneStructureMetric.Point p = structure.points[i];
			p.coordinate[0] = input[index++];
			p.coordinate[1] = input[index++];
			p.coordinate[2] = input[index++];
			if( structure.isHomogenous() )
				p.coordinate[3] = input[index++];
		}

		for (int rigidIndex = 0; rigidIndex < structure.rigids.length; rigidIndex++) {
			SceneStructureMetric.Rigid rigid = structure.rigids[rigidIndex];
			// Decode the rigid body transform from object to world
			if( !rigid.known ) {
				rotation.setParameters(input,index);
				rigid.objectToWorld.R.set(rotation.getRotationMatrix());
				index += rotation.getParameterLength();

				rigid.objectToWorld.T.x = input[index++];
				rigid.objectToWorld.T.y = input[index++];
				rigid.objectToWorld.T.z = input[index++];
			}
		}

		for( int viewIndex = 0; viewIndex < structure.views.length; viewIndex++ ) {
			SceneStructureMetric.View view = structure.views[viewIndex];
			// Decode the rigid body transform from world to view
			if( !view.known ) {
				rotation.setParameters(input,index);
				view.worldToView.R.set(rotation.getRotationMatrix());
				index += rotation.getParameterLength();

				view.worldToView.T.x = input[index++];
				view.worldToView.T.y = input[index++];
				view.worldToView.T.z = input[index++];
			}
		}

		for (int i = 0; i < structure.cameras.length; i++) {
			SceneStructureMetric.Camera camera = structure.cameras[i];
			if( !camera.known ) {
				camera.model.setIntrinsic(input,index);
				index += camera.model.getIntrinsicCount();
			}
		}
	}

	@Override
	public void encode(SceneStructureMetric structure , double[] output ) {
		int index = 0;

		for (int i = 0; i < structure.points.length; i++) {
			SceneStructureMetric.Point p = structure.points[i];
			output[index++] = p.coordinate[0];
			output[index++] = p.coordinate[1];
			output[index++] = p.coordinate[2];
			if( structure.isHomogenous() )
				output[index++] = p.coordinate[3];
		}

		for (int rigidIndex = 0; rigidIndex < structure.rigids.length; rigidIndex++) {
			SceneStructureMetric.Rigid rigid = structure.rigids[rigidIndex];
			// Decode the rigid body transform from object to world
			if( !rigid.known ) {
				rotation.getParameters(rigid.objectToWorld.R,output,index);
				index += rotation.getParameterLength();

				output[index++] = rigid.objectToWorld.T.x;
				output[index++] = rigid.objectToWorld.T.y;
				output[index++] = rigid.objectToWorld.T.z;
			}
		}

		for( int viewIndex = 0; viewIndex < structure.views.length; viewIndex++ ) {
			SceneStructureMetric.View view = structure.views[viewIndex];
			// Decode the rigid body transform from world to view
			if( !view.known ) {
				rotation.getParameters(view.worldToView.R,output,index);
				index += rotation.getParameterLength();

				output[index++] = view.worldToView.T.x;
				output[index++] = view.worldToView.T.y;
				output[index++] = view.worldToView.T.z;
			}
		}

		for (int i = 0; i < structure.cameras.length; i++) {
			SceneStructureMetric.Camera camera = structure.cameras[i];
			if( !camera.known ) {
				camera.model.getIntrinsic(output,index);
				index += camera.model.getIntrinsicCount();
			}
		}
	}
}
