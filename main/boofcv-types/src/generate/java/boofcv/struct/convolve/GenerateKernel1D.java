/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

package boofcv.struct.convolve;

import boofcv.generate.AutoTypeImage;
import boofcv.generate.CodeGeneratorBase;

import java.io.FileNotFoundException;

@SuppressWarnings("OrphanedFormatString")
public class GenerateKernel1D extends CodeGeneratorBase {
	@Override
	public void generateCode() throws FileNotFoundException {
		createFile(AutoTypeImage.F32);
		createFile(AutoTypeImage.F64);
		createFile(AutoTypeImage.S32);
	}

	private void createFile( AutoTypeImage imageType ) throws FileNotFoundException  {
		String suffix = imageType.getAbbreviatedType();
		suffix = suffix.compareTo("S32") == 0 ? "I32" : suffix;
		className = "Kernel1D_"+suffix;

		String sumType = imageType.getSumType();

		setOutputFile(className);
		out.print("/**\n" +
				" * Floating point 1D convolution kernel that extends {@link Kernel1D}.\n" +
				" *\n" +
				" * <p>\n" +
				" * WARNING: Do not modify. Automatically generated by " + getClass().getSimpleName() + ".\n" +
				" * </p>\n" +
				" *\n" +
				" * @author Peter Abeles\n" +
				" */\n" +
				"public class " + className + " extends Kernel1D {\n" +
				"\n" +
				"\tpublic " + sumType + " data[];\n" +
				"\n" +
				"\t/**\n" +
				"\t * Creates a new kernel whose initial values are specified by \"data\" and length is \"width\". \n" +
				"\t * The offset will be set to width/2\n" +
				"\t *\n" +
				"\t * @param data  The value of the kernel. Not modified. Reference is not saved.\n" +
				"\t * @param width The kernels width.\n" +
				"\t */\n" +
				"\tpublic " + className + "(" + sumType + " data[], int width) {\n" +
				"\t\tthis(data,width,width/2);\n" +
				"\t}\n" +
				"\n" +
				"\t/**\n" +
				"\t * Creates a kernel with elements equal to 'data' and with the specified 'width' plus 'offset'\n" +
				"\t *\n" +
				"\t * @param data  The value of the kernel. Not modified. Reference is not saved.\n" +
				"\t * @param width The kernels width.\n" +
				"\t * @param offset Location of the origin in the array\n" +
				"\t */\n" +
				"\tpublic " + className + "(" + sumType + " data[], int width , int offset) {\n" +
				"\t\tsuper(width,offset);\n" +
				"\n" +
				"\t\tthis.data = new " + sumType + "[width];\n" +
				"\t\tSystem.arraycopy(data, 0, this.data, 0, width);\n" +
				"\t}\n" +
				"\n" +
				"\t/**\n" +
				"\t * Create a kernel with elements initialized to zero. Offset is automatically\n" +
				"\t * set to width/2.\n" +
				"\t *\n" +
				"\t * @param width How wide the kernel is. \n" +
				"\t */\n" +
				"\tpublic " + className + "(int width) {\n" +
				"\t\tthis(width,width/2);\n" +
				"\t}\n" +
				"\n" +
				"\t/**\n" +
				"\t * Create a kernel whose elements initialized to zero.\n" +
				"\t *\n" +
				"\t * @param width How wide the kernel is.\n" +
				"\t * @param offset Location of the origin in the array\n" +
				"\t */\n" +
				"\tpublic " + className + "(int width , int offset) {\n" +
				"\t\tsuper(width,offset);\n" +
				"\t\tdata = new " + sumType + "[width];\n" +
				"\t}\n" +
				"\n" +
				"\tprotected " + className + "() {\n" +
				"\t}\n" +
				"\n" +
				"\t@Override\n" +
				"\tpublic double getDouble(int index) {\n" +
				"\t\treturn data[index];\n" +
				"\t}\n" +
				"\n" +
				"\t/**\n" +
				"\t * Creates a kernel whose elements are the specified data array and has\n" +
				"\t * the specified width.\n" +
				"\t *\n" +
				"\t * @param data  The array who will be the kernel's data. Reference is saved.\n" +
				"\t * @param width The kernel's width.\n" +
				"\t * @param offset Location of the origin in the array\n" +
				"\t * @return A new kernel.\n" +
				"\t */\n" +
				"\tpublic static " + className + " wrap(" + sumType + " data[], int width, int offset ) {\n" +
				"\t\t" + className + " ret = new " + className + "();\n" +
				"\t\tret.data = data;\n" +
				"\t\tret.width = width;\n" +
				"\t\tret.offset = offset;\n" +
				"\n" +
				"\t\treturn ret;\n" +
				"\t}\n" +
				"\n" +
				"\t@Override\n" +
				"\tpublic " + className + " copy() {\n" +
				"\t\t"+className+" ret = new "+className+"(width,offset);\n" +
				"\t\tSystem.arraycopy(data,0,ret.data,0,ret.width);\n" +
				"\t\treturn ret;\n" +
				"\t}\n" +
				"\n" +
				"\t@Override\n" +
				"\tpublic boolean isInteger() {\n" +
				"\t\treturn " + imageType.isInteger() + ";\n" +
				"\t}\n" +
				"\n" +
				"\tpublic " + sumType + " get(int i) {\n" +
				"\t\treturn data[i];\n" +
				"\t}\n" +
				"\n" +
				"\tpublic " + sumType + " computeSum() {\n" +
				"\t\t" + sumType + " sum = 0;\n" +
				"\t\tfor( int i = 0; i < data.length; i++ ) {\n" +
				"\t\t\tsum += data[i];\n" +
				"\t\t}\n" +
				"\n" +
				"\t\treturn sum;\n" +
				"\t}\n" +
				"\n" +
				"\tpublic " + sumType + "[] getData() {\n" +
				"\t\treturn data;\n" +
				"\t}\n" +
				"\n" +
				"\tpublic void print() {\n" +
				"\t\tfor (int i = 0; i < width; i++) {\n");
		if( imageType.isInteger() )
			out.print("\t\t\tSystem.out.printf(\"%6d \", data[i]);\n");
		else
			out.print("\t\t\tSystem.out.printf(\"%6.3f \", data[i]);\n");

		out.print("\t\t}\n" +
				"\t\tSystem.out.println();\n" +
				"\t}\n" +
				"}\n\n");

	}

	public static void main( String[] args ) throws FileNotFoundException {
		GenerateKernel1D app = new GenerateKernel1D();
		app.generateCode();
	}
}
