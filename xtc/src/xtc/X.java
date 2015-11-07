/*
 * Figuring out pretty print 
 * 
 * @author Beatrice Mendoza
 * 
 * @author Kate Montgomery
 */

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

import xtc.lang.JavaFiveParser;
import xtc.parser.ParseException;
import xtc.parser.Result;
import xtc.tree.GNode;
import xtc.tree.Node;
import xtc.tree.Printer;
import xtc.tree.Visitor;

public class X extends xtc.util.Tool {
	
	private String className = null;
	private boolean pointer = true;

	public X() 
	{

	}

	public interface ICommand 
	{
		public void run();
	}

	public String getName() 
	{
		return "Beatrice Mendoza";
	}

	public File locate(String name) throws IOException 
	{
		File file = super.locate(name);
		if (Integer.MAX_VALUE < file.length()) 
		{
			throw new IllegalArgumentException(file + ": file too large");
		}
		return file;
	}

	public Node parse(Reader in, File file) throws IOException, ParseException 
	{
		JavaFiveParser parser =
				new JavaFiveParser(in, file.toString(), (int)file.length());
		Result result = parser.pCompilationUnit(0);
		return (Node)parser.value(result);
	}
static StringBuilder zzz = new StringBuilder();
static String testClass = "";
	public StringBuilder translate(Node node) 
	{
		new Visitor() 
		{
			private boolean mainClass = false;
			private boolean inMethod = false;
			private String currMethod = "";
			private boolean constructor = false;
			private boolean inConstructor = false;
			private List<String> classes = new ArrayList<String>();
			private boolean Ptr = false;
			private Map<String, String> consMap = new HashMap<String, String>();
			private int classCount = 0;
			private String superName = "Object";
			private Map<String, ArrayList<String>> methodVars = new HashMap<String, ArrayList<String>>();
			private Map<String, String> initVars = new HashMap<String, String>();
			private Map<String, String> argType = new HashMap<String, String>();
			private Map<String, String> methNames = new HashMap<String, String>();
			private Map<String, String> superMap = new HashMap<String, String>();

			public void visitCompilationUnit(GNode n) 
			{
				consMap.put("Object", "__Object::init(__this);\n");
				visit(n);
			}

			public void visitClassDeclaration(GNode n) {
				classCount++;
				className = n.getString(1);
				classes.add(n.getString(1));
				initVars.put(className, "");
				if (n.getNode(0).size() != 0 || classCount == 1) {
					mainClass = true;
					testClass = n.getString(1);
				} else {
					mainClass = false;
					testClass = "";
				}
				if (n.getNode(3) != null) {
					superName = n.getNode(3).getNode(0).getNode(0).getString(0);	
				}
				superMap.put(className, superName);
				for (int k = 0; k < n.getNode(5).size(); k++) {
					dispatch(n.getNode(5).getNode(k));
				}
				if (!constructor && ((!className.contains("Test") && !className.contains("test")) || classCount == 1)) {
					if (superName.equals("Object")) {
					  zzz.append("__rt::Ptr<"+className+"> "+className+"::init(__rt::Ptr<"+className+"> __this) {\n\t__"+superName+"::init(__this);\n\treturn __this;\n}\n");
					} else {
					  zzz.append("__rt::Ptr<"+className+"> "+className+"::init(__rt::Ptr<"+className+"> __this) {\n\t"+superName+"::init(__this);\n\treturn __this;\n}\n");
					}
				}
				constructor = false;
				superName = "Object";
			}

			public void visitClassBody(GNode n)
			{
				for(Object c : n) {
					if(c != null && c instanceof Node)
						dispatch((Node) c);
				}
			}

			public void visitMethodDeclaration(GNode n) 
			{
				// print type
				inMethod = true;
				if (n.getNode(4).size() > 0 && !n.getString(3).equals("main")) {
					//create map of argument types
					argType.clear();
					//mangle the name for method overloading
					for(int i = 0; i < n.getNode(4).size(); i++) {
						n.set(3, n.getString(3)+"_"+n.getNode(4).getNode(0).getNode(1).getNode(0).getString(0));
					}
					methNames.put(n.getString(3), className);
				}
				currMethod = n.getString(3);
				methodVars.put(n.getString(3), new ArrayList());
				if (n.getNode(2).hasName("VoidType")){
					zzz.append("void ");
				} else if (n.getNode(2).hasName("QualifiedIdentifier") && classes.contains(n.getNode(2).getString(0))) {
					zzz.append("__rt::Ptr<"+n.getNode(2).getString(0)+"> ");
				}else{
					zzz.append(n.getNode(2).getNode(0).getString(0) + " ");
				}
				zzz.append(className + "::" + n.getString(3)+ "(");
				if (n.getString(3).equals("main")) {
				  zzz.append("__rt::Ptr<__rt::Array<String> > args");
				} else {
				  zzz.append("__rt::Ptr<"+className+"> __this");
				  if (n.getNode(4).size() > 0) { zzz.append(", ");}
				  for(int x =0; x < n.getNode(4).size(); x++){
					dispatch(n.getNode(4).getNode(x));
					if(x<n.getNode(4).size()-1)
						zzz.append(", ");
				  }
				}
				zzz.append("){\n");
				dispatch((Node) n.get(7));
				zzz.append("}\n");
				inMethod = false;
				currMethod = "";
			}
			public void visitFormalParameter(GNode n) {
				ArrayList<String> tmp = methodVars.get(currMethod);
				if (classes.contains(n.getNode(1).getNode(0).getString(0))) {
				 zzz.append("__rt::Ptr<"+n.getNode(1).getNode(0).getString(0) + "> " + n.getString(3) + " ");
				 tmp.add(n.getString(3));
				} else {
				if (tmp == null) {
					runtime.console().p("temp is null in X.java").pln().flush();
				}
				tmp.add(n.getString(3));
				 zzz.append(n.getNode(1).getNode(0).getString(0) + " " + n.getString(3) + " ");
				}
			}
			
			public void visitPrimitiveType(GNode n)
			{
				
				if(n.getString(0).equals("boolean"))
					zzz.append("bool");
				else zzz.append("\t"+n.getString(0) + " ");
			}

			public void visitDeclarator(GNode n)
			{
				zzz.append(n.getString(0)); // Finish printing declaration
				
				if(n.getNode(2) != null){				
					zzz.append(" = ");
				}
				if(n.getNode(2) != null && n.getNode(2).hasName("NewClassExpression")){
					if (n.getNode(2).getNode(2).getString(0).equals("Object") || n.getNode(2).getNode(2).getString(0).equals("String")) {
					zzz.append("__"+n.getNode(2).getNode(2).getString(0)+"::init(new __"+n.getNode(2).getNode(2).getString(0)+"()");
					} else {
					zzz.append(n.getNode(2).getNode(2).getString(0)+"::init(new "+n.getNode(2).getNode(2).getString(0)+"()");
					}
					for (int i = 0; i < n.getNode(2).getNode(3).size(); i++) {
					    zzz.append(", ");
					    if (n.getNode(2).getNode(3).getNode(0).hasName("StringLiteral")) {
							dispatch(n.getNode(2).getNode(3).getNode(0));
					    } else {
						zzz.append(n.getNode(2).getNode(3).getNode(0).getString(0));
					    }
					}
					zzz.append(")");
				} else if (n.getNode(2) !=null && n.getNode(2).hasName("NewArrayExpression")) {
			
					zzz.append("new __rt::Array<");
					if (n.getNode(2).getNode(1).size() == 2) {
						// 2D array
						zzz.append("__rt::Ptr<__rt::Array<");
						if (n.getNode(2).getNode(0).hasName("QualifiedIdentifier") && !n.getNode(2).getNode(0).getString(0).equals("Object") && !n.getNode(2).getNode(0).getString(0).equals("String")) {
							zzz.append("__rt::Ptr<"+n.getNode(2).getNode(0).getString(0)+"> > > >(");
						} else {
							zzz.append(n.getNode(2).getNode(0).getString(0) + "> > >(");
						}
						if (n.getNode(2).getNode(1).getNode(0).hasName("UnaryExpression")) {
								zzz.append("-"+n.getNode(2).getNode(1).getNode(0).getNode(1).getString(0)+ ")");
						} else {
							zzz.append (n.getNode(2).getNode(1).getNode(0).getString(0)+ ")");
						}
					} else { 
					if (n.getNode(2).getNode(0).hasName("QualifiedIdentifier") && !n.getNode(2).getNode(0).getString(0).equals("Object") && !n.getNode(2).getNode(0).getString(0).equals("String")) {
						zzz.append("__rt::Ptr<"+n.getNode(2).getNode(0).getString(0)+"> >(");
					} else {
						zzz.append(n.getNode(2).getNode(0).getString(0) + ">(");
					}
					if (n.getNode(2).getNode(1).getNode(0).hasName("UnaryExpression")) {
							zzz.append("-"+n.getNode(2).getNode(1).getNode(0).getNode(1).getString(0)+ ")");
					} else {
						zzz.append (n.getNode(2).getNode(1).getNode(0).getString(0)+ ")");
					}
					}
				} else if(n.getNode(2) !=null && n.getNode(2).hasName("PrimaryIdentifier")){
					zzz.append(n.getNode(2).getString(0));
				}
				else if(n.getNode(2) !=null && n.getNode(2).hasName("StringLiteral")){ 
					dispatch(n.getNode(2));
				}
				else if(n.getNode(2) !=null && n.getNode(2).hasName("SelectionExpression")){ 
					zzz.append(n.getNode(2).getNode(0).getString(0) + "->" + n.getNode(2).getString(1));
				} else if (n.getNode(2) != null) {
					zzz.append(n.getNode(2).getString(0));
				}

			}
			
			public void visitNewClassExpression(GNode n)
			{
			   if (n.getNode(2).getString(0).equals("Object") || n.getNode(2).getString(0).equals("String")) {
				zzz.append("__"+n.getNode(2).getString(0)+"::init(new __"+n.getNode(2).getString(0)+"()");
			   } else {
				zzz.append(n.getNode(2).getString(0)+"::init(new "+n.getNode(2).getString(0)+"()");
			   }
				for (int i = 0; i < n.getNode(3).size(); i++) {
				    zzz.append(", ");
				    if (n.getNode(3).getNode(0).hasName("StringLiteral")) {
						dispatch(n.getNode(3).getNode(0));
				    } else {
					zzz.append(n.getNode(3).getNode(0).getString(0));
				    }
				}
				zzz.append(")");
			}			
			public void visitAdditiveExpression(GNode n)
			{
				if(n.getNode(0).hasName("PrimaryIdentifier")) {
					dispatch(n.getNode(0));
				} else {
					zzz.append(n.getNode(0).getString(0));
				}
				zzz.append(" " + n.getString(1) + " ");
				if(n.getNode(2).hasName("PrimaryIdentifier")) {
					dispatch(n.getNode(2));
				} else {
					zzz.append(n.getNode(2).getString(0));
				}
			}
			public void visitMultiplicativeExpression(GNode n)
			{
				if(n.getNode(0).hasName("PrimaryIdentifier")) {
					dispatch(n.getNode(0));
				} else {
					zzz.append(n.getNode(0).getString(0));
				}
				zzz.append(" " + n.getString(1) + " ");
				if(n.getNode(2).hasName("PrimaryIdentifier")) {
					dispatch(n.getNode(2));
				} else {
					zzz.append(n.getNode(2).getString(0));
				}
		}
		public void visitIntegerLiteral(GNode n)
			{	
				zzz.append(n.getString(0));
			}
			
			public void visitPrimaryIdentifier(GNode n)
			{	
				if(n.getString(0).equals("System")){
					zzz.append("\t"+"std::cout << ");
				}
				else if (!currMethod.equals("") && !currMethod.equals("main")){
					ArrayList<String> tmp = methodVars.get(currMethod);
					if (/*inMethod && */!tmp.contains(n.getString(0))) {
						zzz.append("__this->"+n.getString(0));
					} else {
						zzz.append(n.getString(0));
					}
				} else {
					zzz.append(n.getString(0));
				}
			}
			public void visitBlock(GNode n)
			{
				for(int i = 0; i < n.size(); i++)
				{
					if(n.getNode(i).hasName("Block")){
						zzz.append("{");
						dispatch(n.getNode(i));
						zzz.append("\n}");
					} else if (n.getNode(i).hasName("ForStatement") || n.getNode(i).hasName("WhileStatement")) {
						dispatch(n.getNode(i));
					} else if(n.getNode(i).hasName("ExpressionStatement"))
					{
			
						if(n.getNode(i).getNode(0).hasName("CallExpression"))
						{
							if (n.getNode(i).getNode(0).getNode(0).hasName("PrimaryIdentifier")){
								zzz.append("\t");
								dispatch(n.getNode(i).getNode(0));
								zzz.append("\n");
							}
							else{
								dispatch(n.getNode(i).getNode(0));
								zzz.append("\n");
							}	
						}
						else if(n.getNode(i).getNode(0).getNode(0) != null && n.getNode(i).getNode(0).getNode(0).hasName("SelectionExpression"))
						{
							if(n.getNode(i).getNode(0).getNode(0).getNode(0).hasName("ThisExpression"))
							{
								zzz.append("\tthis->" + n.getNode(i).getNode(0).getNode(0).getString(1) + n.getNode(i).getNode(0).getString(1) + n.getNode(i).getNode(0).getNode(2).getString(0) + ";");
							}
							else
							{
								zzz.append(n.getNode(i).getNode(0).getNode(0).getNode(0).getString(0) + "->" + n.getNode(i).getNode(0).getNode(0).getString(1));
								zzz.append(" " + n.getNode(i).getNode(0).getString(1));
								dispatch(n.getNode(i).getNode(0).getNode(2));
								zzz.append(";\n");
							}
						} else if (n.getNode(i).getNode(0).hasName("Expression")) {
							dispatch(n.getNode(i).getNode(0));
						} else {
							zzz.append("\t" + n.getNode(i).getNode(0).getNode(0).getString(0) + " ");
							zzz.append(n.getNode(i).getNode(0).getString(1) + " ");
							if(n.getNode(i).getNode(0).getNode(2).hasName("ThisExpression")){
								zzz.append("this;");
							}
							else{
								zzz.append(n.getNode(i).getNode(0).getNode(2).getString(0) + ";\n");
							}
							
						}
					}
					else if(n.getNode(i).hasName("FieldDeclaration"))
					{
						if (n.getNode(i).getNode(1).getNode(0).getString(0).equals("String")){
							zzz.append("\t");
							dispatch((Node) n.get(i));
						}
						else{
							dispatch((Node) n.get(i));
						}
					}
					else{
						dispatch((Node) n.get(i));
					}
				}
			}

			public void visitConstructorDeclaration(GNode n) {
				inConstructor = true;
				constructor = true;
				zzz.append("__rt::Ptr<"+className+"> " + className+"::init(__rt::Ptr<"+className +"> __this");
				String put = (className+"::init( __this");
				
				methodVars.put("con"+className, new ArrayList<String>());
				currMethod = "con"+className;

				for (int i = 0; i < n.getNode(3).size(); i++) {
				  put += (", "+ n.getNode(3).getNode(i).getString(3));
				  zzz.append(", ");
				  dispatch(n.getNode(3).getNode(i));
				}
				zzz.append(") {\n");
				put+=(");\n");
				consMap.put(className, put);
				if (consMap.containsKey(superName)) {
					zzz.append("\t"+consMap.get(superName));
				}
				if (null != initVars.get(className)) {
					zzz.append(initVars.get(className)); 
				}
				int i = 0;
				while (i < n.getNode(5).size()) {
					dispatch(n.getNode(5).getNode(i));
					i++;
				}
				zzz.append("\treturn __this;\n}\n");	
				currMethod = "";
				inConstructor = false;			 
			}
			public void  visitExpressionStatement (GNode n) {
			  if (inConstructor) {
				String first = "";
				String next = "";
				if (n.getNode(0).hasName("CallExpression")) {
				  if (n.getNode(0).getString(2).equals("super")) { return;}
				  if (n.getNode(0).getNode(0) == null) {
					zzz.append(first);
				  }

				}
				if (n.getNode(0).getNode(0).hasName("SelectionExpression")) {
					if (n.getNode(0).getNode(0).getNode(0) != null && !n.getNode(0).getNode(0).getNode(0).hasName("ThisExpression") && n.getNode(0).getNode(0).getNode(0).getString(0).equals("System")) {
					    ArrayList<String> tmp = methodVars.get(currMethod);
					    if (!tmp.contains(n.getNode(0).getNode(3).getNode(0).getString(0))) {
					      zzz.append("\tstd::cout << __this->");
					    } else {
					      zzz.append("\tstd::cout <<");
					    }
					    if (!n.getNode(0).getNode(3).getNode(0).hasName("PrimaryIdentifier")) {
						zzz.append("__vptr->");
					    }
					    zzz.append(n.getNode(0).getNode(3).getNode(0).getString(0)+" << std::endl;\n");
					    return;
					}	
					first += "__this";
					if (n.getNode(0).getNode(0).getString(1) != null) {
						first+=("->"+ n.getNode(0).getNode(0).getString(1));
					}
				}
				if (n.getNode(0).hasName("Expression") && n.getNode(0).getNode(0).hasName("PrimaryIdentifier")) {
					first +=( "__this->"+n.getNode(0).getNode(0).getString(0));
				}
				if (n.getNode(0).getNode(2).hasName("ThisExpression")) { 
					next += "__this";
				} else {
					if (n.getNode(0).getNode(2).hasName("StringLiteral")) {
						next+=("__rt::literal("+n.getNode(0).getNode(2).getString(0)+")");
					} else { 
						next += n.getNode(0).getNode(2).getString(0);
					}
				}	
				zzz.append("\t\t" + first + " " + n.getNode(0).getString(1) + " " + next + ";\n");
			  }
			}
			public void visitStringLiteral(GNode n)
			{
				zzz.append("__rt::literal("+n.getString(0)+")");
			}
		
			public void visitFloatingPointLiteral(GNode n) {
				zzz.append(n.getString(0));
			}
	
			public void visitReturnStatement(GNode n)
			{
				zzz.append("\treturn ");
					visit(n);
				zzz.append(";\n");
			}
			
			public void visitArguments(GNode n)
			{
				Node nextArg = null;
				for(Object c : n)
				{
					if(c != null && c instanceof Node)
					{
						if(nextArg != null)
						{
							dispatch(nextArg);
							// Insert commas between arguments
							zzz.append(", ");
						}
						nextArg = (Node) c;
					}
				}
				if(nextArg != null)
				{
					dispatch(nextArg);
				}
			}

			public void visitCallExpression(GNode n)
			{
				String nested_args = "";
				dispatch((Node) n.get(0)); // Print the object
				if(n.getString(2).equals("println"))
				{
					if(!(n.getNode(3).getNode(0).hasName("CallExpression")))
					{
						if(n.getNode(3) != null && n.getNode(3).size() != 0)
						{
							for(int a =0; a < n.getNode(3).size(); a++)
							{
								if(n.getNode(3).getNode(a).hasName("SelectionExpression"))
								{
									if (n.getNode(3).getNode(a).getNode(0).hasName("PrimaryIdentifier")) {
									    zzz.append(n.getNode(3).getNode(a).getNode(0).getString(0) + "->"+ n.getNode(3).getNode(a).getString(1));
									} else {
									    zzz.append(n.getNode(3).getNode(a).getNode(0).getString(0) + "->__vptr->" + n.getNode(3).getNode(a).getString(1));
									}
								}
								else{
									dispatch(n.getNode(3).getNode(a));
								}
								if(a < n.getNode(3).size() -1)
								{
									zzz.append(", ");
								}
						}	}
					}
					else{
						if(n.getNode(3).getNode(0).getNode(0) == null){
							zzz.append(className+"::"+n.getNode(3).getNode(0).getString(2) + "(NULL)");
						}
						else if(n.getNode(3).getNode(0).getNode(0).hasName("SelectionExpression")){
							if (n.getNode(3).getNode(0).getNode(0).getString(1).equals("self") || n.getNode(3).getNode(0).getNode(0).getString(1).equals("some")) {
							  zzz.append(n.getNode(3).getNode(0).getNode(0).getNode(0).getString(0) + "->" + n.getNode(3).getNode(0).getNode(0).getString(1) + "->__vptr->" +n.getNode(3).getNode(0).getString(2) + "(");
							} else {
							  zzz.append(n.getNode(3).getNode(0).getNode(0).getNode(0).getString(0) + "->" + n.getNode(3).getNode(0).getNode(0).getString(1) + "->" +n.getNode(3).getNode(0).getString(2) + "(");
							}
							zzz.append(n.getNode(3).getNode(0).getNode(0).getNode(0).getString(0) + "->" + n.getNode(3).getNode(0).getNode(0).getString(1)+")");
						} else if (n.getNode(3).getNode(0).hasName("SubscriptExpression")) {
							dispatch(n.getNode(3).getNode(0));
						} else{
							dispatch((Node) n.get(3));
						}
					}
					zzz.append(" << std::endl;");
				}
				else if(n.getString(2).equals("print"))
				{
					dispatch((Node) n.get(3)); // Print the value 
					
				}
				else
				{
					if (!classes.contains(n.getNode(0).getString(0))) {
						zzz.append("->__vptr->");
					} else {
						zzz.append("::");
					}
					if (n.getNode(3).size() == 0 ||n.getString(2).equals("toString") || n.getString(2).equals("hashCode") || n.getString(2).equals("getClass") || n.getString(2).equals("equals")) {
						zzz.append(n.getString(2));
					} else {
						zzz.append(n.getString(2));
						StringBuilder mangle = new StringBuilder();
						for(int i = 0; i < n.getNode(3).size(); i++) {
							if (n.getNode(3).getNode(i).hasName("StringLiteral")) {
								mangle.append("_String");
							} else if (n.getNode(3).getNode(i).hasName("FloatingPointLiteral")) {
								mangle.append("_double");
							} else if (n.getNode(3).getNode(i).hasName("CastExpression")) { 
								mangle.append("_"+n.getNode(3).getNode(0).getNode(0).getNode(0).getString(0));
							} else if (n.getNode(3).getNode(i).hasName("NewClassExpression")) {
								mangle.append("_"+n.getNode(3).getNode(i).getNode(2).getString(0));
							} else if (n.getNode(3).getNode(i).hasName("AdditiveExpression")) {
								mangle.append("_"+n.getNode(3).getNode(i).getNode(0).getString(0));
							} else {
							String type = argType.get(n.getNode(3).getNode(i).getString(0));
							if (type == null) {
								runtime.console().p("We have a problem!").pln().flush();
							} else if (type.equals("byte")) {
								mangle.append("_int");
							} else {
								mangle.append("_"+type);
							}
							}
						}
						// check if the name we have created is a real method
						if (methNames.get(n.getString(2)+mangle.toString()) == null) {
							runtime.console().p("We have created the wrong name: "+ n.getString(2)+mangle.toString()).pln().flush();
							int index = n.getNode(3).size()-1;
							int i = 0;
							StringBuilder tmp = new StringBuilder();
							while( i < n.getNode(3).size()) {
								// cast up each arg started from last
								if (i == index) {
								   if (n.getNode(3).getNode(i).hasName("PrimaryIdentifier") && !n.getNode(3).getNode(i).equals("Object")) {
								      if ( i == 0) {
									tmp.append(superMap.get(argType.get(n.getNode(3).getNode(i).getString(0))));
								      } else {
									tmp.append("_"+superMap.get(argType.get(n.getNode(3).getNode(i).getString(0))));
								      }
								    }
								} else {
						 		  if (n.getNode(3).getNode(i).hasName("StringLiteral")) {
								    tmp.append("_String");
								  } else if (n.getNode(3).getNode(i).hasName("FloatingPointLiteral")) {
								    tmp.append("_double");
								  } else if (n.getNode(3).getNode(i).hasName("CastExpression")) { 
								    tmp.append("_"+n.getNode(3).getNode(0).getNode(0).getNode(0).getString(0));
								  } else if (n.getNode(3).getNode(i).hasName("NewClassExpression")) {
								    tmp.append("_"+n.getNode(3).getNode(i).getNode(2).getString(0));
								  } else if (n.getNode(3).getNode(i).hasName("AdditiveExpression")) {
								    tmp.append("_"+n.getNode(3).getNode(i).getNode(0).getString(0));
							          } else {
							            String type = argType.get(n.getNode(3).getNode(i).getString(0));
								  if (type == null) {
								     runtime.console().p("We have a problem!").pln().flush();
								  } else if (type.equals("byte")) {
								    tmp.append("_int");
								  } else {
								    tmp.append("_"+type);
								  }
								
								  }
								}
								runtime.console().p("tmp at: " + tmp.toString()).pln().flush();
								i++;
								if (i == n.getNode(3).size()) {
									// check the map to see if this name is right
									runtime.console().p("Now trying name (size is "+n.getNode(3).size()+", i is "+i+"): "+tmp.toString()).pln().flush();
									if (methNames.get(n.getString(2)+tmp.toString()) == null) {
									if (--index >= 0) {
									  tmp.setLength(0);
									  i = 0; // start over
									} else { return; }
									}
								}
							}
							zzz.append(tmp.toString());
						} else {
							zzz.append(mangle.toString());
						}
					}
					if (n.getNode(0).hasName("PrimaryIdentifier")) {
						zzz.append("(");
						if (classes.contains(n.getNode(0).getString(0)) && !n.getNode(0).getString(0).equals("Object")) {
							zzz.append(n.getNode(0).getString(0).toLowerCase());
						} else {
							zzz.append(n.getNode(0).getString(0));
						}
					} else if (n.getNode(0).hasName("CallExpression")) {
						zzz.append("(");
						dispatch(n.getNode(0).getNode(0));
						zzz.append("->__vptr->"+n.getNode(0).getString(2)+"(");
						dispatch(n.getNode(0).getNode(0));
						zzz.append(")");
					} else if (n.getNode(0).getNode(1) != null && n.getNode(0).getNode(1).hasName("SubscriptExpression")) {
						zzz.append("((*"+n.getNode(0).getNode(1).getNode(0).getString(0)+")["+n.getNode(0).getNode(1).getNode(1).getString(0)+"]");
					} else {
						if (classes.contains(n.getNode(0).getString(0)) && !n.getNode(0).getString(0).equals("Object")) {
							zzz.append("("+n.getNode(0).getString(0).toLowerCase());
						} else {
							zzz.append("("+n.getNode(0).getString(0));
						}
					}
					if (n.getNode(3).size() > 0) {zzz.append(", ");}
					dispatch((Node) n.get(3)); // Print the args
					if(n.getNode(3).size() != 0 &&n.getNode(3).getNode(0).hasName("StringLiteral")) {
						zzz.append(");");
					} else if(n.getNode(3).size() != 0 && n.getNode(3).getNode(0).hasName("PrimaryIdentifier")){
						zzz.append(");");
					} else {
						zzz.append(")");
					}
				}
			}
			public void visitQualifiedIdentifier(GNode n)
			{
				if (Ptr) {
					pointer = false;
					zzz.append(n.getString(0));
					return;
				}	
				if(n.getString(0).equals("String")) {
					pointer =false;
					zzz.append("String ");
				} else if(n.getString(0).equals("Object")) {
					pointer = false;
					zzz.append("\t"+n.getString(0));
				}
				else {
					zzz.append("\t"+n.getString(0));
				}
			}
			
			public void visitFieldDeclaration(GNode n)
			{
			if (!inMethod) {
					if (n.getNode(0).size() > 0 && n.getNode(0).getNode(0).getString(0).equals("static")) {
						zzz.append(n.getNode(1).getNode(0).getString(0) + " "+className+"::"+n.getNode(2).getNode(0).getString(0));
						if (n.getNode(2).getNode(0).getNode(1) == null && n.getNode(1).getNode(0).getString(0).equals("int")) {
						  zzz.append(" = 0;\n");
						} else {
						  zzz.append(";\n");
						}
					}
					if (n.getNode(2).getNode(0).getNode(2) != null) {
						String tmp = initVars.get(className);
						tmp+=("\t__this->"+n.getNode(2).getNode(0).getString(0)+ " = __rt::literal(" +n.getNode(2).getNode(0).getNode(2).getString(0)+");\n");
						initVars.put(className, tmp);
						return;
					}else { return; }
			}
				//add variable to args map
				argType.put(n.getNode(2).getNode(0).getString(0), n.getNode(1).getNode(0).getString(0));
				
				if (inMethod && !currMethod.equals("main")) {
					ArrayList<String> tmp = methodVars.get(currMethod);
					tmp.add(n.getNode(2).getNode(0).getString(0));
				}
				if (n.getNode(1).getNode(1) != null && n.getNode(1).getNode(1).hasName("Dimensions")) {
					Ptr = true;
					zzz.append("\t__rt::Ptr<__rt::Array<");
					if (n.getNode(1).getNode(1).size() == 2) {
						// 2D array
						zzz.append("__rt::Ptr<__rt::Array<");
						if (n.getNode(1).getNode(0).hasName("QualifiedIdentifier") && !n.getNode(1).getNode(0).getString(0).equals("Object") && !n.getNode(1).getNode(0).getString(0).equals("String")) {
							zzz.append("__rt::Ptr<");
							dispatch(n.getNode(1));
							zzz.append("> > > ");
						} else {
							dispatch((Node) n.get(1)); // Print data type
							zzz.append("> > ");
						}
					} else if (n.getNode(1).getNode(0).hasName("QualifiedIdentifier") && !n.getNode(1).getNode(0).getString(0).equals("Object") && !n.getNode(1).getNode(0).getString(0).equals("String")) {
							zzz.append("__rt::Ptr<");
							dispatch(n.getNode(1));
							zzz.append("> ");
					} else {
						dispatch((Node) n.get(1)); // Print data type
					}
					zzz.append("> >");

				}else if(n.getNode(1).getNode(0).hasName("PrimitiveType")){
					pointer = false;
					if (n.getNode(1).getNode(0).getString(0).equals("byte")) {
						n.getNode(1).getNode(0).set(0, "int");
					}
					dispatch(n.getNode(1).getNode(0));
				}
				else if (n.getNode(1).getNode(0).hasName("QualifiedIdentifier") && !n.getNode(1).getNode(0).getString(0).equals("String") && !n.getNode(1).getNode(0).getString(0).equals("Object") && n.getNode(1).getNode(1) == null) {
					pointer = true;
					zzz.append("\t__rt::Ptr<"+n.getNode(1).getNode(0).getString(0)+"> ");
				}
				else if (n.getNode(1).getNode(0).hasName("QualifiedIdentifier")  && (n.getNode(1).getNode(0).getString(0).equals("String") || n.getNode(1).getNode(0).getString(0).equals("Object")) && n.getNode(1).getNode(1) == null) {
					dispatch(n.getNode(1));
					zzz.append(" ");
				} else {
					if (!pointer) {
						dispatch(n.getNode(1));
					}
					zzz.append(" ");
				}
				if (/*!inMethod && */n.getNode(2).getNode(0).getNode(2) != null && n.getNode(2).getNode(0).getNode(2).hasName("PrimaryIdentifier")/* && !mainClass*/ && !Ptr) {
					if (n.getNode(1).getNode(0).hasName("QualifiedIdentifier") && n.getNode(1).getNode(0).getString(0).equals("Object")) {
						zzz.append(" " +n.getNode(2).getNode(0).getString(0) + " =  (Object)" + n.getNode(2).getNode(0).getNode(2).getString(0));
					} else if (n.getNode(1).getNode(0).hasName("QualifiedIdentifier")) {
						zzz.append(n.getNode(2).getNode(0).getString(0) + " = (__rt::Ptr<" + n.getNode(1).getNode(0).getString(0)+ ">)"+ n.getNode(2).getNode(0).getNode(2).getString(0));
					}
				} else {
					if (!inMethod && n.getNode(2).getNode(0).getNode(2) == null && !mainClass) {
						zzz.append(className + "::");
					}
					dispatch((Node) n.get(2)); // Print variable names
				}
				zzz.append(";\n");
				Ptr = false;
				if (n.getNode(1).getNode(1) != null && n.getNode(1).getNode(1).hasName("Dimensions") && n.getNode(1).getNode(1).size() == 2) {
					// need to initialize 2D array
					zzz.append("\tfor(int i = 0; i < "+n.getNode(2).getNode(0).getString(0)+"->length; i++) {\n\t\t(*");
					zzz.append(n.getNode(2).getNode(0).getString(0)+")[i] = new __rt::Array<");
					if (classes.contains(n.getNode(1).getNode(0).getString(0))) {
					  zzz.append("__rt::Ptr<"+n.getNode(1).getNode(0).getString(0)+"> >(" + n.getNode(2).getNode(0).getNode(2).getNode(1).getNode(1).getString(0)+ ");\n\t}\n");
					} else {
					  zzz.append(n.getNode(1).getNode(0).getString(0)+">(" + n.getNode(2).getNode(0).getNode(2).getNode(1).getNode(1).getString(0)+ ");\n\t}\n");
					}
				}
			}
			
			public void visitForStatement(GNode n) {
				String var = n.getNode(0).getNode(2).getNode(0).getString(0);
				zzz.append("\tfor (");
				zzz.append (n.getNode(0).getNode(1).getNode(0).getString(0)+ " " + var); // print type and var	
				if (n.getNode(0).getNode(2).getNode(0).getNode(2) != null) {
					//set value for start of loop
					zzz.append(" = " + n.getNode(0).getNode(2).getNode(0).getNode(2).getString(0) + ";");
				} else {
					zzz.append(";");
				}

				//print conditional clause
				if (n.getNode(0).getNode(3).getNode(2).getNode(0).hasName("SubscriptExpression")) {
					zzz.append(" " + var + " " + n.getNode(0).getNode(3).getString(1) + " ((*" + n.getNode(0).getNode(3).getNode(2).getNode(0).getNode(0).getString(0)+")[" + n.getNode(0).getNode(3).getNode(2).getNode(0).getNode(1).getString(0) + "])");
				} else {
					zzz.append(" " + var + " " + n.getNode(0).getNode(3).getString(1) + " " + n.getNode(0).getNode(3).getNode(2).getNode(0).getString(0));
				}
				if (n.getNode(0).getNode(3).getNode(2).getString(1) != null) {
					zzz.append("->"+n.getNode(0).getNode(3).getNode(2).getString(1)+"; ");
				} else {
					zzz.append("; ");
				}
				
				// print update clause
				zzz.append(var + n.getNode(0).getNode(4).getNode(0).getString(1) + ") {\n");

				// visit the block of the for loop
				dispatch(n.getNode(1));
				zzz.append("\t}\n");
				
			}
	
			public void visitWhileStatement(GNode n) {
				//print conditional statement
				zzz.append("\twhile ("+n.getNode(0).getNode(0).getString(0) + " " + n.getNode(0).getString(1) + " " + n.getNode(0).getNode(2).getString(0) + ") {\n");
				// visit block
				dispatch(n.getNode(1));
				zzz.append("\t}\n");
			}

			public void visitExpression(GNode n) {
				zzz.append("\t\t");
				for (int i = 0; i < n.size(); i++) {
					if (i == 1 && n.get(i) != null) {
						zzz.append(" " + n.getString(i) + " ");
					} else { 
						dispatch(n.getNode(i));
					}	
				}
				zzz.append(";\n");
			}

			public void visitSubscriptExpression(GNode n) {
				zzz.append("(*");
				dispatch(n.getNode(0));
				zzz.append(")[");
				dispatch(n.getNode(1));
				zzz.append("]");
			}		
		
			public void visitCastExpression(GNode n){
				zzz.append("(");
				if (n.getNode(0).getNode(0).hasName("QualifiedIdentifier") && !n.getNode(0).getNode(0).getString(0).equals("Object") && !n.getNode(0).getNode(0).getString(0).equals("String")) {
					zzz.append("(__rt::Ptr<" + n.getNode(0).getNode(0).getString(0)+ ">) ");
				} else {
					zzz.append("(" + n.getNode(0).getNode(0).getString(0) + ") ");
				}
				dispatch(n.getNode(1));
				zzz.append(")");
			}
			public void visit(Node n) 
			{
				for (Object o : n) 
				{
					if (o instanceof Node)
					{						
						dispatch((Node) o);
					}
				}
			}
		}
		.dispatch(node);
	zzz.append("\n int main(int argc, char* argv[]) {\n\t__rt::Ptr<__rt::Array<String> > args = new __rt::Array<String>(argc-1);\n\n\tfor (int32_t i = 1; i < argc; i++) {\n\t\t(*args)[i-1] = __rt::literal(argv[i]);\n\t}\n\n\t" + testClass + "::main(args);\n\n\treturn 0;\n}");
	return zzz;
	}
	public static void main(String[] args) 
	{
		new X().run(args);
	}
}
