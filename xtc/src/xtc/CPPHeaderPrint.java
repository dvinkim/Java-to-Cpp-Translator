/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2012 Robert Grimm
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301,
 * USA.
 */
//package xtc;
import java.util.*;
import java.util.Iterator;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import xtc.lang.JavaFiveParser;
import xtc.parser.ParseException;
import xtc.parser.Result;
import xtc.tree.GNode;
import xtc.tree.Node;
import xtc.tree.Visitor;
import xtc.util.Tool;
/**
 * A CPPHeaderPrint from (a subset of) Java to (a subset of) C++.
 *
 * @author Kelvin Betances
 * @author SD
 */
public class CPPHeaderPrint extends Tool
{
  //ArrayList of public methods/constructors
  ArrayList publicMethods = new ArrayList();

  //ArrayList of private methods
  ArrayList privateMethods = new ArrayList();

  /** Create a new CPPHeaderPrint. */
  public CPPHeaderPrint()
  {
    // Nothing to do.

  }

  public String getName()
  {
    return "Java to ++ CPPHeaderPrint";
  }

  public String getCopy()
  {
    return "(C) 2014 <The Flying Staplers>";
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
  public StringBuilder translate(Node node)
  {
      new Visitor()
      {
	private String className = "";
        private boolean constructor = false;
	private int numClasses = 0;
	private String superName = "";
	private String conString = "";
	private List<String> classes = new ArrayList<String>();
	private Map<String, String> consMap = new HashMap<String, String>();
	private Map<String, String> varMap = new HashMap<String, String>();
	private HashMap<String, ArrayList<String>> classVars = new HashMap<String, ArrayList<String>>();
        public void visitClassDeclaration(GNode n)
        {
          //begin printing header file -->
	  numClasses++;
	  classes.add(n.getString(1));
          zzz.append("struct __"+ n.getString(1)+"_VT;\nclass " + n.getString(1) + "{" + "\n");
	  className = n.getString(1);
	  if (n.getNode(3) != null) {
		// then this class extends another
		superName = n.getNode(3).getNode(0).getNode(0).getString(0);
	  }
	  classVars.put(className, new ArrayList<String>());
	  zzz.append("  public:\n");
	  zzz.append("\t__"+ className + "_VT* __vptr;\n" + "\tstatic __"+className+ "_VT __vtable;\n\tstatic Class __class();\n\t"+className+"();\n");
	  // print parent variables for inheritance
	  if (!superName.equals("")) {
		ArrayList<String> parent = classVars.get(superName);
		ArrayList<String> child = classVars.get(className);
		for (String i : parent) {
			zzz.append("\t"+i+";\n");
			child.add(i);
		}
	  }
          //visit all methods within the current class
          visit(n);

          //print all private methods

          for(int i = 0; i< privateMethods.size(); i++)
          {
            zzz.append(privateMethods.get(i)+"\n");
          }

          //print all public methods and constructors
          for(int i = 0; i< publicMethods.size(); i++)
          {
            zzz.append(publicMethods.get(i)+"\n");
          }
	
	//look through super's var list to see if there's any that need to be inherited
	if (!constructor) {
		zzz.append("\tstatic __rt::Ptr<"+className+"> init(__rt::Ptr<"+className+">);\n");
	} else {
		zzz.append(conString);
		conString = "";
	}
	 // print __delete method for automatic memory mgmt
	  zzz.append("\tstatic void __delete("+className+"* a) {\n\t\tdelete a;\n\t}\n");

        zzz.append("};\n"); //end file

          //empty array list
          publicMethods.clear();
          privateMethods.clear();
	  superName = "";
	}

        public void visitCompilationUnit(GNode n)
        {
          visit(n);


        }

        public void visitConstructorDeclaration(GNode n)
        {
	    String init = "static __rt::Ptr<"+className+"> init(__rt::Ptr<"+className+">";
	    constructor = true;
            //get all the constructor arguments types
            for(int i = 0; i < n.getNode(3).size(); i++)
            {
              //get parameter type of the constructor argument
              String paramType = (", "+n.getNode(3).getNode(i).getNode(1).getNode(0).getString(0));

              //change strings and booleans into C++ syntax
              if(paramType.equals ("String"))
              {
		  paramType = ", String";
              }
              else if(paramType == "boolean")
              {
                paramType = ", bool";
              }
		init+=paramType;
              //name of the parameter
      }
		init+=");\n";
		conString = init;
         }

        public void visitMethodDeclaration(GNode n)
        {
	    String methodParameterString = "";
            if(!n.getString(3).equals("main")) { 
	      methodParameterString = "__rt::Ptr<"+ className+">";
	    }
            String methodReturnType;
            try
            {
                methodReturnType = n.getNode(2).getNode(0).getString(0);
            }catch(Exception e){methodReturnType = "void";}

            if(methodReturnType.equals( "String"))
            {
              methodReturnType = /*"std::string";*/ "String";
            }
            else if(methodReturnType == "boolean")
            {
              methodReturnType = "bool";
            }

            String methodName = n.getString(3);

            //check all method parameters
            for(int i = 0; i < n.getNode(4).size(); i++)
            {
	      if (!n.getString(3).equals("main")) {methodParameterString+=", ";}
              String methodParameterType = n.getNode(4).getNode(i).getNode(1).getNode(0).getString(0);

              if(methodParameterType.equals( "String") && n.getString(3).equals("main"))
              {
                methodParameterType = "__rt::Ptr<__rt::Array<String> >";
              }
              else if(methodParameterType == "boolean")
              {
                methodParameterType = "bool";
              } else if (classes.contains(methodParameterType)) {
		methodParameterType = "__rt::Ptr<"+methodParameterType+">";
	      }

              //add param name
              String methodParameterName = n.getNode(4).getNode(i).getString(3);

               methodParameterString += methodParameterType + " " + methodParameterName;
            }

            //string containing all parts of a method declaration
          String newMethod = ("\tstatic " + methodReturnType + " " + methodName + "(" + methodParameterString + ");"  );

          //check if the method is public or private
//          if(n.getNode(0) != null && (n.getNode(0).getNode(0).getString(0) == "public"))
//          {
            publicMethods.add(newMethod);
 //         }
//          else
//          {
//            privateMethods.add(newMethod);
//          }

        }

	public void visitFieldDeclaration(GNode n) {
		if (!superName.equals("")) {
			ArrayList<String> parent = classVars.get(superName);
			for (String s : parent) {
			  if (n.getNode(1).hasName("VoidType")) {
				if (("void " + n.getNode(2).getNode(0).getString(0)).equals(s)) {
					return;
				}
			  } else {
				if ((n.getNode(1).getNode(0).getString(0) + " " + n.getNode(2).getNode(0).getString(0)).equals(s)) {
					return;
				}
			  }
			}
		}
		String var = "";
		if (n.getNode(0).size() > 0 && n.getNode(0).getNode(0).getString(0).equals("static")) {
			zzz.append("static ");
		}
		if (n.getNode(1).hasName("VoidType")) {
			zzz.append("void ");
			var+=("void ");
		} else if (n.getNode(1).getNode(0).hasName("QualifiedIdentifier") && !n.getNode(1).getNode(0).getString(0).equals("Object") && !n.getNode(1).getNode(0).getString(0).equals("String")) {
			zzz.append("__rt::Ptr<"+n.getNode(1).getNode(0).getString(0)+"> ");
			var+=("__rt::Ptr<"+n.getNode(1).getNode(0).getString(0)+"> ");
	
		} else if (n.getNode(1).getNode(0) != null && n.getNode(1).getNode(0).getString(0).equals("boolean")) {
			zzz.append("bool ");
			var+=("bool ");
		} else {
			zzz.append(n.getNode(1).getNode(0).getString(0)+ " ");
			var+=(n.getNode(1).getNode(0).getString(0)+ " ");
		}
		zzz.append(n.getNode(2).getNode(0).getString(0) + " ");
		var+=(n.getNode(2).getNode(0).getString(0));

		if (!constructor) {
		  ArrayList<String> tmp = classVars.get(className);
		  if (tmp == null) {
			runtime.console().p("Problem with the classVars map").pln().flush();
		  }
		  tmp.add(var);
		}
	
		if (n.getNode(2).getNode(0).getNode(1) != null) {
			zzz.append(n.getNode(2).getNode(0).getString(1) + " ");
			zzz.append(n.getNode(2).getNode(0).getNode(2).getString(0));
		} else {
			varMap.put(n.getNode(2).getNode(0).getString(0), className);
		}
		zzz.append(";\n");
	}

 	public void visitBlock (GNode n) {
 		visit(n);
 	}
 
 	public String visitExpressionStatement (GNode n) {
 		String first = "";
 		String next = "";
		if (n.getNode(0).hasName("CallExpression")) {
		  if (n.getNode(0).getNode(0) == null) {
			return first;
		  }
		  if (className.equals(varMap.get(n.getNode(0).getNode(3).getNode(0).getString(0)))) {
			String ret = "\tstd::cout << " + n.getNode(0).getNode(3).getNode(0).getString(0) + " << std::endl;\n";
			return ret;
		  } else {
			String ret = "\tstd::cout << " + superName+ "::"+ n.getNode(0).getNode(3).getNode(0).getString(0) + " << std::endl;\n";
			return ret;
		  }
		}
 		if (n.getNode(0).getNode(0).hasName("SelectionExpression")) {
 			first += "this";
			if (n.getNode(0).getNode(0).getString(1) != null) {
				first+=("->"+ n.getNode(0).getNode(0).getString(1));
			}
 		} else {
 		  if (className.equals(varMap.get(n.getNode(0).getNode(0).getString(0)))) {
			first += n.getNode(0).getNode(0).getString(0);
		  } else {
			first += (superName + "::" + n.getNode(0).getNode(0).getString(0));
		  }
 		}
 		if (n.getNode(0).getNode(2).hasName("ThisExpression")) { 
 			next += "*this";
 		} else {
			if (n.getNode(0).getNode(2).hasName("StringLiteral")) {
				next+=dispatch(n.getNode(0).getNode(2));
			} else { 
				next += n.getNode(0).getNode(2).getString(0);
			}
 		}	
 		String ret = "\t\t" + first + " " + n.getNode(0).getString(1) + " " + next + ";\n";
 		return ret;
 	}

	public String visitStringLiteral(GNode n) {
		return "__rt::literal("+n.getString(0)+")";
	}

        public void visit(Node n)
        {
          for (Object o : n) if (o instanceof Node) dispatch((Node)o);
        }
      }.dispatch(node);
	return zzz;
  }

  /**
   * Run the CPPHeaderPrint with the specified command line arguments.
   *
   * @param args The command line arguments.
   */
  public static void main(String[] args)
  {
    new CPPHeaderPrint().run(args);

  }

}
