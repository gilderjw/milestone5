package milestone;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import GraphReading.IUserGraphMutator;
import GraphReading.MethodReader;
import ProgramGraph.GraphVizEdge;
import ProgramGraph.GraphVizNode;
import ProgramGraph.IEdge;
import ProgramGraph.INode;
import ProgramGraph.ProgramGraph;
import application.FieldReader;
import application.Utilities;

public class SameSimpleNameMutator implements IUserGraphMutator {
	private List<FieldReader> fr;
	private List<MethodReader> mr;

	public SameSimpleNameMutator(List<FieldReader> fr, List<MethodReader> mr) {
		this.fr = fr;
		this.mr = mr;
	}

	private INode makeNode(INode node) {
		GraphVizNode newNode = new GraphVizNode(node.getClassNode());

		String code = "";

		if(node.getDescription().equals("normal")) {
			ClassNode c = node.getClassNode();
			c.name = c.name.replaceAll("\\$", "_");
			code += c.name.replaceAll("\\/", "_") + " [\n";
			code += "shape =\"record\",\n";
			code += "label = \"{";
			if((Opcodes.ACC_INTERFACE & c.access) != 0){
				//is an interface
				code += "\\<\\<interface\\>\\>\\n";
			} else if((Opcodes.ACC_ABSTRACT & c.access) != 0){
				//is an abstract class
				code += "\\<\\<abstract\\>\\>\\n";
			}
			code += c.name.replaceAll("\\/", "_") + "|";
			List<FieldNode> fields = new ArrayList<FieldNode>();
			for(FieldReader r: this.fr){
				for(FieldNode n : r.getFields(c)){
					fields.add(n);
				}
			}
			for(FieldNode field: fields){
				if((field.access & Opcodes.ACC_PUBLIC) > 0){
					code += "+ ";
				} else if((field.access & Opcodes.ACC_PRIVATE) > 0){
					code += "- ";
				} else if((field.access & Opcodes.ACC_PROTECTED) > 0){
					code += "# ";
				}
				if ((field.access & Opcodes.ACC_STATIC) > 0){
					code += "static ";
				}
				code+= field.name + " : " + Utilities.getClassName(Type.getType(field.desc))+ "\\l";
			}	//
			code += "|";	//
			List<MethodNode> methods = new ArrayList<MethodNode>();	//
			for(MethodReader r: this.mr){
				for(MethodNode n : r.getMethods(c)){
					methods.add(n);
				}
			}
			for(MethodNode method: methods){
				if((method.access & Opcodes.ACC_PUBLIC) > 0){
					code += "+ ";
				} else if((method.access & Opcodes.ACC_PRIVATE) > 0){
					code += "- ";
				} else if((method.access & Opcodes.ACC_PROTECTED) > 0){
					code += "# ";
				} else if((method.access & Opcodes.ACC_DEPRECATED) > 0){
					code += "dep ";
				}
				if ((method.access & Opcodes.ACC_STATIC) > 0){
					code += "static ";
				}
				String methodName = method.name;
				if(methodName.equals("<init>")){
					//Replace with class name if it is a constructor
					methodName = Utilities.getClassName(c.name);
				} else if (methodName.equals("<clinit>")){
					methodName = Utilities.getClassName(c.name);
				}
				code+= " " + methodName +  "(";
				boolean hasArgs = false;
				for(Type argType : Type.getArgumentTypes(method.desc)){
					hasArgs = true;
					code += Utilities.getClassName(argType) + ", ";
				}
				if(hasArgs) {
					code = code.substring(0, code.length() - 2);
				}

				code += ") : " + Utilities.getClassName(Type.getReturnType(method.desc)) + "\\l";
			}

			code += "}\"];\n";
		}
		newNode.setCode(code);
		return newNode;
	}
	private GraphVizEdge makeEdge(INode h, INode t){
		GraphVizEdge ans = new GraphVizEdge(h, t);

		String code = "";

		code += t.getClassNode().name.replaceAll("\\/", "_");
		code += " -> ";
		code += h.getClassNode().name.replaceAll("\\/", "_");

		code += " [arrowhead=\"ovee\", arrowtail=\"ovee\", style=\"solid\", dir=\"both\"";
		code += ", color=yellow, label=\"<<Same Name>>\"];\n";

		ans.setCode(code);

		return ans;
	}
	@Override
	public void mutate(ProgramGraph pg) {
		HashSet<INode> nodesToKill = new HashSet<INode>();
		HashSet<INode> nodesToAdd = new HashSet<INode>();

		HashSet<IEdge> edgesToAdd = new HashSet<IEdge>();

		for (INode n1: pg.getINodes()){
			for (INode n2: pg.getINodes()){
				if((n1 == n2)){
					continue;
				}

				boolean bail = false;
				for(IEdge e : edgesToAdd) {
					String name1 = n1.getClassNode().name;
					String name2 = n2.getClassNode().name;

					if ((e.getHead().name.equals(name1) &&
							e.getTail().name.equals(name2)) ||
							(e.getHead().name.equals(name2) &&
									e.getTail().name.equals(name1))){
						bail = true;
					}
				}

				if(bail) {
					continue;
				}

				String name1 = Utilities.getClassName(n1.getClassNode().name);
				String name2 = Utilities.getClassName(n2.getClassNode().name);

				if(name1.equals(name2)){
					nodesToKill.add(n1);
					nodesToKill.add(n2);

					INode newNode1 = this.makeNode(n1);
					INode newNode2 = this.makeNode(n2);

					nodesToAdd.add(newNode1);
					nodesToAdd.add(newNode2);

					edgesToAdd.add(this.makeEdge(newNode1, newNode2));

				}
			}
		}

		for(INode n : nodesToKill) {
			pg.removeNode(n);
		}

		for(INode n : nodesToAdd) {
			pg.addNode(n);
		}

		for(IEdge e : edgesToAdd) {
			pg.addEdge(e);
		}
	}

}
