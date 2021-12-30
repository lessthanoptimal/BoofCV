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

package boofcv.alg.distort.brown;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

/**
 * Distortion parameters for radial and tangential model
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class RadialTangential_F64 {
	/** Radial distortion parameters */
	public double[] radial;

	/** Tangential distortion parameters */
	@Getter @Setter public double t1, t2;

	public RadialTangential_F64( RadialTangential_F64 original ) {
		this.radial = original.radial.clone();
		this.t1 = original.t1;
		this.t2 = original.t2;
	}

	public RadialTangential_F64( int numRadial ) {
		radial = new double[numRadial];
	}

	public RadialTangential_F64() {
	}

	public RadialTangential_F64( @Nullable /**/double[] radial, /**/double t1, /**/double t2 ) {
		this.setTo(radial, t1, t2);
	}

	public void setTo( @Nullable /**/double[] radial, /**/double t1, /**/double t2 ) {
		if (radial == null)
			this.radial = new double[0];
		else {
			this.radial = new double[radial.length];
			for (int i = 0; i < radial.length; i++) {
				this.radial[i] = (double)radial[i];
			}
		}
		this.t1 = (double)t1;
		this.t2 = (double)t2;
	}
}
