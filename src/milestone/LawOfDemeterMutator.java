package milestone;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import GraphReading.IUserGraphMutator;
import GraphReading.MethodReader;
import ProgramGraph.GraphVizEdge;
import ProgramGraph.IEdge;
import ProgramGraph.INode;
import ProgramGraph.ProgramGraph;
import application.FieldReader;
import application.Utilities;

public class LawOfDemeterMutator implements IUserGraphMutator {


	public LawOfDemeterMutator(List<FieldReader> fr, List<MethodReader> mr) {

	}

	private GraphVizEdge makeEdge(IEdge e){
		INode h = e.getIHead();
		INode t = e.getITail();


		GraphVizEdge ans = new GraphVizEdge(h, t);

		String code = "";

		code += Utilities.getClassName(e.getTail().name);
		code += " -> ";
		code += Utilities.getClassName(e.getHead().name);

		code += " [arrowhead=\"ovee\", style=\"dashed\"";

		if(e.getDescription().contains("many")){
			code += ", headlabel=\"1..m\", labeldistance=3";
		}

		code += ", color=blue, label=\"<<Demeter>>\"];\n";

		ans.setCode(code);

		return ans;
	}

	private List<INode> getFriends(INode n, ProgramGraph pg){
		ArrayList<INode> ret = new ArrayList<>();

		ret.addAll(this.getSubclasses(n, pg));
		ret.addAll(this.parameterClasses(n, pg));
		ret.addAll(this.instantiatedClasses(n, pg));
		ret.addAll(this.getFields(n, pg));

		return ret;
	}

	private List<INode> getSubclasses(INode n, ProgramGraph pg){
		ArrayList<INode> sub = new ArrayList<INode>();
		sub.add(n);

		for(IEdge e: pg.getEdges()){
			if(e.getIHead().equals(n) &&
					(e.getDescription().contains("extends") ||
							e.getDescription().contains("implements"))) {
				sub.addAll(this.getSubclasses(e.getITail(), pg));
			}
		}
		return sub;
	}

	@SuppressWarnings("unchecked")
	private List<INode> getFields(INode n, ProgramGraph pg) {
		ArrayList<INode> fields = new ArrayList<>();

		for (FieldNode fn : (List<FieldNode>) n.getClassNode().fields) {
			for(INode in: pg.getINodes()) {
				if (Utilities.getClassPath(fn.desc).equals(in.getClassNode().name)){
					fields.add(in);
					fields.addAll(this.getSubclasses(in, pg));
				}
			}
		}

		return fields;
	}

	@SuppressWarnings("unchecked")
	private List<INode> instantiatedClasses(INode n, ProgramGraph pg) {
		ArrayList<INode> classes = new ArrayList<>();

		ClassNode main = n.getClassNode();

		for(MethodNode mn : (List<MethodNode>) main.methods){
			Iterator<AbstractInsnNode> iter = mn.instructions.iterator();


			while(iter.hasNext()) {
				AbstractInsnNode node = iter.next();
				if(node.getOpcode() == 0xbb){
					TypeInsnNode no = (TypeInsnNode) node;

					for(INode nod : pg.getINodes()){
						if(nod.getClassNode().name.equals(no.desc)){
							classes.add(nod);
						}
					}

				}
			}
		}
		return classes;
	}

	@SuppressWarnings("unchecked")
	private List<INode> parameterClasses(INode n, ProgramGraph pg){
		ArrayList<INode> params = new ArrayList<>();

		ClassNode main = n.getClassNode();

		for(MethodNode mn : (List<MethodNode>) main.methods){
			String desc = mn.desc;
			desc = desc.substring(desc.indexOf('(') + 1, desc.indexOf(')'));

			for(String s : desc.split(";")){
				for(INode in : pg.getINodes()){
					if (in.getClassNode().name.equals(Utilities.getClassPath(s))) {
						params.add(in);
					}
				}
			}

		}
		return params;
	}

	private boolean hasStatic(INode n){
		for (MethodNode mn : (List<MethodNode>) n.getClassNode().methods) {
			if ((mn.access & (Opcodes.ACC_STATIC)) != 0){
				return true;
			}
		}
		return false;
	}

	@Override
	public void mutate(ProgramGraph pg) {
		HashSet<IEdge> edgesToAdd = new HashSet<IEdge>();
		HashSet<IEdge> edgesToKill = new HashSet<IEdge>();

		for(IEdge edge: pg.getEdges()) {
			if(edge.getDescription().contains("dependency")){
				INode main = edge.getITail();
				INode friend = edge.getIHead();

				if(!(this.getFriends(main, pg).contains(friend) ||
						this.hasStatic(friend))) {
					edgesToKill.add(edge);
					edgesToAdd.add(this.makeEdge(edge));
				}
			}
		}

		for(IEdge e : edgesToKill) {
			pg.removeEdge(e);
		}

		for(IEdge e : edgesToAdd) {
			pg.addEdge(e);
		}


	}

}
