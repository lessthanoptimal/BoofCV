/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.concurrency;

import boofcv.misc.CodeGeneratorBase;

import java.io.FileNotFoundException;

/**
 * @author Peter Abeles
 */
public class GenerateWorkArrays extends CodeGeneratorBase {
	public GenerateWorkArrays() {
		super(false);
	}

	@Override
	public void generate() throws FileNotFoundException {
		create("I","int");
		create("L","long");
		create("D","double");
		create("F","float");
	}

	protected void create( String letter , String type ) throws FileNotFoundException {
		super.className = null; // need to do this to avoid sanity check
		setOutputFile(letter+"WorkArrays");

		String name = letter+"WorkArrays";

		out.print("import java.util.ArrayList;\n" +
				"import java.util.List;\n" +
				"\n" +
				"/**\n" +
				" * Thread safe way to recycle work arrays and maximize memory reuse\n" +
				" *\n" +
				" * @author Peter Abeles\n" +
				" */\n" +
				"public class "+name+" implements WorkArrays {\n" +
				"\tList<"+type+"[]> storage = new ArrayList<>();\n" +
				"\tint length;\n" +
				"\n" +
				"\tpublic "+name+"(int length) {\n" +
				"\t\tthis.length = length;\n" +
				"\t}\n" +
				"\n" +
				"\tpublic "+name+"() {\n" +
				"\t}\n" +
				"\n" +
				"\t/**\n" +
				"\t * Checks to see if the stored arrays have the specified length. If not the length is changed and old\n" +
				"\t * arrays are purged\n" +
				"\t * @param length Desired array length\n" +
				"\t */\n" +
				"\tpublic synchronized void reset( int length ) {\n" +
				"\t\tif( this.length != length ) {\n" +
				"\t\t\tthis.length = length;\n" +
				"\t\t\tstorage.clear();\n" +
				"\t\t}\n" +
				"\t}\n" +
				"\n" +
				"\t/**\n" +
				"\t * If there are arrays in storage one of them is returned, otherwise a new array is returned\n" +
				"\t */\n" +
				"\tpublic synchronized "+type+"[] pop() {\n" +
				"\t\tif( storage.isEmpty() ) {\n" +
				"\t\t\treturn new "+type+"[length];\n" +
				"\t\t} else {\n" +
				"\t\t\treturn storage.remove(storage.size()-1);\n" +
				"\t\t}\n" +
				"\t}\n" +
				"\n" +
				"\t/**\n" +
				"\t * Adds the array to storage. if the array length is unexpected an exception is thrown\n" +
				"\t * @param array array to be recycled.\n" +
				"\t */\n" +
				"\tpublic synchronized void recycle( "+type+"[] array ) {\n" +
				"\t\tif( array.length != length ) {\n" +
				"\t\t\tthrow new IllegalArgumentException(\"Unexpected array length. Expected \"+length+\" found \"+array.length);\n" +
				"\t\t}\n" +
				"\t\tstorage.add(array);\n" +
				"\t}\n" +
				"\n" +
				"\t/**\n" +
				"\t * Length of arrays returned\n" +
				"\t */\n" +
				"\tpublic int length() {\n" +
				"\t\treturn length;\n" +
				"\t}\n" +
				"}\n");
	}

	public static void main(String args[]) throws FileNotFoundException {
		GenerateWorkArrays a = new GenerateWorkArrays();

		a.generate();
	}
}
