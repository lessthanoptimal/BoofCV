/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

import georegression.struct.affine.Affine2D_F64;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 * @author Peter Abeles
 */
public class TestAffine2DCodec {

	@Test
	public void encode() {
		Affine2DCodec codec = new Affine2DCodec();

		Affine2D_F64 model = new Affine2D_F64(1,2,3,4,5,6);
		double param[] = new double[6];

		codec.encode(model,param);

		for( int i = 0; i < 6; i++ ) {
			assertEquals(i+1,param[i],1e-6);
		}

		// decode
		model = new Affine2D_F64();
		codec.decode(param,model);

		assertEquals(1,model.a11,1e-4);
		assertEquals(2,model.a12,1e-4);
		assertEquals(3,model.a21,1e-4);
		assertEquals(4,model.a22,1e-4);
		assertEquals(5,model.tx,1e-4);
		assertEquals(6,model.ty,1e-4);
	}

	@Test
	public void decode() {
		Affine2DCodec codec = new Affine2DCodec();

		Affine2D_F64 model = new Affine2D_F64();
		double param[] = new double[6];


		for( int i = 0; i < 6; i++ ) {
			param[i] = i+1;
		}

		codec.decode(param,model);

		assertEquals(1,model.a11,1e-4);
		assertEquals(2,model.a12,1e-4);
		assertEquals(3,model.a21,1e-4);
		assertEquals(4,model.a22,1e-4);
		assertEquals(5,model.tx,1e-4);
		assertEquals(6,model.ty,1e-4);
	}
}
