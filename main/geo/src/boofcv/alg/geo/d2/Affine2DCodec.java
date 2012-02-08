/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.d2;

import boofcv.numerics.fitting.modelset.ModelCodec;
import georegression.struct.affine.Affine2D_F64;


/**
 * Converts an {@link georegression.struct.affine.Affine2D_F64} to and from an array
 * parameterized format.
 *
 * @author Peter Abeles
 */
public class Affine2DCodec implements ModelCodec<Affine2D_F64> {

	@Override
	public int getParamLength() {
		return 6;
	}

	@Override
	public Affine2D_F64 decode(double[] param, Affine2D_F64 model) {
		if( model == null )
			model = new Affine2D_F64();

		decodeStatic(param, model);

		return model;
	}

	public static void decodeStatic(double[] param, Affine2D_F64 model) {
		model.a11 = (float)param[0];
		model.a12 = (float)param[1];
		model.a21 = (float)param[2];
		model.a22 = (float)param[3];
		model.tx = (float)param[4];
		model.ty = (float)param[5];
	}

	@Override
	public double[] encode(Affine2D_F64 model, double[] param) {
		if( param == null )
			param = new double[6];

		encodeStatic(model, param);

		return param;
	}

	public static void encodeStatic(Affine2D_F64 model, double[] param) {
		param[0] = model.a11;
		param[1] = model.a12;
		param[2] = model.a21;
		param[3] = model.a22;
		param[4] = model.tx;
		param[5] = model.ty;
	}
}
