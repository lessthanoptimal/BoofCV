/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.android.camera2;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Specifies the ID for a camera, taking in account multi-camera systems.
 *
 * @author Peter Abeles
 */
public class CameraID {
	/** ID of this camera */
	public String id;
	/** If part of a multi-camera sensor, this is the logical ID of the group */
	public @Nullable String logical;

	public CameraID( String id ) {
		this.id = id;
	}

	public CameraID( String id, String logical ) {
		this.id = id;
		this.logical = logical;
	}

	public boolean isLogical() {
		return logical == null;
	}

	/** Returns the ID it should use when opening a camera */
	public String getOpenID() {
		return logical != null ? logical : id;
	}

	@Override public String toString() {
		if (logical == null)
			return id;
		return logical + ":" + id;
	}

	@Override public boolean equals( Object obj ) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CameraID a = (CameraID)obj;
		return id.equals(a.id) && Objects.equals(logical, a.logical);
	}

	@Override public int hashCode() {
		int hash = logical == null ? 0 : logical.hashCode();
		return hash | id.hashCode();
	}
}
