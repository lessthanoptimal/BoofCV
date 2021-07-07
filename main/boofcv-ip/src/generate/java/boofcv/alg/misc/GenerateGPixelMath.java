/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.misc;

import boofcv.generate.AutoTypeImage;
import boofcv.generate.CodeGeneratorBase;

import java.io.FileNotFoundException;

import static boofcv.generate.AutoTypeImage.*;

/**
 * @author Peter Abeles
 */
public class GenerateGPixelMath extends CodeGeneratorBase {

	private boolean singleBand;
	private AutoTypeImage input;
	private AutoTypeImage output;

	private final AutoTypeImage[] standardTypes = new AutoTypeImage[]{U8, S8, U16, S16, S32, S64, F32, F64};
	private final AutoTypeImage[] floatTypes = new AutoTypeImage[]{F32, F64};

	private final AutoTypeImage[] signedTypes = new AutoTypeImage[]{S8, S16, S32, S64, F32, F64};
//	private AutoTypeImage[] signedOutputTypes = new AutoTypeImage[]{U8,U16,S32,S64,F32,F64};

	@Override
	public void generateCode() throws FileNotFoundException {
		printPreamble();

		printFunction(operator1(), AutoTypeImage.getGenericTypes());
		printFunction2same(operator2(), AutoTypeImage.getGenericTypes());
		printFunction(abs(), signedTypes, signedTypes);
		printFunction(negative(), signedTypes, signedTypes);
		printFunction2(divide_ISI(false), standardTypes);
		printFunction(divide_ISI(true), standardTypes);
		printFunction2(multiply_ISI(false), standardTypes);
		printFunction(multiply_ISI(true), standardTypes);
		printFunction2(plus_ISI(false), standardTypes);
		printFunction(plus_ISI(true), standardTypes);
		printFunction2(minus_ISI(false), standardTypes);
		printFunction(minus_ISI(true), standardTypes);
		printFunction(minus_SII(false), standardTypes);
		printFunction(minus_SII(true), standardTypes);
		printFunction(log_ISI(false), floatTypes);
		printFunction(logSign_ISI(false), floatTypes);
		printFunction(sqrt(), floatTypes);

		printOtherCrap();

		out.println("}");
	}

	private void printPreamble() {
		out.print("import boofcv.struct.image.*;\n" +
				"\n" +
				"import boofcv.alg.misc.PixelMathLambdas.*;\n" +
				"import boofcv.alg.misc.impl.ImplPixelMath_MT;\n" +
				"import boofcv.alg.misc.impl.ImplPixelMath;\n" +
				"import boofcv.concurrency.BoofConcurrency;\n" +
				"import boofcv.alg.InputSanityCheck;\n" +
				"import javax.annotation.Generated;\n" +
				"\n" +
				"/**\n" +
				" * Generalized version of {@link PixelMath}. Type checking is performed at runtime instead of at compile type.\n" +
				generateDocString("Peter Abeles") +
				"public class " + className + " {\n" +
				"\n");
	}


	private void printFunction( Function f, AutoTypeImage[] types ) {
		printFunction(f, types, types);
	}

