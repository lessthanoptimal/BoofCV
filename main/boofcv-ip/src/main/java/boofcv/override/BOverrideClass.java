/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

package boofcv.override;

import boofcv.core.image.ImageBorderValue;
import boofcv.struct.border.BorderType;
import boofcv.struct.border.ImageBorder;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/// Base class for override operations. Used to help identify overrides.
///
/// @author Peter Abeles
public class BOverrideClass {
	/// True if the border can be passed to an override
	public static boolean canOverride( @Nullable ImageBorder<?> border ) {
		if (border == null)
			return true;
		if (border.type == null)
			return false;
		return border.type != BorderType.ZERO || isValueZero(border);
	}

	/// Border type passed to an override, SKIP if there's no border. The border must pass [#canOverride]
	public static BorderType overrideBorderOrSkip( @Nullable ImageBorder<?> border ) {
		return border == null ? BorderType.SKIP : Objects.requireNonNull(border.type);
	}

	/// False for a value border that isn't zero
	static boolean isValueZero( ImageBorder<?> border ) {
		if (border instanceof ImageBorderValue.Value_I b) return b.value == 0;
		if (border instanceof ImageBorderValue.Value_I64 b) return b.value == 0;
		if (border instanceof ImageBorderValue.Value_F32 b) return b.value == 0;
		if (border instanceof ImageBorderValue.Value_F64 b) return b.value == 0;
		if (border instanceof ImageBorderValue.Value_IL_S32 b) return b.value == 0;
		if (border instanceof ImageBorderValue.Value_IL_S64 b) return b.value == 0;
		if (border instanceof ImageBorderValue.Value_IL_F32 b) return b.value == 0;
		if (border instanceof ImageBorderValue.Value_IL_F64 b) return b.value == 0;
		return true;
	}
}
