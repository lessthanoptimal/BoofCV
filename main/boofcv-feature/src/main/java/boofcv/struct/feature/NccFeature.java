/*
 * Copyright (c) 2024, Peter Abeles. All Rights Reserved.
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

package boofcv.struct.feature;

import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;

/**
 * <p>Description for normalized cross correlation (NCC). The descriptor's value
 * in a NCC feature is the pixel intensity value minus the mean pixel intensity value.
 * </p>
 * value[i] = I(x,y) - mean
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class NccFeature implements TupleDesc<NccFeature> {
	/** Mean pixel intensity   Can be used to reconstruct the original values of the template. */
	public double mean;

	/** standard deviation of pixel intensity */
	public double sigma;

	/** Storage for each element in the feature */
	public @Getter @Setter double[] data;

	public NccFeature( int numFeatures ) {
		this.data = new double[numFeatures];
	}

	protected NccFeature() {}

	@Override
	public NccFeature copy() {
		NccFeature ret = new NccFeature(data.length);
		ret.setTo(this);
		return ret;
	}

	public double get( int index ) {return data[index];}

	public void setTo( double... value ) {
		System.arraycopy(value, 0, this.data, 0, this.data.length);
	}

	public void fill( double value ) {
		Arrays.fill(this.data, value);
	}

	@Override public void setTo( NccFeature src ) {
		System.arraycopy(src.data, 0, data, 0, data.length);
		this.mean = src.mean;
		this.sigma = src.sigma;
	}

	@Override public /**/double /**/getDouble( int index ) {
		return data[index];
	}

	@Override public boolean isEquals( NccFeature tuple ) {
		if (size() != tuple.size()) {
			return false;
		}

		if (mean != tuple.mean)
			return false;

		if (sigma != tuple.sigma)
			return false;

		for (int i = 0; i < size(); i++) {
			if (data[i] != tuple.data[i])
				return false;
		}
		return true;
	}

	@Override public int size() {
		return data.length;
	}

	@Override public NccFeature newInstance() {
		return new NccFeature(data.length);
	}
}
