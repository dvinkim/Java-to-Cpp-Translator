//package xtc;
import java.util.*;
import java.io.File;
import java.io.IOException;
import java.io.Reader;

import xtc.lang.JavaFiveParser;

import xtc.parser.ParseException;
import xtc.parser.Result;

import xtc.tree.GNode;
import xtc.tree.Node;
import xtc.tree.Printer;
import xtc.tree.Visitor;

/**
 * A tool to print VT classes in .h files in C++ from an AST
 *
 * @author Kate Montgomery
 */

class printHeader extends xtc.util.Tool {
  private Node root;
  public printHeader() {
  }

  public interface ICommand {
    public void run();
  }

  public String getName() {
    return "print .h files in C++ tool";
  }

  public File locate(String name) throws IOException {
    File file = super.locate(name);
    if(Integer.MAX_VALUE < file.length()) {
      throw new IllegalArgumentException(file + ": file too large");
    }
    return file;
  }

  public Node parse(Reader in, File file) throws IOException, ParseException {
    JavaFiveParser parser = new JavaFiveParser(in, file.toString(), (int)file.length());
    Result result = parser.pCompilationUnit(0);
    return (Node)parser.value(result);
  }
   
  static StringBuilder zzz = new StringBuilder();
  public StringBuilder translate(Node node) {
    new Visitor() {

      private String className = new String();
      private boolean self = false;
      private boolean delete = false;
      private boolean main = false;
      private List<String> classes = new ArrayList<String>();

      public void visitClassDeclaration(GNode n) {
	className = n.getString(1);
	classes.add(n.getString(1).replace("__", ""));
	if (className.endsWith("_VT")) {
		className = "__"+className;
	}
	if (className.endsWith("_VT")) {
	  int size1 = n.getNode(5).size();
	  zzz.append("struct " + className.replace("____","__") + " {\n");
	  for(int i = 0; i < n.getNode(5).size()-1; i++) {
		if(!n.getNode(5).getNode(i).getName().equals("FieldDeclaration") && n.getNode(5).getNode(i).getString(3).equals("toString")){
		int size = n.getNode(5).size();
		for (int j =0; j < n.getNode(5).getNode(size-1).getNode(4).size(); j++) {
			if(n.getNode(5).getNode(size-1).getNode(4).getNode(j).getNode(0).getNode(0).getString(0).equals("toString") && n.getNode(5).getNode(size-1).getNode(4).getNode(j).getNode(0).getNode(2).getString(0).contains(className.replace("_VT","").replace("__",""))) {
				//do something else
			delete = true;
			break;
			}
		}
		}
		if(!delete) {
			dispatch(n.getNode(5).getNode(i));
		} else {
			dispatch(n.getNode(5).getNode(i));
			delete = false;
		}	
		}		
	  // print delete method declaration for automatic mem mgmt	 
	  buildConstructor(n);
	  zzz.append("};\n");
	  return;
	}
	visit(n);
      }

      public void visitMethodDeclaration(GNode n) {
	if(className.endsWith("_VT")) {
	  if ( n.getString(3).equals("__isa")) {
	    zzz.append("\t"+n.getNode(2).getNode(0).getString(0)+ " "+n.getString(3)+";\n");
 	    zzz.append("\tvoid (*__delete)("+className.replace("_VT", "").replace("____", "")+"*);\n");
	    return;
	  }
	  if (n.getString(3).equals("main")) {
	    zzz.append("\tvoid (*main)(__rt::Ptr<__rt::Array<String> >);\n");
	    return;
	  }
	  if(n.getNode(2).getNode(0).getString(0).equals("std::string")) {
	 	 zzz.append("\tString (*"+n.getString(3)+")(");
	  } else {
		 zzz.append("\t"+n.getNode(2).getNode(0).getString(0)+ " (*"+n.getString(3)+")(");
	  }
	  if(n.getNode(4) != null && n.getNode(4).size() > 0) {
		String tmp = n.getString(3);
		if (!tmp.equals("toString") && !tmp.equals("getClass") && !tmp.equals("hashCode") && !tmp.equals("equals")) {
		  zzz.append("__rt::Ptr<"+className.replace("____", "").replace("_VT", "")+ ">");
		  if (n.getNode(4).size() > 0) { zzz.append(", ");}
		}
	      for(int i = 0; i < n.getNode(4).size(); i++) {
		if (i > 0) { zzz.append(", "); }
		if (classes.contains(n.getNode(4).getNode(i).getString(0).replace("__", ""))) {
			zzz.append("__rt::Ptr<"+n.getNode(4).getNode(i).getString(0).replace("__", "")+ ">");
		} else if(n.getNode(4).getNode(i).getString(0).equals("String")){
			zzz.append("String");
		} else {
			zzz.append(n.getNode(4).getNode(i).getString(0));
		}
	      }
	} else {
		zzz.append("__rt::Ptr<"+className.replace("____", "").replace("_VT", "")+">");
	}
	  zzz.append(");\n");
	} 
	visit(n);
      }

      public void buildConstructor(GNode n) {
	  zzz.append("\n\t"+className.replace("____","__")+"():\n");
	  int k = 0;
	  int size = n.getNode(5).size();
	  int print_count = 0;
	  while (n.getNode(5).getNode(k).getName().equals("FieldDeclaration")) {
	    k++;
	    print_count++;
	  }
	  zzz.append("\t"+n.getNode(5).getNode(k).getString(3)+"("+className.replace("_VT", "").replace("__","")+"::__class()),\n");
	  zzz.append("\t__delete((void(*)(" + className.replace("_VT", "").replace("____", "")+ "*))&"+className.replace("_VT", "").replace("____", "")+"::__delete),\n");
	  int i;
	  for(i = k+1; i < n.getNode(5).size()-1; i++) {
	      zzz.append("\t"+n.getNode(5).getNode(i).getString(3)+"(");
	      for(int j = 0; j < n.getNode(5).getNode(size-1).getNode(4).size(); j++) {
		if (n.getNode(5).getNode(size-1).getNode(4).getNode(j).getNode(0).getNode(0).getString(0).equals(n.getNode(5).getNode(i).getString(3)) && n.getNode(5).getNode(size-1).getNode(4).getNode(j).getNode(0).getNode(2).getString(0).contains(className.replace("_VT","").replace("__","")+"::"+n.getNode(5).getNode(i).getString(3))) {
		self = true;
		// This means that the constructor doesn't need to be cast	
		break;
		}
	      }
	if (n.getNode(5).getNode(i).getString(3).equals("main")) {
		zzz.append("(void(*)(__rt::Ptr<__rt::Array<String> >))");
	} else {
	    if (n.getNode(5).getNode(i).getNode(2).getNode(0).getString(0).equals("std::string")) {
		n.getNode(5).getNode(i).getNode(2).getNode(0).set(0, "String");
	    }
	    zzz.append("("+n.getNode(5).getNode(i).getNode(2).getNode(0).getString(0)+"(*)(");
	    String tmp = n.getNode(5).getNode(i).getString(3);
	    if (!tmp.equals("toString") && !tmp.equals("hashCode") && !tmp.equals("getClass") && !tmp.equals("equals")){
	      zzz.append("__rt::Ptr<"+className.replace("____", "").replace("_VT", "")+">");
	      if (n.getNode(5).getNode(i).getNode(4).size() > 0) { zzz.append(",");}
	    }
	    for (int j = 0; j < n.getNode(5).getNode(i).getNode(4).size(); j++) {
		if(j!= 0) {zzz.append(", ");}
		  if (classes.contains(n.getNode(5).getNode(i).getNode(4).getNode(j).getString(0))) { 
		    zzz.append("__rt::Ptr<"+n.getNode(5).getNode(i).getNode(4).getNode(j).getString(0) + ">");
		  } else {
		    zzz.append(n.getNode(5).getNode(i).getNode(4).getNode(j).getString(0));
		  }
	    }
	    zzz.append("))");
	}
	    self = false;
	    for (int j = 0; j < n.getNode(5).getNode(size-1).getNode(4).size(); j++) {
	      if(n.getNode(5).getNode(size-1).getNode(4).getNode(j).getNode(0).getNode(0).getString(0).equals(n.getNode(5).getNode(i).getString(3))) {
		if (n.getNode(5).getNode(size-1).getNode(4).getNode(j).getNode(0).getNode(2).getString(0).contains("__Object")) {
  	        zzz.append(n.getNode(5).getNode(size-1).getNode(4).getNode(j).getNode(0).getNode(2).getString(0)+")");
		} else {
			runtime.console().p(n.getNode(5).getNode(size-1).getNode(4).getNode(j).getNode(0).getNode(2).getString(0)).pln().flush();
  	        	zzz.append(n.getNode(5).getNode(size-1).getNode(4).getNode(j).getNode(0).getNode(2).getString(0).replace("__","")+")");
		}
		print_count++;
		
		  if (print_count < n.getNode(5).getNode(size-1).getNode(4).size()-1) {
			zzz.append(",\n");
		  } else {
			zzz.append("{}\n");
		  }
	      }
	     }
	}
	main = false;
      }

      public void visit(Node n) {
	for (Object o : n) {
	  if (o instanceof Node) dispatch((Node) o);
	}
      }
    }.dispatch(node);
    return zzz;
  }
}

