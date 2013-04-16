/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.applet;

import boofcv.alg.filter.derivative.GImageDerivativeOps;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


/**
 * @author Peter Abeles
 */
public class FactoryVisualPanel {

	/**
	 * Creates a new instance of the component.  Looks for a constructor
	 * which takes in one or two image types.
	 */
	public static JComponent create( String path , Class imageType )
	{
		try {
			Class<JComponent> compType = (Class<JComponent>)Class.forName(path);

			return findConstructor(compType,imageType);

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.err.println("Couldn't find path name");
			return null;
		} catch (InstantiationException e) {
			System.err.println("Couldn't create a new instance from constructor");
			return null;
		} catch (IllegalAccessException e) {
			System.err.println("IllegalAccessException");
			return null;
		} catch (InvocationTargetException e) {
			System.err.println("InvocationTargetException");
			return null;
		}
	}

	/**
	 * Invokes process() with a single image.
	 */
	public static boolean invokeProcess( JComponent comp , BufferedImage image ) {
		try {
			Method methods[] = comp.getClass().getMethods();
			for( Method m : methods ) {
				Class<?> params [] = m.getParameterTypes();
				if( params.length != 1 || m.getName().compareTo("process") != 0)
					continue;
				if( BufferedImage.class.isAssignableFrom(params[0])) {
					m.invoke(comp,image);
					return true;
				}
			}
			return false;
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Find a constructor with image types as input
	 */
	private static JComponent findConstructor(Class<JComponent> compType , Class imageType )
			throws InvocationTargetException, IllegalAccessException, InstantiationException {
		Constructor<?> cs[] = compType.getConstructors();
		for( Constructor<?> c : cs ) {
			Class<?> params[] = c.getParameterTypes();

			if( params.length == 1 || params.length == 2  ) {
				boolean allGood = true;
				for( int i = 0; i < params.length; i++ ) {
					if( params[i] != Class.class ) {
						allGood = false;
						break;
					}
				}
				if( allGood ) {
					if( params.length == 1 ) {
						return (JComponent)c.newInstance(imageType);
					} else {
						Class derivType = GImageDerivativeOps.getDerivativeType(imageType);
						return (JComponent)c.newInstance(imageType,derivType);
					}
				}
			}
		}

		// try a no argument constructor instead
		for( Constructor<?> c : cs ) {
			Class<?> params[] = c.getParameterTypes();

			if( params.length == 0 ) {
				return (JComponent)c.newInstance();
			}
		}

		System.err.println("Couldn't find an appropriate constructor.");
		return null;
	}
}
