/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.examples;

import boofcv.alg.bow.LearnSceneFromFiles;
import boofcv.gui.ApplicationLauncherApp;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Application which lists most of the demonstration application in a GUI and allows the user to double click
 * to launch one in a new JVM.
 *
 * @author Peter Abeles
 */
public class ExampleLauncherApp extends ApplicationLauncherApp {

	public ExampleLauncherApp() {
		super(false);
	}

	@Override
	protected void createTree(DefaultMutableTreeNode root) {
		List<String> packages = new ArrayList<>();
		packages.add("boofcv.examples.calibration");
		packages.add("boofcv.examples.enhance");
		packages.add("boofcv.examples.features");
		packages.add("boofcv.examples.fiducial");
		packages.add("boofcv.examples.geometry");
		packages.add("boofcv.examples.imageprocessing");
		packages.add("boofcv.examples.recognition");
		packages.add("boofcv.examples.segmentation");
		packages.add("boofcv.examples.sfm");
		packages.add("boofcv.examples.stereo");
		packages.add("boofcv.examples.tracking");

		// Reflections is a weird package that does not behave the way one would expect.  Several hacks below
		for( String p : packages ) {
			Reflections reflections = new Reflections(p,
					new SubTypesScanner(false));

			List<String> listTypes = new ArrayList<>();

			listTypes.addAll(reflections.getAllTypes());
			addAll((Set)reflections.getSubTypesOf(LearnSceneFromFiles.class),listTypes);

			String name = p.split("\\.")[2];

			Collections.sort(listTypes);

			List<Class> classes = new ArrayList<>();
			String classNames[] = listTypes.toArray(new String[1]);
			for( int i = 0; i < classNames.length; i++ ) {
				if( !classNames[i].contains("Example"))
					continue;
				// no idea why this is needed
				if( classNames[i].contains("$"))
					continue;

				try {
					classes.add( Class.forName(classNames[i]));
				} catch (ClassNotFoundException e) {
					throw new RuntimeException(e);
				}
			}

			createNodes(root,name,classes.toArray(new Class[0]));
		}
	}

	private static void addAll( Set<Class> classes , List<String> output ) {
		for( Class c : classes ) {
			output.add( c.getName() );
		}
	}

	public static void main(String[] args) {
		ExampleLauncherApp app = new ExampleLauncherApp();
		app.showWindow("Example Launcher");
	}
}
