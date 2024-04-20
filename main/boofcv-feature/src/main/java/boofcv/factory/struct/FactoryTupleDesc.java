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

package boofcv.factory.struct;

import boofcv.abst.feature.describe.DescriptorInfo;
import boofcv.struct.PackedArray;
import boofcv.struct.feature.*;

/**
 * Factory for creating {@link TupleDesc} and related structures abstractly.
 *
 * @author Peter Abeles
 **/
@SuppressWarnings({"unchecked"})
public class FactoryTupleDesc {
	public static <TD extends TupleDesc<TD>> TD createTuple( int dof, Class<TD> type ) {
		if (type == TupleDesc_F64.class)
			return (TD)new TupleDesc_F64(dof);
		else if (type == TupleDesc_F32.class)
			return (TD)new TupleDesc_F32(dof);
		else if (type == TupleDesc_U8.class)
			return (TD)new TupleDesc_U8(dof);
		else if (type == TupleDesc_B.class)
			return (TD)new TupleDesc_B(dof);
		else
			throw new IllegalArgumentException("Unknown type " + type);
	}

	public static <TD extends TupleDesc<TD>> PackedArray<TD> createPacked( DescriptorInfo<TD> info ) {
		int dof = info.createDescription().size();
		return createPacked(dof, info.getDescriptionType());
	}

	public static <TD extends TupleDesc<TD>> PackedTupleArray<TD> createPacked( int dof, Class<TD> type ) {
		if (type == TupleDesc_F64.class)
			return (PackedTupleArray<TD>)new PackedTupleArray_F64(dof);
		else if (type == TupleDesc_F32.class)
			return (PackedTupleArray<TD>)new PackedTupleArray_F32(dof);
		else if (type == TupleDesc_U8.class)
			return (PackedTupleArray<TD>)new PackedTupleArray_U8(dof);
		else if (type == TupleDesc_B.class)
			return (PackedTupleArray<TD>)new PackedTupleArray_B(dof);
		else
			throw new IllegalArgumentException("Unknown type " + type);
	}

	public static <TD extends TupleDesc<TD>> PackedArray<TD> createPackedBig( DescriptorInfo<TD> info ) {
		int dof = info.createDescription().size();
		return createPackedBig(dof, info.getDescriptionType());
	}

	public static <TD extends TupleDesc<TD>> PackedTupleArray<TD> createPackedBig( int dof, Class<TD> type ) {
		if (type == TupleDesc_F64.class)
			return (PackedTupleArray<TD>)new PackedTupleBigArray_F64(dof);
		else if (type == TupleDesc_F32.class)
			return (PackedTupleArray<TD>)new PackedTupleBigArray_F32(dof);
		else if (type == TupleDesc_U8.class)
			return (PackedTupleArray<TD>)new PackedTupleBigArray_U8(dof);
		else if (type == TupleDesc_B.class)
			return (PackedTupleArray<TD>)new PackedTupleBigArray_B(dof);
		else
			throw new IllegalArgumentException("Unknown type " + type);
	}
}
