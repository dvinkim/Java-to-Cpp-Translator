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
 * A tool to print VT information to .cc files in C++ from an AST
 *
 * @author Kate Montgomery
 */


public class printCC extends xtc.util.Tool {

  public printCC() {}

  public interface ICommand {
    public void run();
  }

  public String getName() {
    return "print .cc files in C++ tool";
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
      private String superName = new String();
      private Map<String, String> superMap = new HashMap<String, String>();
      public void visitClassDeclaration(GNode n) {
	className = n.getString(1).replace("__","");
	if (n.getNode(3) != null) {
	  superName = n.getNode(3).getNode(0).getNode(0).getString(0);
	} else {
	  superName = "__Object";
	}
	superMap.put(className, superName);
	visit(n);
	if (!className.endsWith("_VT")) {
		zzz.append(className+"::"+className+"():__vptr(&__vtable) {}\n");
	}
      }

      public void visitFieldDeclaration(GNode n) {
	if(n.getNode(2).getNode(0).getString(0).equals("__vtable")) {
	  zzz.append(n.getNode(1).getNode(0).getString(0) + " "+className.replace("__", "")+"::"+n.getNode(2).getNode(0).getString(0)+";\n");
	} else if (n.getNode(1).getNode(1) != null && n.getNode(1).getNode(1).hasName("Dimensions")){
		// we have an array so we need to print an array class() implementation
		String type = "";
		String type1 = "";
		String sup = "";
		String sup1 = "";
		String subtype = "";
		String sup_subtype = "";
		if (n.getNode(1).getNode(1).size() == 2) {
			if (n.getNode(2).getNode(0).getNode(2).getNode(0).hasName("QualifiedIdentifier")) {
			  type = "Array<Ptr<"+n.getNode(1).getNode(0).getString(0)+"> > ";
			  type1 = "";
			  subtype = n.getNode(1).getNode(0).getString(0);
			} else {
			  type = "Ptr<Array<"+n.getNode(1).getNode(0).getString(0)+"> > ";
			  type1 = "";
			}
		} else {
			type = n.getNode(1).getNode(0).getString(0);
			if (n.getNode(2).getNode(0).getNode(2).hasName("NewArrayExpression") ) {
				type1 = n.getNode(2).getNode(0).getNode(2).getNode(0).getString(0);
			} 
		}
		if (type.equals(type1)) { 
			type1 = "";
		}
			
		if (type.contains("Object") || type.contains("String")) { 
			if (type1.equals("")) {return; }
			else {
	  			if (superMap.get(className).equals("__Object")) { sup1 = "java::lang::Object";
				} else { sup1 = "Ptr<"+superMap.get(className)+">"; }
				zzz.append("namespace __rt{\n  template<>\n  java::lang::Class Array<Ptr<"+type1+"> >::__class() {\n    static java::lang::Class k = new java::lang::__Class(literal(\"[L"+type1+ ";\"),\n                              Array<"+sup1+">::__class(),\n                              "+ type1 +"::__class());\n    return k;\n  }\n}\n");
				return;
			}
		}
		if (type.contains("int")) {
			zzz.append("namespace __rt{\n  template<>\n  java::lang::Class Array<"+type+">::__class() {\n    static java::lang::Class k = new java::lang::__Class(literal(\"[L"+type+ ";\"),\n                              java::lang::__Object::__class(),\n                              java::lang::__Integer::TYPE());\n    return k;\n  }\n}\n");
			return;
		} else if (superMap.get(className).equals("__Object")) { sup = "java::lang::Object";
		} else { sup = "Ptr<"+superMap.get(className)+">"; }
		zzz.append("namespace __rt{\n  template<>\n  java::lang::Class Array<Ptr<"+type+"> >::__class() {\n    static java::lang::Class k = new java::lang::__Class(literal(\"[L"+type+ ";\"),\n                              Array<"+sup+">::__class(),\n                              "+ n.getNode(1).getNode(0).getString(0) +"::__class());\n    return k;\n  }\n}\n");

		if (!type1.equals("")) {
  			if (superMap.get(className).equals("__Object")) { sup1 = "java::lang::Object";
			} else { sup1 = "Ptr<"+superMap.get(className)+">"; }
			zzz.append("namespace __rt{\n  template<>\n  java::lang::Class Array<Ptr<"+type1+"> >::__class() {\n    static java::lang::Class k = new java::lang::__Class(literal(\"[L"+type1+ ";\"),\n                              Array<"+sup1+">::__class(),\n                              "+ type1 +"::__class());\n    return k;\n  }\n}\n");
		}

		if (!subtype.equals("") && !subtype.contains("Object") && !subtype.contains("String")) {
			if (superMap.get(subtype) == null) { sup_subtype = "java::lang::Object";
			} else if (superMap.get(subtype).equals("__Object")) { sup_subtype = "java::lang::Object";
			} else { sup_subtype = "Ptr<"+superMap.get(subtype)+">"; }
			zzz.append("namespace __rt{\n  template<>\n  java::lang::Class Array<Ptr<"+subtype+"> >::__class() {\n    static java::lang::Class k = new java::lang::__Class(literal(\"[L"+subtype+ ";\"),\n                              Array<"+sup_subtype+">::__class(),\n                              "+ subtype +"::__class());\n    return k;\n  }\n}\n");
		}
	   }	
      }

      public void visitMethodDeclaration(GNode n) {
	if (n.getString(3).equals("__class")) {
	  zzz.append("Class "+className+"::__class() {\n");
	  zzz.append("\tstatic Class k = new __Class(__rt::literal(\""+ n.getNode(7).getNode(0).getNode(2).getNode(0).getNode(2).getNode(3).getNode(0).getString(0).replace("java.lang.","")+"\"), " +superName+"::__class());\n");
	  zzz.append("\treturn k;\n");
	  zzz.append("}\n");
	  return;
	}
	visit(n);
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
