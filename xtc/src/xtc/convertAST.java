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
 * A tool to convert Java AST to C++ AST
 *
 * Used structure of ScopePrinter.java discussed in class.
 *
 * @author Kate Montgomery
 */

public class convertAST extends xtc.util.Tool {

  public static Node root;
  public convertAST() {}

  public interface ICommand {
    public void run();
  }

  public String getName() {
    return "convert Java AST to C++ AST tool";
  }

  public void init() {
    super.init();
    runtime.bool("printAST", "printAST", false, "Print the AST in generic form.").
        bool("locateAST", "optionLocateAST", false, "Include location information when printing the AST in " + "generic form.");
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

  public Node translate(Node node) {	
    root = node;
    new Visitor() {
      private String className = new String();
      private String superName = "__Object";
      private boolean Extends = false;
      private boolean mainClass = true;
      private Map<String, Node> classes = new HashMap<String,Node>();

      private GNode addChild(Node n, int type) {
	String name = n.getName();
	int size = n.size();
	ArrayList<Node> kids = new ArrayList<Node>(size);
	int i;
	for (i=0; i < size; i++) {
	  kids.add(n.getNode(i));	
	}

	if (type == 1) {
	  kids.add(addvptr());
	} else if (type == 2) {
	  kids.add(addvtable());
	} else if (type == 3) {
	  kids.add(addClass());
	} else if (type == 4) {
	  kids.add(addExpr());
	}
	
	GNode parent = GNode.create(name, true);
			
	for(i = 0; i < kids.size(); i++) {
  	  parent.add(kids.get(i));
	  parent.set(i, kids.get(i));
	}

	return parent;
      }

      private GNode addvptr() {
	GNode vptr = GNode.create("FieldDeclaration", true);
	for(int i = 0; i < 3; i++) {
	  vptr.add(null);
	}
	Node modifiers = GNode.create("Modifiers", true);
	vptr.set(0, modifiers);
	Node type = GNode.create("Type", true);
	vptr.set(1, type);
	Node qi = GNode.create("QualifiedIdentifier", true);
	qi.add(className + "_VT*");
	qi.set(0,className + "_VT*");
	Node dim = GNode.create("null");
	type.add(qi);
	type.set(0, qi);
	type.add(dim);
	type.set(1, dim);
	Node decls = GNode.create("Declarators", true);
	vptr.set(2, decls);
	Node decl = GNode.create("Declarator", true);
	decls.add(decl);
	decls.set(0, decl);
	String name = "__vptr";
	decl.add(name);
	decl.set(0,name);
	return vptr;
      }

      private GNode addvtable() {
	GNode vt = GNode.create("FieldDeclaration", true);
	Node modifiers = GNode.create("Modifiers", true);
	vt.add(modifiers);
	vt.set(0, modifiers);
	Node mod = GNode.create("Modifier", true);
	mod.add("static");
	mod.set(0, "static");
	modifiers.add(mod);
	modifiers.set(0, mod);
	Node type = GNode.create("Type", true);
	vt.add(type);
	vt.set(1, type);
	Node qi = GNode.create("QualifiedIdentifier", true);
	qi.add(className + "_VT");
	qi.set(0,className + "_VT");
	type.add(qi);
	type.set(0, qi);
	type.add(null);
	type.set(1, null);
	Node decls = GNode.create("Declarators", true);
	vt.add(decls);
	vt.set(2, decls);
	Node decl = GNode.create("Declarator", true);
	decls.add(decl);
	decls.set(0, decl);
	String name = "__vtable";
	decl.add(name);
	decl.set(0,name);
	return vt;
      }	

      private GNode addClass() {
	GNode vt = GNode.create("MethodDeclaration", true);
	for(int i = 0; i < 8; i++) {
	  vt.add(null);
	}	
	Node modifiers = GNode.create("Modifiers", true);
	vt.set(0, modifiers);
	Node mod = GNode.create("Modifier", true);
	mod.add("static");
	mod.set(0, "static");
	modifiers.add(mod);
	modifiers.set(0, mod);
					
	Node type = GNode.create("Type", true);
	vt.set(2, type);
	Node qi = GNode.create("QualifiedIdentifier", true);
	qi.add("Class");
	qi.set(0,"Class");
	Node dim = GNode.create("null");
	type.add(qi);
	type.set(0, qi);
	type.add(dim);
	type.set(1, dim);

	vt.set(3, "__class");

	Node fp = GNode.create("FormalParameters", true);
	vt.set(4, fp);
	
	GNode block = GNode.create("Block", true);
	ClassBlock(block);
	vt.set(7, block);
	return vt;	
      }			

      public void ClassBlock(GNode n) {
	GNode field = GNode.create("FieldDeclaration", true);
	Node modifiers = GNode.create("Modifiers", true);
	field.add(modifiers);
	field.set(0, modifiers);
	Node mod = GNode.create("Modifier", true);
	mod.add("static");
	mod.set(0, "static");
	modifiers.add(mod);
	modifiers.set(0, mod);
	Node type = GNode.create("Type", true);
	field.add(type);
	field.set(1, type);
	Node qi = GNode.create("QualifiedIdentifier", true);
	qi.add("Class");
	qi.set(0,"Class");
	type.add(qi);
	type.set(0, qi);
	type.add(null);
	type.set(1, null);
	Node decls = GNode.create("Declarators", true);
	field.add(decls);
	field.set(2, decls);
	Node decl = GNode.create("Declarator", true);
	decls.add(decl);
	decls.set(0, decl);
	String name = "k";
	decl.add(name);
	decl.set(0,name);
	decl.add(null);
	decl.set(1, null);
				
	GNode nc = GNode.create("NewClassExpression");
	decl.add(nc);
	decl.set(2, nc);
	
	for(int i = 0; i < 5; i++) {
	  nc.add(null);
	}
	nc.set(2, GNode.create("QualifiedIdentifier", true));
	nc.getNode(2).add("__Class");
	nc.getNode(2).set(0,"__Class");
	nc.set(3, GNode.create("Arguments", true));
	nc.getNode(3).add(null);
	nc.getNode(3).set(0,GNode.create("StringLiteral", true));
	nc.getNode(3).getNode(0).add(null);
	nc.getNode(3).getNode(0).set(0, "java.lang."+className.replace("__",""));
	nc.getNode(3).add(null);
	nc.getNode(3).set(1, GNode.create("QualifiedIdentifier", true));
	nc.getNode(3).getNode(1).add(null);

	if (Extends) {
	  nc.getNode(3).getNode(1).set(0, superName + "::__class");
	} else {
	  nc.getNode(3).getNode(1).set(0, "__Object::__class");
	}
	GNode ret = GNode.create("ReturnStatement", true);
	GNode pri = GNode.create("PrimaryIdentifier", true);
	pri.add("k");
	pri.set(0, "k");
	ret.add(pri);
	ret.set(0, pri);
			
	n.add(field);
	n.add(ret);
	
      }

      public GNode addVT(Node n) {
	GNode vt = GNode.create("ClassDeclaration", true);
	GNode mods = GNode.create("Modifiers", true);
	GNode body = GNode.create("ClassBody", true);
	for(int i=0; i < 6; i++) {
  	  vt.add(null);
	  body.add(null);
	}
	vt.set(0, mods);
	vt.set(1, n.getString(1) + "_VT");
	vt.set(5, body);

	for(int i = 0; i < 5; i++) {
	  GNode field = GNode.create("MethodDeclaration", true);
	  field.add(null);
	  field.set(0, GNode.create("Modifiers", true));
	  field.add(null);
	  field.add(null);
	  field.set(2, GNode.create("Type", true));
	  field.getNode(2).add(null);
	  field.getNode(2).set(0, GNode.create("QualifiedIdentifier", true));
	  field.getNode(2).getNode(0).add(null);
	  field.add(null);
	  field.set(3, "methodNameHere");	
	  field.add(null);
	  field.set(4, GNode.create("FormalParameters", true));
	  field.getNode(4).add(null);
	  field.getNode(4).set(0, GNode.create("FormalParameter", true));
	  field.getNode(4).getNode(0).add(null);
	  field.add(null);
	  field.add(null);
	  field.add(null);
	  field.set(7, GNode.create("Block"));
	  field.getNode(7).add(null);
	  body.set(i, field);
	}
	body.getNode(0).getNode(2).getNode(0).set(0, "Class");
	body.getNode(0).set(3, "__isa");			
	body.getNode(1).getNode(2).getNode(0).set(0, "int32_t");
	body.getNode(1).set(3, "hashCode");	
	body.getNode(1).getNode(4).getNode(0).set(0, n.getString(1).replace("__", ""));
	body.getNode(2).getNode(2).getNode(0).set(0, "bool");
	body.getNode(2).set(3, "equals");
	body.getNode(2).getNode(4).getNode(0).set(0, n.getString(1).replace("__", ""));
	body.getNode(2).getNode(4).add(null);
	body.getNode(2).getNode(4).set(1, GNode.create("FormalParameter", true));
	body.getNode(2).getNode(4).getNode(1).add(null);
	body.getNode(2).getNode(4).getNode(1).set(0, superName.replace("__", ""));
	body.getNode(3).getNode(2).getNode(0).set(0, "Class");
	body.getNode(3).set(3, "getClass");
	body.getNode(3).getNode(4).getNode(0).set(0, n.getString(1).replace("__", ""));
	body.getNode(4).getNode(2).getNode(0).set(0, "std::string");
	body.getNode(4).set(3, "toString");
	body.getNode(4).getNode(4).getNode(0).set(0, n.getString(1).replace("__", ""));

	//build constructor
	GNode con = GNode.create("ConstructorDeclaration", true);
	for(int i=0; i < 5; i++) {
	  con.add(null);
	}

	con.set(0, GNode.create("Modifiers", true));
	con.set(2, n.getString(1)+"_VT");
	con.set(3, GNode.create("FormalParameters", true));
	Node block = GNode.create("Block", true);
	con.set(4, block);

	for(int i=0; i < 5; i++) {
 	  block.add(null);
	  GNode expr = GNode.create("ExpressionStatement", true);
	  expr.add(null);
	  expr.set(0, GNode.create("Expression", true));
	  expr.getNode(0).add(null);
	  expr.getNode(0).set(0, GNode.create("PrimaryIdentifier", true));
	  expr.getNode(0).add(null);
	  expr.getNode(0).getNode(0).add(null);
	  expr.getNode(0).set(1, "=");
	  expr.getNode(0).add(null);
	  expr.getNode(0).set(2, GNode.create("PrimaryIdentifier", true));
	  expr.getNode(0).getNode(2).add(null);
	  expr.getNode(0).getNode(2).set(0, null);
	  block.set(i, expr);
	}
	boolean h = false; boolean e = false; boolean g= false; boolean t = false;
	for(int i=0; i < n.getNode(5).size(); i++) {
	  if(n.getNode(5).getNode(i).getName().equals("MethodDeclaration")) {
	    if (n.getNode(5).getNode(i).getString(3).equals("hashCode")) {
		block.getNode(1).getNode(0).getNode(2).set(0,"&"+n.getString(1)+"::hashCode");
		h = true;
	    } else if (n.getNode(5).getNode(i).getString(3).equals("equals")) {
		block.getNode(2).getNode(0).getNode(2).set(0,"&"+n.getString(1)+"::equals");	
		e = true;
	    } else if (n.getNode(5).getNode(i).getString(3).equals("getClass")) {
		block.getNode(3).getNode(0).getNode(2).set(0,"&"+n.getString(1)+"::getClass");
		g = true;
  	    } else if (n.getNode(5).getNode(i).getString(3).equals("toString")) {
		block.getNode(4).getNode(0).getNode(2).set(0,"&"+n.getString(1)+"::toString");
		t = true;
  	    }
	  }
	}
	if (superName.equals("__Object")) {
	  if(!h) { //runtime.console().p("setting hashcode to super class " + superName).pln().flush();
		block.getNode(1).getNode(0).getNode(2).set(0,"&"+superName+"::hashCode");}
	  if(!e) { block.getNode(2).getNode(0).getNode(2).set(0,"&"+superName+"::equals");}
	  if(!g) { block.getNode(3).getNode(0).getNode(2).set(0,"&"+superName+"::getClass");}
	  if(!t) { block.getNode(4).getNode(0).getNode(2).set(0,"&"+superName+"::toString");}
	} else {
	   Iterator it = classes.entrySet().iterator();
	    while(it.hasNext()) {
		Map.Entry pairs = (Map.Entry)it.next();
	  }
	  Node superVT = classes.get("__"+superName);
	  int superSize = superVT.getNode(5).size();
	  for(int i = 0; i < superVT.getNode(5).getNode(superSize-1).getNode(4).size(); i++) {
	    String tmp = superVT.getNode(5).getNode(superSize-1).getNode(4).getNode(i).getNode(0).getNode(2).getString(0);
	    if(superVT.getNode(5).getNode(superSize-1).getNode(4).getNode(i).getNode(0).getNode(0).getString(0).equals("hashCode") && !h) { 
		block.getNode(1).getNode(0).getNode(2).set(0, tmp);
	    }
	    if(superVT.getNode(5).getNode(superSize-1).getNode(4).getNode(i).getNode(0).getNode(0).getString(0).equals("equals") && !e) { 
		block.getNode(2).getNode(0).getNode(2).set(0,tmp);}
	    if(superVT.getNode(5).getNode(superSize-1).getNode(4).getNode(i).getNode(0).getNode(0).getString(0).equals("getClass") && !g) { 
		block.getNode(3).getNode(0).getNode(2).set(0,tmp);}
	    if(superVT.getNode(5).getNode(superSize-1).getNode(4).getNode(i).getNode(0).getNode(0).getString(0).equals("toString") && !t) { 
		block.getNode(4).getNode(0).getNode(2).set(0, tmp);}
  	  }
	}
	block.getNode(0).getNode(0).getNode(2).set(0,n.getString(1) +"::__class");
	
	block.getNode(0).getNode(0).getNode(0).set(0, "__isa");
	block.getNode(1).getNode(0).getNode(0).set(0, "hashCode");
	block.getNode(2).getNode(0).getNode(0).set(0, "equals");
	block.getNode(3).getNode(0).getNode(0).set(0, "getClass");
	block.getNode(4).getNode(0).getNode(0).set(0, "toString");
	
	body.set(5, con);
	return vt;
      }

      public GNode addExpr() {
	//create new node
	GNode expr = GNode.create("ExpressionStatement", true);
	expr.add(null);
	expr.set(0, GNode.create("Expression", true));
	expr.getNode(0).add(null);
	expr.getNode(0).set(0, GNode.create("PrimaryIdentifier", true));
	expr.getNode(0).add(null);
	expr.getNode(0).getNode(0).add(null);
	expr.getNode(0).getNode(0).set(0, "__vptr");
	expr.getNode(0).set(1, "=");
	expr.getNode(0).add(null);
	expr.getNode(0).set(2, GNode.create("PrimaryIdentifier", true));
	expr.getNode(0).getNode(2).add(null);
	expr.getNode(0).getNode(2).set(0, "&__vtable");

	return expr;				
      }

      private GNode addField(GNode n) {
	n = GNode.ensureVariable(n);
	GNode field = GNode.create("FieldDeclaration", true);
	for(int i = 0; i < 3; i++) {
	  field.add(null);
	}
	Node modifiers = GNode.create("Modifiers", true);
	field.set(0, modifiers);
	Node type = GNode.create("Type", true);
	field.set(1, type);
	Node qi = GNode.create("QualifiedIdentifier", true);
	qi.add(null);
	Node dim = GNode.create("null");
	type.add(qi);
	type.set(0, qi);
	type.add(dim);
	type.set(1, dim);
	Node decls = GNode.create("Declarators", true);
	field.set(2, decls);
	Node decl = GNode.create("Declarator", true);
	decls.add(decl);
	decls.set(0, decl);
	decl.add(null);
	n.getNode(5).add(n.getNode(5).size(), null);
	for(int i = n.getNode(5).size()-1; i > 0; i--) {
	  n.getNode(5).set(i, n.getNode(5).getNode(i-1));
	  if (n.getNode(5).getNode(i) != null) {
	  } else {
	  }
	}

	n.getNode(5).set(0, field);
	
	return n;
      }

      public void visitCompilationUnit(GNode n) {
	n = GNode.ensureVariable(n);

	//add node to include java_lang files
	GNode cc = GNode.create("Import Declaration", true);
	for(int i = 0; i < 3; i++) {
	  cc.add(null);
	}
	Node qi = GNode.create("QualifiedIdentifier", true);
	qi.add("java_lang.cc");
	qi.set(0,"java_lang.cc");
	cc.set(1, qi);
	n.add(cc);
	for(int i=n.size()-1; i > 0; i--) {
	  n.set(i, n.getNode(i-1));
	}
	n.set(1, cc);
	int size = n.size();
	for(int i = 0; i < size; i++) {
	  if(n.getNode(i) != null && n.getNode(i).getName().equals("ClassDeclaration") /*&& n.getNode(i).getNode(0).size() == 0*/) {
	    n.getNode(i).set(1, "__" + n.getNode(i).getString(1));
	    if (n.getNode(i).getNode(3) != null) {
		superName = n.getNode(i).getNode(3).getNode(0).getNode(0).getString(0);
	    } else {
		superName = "__Object";
	    }
	    Node vt = addVT(n.getNode(i));
	    classes.put(n.getNode(i).getString(1), vt);
	    n.add(vt);
	    n.set(n.size()-1, vt);
	  }
	}
	visit(n);
      }		

      public void visitClassDeclaration(GNode n) {
	className =  n.getString(1);
	// change class body node to new class body node with added children
	if (n.getNode(3) != null) {
	  Extends = true;
	  superName = n.getNode(3).getNode(0).getNode(0).getString(0);
	    //add parent VT methods to subclass VT 
	  GNode vt = (GNode)classes.get(className);
	  GNode parent_vt = (GNode)classes.get("__"+superName);
	  if (parent_vt != null) { //which it should always be
	    for(int i = 0; i < parent_vt.getNode(5).size()-1; i++) {
	      if (parent_vt.getNode(5).getNode(i).getName().equals("FieldDeclaration")) {
		String name = parent_vt.getNode(5).getNode(i).getNode(2).getNode(0).getString(0);
		for(int j = 0; j < vt.getNode(5).size()-1; j++) {
		    //set the subclass pointer to the parent pointer
		  if (vt.getNode(5).getNode(j).getName().equals("FieldDeclaration") && vt.getNode(5).getNode(j).getNode(2).getNode(0).getString(0).equals(name)) {
		       //set the ptr to the parent function
		    int size = vt.getNode(5).size();
		      for(int k = 0; k < vt.getNode(5).getNode(size-1).size(); k++) {
			if(vt.getNode(5).getNode(size-1).getNode(4).getNode(k).getNode(0).getNode(0).getString(0).equals(name)) {
			  name = null;
			}
		      }
		  }
		}
		if (name != null) {
		    //then name is not in the subclass VT so needs to be added
		  vt = addField(vt);
		  int size = vt.getNode(5).size();
		  String ret;
		  if (parent_vt.getNode(5).getNode(0).getNode(1).getName().equals("VoidType")){
	  	      ret = "void";
		  } else {
	  	      ret = parent_vt.getNode(5).getNode(0).getNode(1).getNode(0).getString(0);
		  }
		  vt.getNode(5).getNode(0).getNode(1).getNode(0).set(0, ret);
		  vt.getNode(5).getNode(0).getNode(2).getNode(0).set(0, name);
		  GNode conBlock = addChild(vt.getNode(5).getNode(size-1).getNode(4), 4);
		  vt.getNode(5).getNode(size-1).set(4, conBlock);
		  int blockSize = vt.getNode(5).getNode(size-1).getNode(4).size();
		  vt.getNode(5).getNode(size-1).getNode(4).getNode(blockSize-1).getNode(0).getNode(0).set(0, name);
		  String funcptr = new String();
		  int psize = parent_vt.getNode(5).size();
		  for (int j = 0; j < parent_vt.getNode(5).getNode(psize-1).getNode(4).size(); j++) {
		    if (parent_vt.getNode(5).getNode(psize-1).getNode(4).getNode(j).getNode(0).getNode(0).getString(0).equals(name)) {
		      funcptr = parent_vt.getNode(5).getNode(psize-1).getNode(4).getNode(j).getNode(0).getNode(2).getString(0);
		      vt.getNode(5).getNode(size-1).getNode(4).getNode(blockSize-1).getNode(0).getNode(2).set(0, funcptr);
		    }
		  }
		 }
		} else {
		  String name = parent_vt.getNode(5).getNode(i).getString(3);
		  for(int j = 0; j < vt.getNode(5).size()-1; j++) {
		    //set the subclass pointer to the parent pointer
		    if (vt.getNode(5).getNode(j).getName().equals("MethodDeclaration") && vt.getNode(5).getNode(j).getString(3).equals(name) && !name.equals("__isa")) {
		       //set the ptr to the parent function
		      int size = vt.getNode(5).size();
		      for(int k = 0; k < vt.getNode(5).getNode(size-1).size(); k++) {
			if(vt.getNode(5).getNode(size-1).getNode(4).getNode(k).getNode(0).getNode(0).getString(0).equals(name)) {
			    name = null;
			}
		      }
		    }
		  }
		  if (name != null && name != "__isa") {
			//then name is not in the subclass VT so needs to be added
		    vt = addMethod(vt);
		    int size = vt.getNode(5).size();
		    String ret;
		    if (parent_vt.getNode(5).getNode(i).getNode(2).getName().equals("VoidType")){
		      ret = "void";
		    } else {
		      ret = parent_vt.getNode(5).getNode(i).getNode(2).getNode(0).getString(0);
		    }
		    vt.getNode(5).getNode(size-2).getNode(2).getNode(0).set(0, ret);
		    vt.getNode(5).getNode(size-2).set(3, name);
			//set formal params
		    for(int j = 0; j < parent_vt.getNode(5).getNode(i).getNode(4).size(); j++) {
		      vt.getNode(5).getNode(size-2).getNode(4).add(null);
		      vt.getNode(5).getNode(size-2).getNode(4).set(j, GNode.create("FormalParameter", true));
		      vt.getNode(5).getNode(size-2).getNode(4).getNode(j).add(null);
		      if(parent_vt.getNode(5).getNode(i).getNode(2).getName().equals("VoidType")) {
		        vt.getNode(5).getNode(i).getNode(4).getNode(j).set(0, "void");
		      } else {
			vt.getNode(5).getNode(size-2).getNode(4).getNode(j).set(0, parent_vt.getNode(5).getNode(i).getNode(4).getNode(j).getString(0));
		      }
		    }
		  // add to the constructor 		
		    GNode conBlock = addChild(vt.getNode(5).getNode(size-1).getNode(4), 4);
		    vt.getNode(5).getNode(size-1).set(4, conBlock);
		    int blockSize = vt.getNode(5).getNode(size-1).getNode(4).size();
		    vt.getNode(5).getNode(size-1).getNode(4).getNode(blockSize-1).getNode(0).getNode(0).set(0, name);
		    String funcptr = new String();
		    int psize = parent_vt.getNode(5).size();
		    for (int j = 0; j < parent_vt.getNode(5).getNode(psize-1).getNode(4).size(); j++) {
		      if (parent_vt.getNode(5).getNode(psize-1).getNode(4).getNode(j).getNode(0).getNode(0).getString(0).equals(name)) {
		        funcptr = parent_vt.getNode(5).getNode(psize-1).getNode(4).getNode(j).getNode(0).getNode(2).getString(0);
		        vt.getNode(5).getNode(size-1).getNode(4).getNode(blockSize-1).getNode(0).getNode(2).set(0, funcptr);
		      }
		    }
		  }
	        }
	      }
	    }
	  }
	  if (n.getNode(0).size() == 0) {
	    mainClass = false;
	  }
	  if (!(n.getString(1).endsWith("_VT"))/* && !mainClass*/) {
	    GNode newChild = addChild(n.getNode(5), 1);
	    GNode newest = addChild(newChild, 2);
	    GNode last = addChild(newest, 3);
	    n.set(5, last);
	    boolean con = false;
	    for (int i = 0; i < n.getNode(5).size(); i++) {
	      if (n.getNode(5).getNode(i).getName().equals("ConstructorDeclaration")) {
	        con = true;
	      }
	    }
	    if (!con) {
		//if class doesn't have one, add constructor block
	      n.getNode(5).add(null);
	      int size = n.getNode(5).size();
	      n.getNode(5).set(size-1, GNode.create("ConstructorDeclaration", true));
	      for(int i = 0; i < 6; i++) {
	        n.getNode(5).getNode(size-1).add(null);
	      }
	      n.getNode(5).getNode(size-1).set(0, GNode.create("Modifiers", true));
	      n.getNode(5).getNode(size-1).set(2, className.replace("__", ""));
	      n.getNode(5).getNode(size-1).set(5, GNode.create("Block", true));
	    }
	  }
	  visit(n);
 	  Extends = false;
	  superName = "__Object";
	  mainClass = true;
      }


      public void visitConstructorDeclaration(GNode n) {
	//add initialzation of vptr and vtable
	//add new node to the constructor
	//we don't want this to happen in VT classes or main classes
	if (!(n.getString(2).endsWith("_VT")) /*&& !mainClass*/) {
	  n.set(2, "__" + n.getString(2));
	  GNode newBlock = addChild(n.getNode(5), 4);
	  n.set(5, newBlock);
	}
      }	

      public void visitMethodDeclaration(GNode n) {
	//if not in the VT class already
	//add to VTable class which can be found in classes map
	if(className.endsWith("_VT")/* || mainClass*/ ) { return;}
	GNode vt = (GNode)classes.get(className);
	if (vt == null) {
	  runtime.console().p("vt is null, searching for class "+ className).pln().flush();
	}
				
				//get parent class 
	GNode parent_vt = (GNode)classes.get(superName);
	if (parent_vt != null) {
	  for(int i = 0; i < parent_vt.getNode(5).size()-1; i++) {
	  //if the parent field declarations match
	    if (parent_vt.getNode(5).getNode(i).getName().equals("MethodDeclaration") && parent_vt.getNode(5).getNode(i).getString(3).equals(n.getString(3))) {
	    //there is a match
		int size = vt.getNode(5).size();
		for(int j = 0; j < vt.getNode(5).getNode(size-1).getNode(4).size(); j++) {
		  if(vt.getNode(5).getNode(size-1).getNode(4).getNode(j).getNode(0).getNode(0).getString(0).equals(n.getString(3))) {
		    vt.getNode(5).getNode(size-1).getNode(4).getNode(j).getNode(0).getNode(2).set(0, className+"::"+n.getString(3));
		    return;
		  }
		}
	    }
	  }
	}	
	int i = 0;
	while (!(vt.getNode(5).getNode(i).getName().equals("ConstructorDeclaration"))) {
		// if the field names match
	  if (vt.getNode(5).getNode(i).getName().equals("MethodDeclaration") && n.getString(3).equals(vt.getNode(5).getNode(i).getString(3))) {
		//then the method is already in the VT, so we need to update its ptr
	    int size = vt.getNode(5).size();
	    for (int j =0; j < vt.getNode(5).getNode(size-1).getNode(4).size(); j++) {
	      if(vt.getNode(5).getNode(size-1).getNode(4).getNode(j).getNode(0).getNode(0).getString(0).equals(n.getString(3))) {
		  vt.getNode(5).getNode(size-1).getNode(4).getNode(j).getNode(0).getNode(2).set(0, "&"+className+"::"+n.getString(3));
		  return;
	      }
	    }
	  } else if (n.getString(3).equals("__class")) {
  	    return;
	  }
	  i++;
	}
	//if we get here then the method is not in the VT and needs to be added
	vt = addMethod(vt);
	int size = vt.getNode(5).size();
	//i should now be the new field
	String ret;
	if (n.getNode(2).getName().equals("VoidType")){
	  ret = "void";
	} else {
	  ret = n.getNode(2).getNode(0).getString(0);
	}
	vt.getNode(5).getNode(i).getNode(2).getNode(0).set(0, ret);
	vt.getNode(5).getNode(i).set(3, n.getString(3));
	for(int j = 0; j < n.getNode(4).size(); j++) {
	  vt.getNode(5).getNode(i).getNode(4).add(null);
	  vt.getNode(5).getNode(i).getNode(4).set(j, GNode.create("FormalParameter", true));
	  vt.getNode(5).getNode(i).getNode(4).getNode(j).add(null);
	  if(n.getNode(4).getNode(j).getNode(1).getNode(0).equals("VoidType")) {
	    vt.getNode(5).getNode(i).getNode(4).getNode(j).set(0, "void");
	  } else {
	    vt.getNode(5).getNode(i).getNode(4).getNode(j).set(0, n.getNode(4).getNode(j).getNode(1).getNode(0).getString(0));
	  } 		
	}
	//add to the constructor
	GNode conBlock = addChild(vt.getNode(5).getNode(size-1).getNode(4), 4);
	vt.getNode(5).getNode(size-1).set(4, conBlock);
	int blockSize = vt.getNode(5).getNode(size-1).getNode(4).size();
	vt.getNode(5).getNode(size-1).getNode(4).getNode(blockSize-1).getNode(0).getNode(0).set(0, n.getString(3));
	vt.getNode(5).getNode(size-1).getNode(4).getNode(blockSize-1).getNode(0).getNode(2).set(0, "&"+className+"::"+n.getString(3));
      }
   
      public void visitFieldDeclaration(GNode n) {
	//if not in the VT class already
	//add to VTable class which can be found in classes map
	if(className.endsWith("_VT")/* || mainClass*/ ) { return;}
	GNode vt = (GNode)classes.get(className);
	if (vt == null) {
	  runtime.console().p("vt is null, searching for class "+ className).pln().flush();
	}
	String name = n.getNode(2).getNode(0).getString(0);
	if (name.equals("__vtable") || name.equals("__vptr")) { return; }
				//get parent class 
	GNode parent_vt = (GNode)classes.get(superName);
	if (parent_vt != null) {
	  for(int i = 0; i < parent_vt.getNode(5).size()-1; i++) {
	  //if the parent field declarations match
	    if (parent_vt.getNode(5).getNode(i).getName().equals("FieldDeclaration") && parent_vt.getNode(5).getNode(i).getNode(2).getNode(0).getString(0).equals(name)) {
	    //there is a match
		int size = vt.getNode(5).size();
		for(int j = 0; j < vt.getNode(5).getNode(size-1).getNode(4).size(); j++) {
		  if(vt.getNode(5).getNode(size-1).getNode(4).getNode(j).getNode(0).getNode(0).getString(0).equals(name)) {
		//TODO: I'm not sure this is right... just sets to parent rather than to parents ptr (ie if ptr isnt parent)
		    vt.getNode(5).getNode(size-1).getNode(4).getNode(j).getNode(0).getNode(2).set(0, className+"::"+name);
		    return;
		  }
		}
	    }
	  }
	}	
	int i = 0;
	while (!(vt.getNode(5).getNode(i).getName().equals("ConstructorDeclaration"))) {
		// if the field names match
	  if (vt.getNode(5).getNode(i).getName().equals("FieldDeclaration") && name.equals(vt.getNode(5).getNode(i).getNode(2).getNode(0).getString(0))) {
		//then the method is already in the VT, so we need to update its ptr
	    int size = vt.getNode(5).size();
	    for (int j =0; j < vt.getNode(5).getNode(size-1).getNode(4).size(); j++) {
	      if(vt.getNode(5).getNode(size-1).getNode(4).getNode(j).getNode(0).getNode(0).getString(0).equals(n.getString(3))) {
		  vt.getNode(5).getNode(size-1).getNode(4).getNode(j).getNode(0).getNode(2).set(0, "&"+className+"::"+name);
		  return;
	      }
	    }
	  }
	  i++;
	}
	//if we get here then the field is not in the VT and needs to be added
	vt = addField(vt);
	int size = vt.getNode(5).size();
	//i should now be the new field
	String ret;
	if (n.getNode(1).getName().equals("VoidType")){
	  ret = "void";
	} else {
	  ret = n.getNode(1).getNode(0).getString(0);
	}
	vt.getNode(5).getNode(0).getNode(1).getNode(0).set(0, ret);
	vt.getNode(5).getNode(0).getNode(2).getNode(0).set(0, name);
//TODO: add modifiers
/*	for(int j = 0; j < n.getNode(4).size(); j++) {
	  vt.getNode(5).getNode(i).getNode(4).add(null);
	  vt.getNode(5).getNode(i).getNode(4).set(j, GNode.create("FormalParameter", true));
	  vt.getNode(5).getNode(i).getNode(4).getNode(j).add(null);
	  if(n.getNode(4).getNode(j).getNode(1).getNode(0).equals("VoidType")) {
	    vt.getNode(5).getNode(i).getNode(4).getNode(j).set(0, "void");
	  } else {
	    vt.getNode(5).getNode(i).getNode(4).getNode(j).set(0, n.getNode(4).getNode(j).getNode(1).getNode(0).getString(0));
	  } 		
	}
*/	//add to the constructor
	GNode conBlock = addChild(vt.getNode(5).getNode(size-1).getNode(4), 4);
	vt.getNode(5).getNode(size-1).set(4, conBlock);
	int blockSize = vt.getNode(5).getNode(size-1).getNode(4).size();
	vt.getNode(5).getNode(size-1).getNode(4).getNode(blockSize-1).getNode(0).getNode(0).set(0, name);
	vt.getNode(5).getNode(size-1).getNode(4).getNode(blockSize-1).getNode(0).getNode(2).set(0, "&"+className+"::"+name);
 
	
      }

      private GNode addMethod(GNode n) {
	n = GNode.ensureVariable(n);
	GNode field = GNode.create("MethodDeclaration", true);
	field.add(null);
	field.set(0, GNode.create("Modifiers", true));
	field.add(null);
	field.add(null);
	field.set(2, GNode.create("Type", true));
	field.getNode(2).add(null);
	field.getNode(2).set(0, GNode.create("QualifiedIdentifier", true));
	field.getNode(2).getNode(0).add(null);
	field.add(null);
	field.set(3, "methodNameHere");	
	field.add(null);
	field.set(4, GNode.create("FormalParameters", true));
	field.add(null);
	field.add(null);
	field.add(null);
	field.set(7, GNode.create("Block"));
	field.getNode(7).add(null);


	n.getNode(5).add(field);

	
	int i = 0;
	while (!(n.getNode(5).getNode(i).getName().equals("ConstructorDeclaration"))) {
	  i++;
	}		
	int size = n.getNode(5).size();
	n.getNode(5).set(size-1, n.getNode(5).getNode(i));
	n.getNode(5).set(i, field);
	
	return n;
      }
      public void visit(Node n) {
	for (Object o : n) {
	  if (o instanceof Node) dispatch((Node) o);
	}
      }
    }.dispatch(node);
    root = node;
    return root;
 }

  public static void main(String[] args) {
    new convertAST().run(args);
  }
}
