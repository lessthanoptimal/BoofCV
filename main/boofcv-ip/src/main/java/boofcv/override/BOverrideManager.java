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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/// Provides functions for managing overridden functions
public class BOverrideManager {

	public static final List<Class> list = new ArrayList<>();

	public static synchronized void register( Class target ) {
		if (BOverrideClass.class.isAssignableFrom(target)) {
			list.add(target);
		} else {
			throw new RuntimeException("Expected a class derived from " + BOverrideClass.class.getSimpleName());
		}
	}

	/// Removes every override, so BoofCV uses its own implementations again. Sets each public static field of every
	/// registered class to null.
	public static synchronized void clear() {
		for (Class target : list) { // lint:forbidden ignore_line
			for (Field field : target.getFields()) { // lint:forbidden ignore_line
				int modifiers = field.getModifiers();
				if (!Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers) || field.getType().isPrimitive())
					continue;
				try {
					field.set(null, null);
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	/// Prints a summary of what has been overridden
	public static void print() {System.out.println("Total registered " + list.size());}

	public static void main( String[] args ) {BOverrideManager.print();}
}