	private void printFunction( Function f, AutoTypeImage[] typesIn, AutoTypeImage[] typesOut ) {
		out.print(
				"\t/**\n" + f.printJavadoc() +
						"\t */\n" +
						"\tpublic static " + f.printGenerics() + " void " + f.name + "( " + f.printArguments() + " )\n" +
						"\t{\n" +
						"\t\tif( input instanceof ImageGray) {\n");
		singleBand = true;
		printSwitch(f, typesIn, typesOut, "input", "\t\t\t");
		out.print(
				"\t\t} else if( input instanceof ImageInterleaved ) {\n");
		singleBand = false;
		printSwitch(f, typesIn, typesOut, "input", "\t\t\t");
		out.print("\t\t} else if( input instanceof Planar ) {\n" +
				"\t\t\tPlanar in = (Planar)input;\n" +
				"\t\t\tPlanar out = (Planar)output;\n" +
				"\n" +
				"\t\t\tfor (int i = 0; i < in.getNumBands(); i++) {\n" +
				"\t\t\t\t" + f.name + "(" + f.printCallPlanar() + ");\n" +
				"\t\t\t}\n" +
				"\t\t} else {\n" +
				"\t\t\tthrow new IllegalArgumentException(\"Unknown image Type: \"+input.getClass().getSimpleName());\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printFunction2same( Function f, AutoTypeImage[] typesIn) {
		out.print(
				"\t/**\n" + f.printJavadoc() +
						"\t */\n" +
						"\tpublic static " + f.printGenerics() + " void " + f.name + "( " + f.printArguments() + " )\n" +
						"\t{\n" +
						"\t\tif( imgA instanceof ImageGray) {\n");
		singleBand = true;
		printSwitchAssignable(f, typesIn, typesIn, "imgA", "\t\t\t");
		out.print(
				"\t\t} else if( imgA instanceof ImageInterleaved ) {\n");
		singleBand = false;
		printSwitchAssignable(f, typesIn, typesIn, "imgA", "\t\t\t");
		out.print("\t\t} else if( imgA instanceof Planar ) {\n" +
				"\t\t\tPlanar _imgA = (Planar)imgA;\n" +
				"\t\t\tPlanar _imgB = (Planar)imgB;\n" +
				"\t\t\tPlanar out = (Planar)output;\n" +
				"\n" +
				"\t\t\tfor (int i = 0; i < _imgA.getNumBands(); i++) {\n" +
				"\t\t\t\t" + f.name + "(" + f.printCallPlanar() + ");\n" +
				"\t\t\t}\n" +
				"\t\t} else {\n" +
				"\t\t\tthrow new IllegalArgumentException(\"Unknown image Type: \"+imgA.getClass().getSimpleName());\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	private void printFunction2( Function f, AutoTypeImage[] typesIn ) {
		out.print(
				"\t/**\n" + f.printJavadoc() +
						"\t */\n" +
						"\tpublic static " + f.printGenerics() + " void " + f.name + "( " + f.printArguments() + " )\n" +
						"\t{\n" +
						"\t\tif( input instanceof ImageGray) {\n" +
						"\t\t\tif( input.getClass() == output.getClass() ) {\n");
		singleBand = true;
		printSwitch(f, typesIn, typesIn, "input", "\t\t\t\t");
		out.print("\t\t\t} else if( GrayF32.class == output.getClass() ) {\n");
		printSwitch(f, typesIn, F32, "\t\t\t\t");
		out.print(
				"\t\t\t}\n" +
						"\t\t} else if( input instanceof ImageInterleaved ) {\n" +
						"\t\t\tif( input.getClass() == output.getClass() ) {\n");
		singleBand = false;
		printSwitch(f, typesIn, typesIn, "input", "\t\t\t\t\t");
		out.print("\t\t\t} else if( InterleavedF32.class == output.getClass() ) {\n");
		printSwitch(f, typesIn, F32, "\t\t\t\t");
		out.print(
				"\t\t\t}\n" +
						"\t\t} else if( input instanceof Planar ) {\n" +
						"\t\t\tPlanar in = (Planar)input;\n" +
						"\t\t\tPlanar out = (Planar)output;\n" +
						"\n" +
						"\t\t\tfor (int i = 0; i < in.getNumBands(); i++) {\n" +
						"\t\t\t\t" + f.name + "(" + f.printCallPlanar() + ");\n" +
						"\t\t\t}\n" +
						"\t\t} else {\n" +
						"\t\t\tthrow new IllegalArgumentException(\"Unknown image Type: \"+input.getClass().getSimpleName());\n" +
						"\t\t}\n" +
						"\t}\n\n");
	}

	private void printSwitchAssignable( Function f, AutoTypeImage[] typesIn, AutoTypeImage[] typesOut, String imgName, String prefix ) {
		for (int i = 0; i < typesIn.length; i++) {
			input = typesIn[i];
			output = typesOut[i];
			if (i == 0) {
				out.print(prefix + "if (" + name(input) + ".class.isAssignableFrom("+imgName+".getClass())) {\n" +
						prefix + "\tPixelMath." + f.name + "(" + f.printCall() + ");\n");
			} else {
				out.print(prefix + "} else if (" + name(input) + ".class.isAssignableFrom("+imgName+".getClass())) {\n" +
						prefix + "\tPixelMath." + f.name + "(" + f.printCall() + ");\n");
			}
		}
		out.print(prefix + "} else {\n" +
				prefix + "\tthrow new IllegalArgumentException(\"Unknown image Type: \" + "+imgName+".getClass().getSimpleName());\n" +
				prefix + "}\n");
	}

	private void printSwitch( Function f, AutoTypeImage[] typesIn, AutoTypeImage[] typesOut, String imgName, String prefix ) {
		for (int i = 0; i < typesIn.length; i++) {
			input = typesIn[i];
			output = typesOut[i];
			if (i == 0) {
				out.print(prefix + "if (" + name(input) + ".class == "+imgName+".getClass()) {\n" +
						prefix + "\tPixelMath." + f.name + "(" + f.printCall() + ");\n");
			} else {
				out.print(prefix + "} else if (" + name(input) + ".class == "+imgName+".getClass()) {\n" +
						prefix + "\tPixelMath." + f.name + "(" + f.printCall() + ");\n");
			}
		}
		out.print(prefix + "} else {\n" +
				prefix + "\tthrow new IllegalArgumentException(\"Unknown image Type: \" + "+imgName+".getClass().getSimpleName());\n" +
				prefix + "}\n");
	}

	private void printSwitch( Function f, AutoTypeImage[] typesIn, AutoTypeImage typeOut, String prefix ) {
		for (int i = 0; i < typesIn.length; i++) {
			input = typesIn[i];
			output = typeOut;

			if (!input.isInteger()) // TODO hack. assumes what this function is for...
				continue;

			if (i == 0) {
				out.print(prefix + "if (" + name(input) + ".class == input.getClass()) {\n" +
						prefix + "\tPixelMath." + f.name + "(" + f.printCall() + ");\n");
			} else {
				out.print(prefix + "} else if (" + name(input) + ".class == input.getClass()) {\n" +
						prefix + "\tPixelMath." + f.name + "(" + f.printCall() + ");\n");
			}
		}
		out.print(prefix + "} else {\n" +
				prefix + "\tthrow new IllegalArgumentException(\"Unknown image Type: \" + input.getClass().getSimpleName());\n" +
				prefix + "}\n");
	}

	public String name( AutoTypeImage t ) {
		if (singleBand)
			return t.getSingleBandName();
		else
			return t.getInterleavedName();
	}

	public String typecastSum() {
		if (input.isInteger()) {
			if (input.getNumBits() <= 32)
				return "(int)";
			else
				return "(long)";
		} else if (input.getNumBits() <= 32) {
			return "(float)";
		} else {
			return "";
		}
	}

	public Function operator1() {
		Function f = new Function() {
			@Override
			public String printJavadoc() {
				return "\t * Applies the lambda operation to each element in the input image. output[i] = function(input[i])\n" +
						"\t * Both the input and output image can be the same instance.\n";
			}
			@Override
			public String printArguments() {return "T input, Function1 function, T output";}

			@Override
			public String printCall() {
				String generic = input.getGenericAbbreviated();
				return String.format("(%s) input, (Function1_%s)function, (%s) output", name(input), generic, name(output));
			}

			@Override
			public String printCallPlanar() {return "in.getBand(i), function, out.getBand(i)";}
		};
		f.name = "operator1";
		return f;
	}

	public Function operator2() {
		Function f = new Function() {
			@Override
			public String printJavadoc() {
				return "\t * Applies the lambda operation to each element in the two input images. output[i] = function(imA[i],imgB[i])\n" +
						"\t * Both the imgA, imgB, and output images can be the same instance.\n";
			}
			@Override
			public String printArguments() {return "T imgA, Function2 function, T imgB, T output";}

			@Override
			public String printCall() {
				String generic = input.getGenericAbbreviated();
				return String.format("(%s) imgA, (Function2_%s)function, (%s) imgB, (%s) output", name(input), generic, name(input), name(output));
			}

			@Override
			public String printCallPlanar() {return "_imgA.getBand(i), function, _imgB.getBand(i), out.getBand(i)";}
		};
		f.name = "operator2";
		return f;
	}

	public Function abs() {
		Function f = new FunctionTwo() {
			@Override
			public String printJavadoc() {
				return "\t * Sets each pixel in the output image to be the absolute value of the input image.\n" +
						"\t * Both the input and output image can be the same instance.\n" +
						super.printJavadoc();
			}
		};
		f.name = "abs";
		return f;
	}

	public Function negative() {
		Function f = new FunctionTwo() {
			@Override
			public String printJavadoc() {
				return "\t * Changes the sign of every pixel in the image: output[x,y] = -input[x,y]\n" +
						super.printJavadoc();
			}
		};
		f.name = "negative";
		return f;
	}

	public Function divide_ISI( boolean bounded ) {
		FunctionISI f = new FunctionISI(bounded) {
			@Override
			public String printJavadoc() {
				return "\t * Divide each element by a scalar value. Both input and output images can be the same instance.\n" +
						super.printJavadoc();
			}
		};
		f.onlyFloat = true;
		f.name = "divide";
		f.scalarName = "denominator";
		f.scalarJavadoc = "What each element is divided by.";
		return f;
	}

	public Function multiply_ISI( boolean bounded ) {
		FunctionISI f = new FunctionISI(bounded) {
			@Override
			public String printJavadoc() {
				return "\t * Multiply each element by a scalar value. Both input and output images can\n" +
						"\t * be the same instance.\n" +
						super.printJavadoc();
			}
		};
		f.onlyFloat = true;
		f.name = "multiply";
		f.scalarName = "value";
		f.scalarJavadoc = "What each element is multiplied by.";
		return f;
	}

	public Function plus_ISI( boolean bounded ) {
		FunctionISI f = new FunctionISI(bounded) {
			@Override
			public String printJavadoc() {
				return "\t * Each element has the specified number added to it. Both input and output images can\n" +
						"\t * be the same.\n" +
						super.printJavadoc();
			}
		};
		f.name = "plus";
		f.scalarName = "value";
		f.scalarJavadoc = "What is added to each element.";
		return f;
	}

	public Function minus_ISI( boolean bounded ) {
		FunctionISI f = new FunctionISI(bounded) {
			@Override
			public String printJavadoc() {
				return "\t * Subtracts a scalar value from each element. Both input and output images can be the same instance.\n" +
						super.printJavadoc();
			}
		};
		f.name = "minus";
		f.scalarName = "value";
		f.scalarJavadoc = "What is subtracted from each element in input.";
		return f;
	}

	public Function minus_SII( boolean bounded ) {
		FunctionSII f = new FunctionSII(bounded) {
			@Override
			public String printJavadoc() {
				return "\t * Subtracts the value of each element from a scalar value. Both input and output images can be the same instance.\n" +
						"\t * output = value - input\n" +
						super.printJavadoc();
			}
		};
		f.name = "minus";
		f.scalarName = "value";
		f.scalarJavadoc = "Left side of equation.";
		return f;
	}

	public Function log_ISI( boolean bounded ) {
		FunctionISI f = new FunctionISI(bounded) {
			@Override
			public String printJavadoc() {
				return "\t * Sets each pixel in the output image to log( val + input(x,y)) of the input image.\n" +
						"\t * Both the input and output image can be the same instance.\n" +
						super.printJavadoc();
			}
		};
		f.name = "log";
		f.scalarName = "value";
		f.scalarJavadoc = "log( value + input[x,y])";
		return f;
	}

	public Function logSign_ISI( boolean bounded ) {
		FunctionISI f = new FunctionISI(bounded) {
			@Override
			public String printJavadoc() {
				return "\t * Sets each pixel in the output image to sgn*log( val + sgn*input(x,y)) of the input image.\n" +
						"\t * where sng is the sign of input(x,y).\n" +
						super.printJavadoc();
			}
		};
		f.name = "logSign";
		f.scalarName = "value";
		f.scalarJavadoc = "sgn*log( value + sgn*input[x,y])";
		return f;
	}

	public Function sqrt() {
		Function f = new FunctionTwo() {
			@Override
			public String printJavadoc() {
				return "\t * Computes the square root of each pixel in the input image. Both the input and output image can be the\n" +
						"\t * same instance.\n" +
						super.printJavadoc();
			}
		};
		f.name = "sqrt";
		return f;
	}

	class FunctionSII extends Function {
		String scalarName;
		String scalarJavadoc;
		boolean bounded;

		public FunctionSII( boolean bounded ) {
			this.bounded = bounded;
		}

		@Override
		public String printJavadoc() {
			if (bounded) {
				return "\t *\n" +
						"\t * @param " + scalarName + " " + scalarJavadoc + "\n" +
						"\t * @param input The input image. Not modified.\n" +
						"\t * @param lower Lower bound on output. Inclusive.\n" +
						"\t * @param upper Upper bound on output. Inclusive.\n" +
						"\t * @param output The output image. Modified.\n";
			} else {
				return "\t *\n" +
						"\t * @param " + scalarName + " " + scalarJavadoc + "\n" +
						"\t * @param input The input image. Not modified.\n" +
						"\t * @param output The output image. Modified.\n";
			}
		}

		@Override
		public String printArguments() {
			if (bounded)
				return "double " + scalarName + ", T input, double lower, double upper, T output";
			else
				return "double " + scalarName + ", T input, T output";
		}

		@Override
		public String printCall() {
			String t = typecastSum();
			if (bounded)
				return String.format("%s %s, (%s) input, %s lower, %s upper, (%s) output", t, scalarName, name(input), t, t, name(output));
			else
				return String.format("%s %s, (%s) input,  (%s) output", t, scalarName, name(input), name(output));
		}

		@Override
		public String printCallPlanar() {
			if (bounded)
				return scalarName + ", in.getBand(i), lower,upper,out.getBand(i)";
			else
				return scalarName + ", in.getBand(i), out.getBand(i)";
		}
	}

	class FunctionISI extends Function {
		String scalarName;
		String scalarJavadoc;
		boolean bounded;
		boolean onlyFloat = false;

		public FunctionISI( boolean bounded ) {
			this.bounded = bounded;
		}

		@Override
		public String printGenerics() {
			return "<T extends ImageBase<T>,O extends ImageBase<O>>";
		}

		@Override
		public String printJavadoc() {
			if (bounded) {
				return "\t *\n" +
						"\t * @param input The input image. Not modified.\n" +
						"\t * @param " + scalarName + " " + scalarJavadoc + "\n" +
						"\t * @param lower Lower bound on output. Inclusive.\n" +
						"\t * @param upper Upper bound on output. Inclusive.\n" +
						"\t * @param output The output image. Modified.\n";
			} else {
				return "\t *\n" +
						"\t * @param input The input image. Not modified.\n" +
						"\t * @param " + scalarName + " " + scalarJavadoc + "\n" +
						"\t * @param output The output image. Modified.\n";
			}
		}

		@Override
		public String printArguments() {
			if (bounded)
				return "T input, double " + scalarName + ", double lower, double upper, O output";
			else
				return "T input, double " + scalarName + ", O output";
		}

		@Override
		public String printCall() {
			String t = typecastSum();
			String scalarCast;
			if (onlyFloat) {
				if (input == output)
					scalarCast = !input.isInteger() && input.getNumBits() == 32 ? "(float) " : "";
				else { // this is a bit of a hack. Assumes output is F32 always
					scalarCast = "(float)";
				}
			} else
				scalarCast = t;
			if (bounded)
				return String.format("(%s) input, %s%s, %s lower, %s upper, (%s) output", name(input), scalarCast, scalarName, t, t, name(output));
			else
				return String.format("(%s) input, %s%s, (%s) output", name(input), scalarCast, scalarName, name(output));
		}

		@Override
		public String printCallPlanar() {
			if (bounded)
				return "in.getBand(i)," + scalarName + ",lower,upper,out.getBand(i)";
			else
				return "in.getBand(i), " + scalarName + ", out.getBand(i)";
		}
	}

	class FunctionTwo extends Function {

		@Override
		public String printJavadoc() {
			return "\t *\n" +
					"\t * @param input The input image. Not modified.\n" +
					"\t * @param output Where the inverted image is written to. Modified.\n";
		}

		@Override
		public String printArguments() {return "T input , T output";}

		@Override
		public String printCall() {return String.format("(%s) input, (%s) output", name(input), name(output));}

		@Override
		public String printCallPlanar() {return "in.getBand(i), out.getBand(i)";}
	}

	static abstract class Function {
		public String name;

		public String printGenerics() {
			return "<T extends ImageBase<T>>";
		}

		public abstract String printJavadoc();

		public abstract String printArguments();

		public abstract String printCall();

		public abstract String printCallPlanar();
	}

	public void printOtherCrap() {
		out.print("\t/**\n" +
				"\t * <p>\n" +
				"\t * Performs pixel-wise division<br>\n" +
				"\t * output(x,y) = imgA(x,y) / imgB(x,y)\n" +
				"\t * </p>\n" +
				"\t * Only floating point images are supported. If the numerator has multiple bands and the denominator is a single\n" +
				"\t * band then the denominator will divide each band.\n" +
				"\t *\n" +
				"\t * @param imgA Input image. Not modified.\n" +
				"\t * @param imgB Input image. Not modified.\n" +
				"\t * @param output Output image. Modified.\n" +
				"\t */\n" +
				"\tpublic static <N extends ImageBase,D extends ImageBase<D>> void divide(N imgA, D imgB , N output)\n" +
				"\t{\n" +
				"\t\tif( imgA instanceof ImageGray && imgB instanceof ImageGray ) {\n" +
				"\t\t\tif (GrayF32.class == imgA.getClass()) {\n" +
				"\t\t\t\tPixelMath.divide((GrayF32) imgA, (GrayF32) imgB, (GrayF32) output);\n" +
				"\t\t\t} else if (GrayF64.class == imgA.getClass()) {\n" +
				"\t\t\t\tPixelMath.divide((GrayF64) imgA, (GrayF64) imgB, (GrayF64) output);\n" +
				"\t\t\t}\n" +
				"\t\t} else if( imgA instanceof Planar && imgB instanceof ImageGray ) {\n" +
				"\t\t\tPlanar in = (Planar) imgA;\n" +
				"\t\t\tPlanar out = (Planar) output;\n" +
				"\n" +
				"\t\t\tfor (int i = 0; i < in.getNumBands(); i++) {\n" +
				"\t\t\t\tif (GrayF32.class == imgB.getClass()) {\n" +
				"\t\t\t\t\tPixelMath.divide((GrayF32) in.getBand(i), (GrayF32) imgB, (GrayF32) out.getBand(i));\n" +
				"\t\t\t\t} else if (GrayF64.class == imgB.getClass()) {\n" +
				"\t\t\t\t\tPixelMath.divide((GrayF64) in.getBand(i), (GrayF64) imgB, (GrayF64) out.getBand(i));\n" +
				"\t\t\t\t}\n" +
				"\t\t\t}\n" +
				"\t\t} else if( imgA instanceof Planar && imgB instanceof Planar ) {\n" +
				"\t\t\tPlanar inA = (Planar) imgA;\n" +
				"\t\t\tPlanar inB = (Planar) imgB;\n" +
				"\t\t\tPlanar out = (Planar) output;\n" +
				"\n" +
				"\t\t\tfor (int i = 0; i < inA.getNumBands(); i++) {\n" +
				"\t\t\t\tdivide(inA.getBand(i), inB.getBand(i), out.getBand(i));\n" +
				"\t\t\t}\n" +
				"\t\t} else {\n" +
				"\t\t\tthrow new IllegalArgumentException(\"Unknown image Type: \"+imgA.getClass().getSimpleName());\n" +
				"\t\t}\n" +
				"\t}\n\n");
		out.print("\t/**\n" +
				"\t * <p>\n" +
				"\t * Performs pixel-wise multiplication<br>\n" +
				"\t * output(x,y) = imgA(x,y) * imgB(x,y)\n" +
				"\t * </p>\n" +
				"\t * Only floating point images are supported. If one image has multiple bands and the other is gray then\n" +
				"\t * the gray image will be multiplied by each band in the multiple band image.\n" +
				"\t *\n" +
				"\t * @param imgA Input image. Not modified.\n" +
				"\t * @param imgB Input image. Not modified.\n" +
				"\t * @param output Output image. Modified.\n" +
				"\t */\n" +
				"\tpublic static <N extends ImageBase,D extends ImageBase<D>> void multiply(N imgA, D imgB , N output)\n" +
				"\t{\n" +
				"\t\tif( imgA instanceof ImageGray && imgB instanceof ImageGray ) {\n" +
				"\t\t\tif (GrayF32.class == imgA.getClass()) {\n" +
				"\t\t\t\tPixelMath.multiply((GrayF32) imgA, (GrayF32) imgB, (GrayF32) output);\n" +
				"\t\t\t} else if (GrayF64.class == imgA.getClass()) {\n" +
				"\t\t\t\tPixelMath.multiply((GrayF64) imgA, (GrayF64) imgB, (GrayF64) output);\n" +
				"\t\t\t}\n" +
				"\t\t} else if( imgA instanceof Planar && imgB instanceof Planar ) {\n" +
				"\t\t\tPlanar inA = (Planar) imgA;\n" +
				"\t\t\tPlanar inB = (Planar) imgB;\n" +
				"\t\t\tPlanar out = (Planar) output;\n" +
				"\n" +
				"\t\t\tfor (int i = 0; i < inA.getNumBands(); i++) {\n" +
				"\t\t\t\tmultiply(inA.getBand(i), inB.getBand(i), out.getBand(i));\n" +
				"\t\t\t}\n" +
				"\t\t} else if( imgA instanceof Planar || imgB instanceof Planar ) {\n" +
				"\t\t\tPlanar in;\n" +
				"\t\t\tImageGray gray;\n" +
				"\t\t\tPlanar out = (Planar) output;\n" +
				"\n" +
				"\t\t\tif( imgA instanceof Planar ) {\n" +
				"\t\t\t\tin = (Planar)imgA;\n" +
				"\t\t\t\tgray = (ImageGray)imgB;\n" +
				"\t\t\t} else {\n" +
				"\t\t\t\tin = (Planar)imgB;\n" +
				"\t\t\t\tgray = (ImageGray)imgA;\n" +
				"\t\t\t}\n" +
				"\n" +
				"\t\t\tfor (int i = 0; i < in.getNumBands(); i++) {\n" +
				"\t\t\t\tif (GrayF32.class == gray.getClass()) {\n" +
				"\t\t\t\t\tPixelMath.multiply((GrayF32) in.getBand(i), (GrayF32) gray, (GrayF32) out.getBand(i));\n" +
				"\t\t\t\t} else if (GrayF64.class == gray.getClass()) {\n" +
				"\t\t\t\t\tPixelMath.multiply((GrayF64) in.getBand(i), (GrayF64) gray, (GrayF64) out.getBand(i));\n" +
				"\t\t\t\t}\n" +
				"\t\t\t}\n" +
				"\t\t} else {\n" +
				"\t\t\tthrow new IllegalArgumentException(\"Unknown image Type: \"+imgA.getClass().getSimpleName());\n" +
				"\t\t}\n" +
				"\t}\n\n");
		out.println("\t/**\n" +
				"\t * Raises each pixel in the input image to the power of two. Both the input and output image can be the same\n" +
				"\t * instance.\n" +
				"\t *\n" +
				"\t * @param input The input image. Not modified.\n" +
				"\t * @param output Where the pow2 image is written to. Modified.\n" +
				"\t */\n" +
				"\tpublic static <A extends ImageBase<A>,B extends ImageBase<B>>\n" +
				"\tvoid pow2(A input , B output ) {\n" +
				"\t\tif( input instanceof ImageGray ) {\n" +
				"\t\t\tif (GrayU8.class == input.getClass()) {\n" +
				"\t\t\t\tPixelMath.pow2((GrayU8) input, (GrayU16) output);\n" +
				"\t\t\t} else if (GrayU16.class == input.getClass()) {\n" +
				"\t\t\t\tPixelMath.pow2((GrayU16) input, (GrayS32) output);\n" +
				"\t\t\t} else if (GrayF32.class == input.getClass()) {\n" +
				"\t\t\t\tPixelMath.pow2((GrayF32) input, (GrayF32) output);\n" +
				"\t\t\t} else if (GrayF64.class == input.getClass()) {\n" +
				"\t\t\t\tPixelMath.pow2((GrayF64) input, (GrayF64) output);\n" +
				"\t\t\t} else {\n" +
				"\t\t\t\tthrow new IllegalArgumentException(\"Unknown image Type: \" + input.getClass().getSimpleName());\n" +
				"\t\t\t}\n" +
				"\t\t} else if( input instanceof ImageInterleaved ) {\n" +
				"\t\t\tif (InterleavedU8.class == input.getClass()) {\n" +
				"\t\t\t\tPixelMath.pow2((InterleavedU8) input, (InterleavedU16) output);\n" +
				"\t\t\t} else if (InterleavedU16.class == input.getClass()) {\n" +
				"\t\t\t\tPixelMath.pow2((InterleavedU16) input, (InterleavedS32) output);\n" +
				"\t\t\t} else if (InterleavedF32.class == input.getClass()) {\n" +
				"\t\t\t\tPixelMath.pow2((InterleavedF32) input, (InterleavedF32) output);\n" +
				"\t\t\t} else if (InterleavedF64.class == input.getClass()) {\n" +
				"\t\t\t\tPixelMath.pow2((InterleavedF64) input, (InterleavedF64) output);\n" +
				"\t\t\t} else {\n" +
				"\t\t\t\tthrow new IllegalArgumentException(\"Unknown image Type: \" + input.getClass().getSimpleName());\n" +
				"\t\t\t}\n" +
				"\t\t} else if( input instanceof Planar ) {\n" +
				"\t\t\tPlanar in = (Planar)input;\n" +
				"\t\t\tPlanar out = (Planar)output;\n" +
				"\n" +
				"\t\t\tfor (int i = 0; i < in.getNumBands(); i++) {\n" +
				"\t\t\t\tpow2( in.getBand(i), out.getBand(i));\n" +
				"\t\t\t}\n" +
				"\t\t} else {\n" +
				"\t\t\tthrow new IllegalArgumentException(\"Unknown image Type: \"+input.getClass().getSimpleName());\n" +
				"\t\t}\n" +
				"\t}\n\n");
		out.print("\t/**\n" +
				"\t * <p>\n" +
				"\t * Performs pixel-wise addition<br>\n" +
				"\t * d(x,y) = inputA(x,y) + inputB(x,y)\n" +
				"\t * </p>\n" +
				"\t * @param inputA Input image. Not modified.\n" +
				"\t * @param inputB Input image. Not modified.\n" +
				"\t * @param output Output image. Modified.\n" +
				"\t */\n" +
				"\tpublic static <T extends ImageBase<T>, O extends ImageBase>\n" +
				"\tvoid add(T inputA, T inputB, O output) {\n" +
				"\t\tif( inputA instanceof ImageGray) {\n" +
				"\t\t\tif (GrayU8.class == inputA.getClass()) {\n" +
				"\t\t\t\tPixelMath.add((GrayU8) inputA, (GrayU8) inputB, (GrayU16) output);\n" +
				"\t\t\t} else if (GrayS8.class == inputA.getClass()) {\n" +
				"\t\t\t\tPixelMath.add((GrayS8) inputA, (GrayS8) inputB, (GrayS16) output);\n" +
				"\t\t\t} else if (GrayU16.class == inputA.getClass()) {\n" +
				"\t\t\t\tPixelMath.add((GrayU16) inputA, (GrayU16) inputB, (GrayS32) output);\n" +
				"\t\t\t} else if (GrayS16.class == inputA.getClass()) {\n" +
				"\t\t\t\tPixelMath.add((GrayS16) inputA, (GrayS16) inputB, (GrayS32) output);\n" +
				"\t\t\t} else if (GrayS32.class == inputA.getClass()) {\n" +
				"\t\t\t\tPixelMath.add((GrayS32) inputA, (GrayS32) inputB, (GrayS32) output);\n" +
				"\t\t\t} else if (GrayS64.class == inputA.getClass()) {\n" +
				"\t\t\t\tPixelMath.add((GrayS64) inputA, (GrayS64) inputB, (GrayS64) output);\n" +
				"\t\t\t} else if (GrayF32.class == inputA.getClass()) {\n" +
				"\t\t\t\tPixelMath.add((GrayF32) inputA, (GrayF32) inputB, (GrayF32) output);\n" +
				"\t\t\t} else if (GrayF64.class == inputA.getClass()) {\n" +
				"\t\t\t\tPixelMath.add((GrayF64) inputA, (GrayF64) inputB, (GrayF64) output);\n" +
				"\t\t\t} else {\n" +
				"\t\t\t\tthrow new IllegalArgumentException(\"Unknown image Type: \" + inputA.getClass().getSimpleName());\n" +
				"\t\t\t}\n" +
				"\t\t} else if (inputA instanceof Planar) {\n" +
				"\t\t\tPlanar inA = (Planar)inputA;\n" +
				"\t\t\tPlanar inB = (Planar)inputB;\n" +
				"\t\t\tPlanar out = (Planar)output;\n" +
				"\n" +
				"\t\t\tfor (int i = 0; i < inA.getNumBands(); i++) {\n" +
				"\t\t\t\tadd(inA.getBand(i),inB.getBand(i),out.getBand(i));\n" +
				"\t\t\t}\n" +
				"\t\t} else {\n" +
				"\t\t\tthrow new IllegalArgumentException(\"Unknown image Type: \"+inputA.getClass().getSimpleName());\n" +
				"\t\t}\n" +
				"\t}\n\n");

		out.print("\t/**\n" +
				"\t * Bounds image pixels to be between these two values.\n" +
				"\t *\n" +
				"\t * @param input Input image.\n" +
				"\t * @param min minimum value. Inclusive.\n" +
				"\t * @param max maximum value. Inclusive.\n" +
				"\t */\n" +
				"\tpublic static <T extends ImageBase<T>> void boundImage(T input , double min , double max ) {\n" +
				"\t\tif( input instanceof ImageGray ) {\n" +
				"\t\t\tif (GrayU8.class == input.getClass()) {\n" +
				"\t\t\t\tPixelMath.boundImage((GrayU8) input, (int) min, (int) max);\n" +
				"\t\t\t} else if (GrayS8.class == input.getClass()) {\n" +
				"\t\t\t\tPixelMath.boundImage((GrayS8) input, (int) min, (int) max);\n" +
				"\t\t\t} else if (GrayU16.class == input.getClass()) {\n" +
				"\t\t\t\tPixelMath.boundImage((GrayU16) input, (int) min, (int) max);\n" +
				"\t\t\t} else if (GrayS16.class == input.getClass()) {\n" +
				"\t\t\t\tPixelMath.boundImage((GrayS16) input, (int) min, (int) max);\n" +
				"\t\t\t} else if (GrayS32.class == input.getClass()) {\n" +
				"\t\t\t\tPixelMath.boundImage((GrayS32) input, (int) min, (int) max);\n" +
				"\t\t\t} else if (GrayS64.class == input.getClass()) {\n" +
				"\t\t\t\tPixelMath.boundImage((GrayS64) input, (long) min, (long) max);\n" +
				"\t\t\t} else if (GrayF32.class == input.getClass()) {\n" +
				"\t\t\t\tPixelMath.boundImage((GrayF32) input, (float) min, (float) max);\n" +
				"\t\t\t} else if (GrayF64.class == input.getClass()) {\n" +
				"\t\t\t\tPixelMath.boundImage((GrayF64) input, min, max);\n" +
				"\t\t\t} else {\n" +
				"\t\t\t\tthrow new IllegalArgumentException(\"Unknown image Type: \" + input.getClass().getSimpleName());\n" +
				"\t\t\t}\n" +
				"\t\t} else if( input instanceof Planar ) {\n" +
				"\t\t\tPlanar in = (Planar)input;\n" +
				"\n" +
				"\t\t\tfor (int i = 0; i < in.getNumBands(); i++) {\n" +
				"\t\t\t\tboundImage( in.getBand(i), min, max);\n" +
				"\t\t\t}\n" +
				"\t\t} else {\n" +
				"\t\t\tthrow new IllegalArgumentException(\"Unknown image Type: \"+input.getClass().getSimpleName());\n" +
				"\t\t}\n" +
				"\t}\n\n");
		out.print("\t/**\n" +
				"\t * <p>\n" +
				"\t * Computes the absolute value of the difference between each pixel in the two images.<br>\n" +
				"\t * d(x,y) = |img1(x,y) - img2(x,y)|\n" +
				"\t * </p>\n" +
				"\t * @param inputA Input image. Not modified.\n" +
				"\t * @param inputB Input image. Not modified.\n" +
				"\t * @param output Absolute value of difference image. Modified.\n" +
				"\t */\n" +
				"\tpublic static <T extends ImageBase<T>> void diffAbs(T inputA , T inputB , T output) {\n" +
				"\t\tif( inputA instanceof ImageGray ) {\n" +
				"\t\t\tif (GrayU8.class == inputA.getClass()) {\n" +
				"\t\t\t\tPixelMath.diffAbs((GrayU8) inputA, (GrayU8) inputB, (GrayU8) output);\n" +
				"\t\t\t} else if (GrayS8.class == inputA.getClass()) {\n" +
				"\t\t\t\tPixelMath.diffAbs((GrayS8) inputA, (GrayS8) inputB, (GrayS8) output);\n" +
				"\t\t\t} else if (GrayU16.class == inputA.getClass()) {\n" +
				"\t\t\t\tPixelMath.diffAbs((GrayU16) inputA, (GrayU16) inputB, (GrayU16) output);\n" +
				"\t\t\t} else if (GrayS16.class == inputA.getClass()) {\n" +
				"\t\t\t\tPixelMath.diffAbs((GrayS16) inputA, (GrayS16) inputB, (GrayS16) output);\n" +
				"\t\t\t} else if (GrayS32.class == inputA.getClass()) {\n" +
				"\t\t\t\tPixelMath.diffAbs((GrayS32) inputA, (GrayS32) inputB, (GrayS32) output);\n" +
				"\t\t\t} else if (GrayS64.class == inputA.getClass()) {\n" +
				"\t\t\t\tPixelMath.diffAbs((GrayS64) inputA, (GrayS64) inputB, (GrayS64) output);\n" +
				"\t\t\t} else if (GrayF32.class == inputA.getClass()) {\n" +
				"\t\t\t\tPixelMath.diffAbs((GrayF32) inputA, (GrayF32) inputB, (GrayF32) output);\n" +
				"\t\t\t} else if (GrayF64.class == inputA.getClass()) {\n" +
				"\t\t\t\tPixelMath.diffAbs((GrayF64) inputA, (GrayF64) inputB, (GrayF64) output);\n" +
				"\t\t\t} else {\n" +
				"\t\t\t\tthrow new IllegalArgumentException(\"Unknown image Type: \" + inputA.getClass().getSimpleName());\n" +
				"\t\t\t}\n" +
				"\t\t} else if( inputA instanceof Planar ) {\n" +
				"\t\t\tPlanar inA = (Planar)inputA;\n" +
				"\t\t\tPlanar inB = (Planar)inputB;\n" +
				"\t\t\tPlanar out = (Planar)output;\n" +
				"\n" +
				"\t\t\tfor (int i = 0; i < inA.getNumBands(); i++) {\n" +
				"\t\t\t\tdiffAbs( inA.getBand(i), inB.getBand(i), out.getBand(i));\n" +
				"\t\t\t}\n" +
				"\t\t} else {\n" +
				"\t\t\tthrow new IllegalArgumentException(\"Unknown image Type: \"+inputA.getClass().getSimpleName());\n" +
				"\t\t}\n" +
				"\t}\n\n");
		out.print("\t/**\n" +
				"\t * Computes the standard deviation of each pixel in a local region.\n" +
				"\t *\n" +
				"\t * @param mean (Input) Image with local mean\n" +
				"\t * @param pow2 (Input) Image with local mean pixel-wise power of 2\n" +
				"\t * @param stdev (Output) standard deviation of each pixel. Can be same instance as either input.\n" +
				"\t */\n" +
				"\tpublic static <T extends ImageBase<T>, B extends ImageBase<B>> void stdev(T mean , B pow2 , T stdev ) {\n" +
				"\t\tif( mean instanceof ImageGray ) {\n" +
				"\t\t\tif (GrayU8.class == mean.getClass()) {\n" +
				"\t\t\t\tPixelMath.stdev((GrayU8) mean, (GrayU16) pow2, (GrayU8) stdev);\n" +
				"\t\t\t} else if (GrayU16.class == mean.getClass()) {\n" +
				"\t\t\t\tPixelMath.stdev((GrayU16) mean, (GrayS32) pow2, (GrayU16) stdev);\n" +
				"\t\t\t} else if (GrayF32.class == mean.getClass()) {\n" +
				"\t\t\t\tPixelMath.stdev((GrayF32) mean, (GrayF32) pow2, (GrayF32) stdev);\n" +
				"\t\t\t} else if (GrayF64.class == mean.getClass()) {\n" +
				"\t\t\t\tPixelMath.stdev((GrayF64) mean, (GrayF64) pow2, (GrayF64) stdev);\n" +
				"\t\t\t} else {\n" +
				"\t\t\t\tthrow new IllegalArgumentException(\"Unknown image Type: \" + mean.getClass().getSimpleName());\n" +
				"\t\t\t}\n" +
				"\t\t} else if( mean instanceof Planar ) {\n" +
				"\t\t\tPlanar inA = (Planar)mean;\n" +
				"\t\t\tPlanar inB = (Planar)pow2;\n" +
				"\t\t\tPlanar out = (Planar)stdev;\n" +
				"\n" +
				"\t\t\tfor (int i = 0; i < inA.getNumBands(); i++) {\n" +
				"\t\t\t\tstdev( inA.getBand(i), inB.getBand(i), out.getBand(i));\n" +
				"\t\t\t}\n" +
				"\t\t} else {\n" +
				"\t\t\tthrow new IllegalArgumentException(\"Unknown image Type: \"+mean.getClass().getSimpleName());\n" +
				"\t\t}\n" +
				"\t}\n\n");

		out.println("\t/**\n" +
				"\t * <p>\n" +
				"\t * Performs pixel-wise subtraction, but ensures the result is between two bounds.<br>\n" +
				"\t * d(x,y) = imgA(x,y) - imgB(x,y)\n" +
				"\t * </p>\n" +
				"\t * @param inputA Input image. Not modified.\n" +
				"\t * @param inputB Input image. Not modified.\n" +
				"\t * @param output Output image. Modified.\n" +
				"\t */\n" +
				"\tpublic static <T extends ImageBase<T>, O extends ImageBase>\n" +
				"\tvoid subtract(T inputA, T inputB, O output) {\n" +
				"\t\tif( inputA instanceof ImageGray){\n" +
				"\t\t\tif( GrayU8.class == inputA.getClass() ) {\n" +
				"\t\t\t\tPixelMath.subtract((GrayU8) inputA, (GrayU8)inputB, (GrayI16) output);\n" +
				"\t\t\t} else if( GrayS8.class == inputA.getClass() ) {\n" +
				"\t\t\t\tPixelMath.subtract((GrayS8) inputA, (GrayS8)inputB, (GrayS16) output);\n" +
				"\t\t\t} else if( GrayU16.class == inputA.getClass() ) {\n" +
				"\t\t\t\tPixelMath.subtract((GrayU16) inputA, (GrayU16)inputB, (GrayS32) output);\n" +
				"\t\t\t} else if( GrayS16.class == inputA.getClass() ) {\n" +
				"\t\t\t\tPixelMath.subtract((GrayS16) inputA, (GrayS16)inputB, (GrayS32) output);\n" +
				"\t\t\t} else if( GrayS32.class == inputA.getClass() ) {\n" +
				"\t\t\t\tPixelMath.subtract((GrayS32) inputA, (GrayS32)inputB, (GrayS32) output);\n" +
				"\t\t\t} else if( GrayS64.class == inputA.getClass() ) {\n" +
				"\t\t\t\tPixelMath.subtract((GrayS64) inputA, (GrayS64)inputB, (GrayS64) output);\n" +
				"\t\t\t} else if( GrayF32.class == inputA.getClass() ) {\n" +
				"\t\t\t\tPixelMath.subtract((GrayF32) inputA, (GrayF32)inputB, (GrayF32) output);\n" +
				"\t\t\t} else if( GrayF64.class == inputA.getClass() ) {\n" +
				"\t\t\t\tPixelMath.subtract((GrayF64) inputA, (GrayF64)inputB, (GrayF64) output);\n" +
				"\t\t\t} else {\n" +
				"\t\t\t\tthrow new IllegalArgumentException(\"Unknown image Type: \"+inputA.getClass().getSimpleName());\n" +
				"\t\t\t}\n" +
				"\t\t} else if (inputA instanceof Planar) {\n" +
				"\t\t\tPlanar inA = (Planar)inputA;\n" +
				"\t\t\tPlanar inB = (Planar)inputB;\n" +
				"\t\t\tPlanar out = (Planar)output;\n" +
				"\n" +
				"\t\t\tfor (int i = 0; i < inA.getNumBands(); i++) {\n" +
				"\t\t\t\tsubtract(inA.getBand(i),inB.getBand(i),out.getBand(i));\n" +
				"\t\t\t}\n" +
				"\t\t} else {\n" +
				"\t\t\tthrow new IllegalArgumentException(\"Unknown image Type: \"+inputA.getClass().getSimpleName());\n" +
				"\t\t}\n" +
				"\t}\n\n");
	}

	public static void main( String[] args ) throws FileNotFoundException {
		var gen = new GenerateGPixelMath();
		gen.setModuleName("boofcv-ip");
		gen.parseArguments(args);
		gen.generate();
	}
}
