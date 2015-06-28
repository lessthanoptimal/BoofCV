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

package boofcv.app;

/**
 * Application which provides help for command-line applications
 *
 * @author Peter Abeles
 */
public class Help {
	public static void main(String[] args) {
		Class []options = new Class[]{CreateFiducialSquareImageEPS.class};

		System.out.println("Trying to run a command-line application?  Here are your options!");
		System.out.println();

		for( Class c : options ) {
			System.out.println("  "+c.getName());
		}
		System.out.println("Example:");
		System.out.println("java -cp applications.jar "+CreateFiducialSquareImageEPS.class.getName());
	}
}
