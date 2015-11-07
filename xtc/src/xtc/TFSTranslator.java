//package xtc;

import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.Reader;
import java.io.PrintWriter;

import xtc.lang.JavaFiveParser;

import xtc.parser.ParseException;
import xtc.parser.Result;

import xtc.tree.Node;

import xtc.util.Tool;

/**
 * Merges all of the separate files into a single Java to C++ translator.
 *
 * @author Dongvin Kim
 * @version 1.0.0
 */
public class TFSTranslator extends Tool {

  public TFSTranslator() {}

  public String getName() {
    return "The Flying Staplers' Java to C++ Translator";
  }

  public String getCopy() {
    return "(C) 2014 The Flying Staplers";
  }

  public File locate(String name) throws IOException {
    File file = super.locate(name);
    if (Integer.MAX_VALUE < file.length()) {
      throw new IllegalArgumentException(file + ": file too large");
    }
    return file;
  }

  public Node parse(Reader in, File file) throws IOException, ParseException {
    JavaFiveParser parser =
      new JavaFiveParser(in, file.toString(), (int)file.length());
    Result result = parser.pCompilationUnit(0);
    return (Node)parser.value(result);
  }

  public Node translate(String name) {

    // Locate the file.
    File file = null;
    try {
        file = locate(name);
    } catch (IOException e) {
	//ignore
    }

    // Open the file.
    Reader in = null;
    try {
        in = runtime.getReader(file);
    } catch (IOException e) {
        //ignore
    }

    // Parse the file.
    Node root=null;
    try {
      root = parse(in, file);
    } catch (IOException x) {
	//ignore
    } catch (ParseException x) {
	//ignore
    } finally {
      // Close the file.
      try {
        in.close();
      } catch (IOException x) {
        // Ignore.
      }
    }

    // Return the AST.
    return root;

  }

  public static void main(String[] args) {
	if(args.length != 1) {
		System.out.println("=============================================================================");
		System.out.println("Please provide a single command line argument: the Java file to be translated");
                System.out.println("=============================================================================");
	} else if(!args[0].endsWith(".java")) {
		System.out.println("=====================");
		System.out.println("Not a valid Java file");
                System.out.println("=====================");
	} else {
		String fileName = args[0].substring(0,args[0].length()-5);
		String fileNameRaw = fileName;
		boolean flag = true;
		while(flag) {
			int last = fileNameRaw.indexOf("/");
			if(last == -1) {
				flag = false;
			} else {
				fileNameRaw = fileNameRaw.substring(last+1);
			}
		}
		try{
			PrintWriter writeH = new PrintWriter(fileName+".h");
			PrintWriter writeCC = new PrintWriter(fileName+".cc");
       	        	Node theAST = new TFSTranslator().translate(args[0]);
       		        StringBuilder CCbuff = new StringBuilder();
       		        StringBuilder Hbuff = new StringBuilder();
			
			String cc = ("#include \""+fileNameRaw+".h\"\nusing namespace java::lang;\n");
			writeCC.print(cc);
			CCbuff.append(new X().translate(theAST));	
			Hbuff.append(new CPPHeaderPrint().translate(theAST));

			Node newAST = new convertAST().translate(theAST);
			CCbuff.insert(0, new printCC().translate(newAST));
			Hbuff.append( new printHeader().translate(newAST));
			Hbuff.insert(0, "#include <iostream>\n#include \"java_lang.cc\"\nusing namespace java::lang;\n");
			writeCC.print(CCbuff.toString());
			writeH.print(Hbuff.toString());
                	writeH.close();
			writeCC.close();
		} catch(FileNotFoundException e) {}
//		} catch(Exception e) {
//			System.out.println("There was an error.");
//			System.out.println(e.toString());
//		}
	}
  }

}
