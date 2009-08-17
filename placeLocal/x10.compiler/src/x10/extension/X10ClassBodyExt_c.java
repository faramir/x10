/*
 *
 * (C) Copyright IBM Corporation 2006-2008
 *
 *  This file is part of X10 Language.
 *
 */
package x10.extension;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import polyglot.ast.Block_c;
import polyglot.ast.Call;
import polyglot.ast.Call_c;
import polyglot.ast.ClassBody_c;
import polyglot.ast.Expr;
import polyglot.ast.Formal;
import polyglot.ast.Formal_c;
import polyglot.ast.Local;
import polyglot.ast.MethodDecl;
import polyglot.ast.MethodDecl_c;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.TypeNode;
import polyglot.frontend.ExtensionInfo;
import polyglot.types.ClassType;
import polyglot.types.MethodDef;
import polyglot.types.MethodInstance;
import polyglot.types.Name;
import polyglot.types.Type;
import polyglot.types.Types;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import x10.types.X10Flags;
import x10.types.X10TypeMixin;
import x10.types.X10TypeSystem;


/**
 * Implementation of extern (previously known as native) calls.
 * Check class bodies for 'extern' keyword (aka native)
 * and generate approriate wrappers and stubs to support
 * the simplified JNI-like interface to native code from X10.
 *
 * @author donawa
 * @author igor
 */
public class X10ClassBodyExt_c extends X10Ext_c {

	private BufferedWriter wrapperFile;
	X10TypeSystem typeSystem;

	private final Name KgetBackingArrayMethod = Name.make("getBackingArray");

        private final Name KgetDescriptorMethod =  Name.make("getDescriptor");
	private final String KdescriptorNameSuffix = "_x10DeScRiPtOr";
	private final String KPtrNameSuffix = "_x10PoInTeR";
	String[] wrapperPrologue = {
			"/*Automatically generated -- DO NOT EDIT THIS FILE */\n",
			"#include <sys/types.h>\n", "#include <jni.h>\n",
			"#ifdef __cplusplus\n", "extern \"C\" {\n", "#endif\n", "" };

	String[] wrapperEpilogue = { "\n", "#ifdef __cplusplus\n", "}\n",
			"#endif\n" };

