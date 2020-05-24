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

package boofcv.alg.descriptor;

import boofcv.abst.feature.associate.AssociateDescriptionSets;
import boofcv.abst.feature.describe.DescriptorInfo;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.feature.TupleDesc_F64;
import org.ddogleg.struct.FastAccess;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.List;

/**
 * Various utilities related to image features
 *
 * @author Peter Abeles
 */
public class UtilFeature {
	/**
	 * Creates a FastQueue and declares new instances of the descriptor using the provided
	 * {@link DetectDescribePoint}.  The queue will have declareInstance set to true, otherwise
	 * why would you be using this function?
	 */
	public static <TD extends TupleDesc>
	FastQueue<TD> createQueue(final DescriptorInfo<TD> detDesc , int initialMax ) {
		FastQueue<TD> ret = new FastQueue<>(detDesc::createDescription);
		ret.growArray(initialMax);
		return ret;
	}

	public static FastQueue<TupleDesc_F64> createQueueF64( final int length ) {
		return new FastQueue<>(()->new TupleDesc_F64(length));
	}

	/**
	 * Concats the list of tuples together into one big feature.  The combined feature must be large
	 * enough to store all the inputs.
	 *
	 * @param inputs List of tuples.
	 * @param combined Storage for combined output.  If null a new instance will be declared.
	 * @return Resulting combined.
	 */
	public static TupleDesc_F64 combine( List<TupleDesc_F64> inputs , TupleDesc_F64 combined ) {
		int N = 0;
		for (int i = 0; i < inputs.size(); i++) {
			N += inputs.get(i).size();
		}
		if( combined == null ) {
			combined = new TupleDesc_F64(N);
		} else {
			if (N != combined.size())
				throw new RuntimeException("The combined feature needs to be " + N + "  not " + combined.size());
		}

		int start = 0;
		for (int i = 0; i < inputs.size(); i++) {
			double v[] = inputs.get(i).value;
			System.arraycopy(v,0,combined.value,start,v.length);
			start += v.length;
		}

		return combined;
	}

	/**
	 * <p>
	 * Normalized the tuple such that the L2-norm is equal to 1.  This is also often referred to as
	 * the Euclidean or frobenius (all though that's a matrix norm).
	 * </p>
	 *
	 * <p>
	 * value[i] = value[i]/sqrt(sum(value[j]*value[j], for all j))
	 * </p>
	 *
	 * @param desc tuple
	 */
	public static void normalizeL2( TupleDesc_F64 desc ) {
		double norm = 0;
		for (int i = 0; i < desc.size(); i++) {
			double v = desc.value[i];
			norm += v*v;
		}
		if( norm == 0 )
			return;

		norm = Math.sqrt(norm);
		for (int i = 0; i < desc.size(); i++) {
			desc.value[i] /= norm;
		}
	}

	/**
	 * <p>
	 * Normalized the tuple such that it's sum is equal to 1.
	 * </p>
	 *
	 * <p>
	 * value[i] = value[i]/sqrt(sum(value[j], for all j))
	 * </p>
	 *
	 * @param desc tuple
	 */
	public static void normalizeSumOne( TupleDesc_F64 desc ) {
		double sum = 0;
		for (int i = 0; i < desc.size(); i++) {
			double v = desc.value[i];
			sum += v;
		}
		if( sum == 0 )
			return;

		for (int i = 0; i < desc.size(); i++) {
			desc.value[i] /= sum;
		}
	}

	/**
	 * Adds the feature descriptor and its set to the association algorithm
	 * @param descriptors (Input) Descriptor of each feature
	 * @param sets (Input) Set each feature belongs in
	 * @param association (Output) association algorithm which is having its source configured
	 */
	public static <TD extends TupleDesc>
	void setSource(FastAccess<TD> descriptors , GrowQueue_I32 sets , AssociateDescriptionSets<TD> association )
	{
		assert(descriptors.size==sets.size);

		association.clearSource();
		final int N = descriptors.size;
		for (int i = 0; i < N; i++) {
			association.addSource(descriptors.data[i],sets.data[i]);
		}
	}

	/**
	 * Adds the feature descriptor and its set to the association algorithm
	 * @param descriptors (Input) Descriptor of each feature
	 * @param sets (Input) Set each feature belongs in
	 * @param association (Output) association algorithm which is having its destination configured
	 */
	public static <TD extends TupleDesc>
	void setDestination(FastAccess<TD> descriptors , GrowQueue_I32 sets , AssociateDescriptionSets<TD> association )
	{
		assert(descriptors.size==sets.size);

		association.clearDestination();
		final int N = descriptors.size;
		for (int i = 0; i < N; i++) {
			association.addDestination(descriptors.data[i],sets.data[i]);
		}
	}
}