	private void generateWrapperPrologue() {

		try {
			for (int i = 0; i < wrapperPrologue.length; ++i) {
				wrapperFile.write(wrapperPrologue[i]);
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new InternalCompilerError("Problems writing to " + wrapperFile);
		}
	}

	private void generateWrapperEpilogue() {
		try {
			for (int i = 0; i < wrapperEpilogue.length; ++i) {
				wrapperFile.write(wrapperEpilogue[i]);
			}
			wrapperFile.close();
		} catch (IOException e) {
			e.printStackTrace();
			throw new InternalCompilerError("Problems with " + wrapperFile );
		}
	}

	/**
	 * Create a text file with suffix _x10stub.c, one for each outermost class which
	 * contains x10 extern methods
	 * @param containingClassName
	 */
	private void createWrapperFile(String containingClassName, File output_dir) {
		String fileName = containingClassName + "_x10stub.c";
		Date timeStamp = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat();

		try {
			wrapperFile = new BufferedWriter(new FileWriter(new File(output_dir, fileName)));
			wrapperFile.write("/*\n * Filename:"+fileName +
					"\n * Generated: "+formatter.format(timeStamp)+" */\n");

		} catch (IOException e) {
			e.printStackTrace();
			throw new InternalCompilerError("Problems writing to "+wrapperFile);
		}
	}

	private String typeToCType(Type theType) {
		if (theType.isPrimitive()) {
			return typeToCString(theType);
		}
		else // theType.isClass()
		{
			   X10TypeSystem ts = typeSystem;
			   if (ts.isRail(theType) || ts.isValRail(theType)) {
			       Type base = X10TypeMixin.getParameterType(theType, 0);
			       if (! base.isPrimitive())
				   return "jobject*";
			       return typeToCString(base)+"*";
			   }
			return "jobject";
		}
	}

	private String typeToCString(Type theType) {
		if (theType.isPrimitive()) {
			//System.out.println(theType.toString() + " is primitive");
                        if (theType.isInt())
				return "signed int";
			if (theType.isChar())
				return "signed short";
			if (theType.isBoolean())
				return "unsigned char";
			if (theType.isByte())
				return "signed char";
			if (theType.isShort())
				return "signed short";
			if (theType.isLong())
				return "jlong"; // best to use jni.h's defn
			if (theType.isFloat())
				return "float";
			if (theType.isDouble())
				return "double";
			if (theType.isVoid())
				return "void";

			throw new InternalCompilerError("Unexpected type" + theType.toString());
		}
		else {
			   X10TypeSystem ts = typeSystem;
			   if (ts.isRail(theType) || ts.isValRail(theType)) {
			       Type base = X10TypeMixin.getParameterType(theType, 0);
			       if (base.isPrimitive())
				   return typeToCString(base)+"Array";
			       throw new InternalCompilerError("Only primitive arrays are supported, not "+theType.toString());
			   }
			   throw new InternalCompilerError("Unexpected type"+theType.toString());
                }
	}

	private String typeToJavaSigString(Type theType) {
   
		if (theType.isPrimitive()) {
			//System.out.println(theType.toString() + " is primitive");
		
			if (theType.isInt())
				return "I";
			if (theType.isChar())
				return "J";
			if (theType.isBoolean())
				return "Z";
			if (theType.isByte())
				return "B";
			if (theType.isShort())
				return "S";
			if (theType.isLong())
				return "J";
			if (theType.isFloat())
				return "F";
			if (theType.isDouble())
				return "D";
			if (theType.isVoid())
				return "V";
			throw new InternalCompilerError("Unexpected type" + theType.toString());
		} else {
		    X10TypeSystem ts = typeSystem;
			   if (ts.isRail(theType) || ts.isValRail(theType)) {
			       Type base = X10TypeMixin.getParameterType(theType, 0);
			       if (base.isPrimitive())
				   return "["+typeToJavaSigString(base);
			       throw new InternalCompilerError("Only primitive arrays are supported, not "+theType.toString());
			   }

			   throw new InternalCompilerError("Only java arrays are supported, not "+theType.toString());
                }
	}

	private String generateJavaSignature(MethodDecl_c method) {
		MethodDef mi = method.methodDef();
		String signature = "";// "("

		for (ListIterator<Formal> i = method.formals().listIterator(); i.hasNext();) {
			Formal parameter = i.next(); 
			X10TypeSystem ts = typeSystem;

			if(parameter.declType().isPrimitive() || 	
			   ts.isRail(parameter.declType()) || ts.isValRail(parameter.declType())) {
                           signature += typeToJavaSigString(parameter.declType());
                        }
                        else {
                        // assume this is an X10 array object.  Determine backing array type and add
                        // descriptor signature
                           ClassType ct = (ClassType)parameter.declType().toClass();
                           MethodInstance backingMethod = findMethod(ct,KgetBackingArrayMethod);
                           if(null == backingMethod) throw new InternalCompilerError("Could not find "+KgetBackingArrayMethod+" in class "+ct);
                           signature += typeToJavaSigString(backingMethod.returnType());
                           signature +=typeToJavaSigString( typeSystem.Rail(typeSystem.Int()));

                        }
		}
		if (false) signature += ")" + typeToJavaSigString(mi.returnType().get());
		return signature;
	}

	private String JNImangle(Name inName) { return JNImangle(inName.toString()); }

	private static final String zeros = "0000";
	/**
	 * replace '_' with '_1'
	 *         '<unicode>' with '_0<unicode>'
	 *         ';' with '_2'
	 *         '[' with '_3'
         * and strip out anything within "/*" and "*\/"
         * assume '*' and '/' are illegal characters
	 * @param inName
	 * @return
	 */
	private String JNImangle(String inName) {
		char [] charName = inName.toCharArray();
		StringBuffer buffer = new StringBuffer(inName.length());
                boolean seenForwardSlash=false;
                boolean inCommentMode=false;
                boolean lastCharAsterix=false;
                char lastChar='a';
		for (int i = 0; i < inName.length(); ++i) {
			char ch = inName.charAt(i);
                       
                        if(inCommentMode){
                          if( ch == '/' && lastChar == '*')
                             inCommentMode =false;
                        }
                        else
                           switch (ch) {
                                 case '/': /* do not record */
                                      break;
                                case '*':
                                        if(lastChar == '/')
                                           inCommentMode=true;
                                        break;
				case '_':
					buffer.append("_1");
					break;
				case ';':
					buffer.append("_2");
					break;
				case '[':
					buffer.append("_3");
					break;
				case '.':
					buffer.append("_");
					break;
				default:
                                        if (Character.isLetterOrDigit(ch))
						buffer.append(ch);
					else {
						String hex = Integer.toHexString((int)ch);
						hex = zeros.substring(hex.length()) + hex;
						buffer.append("_0").append(hex);
					}
					break;
			}
                        lastChar=ch;
		}
		if(false)System.out.println("convert from "+inName+" to "+buffer.toString());
		return buffer.toString();
	}

	private static final String JNI_PREFIX = "Java_";

	/**
	 * Apply same mangling algorithm as javah does so we can automatically
	 * generate a name for the JNI code.
	 * @param method
	 * @param isOverloaded
	 * @return
	 */
	private Name generateJNIName(MethodDecl_c method, boolean isOverloaded) {
		String name = JNI_PREFIX +
			JNImangle(canonicalTypeString(method.methodDef().container().get())) +
			"_" + JNImangle(generateX10NativeName(method));
		if (isOverloaded)
			name += "__" + JNImangle(generateJavaSignature(method));
		return Name.make(name);
	}

	/**
	 * Apply a milder mangling algorithm (that of Sun's javah).
	 * Needed for broken VMs.
	 * @param method
	 * @param isOverloaded
	 * @return
	 */
	private Name generateJNIAlias(MethodDecl_c method, boolean isOverloaded) {
		String name = JNI_PREFIX +
			JNImangle(method.methodDef().container().get().toString()) +
			"_" + JNImangle(generateX10AliasName(method));
		if (isOverloaded)
			name += "__" + JNImangle(generateJavaSignature(method));
		return Name.make(name);
	}

	/**
	 * Convert the input type to a canonical fully-qualified string,
	 * with '.'s separating packages, and '$'s separating nested classes.
	 * @param t the type (must be a class type)
	 */
	private String canonicalTypeString(Type t) {
		String s = "";
		ClassType cl = t.toClass();
		for (; cl.isNested(); cl = cl.outer()) {
			if (cl.isAnonymous())
				throw new RuntimeException("Anonymous inner classes not supported yet");
			s = "$" + cl.name() + s;
		}
		return cl.fullName() + s;
	}

	private Name generateX10NativeName(MethodDecl_c method) {
		return Name.make(JNImangle(canonicalTypeString(method.methodDef().container().get())) + "_" + method.name().id());
	}

	private Name generateX10AliasName(MethodDecl_c method) {
		return Name.make(JNImangle(method.methodDef().container().get().toString()) + "_" + method.name().id());
	}

	/**
	 * if instead we use JNI methods to get the base address, then pass
	 * the array object and the descriptor (an array of ints) to the native method
	 * @param nativeMethod
	 * @param nf node factory
	 * @return new method declaration of java jni call
	 */
	private MethodDecl_c createNewNative(MethodDecl_c nativeMethod, NodeFactory nf) {

		MethodDef mi = nativeMethod.methodDef();

		// FIXME: [IP] This looks like a bug -- in the stub, we do something else if the method is overloaded
		Name nativeName = generateX10NativeName(nativeMethod);
		MethodDecl_c newNative = (MethodDecl_c)nativeMethod.name(nativeMethod.name().id(nativeName));
		ArrayList newFormals = new ArrayList();

		TypeNode longType = nf.CanonicalTypeNode(nativeMethod.position(), typeSystem.Long());
		TypeNode arrayOfIntType = nf.CanonicalTypeNode(nativeMethod.position(), 
							       typeSystem.Rail(typeSystem.Int()));
		boolean seenNonPrimitive = false;
		/* 
                 * determine type of backing array as pass it as well
                 * as the descriptor array (an array of ints)
		 */
		for (ListIterator i = nativeMethod.formals().listIterator(); i.hasNext();) {
			Formal_c parameter = (Formal_c) i.next();
			if (parameter.declType().isPrimitive())
				newFormals.add(parameter);
			else {
			  seenNonPrimitive = true;
			  ClassType ct = (ClassType)parameter.declType().toClass();
			  MethodInstance backingMethod = findMethod(ct,KgetBackingArrayMethod);
			  if(null == backingMethod) throw new InternalCompilerError("Could not find "+KgetBackingArrayMethod+" in class "+ct);
			  TypeNode theReturnType = nf.CanonicalTypeNode(nativeMethod.position(),backingMethod.returnType());
			  newFormals.add(parameter.type(theReturnType));
			  Formal_c paramDescriptor = (Formal_c)parameter.name(parameter.name().id(Name.make(parameter.name().id() + KdescriptorNameSuffix)));
			  newFormals.add(paramDescriptor.type(arrayOfIntType)); // another for descriptor
			}
		}
		if (seenNonPrimitive) {
			newNative = (MethodDecl_c)newNative.formals(newFormals);
		}
		return newNative;
	}

/* search for the given method.  The method is expected to be found in the regular hierarchy,
 * but it may be in an interface--if initial search fails, start looking in the interfaces
 */
        private  MethodInstance findMethod(ClassType ct,Name targetName){
		MethodInstance targetMI=null,memberMI=null;
		final boolean trace=false;
                ClassType currentClass=ct;
                while(currentClass!=null) {
                   if(trace) System.out.println("Searching class "+currentClass);
   		   List methods = currentClass.methods();
                   for(ListIterator j = methods.listIterator();j.hasNext();){
                   memberMI = (MethodInstance)j.next();
                   if(trace) System.out.println("inspecting member:"+memberMI.name());
                   if (memberMI.name().equals(targetName))
                      return memberMI;
                   }
                   currentClass = (ClassType)currentClass.superClass();
                }
                /* now start looking in the interfaces...*/ 
                if(trace) System.out.println("Search the interfaces....");
                currentClass = ct;
                while(currentClass!=null) {
                   List interfaceMethods = currentClass.interfaces();

                   for (ListIterator j = interfaceMethods.listIterator(); j.hasNext();) {
                      ClassType implementationClass = (ClassType)j.next();
                      if(trace)System.out.println("looking at interface "+implementationClass);
                      List methods = implementationClass.methods();

                      for(ListIterator k = methods.listIterator();k.hasNext();){
                         memberMI = (MethodInstance)k.next();
                         if(trace) System.out.println("inspecting member:"+memberMI.name());
                         if (memberMI.name().equals(targetName))
                            return memberMI;
                      }
                   }
                   currentClass = (ClassType)currentClass.superClass();
                }
		return null;
        }

	/**
	 * Create java wrapper which calls a JNI call which implements
	 * interface with X10 extern
	 * @param nativeMethod
	 * @param nf node factory
	 * @return wrapper method
	 */
	private MethodDecl_c createNativeWrapper(MethodDecl_c nativeMethod, NodeFactory nf) {
        boolean trace = false;
		nativeMethod = (MethodDecl_c)nativeMethod.flags(nativeMethod.flags().flags(nativeMethod.flags().flags().clearNative()));
		MethodDef mi = nativeMethod.methodDef();
		Position pos = nativeMethod.position();
		MethodDecl_c nativeWrapper = nativeMethod;

		ArrayList newArgs = new ArrayList();
		// FIXME: [IP] This looks like a bug -- in the stub, we do something else if the method is overloaded
		Name jniName = generateX10NativeName(nativeMethod);

		TypeNode receiver = nf.CanonicalTypeNode(pos, mi.container());

		Call jniCall = nf.Call(pos, receiver, nf.Id(pos, jniName), newArgs);
		jniCall = (Call_c)jniCall.targetImplicit(true);
		jniCall = (Call_c)jniCall.methodInstance(mi.asInstance());

                Name descriptorName = KgetDescriptorMethod;
	
		ArrayList args = new ArrayList();
		for (ListIterator i = nativeMethod.formals().listIterator(); i.hasNext();) {
			Formal_c parameter = (Formal_c) i.next();

			if (parameter.declType().isPrimitive()) {
				Local arg= nf.Local(pos, parameter.name());
				// RMF 11/2/2005 - Set the type of the Local. This would normally
				// be handled by type-checking, but we're past that point now...
				arg= (Local) arg.type(parameter.declType());
				// RMF 7/10/2006 - Make sure the Local has an associated LocalInstance;
				// the InitChecker will need to see it...
				arg= (Local) arg.localInstance(typeSystem.localDef(pos, parameter.flags().flags(), Types.ref(arg.type()), arg.name().id()).asInstance());
				args.add(arg);
			} else {
				ClassType ct = (ClassType)parameter.declType().toClass();
				if (null == ct)
					throw new InternalCompilerError("Problems with array "+parameter.name().id());

				if (trace)System.out.println("Processing "+parameter.name().id()+"::"+parameter);

				/**
				 * Look for the implementation of the getbackingarray method interface.  Start in this method
				 * and keep looking up inheritance tree.
				 */
				MethodInstance memberMI = null;
				MethodInstance  arrayDescriptorMI = null, backingArrayMI=null;
				ClassType currentClass = ct;
				boolean doneSearch = false;
                               
				while (currentClass != null && !doneSearch) {
					List interfaceMethods = currentClass.interfaces();
                               
					for (ListIterator j = interfaceMethods.listIterator(); j.hasNext();) {
						ClassType implementationClass = (ClassType)j.next();
                                                if(trace)System.out.println("looking at interface "+implementationClass);
						List methods = implementationClass.methods();
						for (ListIterator k = methods.listIterator(); k.hasNext();) {
							memberMI = (MethodInstance) k.next();
							if (trace) System.out.println("inspecting interface member:"+ memberMI.name());
							if (memberMI.name().equals(descriptorName))
								arrayDescriptorMI = memberMI;
                                                        if(memberMI.name().equals(KgetBackingArrayMethod))
                                                           backingArrayMI = memberMI;
                                                         
                                                        if(arrayDescriptorMI!=null)
								break;
							
						}
					}

                                        /* look for getBackingArray method.  Note that it is not an interface */
					if(backingArrayMI == null){
                                           backingArrayMI = findMethod(currentClass,KgetBackingArrayMethod);
					}

					if (arrayDescriptorMI != null && (backingArrayMI !=null)){
						doneSearch = true;
					}
					currentClass = (ClassType)currentClass.superClass();
				}
				if (null == arrayDescriptorMI) 
					throw new InternalCompilerError("Could not find "+descriptorName+" in class "+ ct.fullName());
				if (null == backingArrayMI)
					throw new InternalCompilerError("Could not find "+KgetBackingArrayMethod+" in class "+ ct.fullName());
				

				Local getAddrTarget= nf.Local(pos, parameter.name());
			        // RMF 11/3/2005 - Set the type of getAddr call's target. This
				// would normally be handled by type-checking, but we're past
				// that point now...
				getAddrTarget= (Local) getAddrTarget.type(parameter.type().type());
				// RMF 7/10/2006 - Make sure the Local has an associated LocalInstance;
				// the InitChecker will need to see it...
				getAddrTarget= (Local) getAddrTarget.localInstance(typeSystem.localDef(pos, parameter.flags().flags(), 
                                                                                                            Types.ref(getAddrTarget.type()), 
                                                                                                            getAddrTarget.name().id()).asInstance());
				
                                Call getAddr = nf.Call(pos, getAddrTarget, nf.Id(pos, KgetBackingArrayMethod));
                                getAddr = (Call_c)getAddr.methodInstance(backingArrayMI);
                                // RMF 11/3/2005 - Set the type of getAddr call. This would
                                // normally be handled by type-checking, but we're past that
                                // point now...
                                getAddr = (Call) getAddr.type(backingArrayMI.returnType());
                                args.add(getAddr);


				Call getDescriptor = nf.Call(pos, getAddrTarget, nf.Id(pos, descriptorName));
				getDescriptor = (Call_c)getDescriptor.methodInstance(arrayDescriptorMI);
				// RMF 11/3/2005 - Set the type of getDescriptor call. This
				// would normally be handled by type-checking, but we're past
				// that point now...
				getDescriptor = (Call) getDescriptor.type(arrayDescriptorMI.returnType());
				args.add(getDescriptor);

			}
		}

		jniCall = (Call_c)jniCall.arguments(args);

		// RMF 11/2/2005 - Set type of jniCall to that of method's return type.
		// This would ordinarily be taken care of by type-checking, but we're
		// past that point...
		jniCall = (Call) jniCall.type(nativeMethod.returnType().type());

		ArrayList newStmts = new ArrayList();
		if (nativeMethod.methodDef().returnType().get().isVoid())
			newStmts.add(nf.Eval(pos, (Expr)jniCall));
		else
			newStmts.add(nf.Return(pos, (Expr)jniCall));

		Block_c newBlock = (Block_c)nf.Block(pos, newStmts);

		nativeWrapper = (MethodDecl_c)nativeWrapper.body(newBlock);
		return nativeWrapper;
	}

	private String maybeCast(String to, String from) {
		if (!to.equals(from))
			return "("+to+")";
		return "";
	}

	private String maybeCast(Type theType) {
		return maybeCast(typeToCType(theType), typeToJNIString(theType));
	}

	private String maybeUncast(Type theType) {
		return maybeCast(typeToJNIString(theType), typeToCType(theType));
	}


       private String generateAcquireStmt(String arrayName,String ptrName) {
          String stmt ="#ifdef __cplusplus\n";
          stmt +="  void *"+ptrName+" = (env)->GetPrimitiveArrayCritical("+arrayName+",0);\n";
          stmt += "#else\n";
          stmt +="  void *"+ptrName+" = (*env)->GetPrimitiveArrayCritical(env,"+arrayName+",0);\n";
          stmt += "#endif\n";
          return stmt;
       }
       private String generateReleaseStmt(String arrayName,String ptrName) {
             String stmt = "#ifdef __cplusplus\n";
             stmt +="  (env)->ReleasePrimitiveArrayCritical("+arrayName+","+ptrName+",0);\n"; 
             stmt += "#else\n";
             stmt +="  (*env)->ReleasePrimitiveArrayCritical(env,"+arrayName+","+ptrName+",0);\n";
             stmt += "#endif\n";
            return stmt;
         }


	/**
	 * Create C stub that user will later compile into a dynamic library
	 * contains JNI signature C code which calls the expected X10 routine
	 * @param nativeMethod
	 * @param isOverloaded
	 */
	private void generateStub(MethodDecl_c nativeMethod, boolean isOverloaded) {

	    Name newName = generateX10NativeName(nativeMethod);
		if (isOverloaded)
			newName = Name.make(newName + "__" + JNImangle(generateJavaSignature(nativeMethod)));


		String jniCall, wrapperCall, wrapperDecl, saveTheValue = "";

		wrapperCall = "  "+newName + "(";
		wrapperDecl = "extern " + typeToCType(nativeMethod.methodDef().returnType().get()) + " ";
		wrapperDecl += wrapperCall;

		newName = generateJNIName(nativeMethod, isOverloaded);

		String parm = nativeMethod.flags().flags().isStatic()
						? "jclass cls" : "jobject obj";

		jniCall = "JNIEXPORT " + typeToJNIString(nativeMethod.methodDef().returnType().get()) + " JNICALL\n"
		        + newName + "(JNIEnv *env, " + parm;

		String returnedValue="";
		if (!nativeMethod.methodDef().returnType().get().isVoid()){
			String tempName="_x10ReTuRnVaL";
			saveTheValue = typeToCType(nativeMethod.methodDef().returnType().get()) + 
			" "+ tempName+"="+
			maybeUncast(nativeMethod.methodDef().returnType().get());
			returnedValue="return "+tempName+";\n";
		}
		String commaString = "";
                String releaseStmts="";
                String acquireStmts="";

		for (ListIterator i = nativeMethod.formals().listIterator();
				i.hasNext();)
		{
			Formal_c parameter = (Formal_c) i.next();


			if (parameter.declType().isPrimitive()) { 
			   jniCall += ", " + typeToJNIString(parameter.declType())
					   + " " + parameter.name().id();
			   wrapperDecl += commaString
					+ typeToCType(parameter.declType()) + " "
					+ parameter.name().id();
			   wrapperCall += commaString + maybeCast(parameter.declType()) + parameter.name().id();
			}
			else {
			   String arrayPtr = parameter.name().id()+KPtrNameSuffix;
                           acquireStmts += generateAcquireStmt(parameter.name().id().toString(),arrayPtr);
                           releaseStmts = generateReleaseStmt(parameter.name().id().toString(),arrayPtr) + releaseStmts; // release in reverse order
			   
			   ClassType ct = (ClassType)parameter.declType().toClass();
			   MethodInstance backingMethod = findMethod(ct,KgetBackingArrayMethod);
			   if(null == backingMethod) throw new InternalCompilerError("Could not find "+KgetBackingArrayMethod+" in class "+ct);

			   jniCall += ", " + typeToJNIString(backingMethod.returnType())
					   + " " + parameter.name().id();
			   wrapperDecl += commaString
					+ typeToCType(backingMethod.returnType()) + " "
					+ arrayPtr;
			   wrapperCall += commaString + maybeCast(backingMethod.returnType()) + arrayPtr;
			   
			}

			// if we see an array type there must be a descriptor right after
			if (!parameter.declType().isPrimitive()) {
                           String descriptorName = parameter.name().id()+KdescriptorNameSuffix;
                           String descriptorPtrName = descriptorName+KPtrNameSuffix;
                           jniCall += ", jintArray " + descriptorName;
                           acquireStmts += generateAcquireStmt(descriptorName,descriptorPtrName);
                           releaseStmts = generateReleaseStmt(descriptorName,descriptorPtrName)+ releaseStmts;
  
                           wrapperCall += ", (int*) "+descriptorPtrName;
                           wrapperDecl += ", int* " + descriptorName;  
			}

			commaString = ", ";
		}
		jniCall += ")";
		wrapperCall += ")";
		wrapperDecl += ");";
                

		String jniAlias = "";
		// Only generate the alias if inner class
		if (nativeMethod.methodDef().container().get().toClass().isNested()) {
		        Name aliasName = generateJNIAlias(nativeMethod, isOverloaded);

			jniAlias = "#ifndef __WIN32__\n" 
			         + "extern JNIEXPORT __typeof(" + newName + ") JNICALL\n"
			         + aliasName + "\n"
			         + "__attribute((alias(\"" + newName + "\")));\n"
			         + "#endif\n"
			         + "\n";
		}

		try {
			wrapperFile.write("\n"
					+ "/* * * * * * * */\n"
					+ wrapperDecl + "\n"
					+ jniCall + " {\n"
					+ acquireStmts+"\n"
					+ "\n"
					+ saveTheValue + wrapperCall + ";\n"
					+ "\n"
					+ releaseStmts + returnedValue + "}\n"
					+ jniAlias);
			// Also generate the underscored alias for retarded
                        // Win32 loaders
			wrapperFile.write("#ifndef __WIN32__\n"
			                + "extern JNIEXPORT __typeof(" + newName + ") JNICALL\n"
			                + "_" + newName + "\n"
			                + "__attribute((alias(\"" + newName + "\")));\n" 
			                + "#endif\n" 
			                + "\n");
		} catch (IOException e) {
			e.printStackTrace();
			throw new InternalCompilerError("Problems writing file");
		}
	}

	/**
	 * Primitive types get translated to their corresponding JNI type.
	 * @param theType
	 * @return JNI name for the type
	 */
	private String typeToJNIString(Type theType) {
		if (theType.isPrimitive()) {
			//System.out.println(theType.toString() + " is primitive");
			if (theType.isInt())
				return "jint";
			if (theType.isBoolean())
				return "jboolean";
			if (theType.isByte())
				return "jbyte";
			if (theType.isShort())
				return "jshort";
			if (theType.isLong())
				return "jlong";
			if (theType.isFloat())
				return "jfloat";
			if (theType.isChar())
				return "jchar";
			if (theType.isDouble())
				return "jdouble";
			if (theType.isVoid())
				return "void";
                        throw new InternalCompilerError("Unhandled type:"+theType);
		} else {
		   X10TypeSystem ts = typeSystem;
		   if (ts.isRail(theType) || ts.isValRail(theType)) {
		       Type base = X10TypeMixin.getParameterType(theType, 0);
		       return typeToJNIString(base)+"Array";
		   }
                }
	
		return "<unknown>";
	}

	private static int containingClassDepth = 0; // use to create stub file for outermost class w/ natives

	/**
	 * Identify native (aka extern) x10 methods and create a wrapper with
	 * the same name.  The wrapper makes a JNI call to a routine which
	 * then calls the expected X10 native call.
	 * e.g.
	 * <code>
	 * class C {
	 *   static int extern foo(int x);
	 * }
	 * </code>
	 * would result in java code
	 * <code>
	 * class C {
	 *   static int native C_foo(int x);
	 *   static int foo(int x) { return C_foo(x); }
	 * }
	 * </code>
	 *
	 * <code>C_foo</code> is a native C call, which would end up looking like
	 * <code>int Java_C_C_1foo(int x) { return C_foo(x); }</code>
	 * Stub files for each containing class are generated containing
	 * the C wrappers.  It is up to the user to compile these, along
	 * with the actual native implementation of <code>C_foo(int)</code>, into
	 * a dynamic library, and ensure that the X10 program can find them
	 */
	public Node rewrite(X10TypeSystem ts, NodeFactory nf, ExtensionInfo info) {
		typeSystem = ts;
		Type tt = ts.Long();
		boolean seenNativeMethodDecl = false;

		ClassBody_c cb = (ClassBody_c) node();
		List members = cb.members();
		Map methodHash = null;
		ArrayList newListOfMembers = new ArrayList();
		for (ListIterator i = members.listIterator(); i.hasNext();) {
			Object o = i.next();
			if (o instanceof MethodDecl) {
				MethodDecl_c md = (MethodDecl_c) o;
				MethodDef mi = md.methodDef();

				if (!X10Flags.toX10Flags(mi.flags()).isExtern()) {
					newListOfMembers.add(o);
					continue;
				}

				if (!seenNativeMethodDecl) {
					// JNI signature changes depends on whether the method
					// is overloaded or not.  Determine that by scanning
					// through and hashing all native method names
					methodHash = buildNativeMethodHash(members);
					if (0 == containingClassDepth++) {
						createWrapperFile(JNImangle(canonicalTypeString(mi.container().get())),
										  info.getOptions().output_directory);
						generateWrapperPrologue();
					}
					seenNativeMethodDecl = true;
				}

				boolean isOverLoaded = (null != methodHash.get(md.name().id()));

				generateStub(md, isOverLoaded);
				newListOfMembers.add(createNewNative(md, nf));
				newListOfMembers.add(createNativeWrapper(md, nf));
			}
			else
				newListOfMembers.add(o);
		}

		if (seenNativeMethodDecl) {
			--containingClassDepth;
			if (0 == containingClassDepth)
				generateWrapperEpilogue();
		}

		cb = (ClassBody_c)cb.members(newListOfMembers);
		return cb;
	}

	private Map<Name,MethodDecl> buildNativeMethodHash(List members) {
		Map<Name,MethodDecl> methodHash = new HashMap();
		for (ListIterator j = members.listIterator(); j.hasNext();) {
			Object theObj = j.next();
			if (!(theObj instanceof MethodDecl))
				continue;
			MethodDecl_c methodDecl = (MethodDecl_c) theObj;
			if (!X10Flags.toX10Flags(methodDecl.methodDef().flags()).isExtern())
				continue;

			if (methodHash.containsKey(methodDecl.name().id())) {
				methodHash.put(methodDecl.name().id(), methodDecl); // more than one instance
			} else {
				methodHash.put(methodDecl.name().id(), null);
			}
		}
		return methodHash;
	}
}

