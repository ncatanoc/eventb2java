package eventb2javajml_plugin.machine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;
import org.eventb.core.ast.Assignment;
import org.eventb.core.ast.AssociativeExpression;
import org.eventb.core.ast.AssociativePredicate;
import org.eventb.core.ast.AtomicExpression;
import org.eventb.core.ast.BecomesEqualTo;
import org.eventb.core.ast.BecomesMemberOf;
import org.eventb.core.ast.BecomesSuchThat;
import org.eventb.core.ast.BinaryExpression;
import org.eventb.core.ast.BinaryPredicate;
import org.eventb.core.ast.BoolExpression;
import org.eventb.core.ast.BoundIdentDecl;
import org.eventb.core.ast.BoundIdentifier;
import org.eventb.core.ast.Expression;
import org.eventb.core.ast.ExtendedExpression;
import org.eventb.core.ast.ExtendedPredicate;
import org.eventb.core.ast.Formula;
import org.eventb.core.ast.FormulaFactory;
import org.eventb.core.ast.FreeIdentifier;
import org.eventb.core.ast.IParseResult;
import org.eventb.core.ast.ISimpleVisitor2;
import org.eventb.core.ast.IntegerLiteral;
//import org.eventb.core.ast.LanguageVersion;
import org.eventb.core.ast.LiteralPredicate;
import org.eventb.core.ast.MultiplePredicate;
import org.eventb.core.ast.Predicate;
import org.eventb.core.ast.PredicateVariable;
import org.eventb.core.ast.QuantifiedExpression;
import org.eventb.core.ast.QuantifiedPredicate;
import org.eventb.core.ast.RelationalPredicate;
import org.eventb.core.ast.SetExtension;
import org.eventb.core.ast.SimplePredicate;
import org.eventb.core.ast.Type;
import org.eventb.core.ast.UnaryExpression;
import org.eventb.core.ast.UnaryPredicate;


public class Pred2JavaJml implements ISimpleVisitor2 {

	ArrayList<String> boundIdentifiers = new ArrayList<String>();

	public String transBecomes;
	boolean isaSubset;

	JmlType inv_type_func_rel;

	String translation;

	String FreeIdentVar;

	/*** Regarding the translation of a predicate ***/ 
	private boolean predTrans;

	private boolean expTrans;

	Stack<String> varFuncRel = new Stack<String>();
	Stack<JmlType> setExtType = new Stack<JmlType>();
		
	private Stack<JmlType> mapstoType = new Stack<JmlType>();
	boolean predicate_guard = false;


	/*** Regarding the translation of an assignment ***/
	private boolean assignmentTrans;
	private String assignamentType;
	// HashMap<String, ArrayList<String>> -> <name, <translation, Type>>
	private HashMap<String, ArrayList<String>> transAssignment;

	boolean is_assig = false;


	/*** Regarding to type variables translation ***/
	private boolean gettingType;
	// Variable (constant) types is stored in varType dictionary.
	// HashMap<Var name,type> varType;
	public HashMap<String,JmlType> varType;
	public JmlType currentType = new JmlType();

	public String currentVar = "";

	private boolean boolExp = false;

	Pred2JavaJml(){
		predTrans = false;
		expTrans = false;
		varFuncRel.clear();
		setExtType.clear();
		mapstoType.clear();
		is_assig = false;
		assignmentTrans = false;
		assignamentType = "";
		transAssignment = new HashMap<String, ArrayList<String>>();
		gettingType = false;
		varType = new HashMap<String,JmlType>();
		predicate_guard = false;
		inv_type_func_rel = null;
	}

	public String getInternalType(String varName){
		if (varType.containsKey(varName)){
			return varType.get(varName).getInternalType();
		}else
			return "none";
	}

	public String getJmlType(String varName){
		if (varType.containsKey(varName)){
			return varType.get(varName).getJmlType();
		}else
			return "none";
	}

	public void setVarTypeSet(String name){
		JmlType type = new JmlType(name, JmlType.setT);
		type.update(new JmlType("",JmlType.intT));
		varType.put(name, type);
	}


	@SuppressWarnings("unchecked")
	public String getvariableType(String varName, String varDef, boolean storeVar){
		Print("getvariableType\n====\n");
		predicate_guard = false;
		if (varType.containsKey(varName)){
			return varType.get(varName).getJmlType();
		}else{
			currentType = new JmlType();
			//copy the values of the global variables in case it is getting a nested type
			boolean tmpGettingType = gettingType;
			boolean tmpPredTrans = predTrans;
			boolean tmpAssignmentTrans = assignmentTrans;
			String tmpTranslation = translation;
			String tmpFreeIdentVar = FreeIdentVar;
			boolean tmp_is_assig = is_assig;
			Stack<String> tmp_varFuncRel = (Stack<String>) varFuncRel.clone();
			Stack<JmlType> tmp_setExtType = new Stack<JmlType>();
			if (setExtType.size() != 0){
				tmp_setExtType = clone(setExtType);
			}
			
			Stack<JmlType> tmp_mapstoType = new Stack<JmlType>(); 
			if (mapstoType.size() != 0){
				tmp_mapstoType = clone(mapstoType);
			}
			varFuncRel.clear();
			setExtType.clear();
			mapstoType.clear();
			boolExp = false;
			gettingType = true;
			predTrans = false;
			assignmentTrans = false;
			translation = "";
			FreeIdentVar = "";
			brel = false;
			Exp(varDef);
			//get back the values of the global variables in case it is getting a nested type
			gettingType = tmpGettingType;
			predTrans = tmpPredTrans;
			is_assig = tmp_is_assig;
			assignmentTrans = tmpAssignmentTrans;
			varFuncRel = (Stack<String>) tmp_varFuncRel.clone();
			if (tmp_setExtType.size() == 0){
				setExtType.clear();
			}else{
				setExtType = clone(tmp_setExtType);
			}
			
			if (tmp_mapstoType.size() != 0){
				mapstoType = clone(tmp_mapstoType);
			}
			translation = currentType.getJmlType();
			String tmp3 = translation;
			translation = tmpTranslation;
			FreeIdentVar = tmpFreeIdentVar;
			if (storeVar && !boolExp){
				varType.put(varName, currentType);
			}
			return tmp3;
		}
	}

	public String Pre(String predicate, boolean trans_imp){
		Print("Pre\n====\n");
		predicate_guard = trans_imp;
		translation = "";
		FreeIdentVar = "";
		boolean tmpPredTrans = predTrans;
		boolean tmpAssignmentTrans = assignmentTrans;
		boolean tmpGettingType = gettingType;
		boolExp = false;
		predTrans = true;
		assignmentTrans = false;
		gettingType = false;
		varFuncRel.clear();
		setExtType.clear();
		mapstoType.clear();
		is_assig = false;
		inv_type_func_rel = null;
		top_rel = true;
		final FormulaFactory ff = FormulaFactory.getDefault();
		final IParseResult result = 
				ff.parsePredicate(predicate, null);
		final Predicate p = result.getParsedPredicate();
		p.accept(this);

		predTrans = tmpPredTrans;
		assignmentTrans = tmpAssignmentTrans;
		gettingType = tmpGettingType;
		return translation;

	}

	public String variant(String expression){
		Print("Exp\n====\n");
		translation = "";
		as = true;
		is_assig = false;
		boolean predTrans_tmp = predTrans;
		boolean assignmentTrans_tmp = assignmentTrans;
		predTrans = true;
		assignmentTrans = false;
		final FormulaFactory ff = FormulaFactory.getDefault();
		final IParseResult result = 
				ff.parseExpression(expression, null);
		final Expression e = result.getParsedExpression();
		e.accept(this);
		predTrans = predTrans_tmp;
		assignmentTrans = assignmentTrans_tmp;
		as = false;
		return translation;
	}

	public void Exp(String expression){
		Print("Exp\n====\n");
		is_assig = false;
		final FormulaFactory ff = FormulaFactory.getDefault();
		final IParseResult result = 
				ff.parseExpression(expression, null);
		final Expression e = result.getParsedExpression();
		e.accept(this);
	}

	// Translates EventB assignments (var := Expression) into JML
	@SuppressWarnings("unchecked")
	public HashMap<String,ArrayList<String>> Assignment(String assig){
		Print("Assignment\n=====\n");
		is_assig = true;
		predicate_guard = true;
		transAssignment = new HashMap<String,ArrayList<String>>();
		//copy the values of the global variables
		boolean tmpAssignmentTrans = assignmentTrans; 
		boolean tmpGettingType = gettingType;
		boolean tmpPredTrans = predTrans;
 		Stack<String> tmp_varFuncRel = (Stack<String>) varFuncRel.clone();
		Stack<JmlType> tmp_setExtType = new Stack<JmlType>();
		if (setExtType.size() != 0){
			tmp_setExtType = clone(setExtType);
		}
		

		Stack<JmlType> tmp_mapstoType = new Stack<JmlType>();
		if (mapstoType.size() != 0){
			tmp_mapstoType = clone(mapstoType);
		}
		assignmentTrans = true;
		gettingType = false;
		predTrans = false;
		boolExp = false;
		final FormulaFactory ff = FormulaFactory.getDefault();
		final IParseResult result = 
				ff.parseAssignment(assig, null);

		final Assignment a = result.getParsedAssignment();	
		a.accept(this);
		assignmentTrans = tmpAssignmentTrans;
		gettingType = tmpGettingType;
		predTrans = tmpPredTrans;
		varFuncRel = (Stack<String>) tmp_varFuncRel.clone();
		if (tmp_setExtType.size() == 0){
			setExtType.clear();
		}else{
			setExtType = clone(tmp_setExtType);
		}
		if (tmp_mapstoType.size() != 0){
			mapstoType = clone(tmp_mapstoType);
		}
		return transAssignment;
	}


	@Override
	public void visitBecomesEqualTo(BecomesEqualTo assignment) {
		Print("visitBecomesEqualTo");
		// not Predicate (???)
		FreeIdentifier[] ident = assignment.getAssignedIdentifiers();
		Expression[] exp = assignment.getExpressions();
		for (int i=0; i < ident.length;i++){
			mapstoType.clear();
			currentVar = ident[i].toString();
			translation = "";
			FreeIdentVar = "";
			assignamentType = "";
			exp[i].accept(this);
			if (!boolExp || is_assig){
				ArrayList<String> s = new ArrayList<String>();
				if (varType.containsKey(currentVar)){
					if (varType.get(currentVar).getInternalType().equals(JmlType.relT)){
						translation = translation.replace("BSet.EMPTY", "BRelation.EMPTY");
					}
				}
				s.add(assignamentType);
				s.add(translation);
				transAssignment.put(currentVar,s);
			}
			currentVar = "";
		}
	}

	public boolean member_of = false;
	@Override
	public void visitBecomesMemberOf(BecomesMemberOf assignment) {
		Print("\n visitBecomesMemberOf");
		// Becomes Member Of
		FreeIdentifier[] ident = assignment.getAssignedIdentifiers();
		Expression exp = assignment.getSet();
		translation = "";
		FreeIdentVar = "";
		exp.accept(this);
		String t = "";
		for (FreeIdentifier var: ident){
			String var_name = var.getName();
			String var_type = varType.get(var_name).getJmlType(); 
			t = "(\\exists " + var_type + " " + var_name + "_localVar; " + translation + 
					//".has(" + var_name + "_localVar); " + var_name + ".equals(" + var_name + "_localVar))";
					".has(" + var_name + "_localVar); machine." + var_name + ".equals(" + var_name + "_localVar))";
		}

		transBecomes = translation;

		translation = t;

		/*******/
		if (exp instanceof UnaryExpression){
			if (exp.getTag() == Formula.POW){
				for (FreeIdentifier var: ident){
				}
			}
		}
		/*******/

		assignamentType = "BECOMES";
		ArrayList<String> s = new ArrayList<String>();
		s.add(assignamentType);
		s.add(translation);
		for (FreeIdentifier var: ident){
			if (!boolExp || is_assig){
				transAssignment.put(var.toString(), s);
			}
		}
	}

	public HashMap<Integer,String> becomes_vars = new HashMap<Integer,String>(); 
	@Override
	public void visitBecomesSuchThat(BecomesSuchThat assignment) {
		Print("\n visitBecomesSuchThat");
		becomes_vars = new HashMap<Integer,String>();
		FreeIdentifier[] ident = assignment.getAssignedIdentifiers();

		for (int bound = 0; bound < ident.length; bound++){
			becomes_vars.put(bound, "tmp_" + ident[(ident.length-bound)-1].getName());
		}

		translation = "";
		FreeIdentVar = "";
		assignment.getCondition().accept(this);

		String t = "";
		String par = "";
		String vars_in_java = "";
		String predicate = translation;
		for (FreeIdentifier var: ident){
			String var_name = var.getName();
			String var_type = varType.get(var_name).getJmlType();
			par += ")";
			t += "(\\exists " + var_type + " tmp_" + var_name + "; " ;
			vars_in_java += var_name + ",";
		}
		translation = t + predicate + "; ";

		for (int var = 0; var < ident.length; var++){
			translation += "machine." + ident[var].getName() + ".equals(tmp_" + ident[var].getName() + ")";
			if (var+1 < ident.length){
				translation += " && ";
			}else{
				translation += par;
			}
		}

		assignamentType = "NONDET";
		ArrayList<String> s = new ArrayList<String>();
		s.add(assignamentType);
		s.add(translation);
		s.add(vars_in_java);
		s.add(predicate);
		//for (FreeIdentifier var: ident){
		if (!boolExp || is_assig){
			//transAssignment.put(var.toString(), s);
			transAssignment.put(ident[0].getName(), s);
		}
		//}
	}

	@Override
	public void visitBoundIdentDecl(BoundIdentDecl boundIdentDecl) {
		Print("\n visitBoundIdentDecl");
		Print("\n   -- " + boundIdentDecl.getName());
		//store the bounded variables
		if (predTrans){
			translation += boundIdentDecl.getName();
		}
		//boundIdentifiers.push(boundIdentDecl.getName());
		boundIdentifiers.add(0,boundIdentDecl.getName());

	}

	@Override
	public void visitAssociativeExpression(AssociativeExpression expression) {
		Print("\n visitAssociativeExpression");
		switch (expression.getTag()){
		case 	Formula.BUNION:
			Print("BUNION");
			if (predTrans || assignmentTrans){
				String tmp = translation;
				translation = "";
				int i = expression.getChildren().length;
				String par = "";

				Stack<JmlType> tmp_setExtType = new Stack<JmlType>();
								
				boolean mapstoType_needed = true;
				Stack<JmlType> tmp_mapstoType = new Stack<JmlType>();
				for (Expression exprs : expression.getChildren()){
					exprs.accept(this);
					if (mapstoType_needed){
						mapstoType_needed = false;
						if (mapstoType.size() != 0){
							tmp_mapstoType = clone(mapstoType);
							//(07.11)VR: Bug Type
							//		removed mapstoType.clear() -> it was a copy/paste error
							//		I also changed the other ones.
						}
					}

					//SetExtType needs just the first expression
					if (setExtType.size() != 0 && tmp_setExtType.size() == 0){
						tmp_setExtType = clone(setExtType);
						setExtType.clear();
					}
					
					if (i > 1){
						translation += ".union(";
						par += ")";
					}i--;
				}

				if (tmp_setExtType.size() == 0){
					setExtType.clear();
				}else{
					setExtType = clone(tmp_setExtType);
				}
				
				if (tmp_mapstoType.size() != 0){
					mapstoType = clone(tmp_mapstoType);
				}

				translation += par;
				translation = tmp + "(" + translation + ")";
			}
			break;
		case 	Formula.BINTER:
			Print("BINTER");
			if (predTrans || assignmentTrans){
				String tmp = translation;
				translation = "";
				int i = expression.getChildren().length;
				String par = "";

				Stack<JmlType> tmp_setExtType = new Stack<JmlType>(); 
				
				boolean mapstoType_needed = true;
				Stack<JmlType> tmp_mapstoType = new Stack<JmlType>();

				for (Expression exprs : expression.getChildren()){
					exprs.accept(this);
					if (mapstoType_needed){
						mapstoType_needed = false;
						if (mapstoType.size() != 0){
							tmp_mapstoType = clone(mapstoType);
						}
					}
					//SetExtType needs just the first expression
					if (setExtType.size() != 0 && tmp_setExtType.size() == 0){
						tmp_setExtType = clone(setExtType);
						setExtType.clear();
					}
					
					if (i > 1){
						translation += ".intersection(";
						par += ")";
					}i--;
				}
				translation += par;
				translation = tmp + "(" + translation + ")";

				if (tmp_setExtType.size() == 0){
					setExtType.clear();
				}else{
					setExtType = clone(tmp_setExtType);
				}
				
				if (tmp_mapstoType.size() != 0){
					mapstoType = clone(tmp_mapstoType);
				}

			}
			break;
		case 	Formula.BCOMP:
			Print("BCOMP");
			if (predTrans || assignmentTrans){
				String tmp = translation;
				translation = "";
				int i = expression.getChildren().length;
				String par = "";

				Stack<JmlType> tmp_setExtType = new Stack<JmlType>();
				boolean mapstoType_needed = true;
				Stack<JmlType> tmp_mapstoType = new Stack<JmlType>();
				for (Expression exprs : expression.getChildren()){
					exprs.accept(this);
					if (mapstoType_needed){
						mapstoType_needed = false;
						if (mapstoType.size() != 0){
							tmp_mapstoType = clone(mapstoType);
						}
					}

					//SetExtType needs just the first part
					if (setExtType.size() != 0 && tmp_setExtType.size() == 0){
						tmp_setExtType = clone(setExtType);
						setExtType.clear();
					}

					if (i > 1){
						translation += ".backwardCompose(";
						par += ")";
					}i--;
				}
				translation += par;
				translation = tmp + "(" + translation + ")";

				if (tmp_setExtType.size() == 0){
					setExtType.clear();
				}else{
					setExtType = clone(tmp_setExtType);
				}

				if (tmp_mapstoType.size() != 0){
					mapstoType = clone(tmp_mapstoType);
				}

			}
			break;
		case 	Formula.FCOMP:
			Print("FCOMP");
			if (predTrans || assignmentTrans){
				String tmp = translation;
				translation = "";
				int i = expression.getChildren().length;
				String par = "";

				Stack<JmlType> tmp_setExtType = new Stack<JmlType>(); 
				boolean mapstoType_needed = false;
				Stack<JmlType> tmp_mapstoType = new Stack<JmlType>();

				for (Expression exprs : expression.getChildren()){
					exprs.accept(this);
					if (mapstoType_needed){
						mapstoType_needed = false;
						if (mapstoType.size() != 0){
							tmp_mapstoType = clone(mapstoType);
						}

					}

					//SetExtType needs just the first part
					if (setExtType.size() != 0 && tmp_setExtType.size() == 0){
						tmp_setExtType = clone(setExtType);
						setExtType.clear();
					}

					if (i > 1){
						translation += ".compose(";
						par += ")";
					}i--;
				}
				translation += par;
				translation = tmp + "(" + translation + ")";

				if (tmp_setExtType.size() == 0){
					setExtType.clear();
				}else{
					setExtType = clone(tmp_setExtType);
				}
				if (tmp_mapstoType.size() != 0){
					mapstoType = clone(tmp_mapstoType);
				}

			}
			break;
		case 	Formula.OVR:
			Print("OVR");
			if (predTrans || assignmentTrans){
				String tmp = translation;
				translation = "";
				int i = expression.getChildren().length;
				String par = "";

				Stack<JmlType> tmp_setExtType = new Stack<JmlType>(); 
				boolean mapstoType_needed = true;
				Stack<JmlType> tmp_mapstoType = new Stack<JmlType>();
				for (Expression exprs : expression.getChildren()){
					exprs.accept(this);
					if (mapstoType_needed){
						mapstoType_needed = false;
						if (mapstoType.size() != 0){
							tmp_mapstoType = clone(mapstoType);
						}
					}
					//SetExtType needs just the first part
					if (setExtType.size() != 0 && tmp_setExtType.size() == 0){
						tmp_setExtType = clone(setExtType);
						setExtType.clear();
					}
					if (i > 1){
						translation += ".override(";
						par += ")";
					}i--;
				}
				translation += par;
				translation = tmp + "(" + translation + ")";

				if (tmp_setExtType.size() == 0){
					setExtType.clear();
				}else{
					setExtType = clone(tmp_setExtType);
				}

				if (tmp_mapstoType.size() != 0){
					mapstoType = clone(tmp_mapstoType);
				}

			}
			break;
		case 	Formula.PLUS:
			Print("PLUS");
			int i = expression.getChildren().length;

			Stack<JmlType> tmp_setExtType = new Stack<JmlType>(); 
			translation += "new Integer(";

			boolean mapstoType_needed = true;
			Stack<JmlType> tmp_mapstoType = new Stack<JmlType>();

			for (Expression exprs : expression.getChildren()){
				exprs.accept(this);
				if (mapstoType_needed){
					mapstoType_needed = false;
					if (mapstoType.size() != 0){
						tmp_mapstoType = clone(mapstoType);
					}
				}

				//SetExtType needs just the first part
				if (setExtType.size() != 0 && tmp_setExtType.size() == 0){
					tmp_setExtType = clone(setExtType);
					setExtType.clear();
				}
				if (i > 1){
					translation += " + ";
				} i--;
			}
			translation += ")";
			if (tmp_setExtType.size() == 0){
				setExtType.clear();
			}else{
				setExtType = clone(tmp_setExtType);
			}
			if (tmp_mapstoType.size() != 0){
				mapstoType = clone(tmp_mapstoType);
			}
			break;
		case 	Formula.MUL:
			Print("MUL");
			int i1 = expression.getChildren().length;
			tmp_setExtType = new Stack<JmlType>(); 
			mapstoType_needed = true;
			tmp_mapstoType = new Stack<JmlType>();

			translation += "new Integer(";
			for (Expression exprs : expression.getChildren()){
				exprs.accept(this);
				if (mapstoType_needed){
					mapstoType_needed = false;
					if (mapstoType.size() != 0){
						tmp_mapstoType = clone(mapstoType);
					}
				}


				//SetExtType needs just the first part
				if (setExtType.size() != 0 && tmp_setExtType.size() == 0){
					tmp_setExtType = clone(setExtType);
					setExtType.clear();
				}

				if (i1 > 1){
					translation += " * ";
				} i1--;
			}
			translation += ")";
			if (tmp_setExtType.size() == 0){
				setExtType.clear();
			}else{
				setExtType = clone(tmp_setExtType);
			}
			if (tmp_mapstoType.size() != 0){
				mapstoType = clone(tmp_mapstoType);
			}
			break;
		}
	}

	public boolean top_rel = true;

	@Override
	public void visitAtomicExpression(AtomicExpression expression) {
		Print("\n visitAtomicExpression");

		/*{INTEGER, NATURAL, NATURAL1, BOOL, TRUE, FALSE, EMPTYSET, KPRED, KSUCC,
		  KPRJ1_GEN, KPRJ2_GEN, KID_GEN}.*/

		switch (expression.getTag()){
		case	Formula.INTEGER:
			Print("INTEGER");
			if (predTrans){
				translation += "INT.instance";
				//Update SetExtension type if needed
				if (setExtType.size() != 0){
					setExtType.peek().update(new JmlType("",JmlType.intT));
				}
			}else
				if (gettingType){
					//translation += "Integer";
					if (currentType.d){
						currentType = new JmlType(JmlType.intT);
					}else{
						if (currentType.getInternalType().equals(JmlType.relT) ||
								currentType.getInternalType().equals(JmlType.setT)||
								currentType.getInternalType().equals(JmlType.pairT)){

							currentType.update(new JmlType("",JmlType.intT));
						}
					}
				}else
					if (assignmentTrans){
						translation += "INT.instance";
					}
			break;
		case	Formula.BOOL:
			Print("BOOL");
			if (predTrans || assignmentTrans){
				translation += "BOOL.instance";
				//Update SetExtension type if needed
				if (setExtType.size() != 0){
					setExtType.peek().update(new JmlType("",JmlType.boolT));
				}
			}
			else
				if (gettingType){
					if (currentType.d){
						//The type needs to be created
						currentType = new JmlType(JmlType.boolT);
					}else{
						//The type is already created, it needs to be updated
						if (currentType.getInternalType().equals(JmlType.relT) ||
								currentType.getInternalType().equals(JmlType.setT) ||
								currentType.getInternalType().equals(JmlType.pairT)){
							currentType.update(new JmlType("",JmlType.boolT));
						}
					}

					//translation += "Boolean";
				}
			break;
		case	Formula.TRUE:
			Print("TRUE");
			if (assignmentTrans){
				translation += "true";
				if (assignamentType.equals("")){
					assignamentType = JmlType.NATIVE;
				}
			}else if(predTrans){
				translation += "true";
			}
			//Update SetExtension type if needed
			if (setExtType.size() != 0){
				setExtType.peek().update(new JmlType("",JmlType.boolT));
			}
			if (mapstoType.size() != 0){
				mapstoType.peek().update(new JmlType("",JmlType.boolT));
			}
			break;
		case	Formula.FALSE:
			Print("FALSE1");
			if (assignmentTrans){
				translation += "false";
				if (assignamentType.equals("")){
					assignamentType = JmlType.NATIVE;
				}
			}else if(predTrans){
				translation += "false";
			}
			//Update SetExtension type if needed
			if (setExtType.size() != 0){
				setExtType.peek().update(new JmlType("",JmlType.boolT));
			}
			if (mapstoType.size() != 0){
				mapstoType.peek().update(new JmlType("",JmlType.boolT));
			}
			break;
		case	Formula.NATURAL:
			Print("NATURAL");
			if (predTrans || assignmentTrans){
				translation += "NAT.instance";
				//Update SetExtension type if needed
				if (setExtType.size() != 0){
					setExtType.peek().update(new JmlType("",JmlType.intT));
				}
				//(12.11)VR: Bug Type
				//		It is missing to update both mapstoType variable
				if (mapstoType.size() != 0){
					mapstoType.peek().update(new JmlType("",JmlType.intT));
				}
			}
			break;
		case	Formula.NATURAL1:
			Print("NATURAL1");
			if (predTrans || assignmentTrans){
				translation += "NAT1.instance";
				//Update SetExtension type if needed
				if (setExtType.size() != 0){
					setExtType.peek().update(new JmlType("",JmlType.intT));
				}
				if (mapstoType.size() != 0){
					mapstoType.peek().update(new JmlType("",JmlType.intT));
				}
			}
			break;
		case	Formula.EMPTYSET:
			Print("EMPTYSET");
			if (assignmentTrans){
				if (assignamentType.equals("")){
					assignamentType = "EMPTY";
				}
				translation += "BSet.EMPTY";
			}else if (predTrans){
				translation += "BSet.EMPTY";
			}
			
			getvariableType("",expression.getType().toString(), false);
			

			//Update SetExtension and Mapsto type if needed
			if (setExtType.size() != 0){
				setExtType.peek().update(new JmlType (currentType));
			}
			if (mapstoType.size() != 0){
				mapstoType.peek().update(new JmlType (currentType));
			}
			break;
		case	Formula.KPRED:
			Print("KPRED");
			
			/*
			 * The translation for PRED syntax is done in 
			 * FUNIMAGE since Rodin generates for pred(exp) this AST
			 * 					FUNIMAGE
			 * 					   /\
			 * 					  /	 \
			 * 				  KPRED   exp
			 */
			
			break;
		case	Formula.KSUCC:
			Print("KSUCC");
			/*
			 * The translation for SUCC syntax is done in 
			 * FUNIMAGE since Rodin generates for succ(exp) this AST
			 * 					FUNIMAGE
			 * 					   /\
			 * 					  /	 \
			 * 				  KSUCC   exp
			 */
			break;
		case	Formula.KPRJ1_GEN:
			Print("KPRJ1_GEN");
			break;
		case	Formula.KPRJ2_GEN:
			Print("KPRJ2_GEN");
			break;
		case	Formula.KID_GEN:
			Print("KID_GEN");
			translation += "(new ID())";
			break;
		}		
	}


	private boolean brel = false;

	@Override
	public void visitBinaryExpression(BinaryExpression expression) {
		Print("\n visitBinaryExpression");
		// Binary Expression (check)
		// expression {} expression
		switch (expression.getTag()){
		case Formula.MINUS:
			Print("MINUS");
			Stack<JmlType> tmp_mapstoType = new Stack<JmlType>();
			if (mapstoType.size() != 0){
				mapstoType.peek().update(new JmlType("",JmlType.intT));
				tmp_mapstoType = clone(mapstoType);
				tmp_mapstoType.clear();
			}


			Stack<JmlType> tmp_setExtType = new Stack<JmlType>();
			//SetExtType is an Integer
			if (setExtType.size() != 0){
				setExtType.peek().update(new JmlType("",JmlType.intT));
				tmp_setExtType = clone(setExtType);
				setExtType.clear();
			}

			if (assignmentTrans || predTrans){

				translation += "new Integer(";
				expression.getLeft().accept(this);
				translation += " - ";
				expression.getRight().accept(this);
				translation += ")";
			}

			//returns back the values of setExtType 
			if (tmp_setExtType.size() == 0){
				setExtType.clear();
			}else{
				setExtType = clone(tmp_setExtType);
			}
			if (tmp_mapstoType.size() != 0){
				mapstoType = clone(tmp_mapstoType);
			}
			break;
		case Formula.MAPSTO:
			Print("MAPSTO");

			mapstoType.add(new JmlType("",JmlType.pairT));
			
			if (setExtType.size() != 0){
				setExtType.peek().update(new JmlType("",JmlType.pairT));
			}


			String tmp2 = translation;
			translation = "";

			translation += "new "+ "ERROR_TYPE_MAPSTO(";

			expression.getLeft().accept(this);


			translation += ",";

			expression.getRight().accept(this);

			//Update the type
			JmlType the_type = mapstoType.peek();

			translation = translation.replace("ERROR_TYPE_MAPSTO",the_type.getJmlType());

			if (mapstoType.size() > 1){
				JmlType tt = mapstoType.pop();
				mapstoType.peek().update(tt);
			}else{
				mapstoType.clear();
			}

			translation += ")";
			translation = tmp2 + translation;
			break;
		case Formula.DIV:
			Print("DIV");
			tmp_mapstoType = new Stack<JmlType>();
			if (mapstoType.size() != 0){
				mapstoType.peek().update(new JmlType(JmlType.intT));
				tmp_mapstoType = clone(mapstoType);
				mapstoType.clear();
			}
			tmp_setExtType = new Stack<JmlType>();
			//SetExtType is an Integer
			if (setExtType.size() != 0){
				setExtType.peek().update(new JmlType("",JmlType.intT));
				tmp_setExtType = clone(setExtType);
				setExtType.clear();
			}
			if (assignmentTrans || predTrans){
				translation += "new Integer(";
				expression.getLeft().accept(this);
				translation += " / ";
				expression.getRight().accept(this);
				translation += ")";
			}
			//returns back the values of setExtType 
			if (tmp_setExtType.size() == 0){
				setExtType.clear();
			}else{
				setExtType = clone(tmp_setExtType);
			}
			if (tmp_mapstoType.size() != 0){
				mapstoType = clone(tmp_mapstoType);
			}
			break;
		case Formula.MOD:
			Print("MOD");
			tmp_mapstoType = new Stack<JmlType>();
			if (mapstoType.size() != 0){
				mapstoType.peek().update(new JmlType(JmlType.intT));
				tmp_mapstoType = clone(mapstoType);
				mapstoType.clear();
			}

			tmp_setExtType = new Stack<JmlType>();
			//SetExtType is an Integer
			if (setExtType.size() != 0){
				setExtType.peek().update(new JmlType("",JmlType.intT));
				tmp_setExtType = clone(setExtType);
				setExtType.clear();
			}

			if (assignmentTrans || predTrans){
				translation += "new Integer(";
				expression.getLeft().accept(this);
				translation += " % ";
				expression.getRight().accept(this);
				translation += ")";
			}
			if (tmp_mapstoType.size() != 0){
				mapstoType = clone(tmp_mapstoType);
			}


			break;
		case Formula.EXPN:
			Print("EXPN");
			translation += "Math.pow(";
			expression.getLeft().accept(this);
			translation += ",";
			expression.getRight().accept(this);
			translation += ");";
			break;
		case Formula.REL:
			Print("REL");
			if (predTrans){
				if (isaSubset && varFuncRel.size() == 1){
					String newType = getvariableType("",expression.getType().toString(), false);
					//since the Func/Rel is defined as Power Set, we need to capture just the inside type
					newType = newType.replaceFirst("BSet<", "");
					//and eliminate the last '>'
					newType = newType.substring(0, newType.length()-1);
					translation += "(\\forall "+newType+" elems_" + varFuncRel.peek() + "; " + varFuncRel.peek() + ".has(elems_" + varFuncRel.peek() + "); ";
					varFuncRel.set(0, "elems_"+varFuncRel.peek());
				}

				String tmp = translation;
				String main = "";

				if (top_rel){
					main += " && ";
				}
				main += "BRelation.cross(?left_rel?,?right_rel?)";
				String rightSide = "";
				String leftSide = "";
				translation = "";
				FreeIdentVar = "";

				boolean top_rel_tmp = top_rel;
				if (top_rel){
					tmp += " ?left_info?.domain().isSubset(";
					top_rel = false;
				}

				expression.getLeft().accept(this);

				top_rel = top_rel_tmp;

				if (FreeIdentVar.equals("")){
					leftSide = translation;
				}else{
					leftSide = FreeIdentVar;
					FreeIdentVar = "";
				}

				main = main.replaceAll("\\?left_rel\\?", translation);

				if (top_rel){
					tmp += translation + ")";
				}

				translation = "";
				top_rel_tmp = top_rel;
				if (top_rel){
					tmp += " && ?left_info?.range().isSubset(";
					top_rel = false;
				}


				expression.getRight().accept(this);
				top_rel = top_rel_tmp;
				if (FreeIdentVar.equals("")){
					leftSide = translation;
				}else{
					leftSide = FreeIdentVar;
					FreeIdentVar = "";
				}


				main = main.replaceAll("\\?right_rel\\?", translation);

				if (top_rel){
					tmp += translation + ")";
				}


				if (FreeIdentVar.equals("")){
					rightSide = translation;
				}else{
					rightSide = FreeIdentVar;
					FreeIdentVar = "";
				}

				translation = tmp + main;



				//Checks if either left or right side visits an ID
				if (expression.getRight().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", leftSide);
					}
				}else if(expression.getLeft().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", rightSide);
					}
				}
				if (varFuncRel.size() == 1){
					varFuncRel.pop();
					if (isaSubset){
						translation += ")";
					}
				}
			}else if(assignmentTrans){
				translation += "BRelation.cross(";
				expression.getLeft().accept(this);
				translation += ",";
				expression.getRight().accept(this);
				translation += ")";
				if (member_of){
					translation += ".pow()";
				}
			}else{
				expression.getLeft().accept(this);
				expression.getRight().accept(this);
			}
			break;
		case Formula.TREL:
			Print("TREL");
			if (predTrans){
				if (isaSubset && varFuncRel.size() == 1){
					String newType = getvariableType("",expression.getType().toString(), false);
					//since the Func/Rel is defined as Power Set, we need to capture just the inside type
					newType = newType.replaceFirst("BSet<", "");
					//and eliminate the last '>'
					newType = newType.substring(0, newType.length()-1);
					translation += "(\\forall "+newType+" elems_" + varFuncRel.peek() + "; " + varFuncRel.peek() + ".has(elems_" + varFuncRel.peek() + "); ";
					varFuncRel.set(0, "elems_"+varFuncRel.peek());
				}

				String tmp = translation;
				String main = "";

				if (top_rel){
					main += " && ";
				}
				main += "BRelation.cross(?left_rel?,?right_rel?)";
				String rightSide = "";
				String leftSide = "";
				translation = "";
				FreeIdentVar = "";

				boolean top_rel_tmp = top_rel;
				if (top_rel){
					tmp += " ?left_info?.domain().equals(";
					top_rel = false;
				}

				expression.getLeft().accept(this);

				top_rel = top_rel_tmp;

				if (FreeIdentVar.equals("")){
					leftSide = translation;
				}else{
					leftSide = FreeIdentVar;
					FreeIdentVar = "";
				}

				main = main.replaceAll("\\?left_rel\\?", translation);

				if (top_rel){
					tmp += translation + ")";
				}

				translation = "";
				top_rel_tmp = top_rel;
				if (top_rel){
					tmp += " && ?left_info?.range().isSubset(";
					top_rel = false;
				}


				expression.getRight().accept(this);
				top_rel = top_rel_tmp;
				if (FreeIdentVar.equals("")){
					leftSide = translation;
				}else{
					leftSide = FreeIdentVar;
					FreeIdentVar = "";
				}


				main = main.replaceAll("\\?right_rel\\?", translation);

				if (top_rel){
					tmp += translation + ")";
				}


				if (FreeIdentVar.equals("")){
					rightSide = translation;
				}else{
					rightSide = FreeIdentVar;
					FreeIdentVar = "";
				}

				translation = tmp + main;



				//Checks if either left or right side visits an ID
				if (expression.getRight().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", leftSide);
					}
				}else if(expression.getLeft().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", rightSide);
					}
				}
				if (varFuncRel.size() == 1){
					varFuncRel.pop();
					if (isaSubset){
						translation += ")";
					}
				}
			}else if(assignmentTrans){
				translation += "BRelation.cross(";
				expression.getLeft().accept(this);
				translation += ",";
				expression.getRight().accept(this);
				translation += ")";
				if (member_of){
					translation += ".pow()";
				}
			}else{
				expression.getLeft().accept(this);
				expression.getRight().accept(this);
			}
			break;
		case Formula.SREL:
			Print("SREL");
			if (predTrans){
				if (isaSubset && varFuncRel.size() == 1){
					String newType = getvariableType("",expression.getType().toString(), false);
					//since the Func/Rel is defined as Power Set, we need to capture just the inside type
					newType = newType.replaceFirst("BSet<", "");
					//and eliminate the last '>'
					newType = newType.substring(0, newType.length()-1);
					translation += "(\\forall "+newType+" elems_" + varFuncRel.peek() + "; " + varFuncRel.peek() + ".has(elems_" + varFuncRel.peek() + "); ";
					varFuncRel.set(0, "elems_"+varFuncRel.peek());
				}

				String tmp = translation;
				String main = "";

				if (top_rel){
					main += " && ";
				}
				main += "BRelation.cross(?left_rel?,?right_rel?)";
				String rightSide = "";
				String leftSide = "";
				translation = "";
				FreeIdentVar = "";

				boolean top_rel_tmp = top_rel;
				if (top_rel){
					tmp += " ?left_info?.domain().isSubset(";
					top_rel = false;
				}

				expression.getLeft().accept(this);

				top_rel = top_rel_tmp;

				if (FreeIdentVar.equals("")){
					leftSide = translation;
				}else{
					leftSide = FreeIdentVar;
					FreeIdentVar = "";
				}

				main = main.replaceAll("\\?left_rel\\?", translation);

				if (top_rel){
					tmp += translation + ")";
				}

				translation = "";
				top_rel_tmp = top_rel;
				if (top_rel){
					tmp += " && ?left_info?.range().equals(";
					top_rel = false;
				}


				expression.getRight().accept(this);
				top_rel = top_rel_tmp;
				if (FreeIdentVar.equals("")){
					leftSide = translation;
				}else{
					leftSide = FreeIdentVar;
					FreeIdentVar = "";
				}


				main = main.replaceAll("\\?right_rel\\?", translation);

				if (top_rel){
					tmp += translation + ")";
				}


				if (FreeIdentVar.equals("")){
					rightSide = translation;
				}else{
					rightSide = FreeIdentVar;
					FreeIdentVar = "";
				}

				translation = tmp + main;



				//Checks if either left or right side visits an ID
				if (expression.getRight().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", leftSide);
					}
				}else if(expression.getLeft().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", rightSide);
					}
				}
				if (varFuncRel.size() == 1){
					varFuncRel.pop();
					if (isaSubset){
						translation += ")";
					}
				}
			}else if(assignmentTrans){
				translation += "BRelation.cross(";
				expression.getLeft().accept(this);
				translation += ",";
				expression.getRight().accept(this);
				translation += ")";
				if (member_of){
					translation += ".pow()";
				}
			}else{
				expression.getLeft().accept(this);
				expression.getRight().accept(this);
			}
			break;
		case Formula.STREL:
			Print("STREL");
			if (predTrans){
				if (isaSubset && varFuncRel.size() == 1){
					String newType = getvariableType("",expression.getType().toString(), false);
					//since the Func/Rel is defined as Power Set, we need to capture just the inside type
					newType = newType.replaceFirst("BSet<", "");
					//and eliminate the last '>'
					newType = newType.substring(0, newType.length()-1);
					translation += "(\\forall "+newType+" elems_" + varFuncRel.peek() + "; " + varFuncRel.peek() + ".has(elems_" + varFuncRel.peek() + "); ";
					varFuncRel.set(0, "elems_"+varFuncRel.peek());
				}
				String tmp = translation;
				String main = "";

				if (top_rel){
					main += " && ";
				}
				main += "BRelation.cross(?left_rel?,?right_rel?)";
				String rightSide = "";
				String leftSide = "";
				translation = "";
				FreeIdentVar = "";

				boolean top_rel_tmp = top_rel;
				if (top_rel){
					tmp += " ?left_info?.domain().equals(";
					top_rel = false;
				}

				expression.getLeft().accept(this);

				top_rel = top_rel_tmp;

				if (FreeIdentVar.equals("")){
					leftSide = translation;
				}else{
					leftSide = FreeIdentVar;
					FreeIdentVar = "";
				}

				main = main.replaceAll("\\?left_rel\\?", translation);

				if (top_rel){
					tmp += translation + ")";
				}

				translation = "";
				top_rel_tmp = top_rel;
				if (top_rel){
					tmp += " && ?left_info?.range().equals(";
					top_rel = false;
				}


				expression.getRight().accept(this);
				top_rel = top_rel_tmp;
				if (FreeIdentVar.equals("")){
					leftSide = translation;
				}else{
					leftSide = FreeIdentVar;
					FreeIdentVar = "";
				}


				main = main.replaceAll("\\?right_rel\\?", translation);

				if (top_rel){
					tmp += translation + ")";
				}


				if (FreeIdentVar.equals("")){
					rightSide = translation;
				}else{
					rightSide = FreeIdentVar;
					FreeIdentVar = "";
				}

				translation = tmp + main;



				//Checks if either left or right side visits an ID
				if (expression.getRight().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", leftSide);
					}
				}else if(expression.getLeft().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", rightSide);
					}
				}
				if (varFuncRel.size() == 1){
					varFuncRel.pop();
					if (isaSubset){
						translation += ")";
					}
				}
			}else if(assignmentTrans){
				translation += "BRelation.cross(";
				expression.getLeft().accept(this);
				translation += ",";
				expression.getRight().accept(this);
				translation += ")";
				if (member_of){
					translation += ".pow()";
				}
			}else{
				expression.getLeft().accept(this);
				expression.getRight().accept(this);
			}
			break;
		case Formula.PFUN:
			Print("PFUN");
			if (predTrans){
				if (isaSubset && varFuncRel.size() == 1){
					String newType = getvariableType("",expression.getType().toString(), false);
					//since the Func/Rel is defined as Power Set, we need to capture just the inside type
					newType = newType.replaceFirst("BSet<", "");
					//and eliminate the last '>'
					newType = newType.substring(0, newType.length()-1);
					translation += "(\\forall "+newType+" elems_" + varFuncRel.peek() + "; " + varFuncRel.peek() + ".has(elems_" + varFuncRel.peek() + "); ";
					varFuncRel.set(0, "elems_"+varFuncRel.peek());
				}

				String tmp = translation;
				String main = "";

				if (top_rel){
					main += " && ?left_info?.isaFunction() && ";
				}
				main += "BRelation.cross(?left_rel?,?right_rel?)";
				String rightSide = "";
				String leftSide = "";
				translation = "";
				FreeIdentVar = "";

				boolean top_rel_tmp = top_rel;
				if (top_rel){
					tmp += " ?left_info?.domain().isSubset(";
					top_rel = false;
				}

				expression.getLeft().accept(this);

				top_rel = top_rel_tmp;

				if (FreeIdentVar.equals("")){
					leftSide = translation;
				}else{
					leftSide = FreeIdentVar;
					FreeIdentVar = "";
				}

				main = main.replaceAll("\\?left_rel\\?", translation);

				if (top_rel){
					tmp += translation + ")";
				}

				translation = "";
				top_rel_tmp = top_rel;
				if (top_rel){
					tmp += " && ?left_info?.range().isSubset(";
					top_rel = false;
				}


				expression.getRight().accept(this);
				top_rel = top_rel_tmp;
				if (FreeIdentVar.equals("")){
					leftSide = translation;
				}else{
					leftSide = FreeIdentVar;
					FreeIdentVar = "";
				}


				main = main.replaceAll("\\?right_rel\\?", translation);

				if (top_rel){
					tmp += translation + ")";
				}


				if (FreeIdentVar.equals("")){
					rightSide = translation;
				}else{
					rightSide = FreeIdentVar;
					FreeIdentVar = "";
				}

				translation = tmp + main;



				//Checks if either left or right side visits an ID
				if (expression.getRight().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", leftSide);
					}
				}else if(expression.getLeft().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", rightSide);
					}
				}
				if (varFuncRel.size() == 1){
					varFuncRel.pop();
					if (isaSubset){
						translation += ")";
					}
				}
			}else if(assignmentTrans){
				translation += "BRelation.cross(";
				expression.getLeft().accept(this);
				translation += ",";
				expression.getRight().accept(this);
				translation += ")";
				if (member_of){
					translation += ".pow()";
				}
			}else{
				expression.getLeft().accept(this);
				expression.getRight().accept(this);
			}
			break;
		case Formula.TFUN:
			Print("TFUN");
			if (predTrans){
				if (isaSubset && varFuncRel.size() == 1){
					String newType = getvariableType("",expression.getType().toString(), false);
					//since the Func/Rel is defined as Power Set, we need to capture just the inside type
					newType = newType.replaceFirst("BSet<", "");
					//and eliminate the last '>'
					newType = newType.substring(0, newType.length()-1);
					translation += "(\\forall "+newType+" elems_" + varFuncRel.peek() + "; " + varFuncRel.peek() + ".has(elems_" + varFuncRel.peek() + "); ";
					varFuncRel.set(0, "elems_"+varFuncRel.peek());

				}

				String tmp = translation;
				String main = "";

				if (top_rel){
					main += " && ?left_info?.isaFunction() && ";
				}
				main += "BRelation.cross(?left_rel?,?right_rel?)";
				String rightSide = "";
				String leftSide = "";
				translation = "";
				FreeIdentVar = "";

				boolean top_rel_tmp = top_rel;
				if (top_rel){
					tmp += " ?left_info?.domain().equals(";
					top_rel = false;
				}

				expression.getLeft().accept(this);

				top_rel = top_rel_tmp;

				if (FreeIdentVar.equals("")){
					leftSide = translation;
				}else{
					leftSide = FreeIdentVar;
					FreeIdentVar = "";
				}

				main = main.replaceAll("\\?left_rel\\?", translation);

				if (top_rel){
					tmp += translation + ")";
				}

				translation = "";
				top_rel_tmp = top_rel;
				if (top_rel){
					tmp += " && ?left_info?.range().isSubset(";
					top_rel = false;
				}


				expression.getRight().accept(this);
				top_rel = top_rel_tmp;
				if (FreeIdentVar.equals("")){
					leftSide = translation;
				}else{
					leftSide = FreeIdentVar;
					FreeIdentVar = "";
				}


				main = main.replaceAll("\\?right_rel\\?", translation);

				if (top_rel){
					tmp += translation + ")";
				}


				if (FreeIdentVar.equals("")){
					rightSide = translation;
				}else{
					rightSide = FreeIdentVar;
					FreeIdentVar = "";
				}

				translation = tmp + main;



				//Checks if either left or right side visits an ID
				if (expression.getRight().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", leftSide);
					}
				}else if(expression.getLeft().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", rightSide);
					}
				}
				if (varFuncRel.size() == 1){
					varFuncRel.pop();
					if (isaSubset){
						translation += ")";
					}
				}
			}else if(assignmentTrans){
				translation += "BRelation.cross(";
				expression.getLeft().accept(this);
				translation += ",";
				expression.getRight().accept(this);
				translation += ")";
				if (member_of){
					translation += ".pow()";
				}
			}else{
				expression.getLeft().accept(this);
				expression.getRight().accept(this);
			}
			break;
		case Formula.PINJ:
			Print("PINJ");
			if (predTrans){
				if (isaSubset && varFuncRel.size() == 1){
					String newType = getvariableType("",expression.getType().toString(), false);
					//since the Func/Rel is defined as Power Set, we need to capture just the inside type
					newType = newType.replaceFirst("BSet<", "");
					//and eliminate the last '>'
					newType = newType.substring(0, newType.length()-1);
					translation += "(\\forall "+newType+" elems_" + varFuncRel.peek() + "; " + varFuncRel.peek() + ".has(elems_" + varFuncRel.peek() + "); ";
					varFuncRel.set(0, "elems_"+varFuncRel.peek());
				}

				String tmp = translation;
				String main = "";

				if (top_rel){
					main += " && ?left_info?.isaFunction() && ?left_info?.inverse().isaFunction() && ";
				}
				main += "BRelation.cross(?left_rel?,?right_rel?)";
				String rightSide = "";
				String leftSide = "";
				translation = "";
				FreeIdentVar = "";

				boolean top_rel_tmp = top_rel;
				if (top_rel){
					tmp += " ?left_info?.domain().isSubset(";
					top_rel = false;
				}

				expression.getLeft().accept(this);

				top_rel = top_rel_tmp;

				if (FreeIdentVar.equals("")){
					leftSide = translation;
				}else{
					leftSide = FreeIdentVar;
					FreeIdentVar = "";
				}

				main = main.replaceAll("\\?left_rel\\?", translation);

				if (top_rel){
					tmp += translation + ")";
				}

				translation = "";
				top_rel_tmp = top_rel;
				if (top_rel){
					tmp += " && ?left_info?.range().isSubset(";
					top_rel = false;
				}


				expression.getRight().accept(this);
				top_rel = top_rel_tmp;
				if (FreeIdentVar.equals("")){
					leftSide = translation;
				}else{
					leftSide = FreeIdentVar;
					FreeIdentVar = "";
				}


				main = main.replaceAll("\\?right_rel\\?", translation);

				if (top_rel){
					tmp += translation + ")";
				}


				if (FreeIdentVar.equals("")){
					rightSide = translation;
				}else{
					rightSide = FreeIdentVar;
					FreeIdentVar = "";
				}

				translation = tmp + main;



				//Checks if either left or right side visits an ID
				if (expression.getRight().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", leftSide);
					}
				}else if(expression.getLeft().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", rightSide);
					}
				}
				if (varFuncRel.size() == 1){
					varFuncRel.pop();
					if (isaSubset){
						translation += ")";
					}
				}
			}else if(assignmentTrans){
				translation += "BRelation.cross(";
				expression.getLeft().accept(this);
				translation += ",";
				expression.getRight().accept(this);
				translation += ")";
				if (member_of){
					translation += ".pow()";
				}
			}else{
				expression.getLeft().accept(this);
				expression.getRight().accept(this);
			}
			break;
		case Formula.TINJ:
			Print("TINJ");
			if (predTrans){
				if (isaSubset && varFuncRel.size() == 1){
					String newType = getvariableType("",expression.getType().toString(), false);
					//since the Func/Rel is defined as Power Set, we need to capture just the inside type
					newType = newType.replaceFirst("BSet<", "");
					//and eliminate the last '>'
					newType = newType.substring(0, newType.length()-1);
					translation += "(\\forall "+newType+" elems_" + varFuncRel.peek() + "; " + varFuncRel.peek() + ".has(elems_" + varFuncRel.peek() + "); ";
					varFuncRel.set(0, "elems_"+varFuncRel.peek());
				}

				String tmp = translation;
				String main = "";

				if (top_rel){
					main += " && ?left_info?.isaFunction() && ?left_info?.inverse().isaFunction() && ";
				}
				main += "BRelation.cross(?left_rel?,?right_rel?)";
				String rightSide = "";
				String leftSide = "";
				translation = "";
				FreeIdentVar = "";

				boolean top_rel_tmp = top_rel;
				if (top_rel){
					tmp += " ?left_info?.domain().equals(";
					top_rel = false;
				}

				expression.getLeft().accept(this);

				top_rel = top_rel_tmp;

				if (FreeIdentVar.equals("")){
					leftSide = translation;
				}else{
					leftSide = FreeIdentVar;
					FreeIdentVar = "";
				}

				main = main.replaceAll("\\?left_rel\\?", translation);

				if (top_rel){
					tmp += translation + ")";
				}

				translation = "";
				top_rel_tmp = top_rel;
				if (top_rel){
					tmp += " && ?left_info?.range().isSubset(";
					top_rel = false;
				}


				expression.getRight().accept(this);
				top_rel = top_rel_tmp;
				if (FreeIdentVar.equals("")){
					leftSide = translation;
				}else{
					leftSide = FreeIdentVar;
					FreeIdentVar = "";
				}


				main = main.replaceAll("\\?right_rel\\?", translation);

				if (top_rel){
					tmp += translation + ")";
				}


				if (FreeIdentVar.equals("")){
					rightSide = translation;
				}else{
					rightSide = FreeIdentVar;
					FreeIdentVar = "";
				}

				translation = tmp + main;



				//Checks if either left or right side visits an ID
				if (expression.getRight().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", leftSide);
					}
				}else if(expression.getLeft().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", rightSide);
					}
				}
				if (varFuncRel.size() == 1){
					varFuncRel.pop();
					if (isaSubset){
						translation += ")";
					}
				}
			}else if(assignmentTrans){
				translation += "BRelation.cross(";
				expression.getLeft().accept(this);
				translation += ",";
				expression.getRight().accept(this);
				translation += ")";
				if (member_of){
					translation += ".pow()";
				}
			}else{
				expression.getLeft().accept(this);
				expression.getRight().accept(this);
			}
			break;
		case Formula.PSUR:
			Print("PSUR");
			if (predTrans){
				if (isaSubset && varFuncRel.size() == 1){
					String newType = getvariableType("",expression.getType().toString(), false);
					//since the Func/Rel is defined as Power Set, we need to capture just the inside type
					newType = newType.replaceFirst("BSet<", "");
					//and eliminate the last '>'
					newType = newType.substring(0, newType.length()-1);
					translation += "(\\forall "+newType+" elems_" + varFuncRel.peek() + "; " + varFuncRel.peek() + ".has(elems_" + varFuncRel.peek() + "); ";
					varFuncRel.set(0, "elems_"+varFuncRel.peek());
				}

				String tmp = translation;
				String main = "";

				if (top_rel){
					main += " && ?left_info?.isaFunction() && ";
				}
				main += "BRelation.cross(?left_rel?,?right_rel?)";
				String rightSide = "";
				String leftSide = "";
				translation = "";
				FreeIdentVar = "";

				boolean top_rel_tmp = top_rel;
				if (top_rel){
					tmp += " ?left_info?.domain().isSubset(";
					top_rel = false;
				}

				expression.getLeft().accept(this);

				top_rel = top_rel_tmp;

				if (FreeIdentVar.equals("")){
					leftSide = translation;
				}else{
					leftSide = FreeIdentVar;
					FreeIdentVar = "";
				}

				main = main.replaceAll("\\?left_rel\\?", translation);

				if (top_rel){
					tmp += translation + ")";
				}

				translation = "";
				top_rel_tmp = top_rel;
				if (top_rel){
					tmp += " && ?left_info?.range().equals(";
					top_rel = false;
				}


				expression.getRight().accept(this);
				top_rel = top_rel_tmp;
				if (FreeIdentVar.equals("")){
					leftSide = translation;
				}else{
					leftSide = FreeIdentVar;
					FreeIdentVar = "";
				}


				main = main.replaceAll("\\?right_rel\\?", translation);

				if (top_rel){
					tmp += translation + ")";
				}


				if (FreeIdentVar.equals("")){
					rightSide = translation;
				}else{
					rightSide = FreeIdentVar;
					FreeIdentVar = "";
				}

				translation = tmp + main;



				//Checks if either left or right side visits an ID
				if (expression.getRight().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", leftSide);
					}
				}else if(expression.getLeft().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", rightSide);
					}
				}
				if (varFuncRel.size() == 1){
					varFuncRel.pop();
					if (isaSubset){
						translation += ")";
					}
				}
			}else if(assignmentTrans){
				translation += "BRelation.cross(";
				expression.getLeft().accept(this);
				translation += ",";
				expression.getRight().accept(this);
				translation += ")";
				if (member_of){
					translation += ".pow()";
				}
			}else{
				expression.getLeft().accept(this);
				expression.getRight().accept(this);
			}
			break;
		case Formula.TSUR:
			Print("TSUR");
			if (predTrans){
				if (isaSubset && varFuncRel.size() == 1){
					String newType = getvariableType("",expression.getType().toString(), false);
					//since the Func/Rel is defined as Power Set, we need to capture just the inside type
					newType = newType.replaceFirst("BSet<", "");
					//and eliminate the last '>'
					newType = newType.substring(0, newType.length()-1);
					translation += "(\\forall "+newType+" elems_" + varFuncRel.peek() + "; " + varFuncRel.peek() + ".has(elems_" + varFuncRel.peek() + "); ";
					varFuncRel.set(0, "elems_"+varFuncRel.peek());
				}

				String tmp = translation;
				String main = "";

				if (top_rel){
					main += " && ?left_info?.isaFunction() && ";
				}
				main += "BRelation.cross(?left_rel?,?right_rel?)";
				String rightSide = "";
				String leftSide = "";
				translation = "";
				FreeIdentVar = "";

				boolean top_rel_tmp = top_rel;
				if (top_rel){
					tmp += " ?left_info?.domain().equals(";
					top_rel = false;
				}

				expression.getLeft().accept(this);

				top_rel = top_rel_tmp;

				if (FreeIdentVar.equals("")){
					leftSide = translation;
				}else{
					leftSide = FreeIdentVar;
					FreeIdentVar = "";
				}

				main = main.replaceAll("\\?left_rel\\?", translation);

				if (top_rel){
					tmp += translation + ")";
				}

				translation = "";
				top_rel_tmp = top_rel;
				if (top_rel){
					tmp += " && ?left_info?.range().equals(";
					top_rel = false;
				}


				expression.getRight().accept(this);
				top_rel = top_rel_tmp;
				if (FreeIdentVar.equals("")){
					leftSide = translation;
				}else{
					leftSide = FreeIdentVar;
					FreeIdentVar = "";
				}


				main = main.replaceAll("\\?right_rel\\?", translation);

				if (top_rel){
					tmp += translation + ")";
				}


				if (FreeIdentVar.equals("")){
					rightSide = translation;
				}else{
					rightSide = FreeIdentVar;
					FreeIdentVar = "";
				}

				translation = tmp + main;



				//Checks if either left or right side visits an ID
				if (expression.getRight().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", leftSide);
					}
				}else if(expression.getLeft().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", rightSide);
					}
				}
				if (varFuncRel.size() == 1){
					varFuncRel.pop();
					if (isaSubset){
						translation += ")";
					}
				}
			}else if(assignmentTrans){
				translation += "BRelation.cross(";
				expression.getLeft().accept(this);
				translation += ",";
				expression.getRight().accept(this);
				translation += ")";
				if (member_of){
					translation += ".pow()";
				}
			}else{
				expression.getLeft().accept(this);
				expression.getRight().accept(this);
			}
			break;
		case Formula.TBIJ:
			Print("TBIJ");
			if (predTrans){
				if (isaSubset && varFuncRel.size() == 1){
					String newType = getvariableType("",expression.getType().toString(), false);
					//since the Func/Rel is defined as Power Set, we need to capture just the inside type
					newType = newType.replaceFirst("BSet<", "");
					//and eliminate the last '>'
					newType = newType.substring(0, newType.length()-1);
					translation += "(\\forall "+newType+" elems_" + varFuncRel.peek() + "; " + varFuncRel.peek() + ".has(elems_" + varFuncRel.peek() + "); ";
					varFuncRel.set(0, "elems_"+varFuncRel.peek());
				}

				String tmp = translation;
				String main = "";

				if (top_rel){
					main += " && ?left_info?.isaFunction() && ?left_info?.inverse().isaFunction() && ";
				}
				main += "BRelation.cross(?left_rel?,?right_rel?)";
				String rightSide = "";
				String leftSide = "";
				translation = "";
				FreeIdentVar = "";

				boolean top_rel_tmp = top_rel;
				if (top_rel){
					tmp += " ?left_info?.domain().equals(";
					top_rel = false;
				}

				expression.getLeft().accept(this);

				top_rel = top_rel_tmp;

				if (FreeIdentVar.equals("")){
					leftSide = translation;
				}else{
					leftSide = FreeIdentVar;
					FreeIdentVar = "";
				}

				main = main.replaceAll("\\?left_rel\\?", translation);

				if (top_rel){
					tmp += translation + ")";
				}

				translation = "";
				top_rel_tmp = top_rel;
				if (top_rel){
					tmp += " && ?left_info?.range().equals(";
					top_rel = false;
				}


				expression.getRight().accept(this);
				top_rel = top_rel_tmp;
				if (FreeIdentVar.equals("")){
					leftSide = translation;
				}else{
					leftSide = FreeIdentVar;
					FreeIdentVar = "";
				}


				main = main.replaceAll("\\?right_rel\\?", translation);

				if (top_rel){
					tmp += translation + ")";
				}


				if (FreeIdentVar.equals("")){
					rightSide = translation;
				}else{
					rightSide = FreeIdentVar;
					FreeIdentVar = "";
				}

				translation = tmp + main;



				//Checks if either left or right side visits an ID
				if (expression.getRight().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", leftSide);
					}
				}else if(expression.getLeft().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", rightSide);
					}
				}
				if (varFuncRel.size() == 1){
					varFuncRel.pop();
					if (isaSubset){
						translation += ")";
					}
				}
			}else if(assignmentTrans){
				translation += "BRelation.cross(";
				expression.getLeft().accept(this);
				translation += ",";
				expression.getRight().accept(this);
				translation += ")";
				if (member_of){
					translation += ".pow()";
				}
			}else{
				expression.getLeft().accept(this);
				expression.getRight().accept(this);
			}
			break;
		case Formula.SETMINUS:
			// Set difference
			Print("SETMINUS");
			if (predTrans || assignmentTrans){
				String tmp = translation;
				translation = "";
				FreeIdentVar = "";
				String rightSide = "";
				String leftSide = "";

				expression.getLeft().accept(this);

				tmp_mapstoType = new Stack<JmlType>();
				if (mapstoType.size() != 0){
					tmp_mapstoType = clone(mapstoType);
					mapstoType.clear();
				}

				tmp_setExtType = new Stack<JmlType>();
				//SetExtType needs just the left part
				if (setExtType.size() != 0){
					tmp_setExtType = clone(setExtType);
					setExtType.clear();
				}

				if (FreeIdentVar.equals("")){
					leftSide = translation;
				}else{
					leftSide = FreeIdentVar;
					FreeIdentVar = "";
				}
				translation = tmp + translation + ".difference(";
				tmp = translation;
				translation = "";
				FreeIdentVar = "";
				expression.getRight().accept(this);

				//returns back the values of setExtType 
				if (tmp_setExtType.size() == 0){
					setExtType.clear();
				}else{
					setExtType = clone(tmp_setExtType);
				}

				if (tmp_mapstoType.size() != 0){
					mapstoType = clone(tmp_mapstoType);
				}

				if (FreeIdentVar.equals("")){
					rightSide = translation;
				}else{
					rightSide = FreeIdentVar;
					FreeIdentVar = "";
				}
				translation = tmp + translation;
				translation += ")";
				//Checks if either left or right side visits a ID
				if (expression.getRight().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", leftSide);
					}
				}else if(expression.getLeft().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", rightSide);
					}
				}
			}
			break;
		case Formula.DOMRES:
			Print("DOMRES");
			if (predTrans || assignmentTrans){
				String tmp = translation;
				translation = "";
				FreeIdentVar = "";
				expression.getRight().accept(this);

				tmp_mapstoType = new Stack<JmlType>();
				if (mapstoType.size() != 0){
					tmp_mapstoType = clone(mapstoType);
					mapstoType.clear();
				}


				tmp_setExtType = new Stack<JmlType>();
				//SetExtType needs just the left part
				if (setExtType.size() != 0){
					tmp_setExtType = clone(setExtType);
					setExtType.clear();
				}

				String rightSide = "";

				if (FreeIdentVar.equals("")){
					rightSide = translation;
				}else{
					rightSide = FreeIdentVar;
					FreeIdentVar = "";
				}
				String leftSide = "";
				translation = tmp + translation + ".restrictDomainTo(";
				tmp = translation;
				translation = "";
				FreeIdentVar = "";
				expression.getLeft().accept(this);

				//returns back the values of setExtType 
				if (tmp_setExtType.size() == 0){
					setExtType.clear();
				}else{
					setExtType = clone(tmp_setExtType);
				}
				if (tmp_mapstoType.size() != 0){
					mapstoType = clone(tmp_mapstoType);
				}

				if (FreeIdentVar.equals("")){
					leftSide = translation;
				}else{
					leftSide = FreeIdentVar;
					FreeIdentVar = "";
				}
				translation = tmp + translation;
				translation += ")";
				//Checks if either left or right side visits a ID
				if (expression.getRight().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", leftSide);
					}
				}else if(expression.getLeft().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", rightSide);
					}
				}
			}
			break;
		case Formula.DOMSUB:
			Print("DOMSUB");
			if (predTrans || assignmentTrans){
				String tmp = translation;
				translation = "";
				FreeIdentVar = "";
				String rightSide = "";
				String leftSide = "";
				expression.getRight().accept(this);

				tmp_mapstoType = new Stack<JmlType>();
				if (mapstoType.size() != 0){
					tmp_mapstoType = clone(mapstoType);
					mapstoType.clear();
				}


				tmp_setExtType = new Stack<JmlType>();
				//SetExtType needs just the left part
				if (setExtType.size() != 0){
					tmp_setExtType = clone(setExtType);
					setExtType.clear();	
				}

				if (FreeIdentVar.equals("")){
					rightSide = translation;
				}else{
					rightSide = FreeIdentVar;
					FreeIdentVar = "";
				}
				translation = tmp + translation + ".domainSubtraction(";
				tmp = translation;
				translation = "";
				FreeIdentVar = "";
				expression.getLeft().accept(this);

				//returns back the values of setExtType 
				if (tmp_setExtType.size() == 0){
					setExtType.clear();
				}else{
					setExtType = clone(tmp_setExtType);
				}

				if (tmp_mapstoType.size() != 0){
					mapstoType = clone(tmp_mapstoType);
				}

				if (FreeIdentVar.equals("")){
					leftSide = translation;
				}else{
					leftSide = FreeIdentVar;
					FreeIdentVar = "";
				}
				translation = tmp + translation;
				translation += ")";
				//Checks if either left or right side visits a ID
				if (expression.getRight().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", leftSide);
					}
				}else if(expression.getLeft().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", rightSide);
					}
				}
			}
			break;
		case Formula.RANRES:
			Print("RANRES");
			if (predTrans || assignmentTrans){
				String tmp = translation;
				translation = "";
				FreeIdentVar = "";
				String rightSide = "";
				String leftSide = "";
				expression.getLeft().accept(this);

				tmp_mapstoType = new Stack<JmlType>();
				if (mapstoType.size() != 0){
					tmp_mapstoType = clone(mapstoType);
					mapstoType.clear();
				}


				tmp_setExtType = new Stack<JmlType>();
				//SetExtType needs just the left part
				if (setExtType.size() != 0){
					tmp_setExtType = clone(setExtType);
					setExtType.size();
				}

				if (FreeIdentVar.equals("")){
					leftSide = translation;
				}else{
					leftSide = FreeIdentVar;
					FreeIdentVar = "";
				}
				translation = tmp + translation + ".restrictRangeTo(";
				tmp = translation;
				translation = "";
				FreeIdentVar = "";
				expression.getRight().accept(this);

				//returns back the values of setExtType 
				if (tmp_setExtType.size() == 0){
					setExtType.clear();
				}else{
					setExtType = clone(tmp_setExtType);
				}

				if (tmp_mapstoType.size() != 0){
					mapstoType = clone(tmp_mapstoType);
				}

				if (FreeIdentVar.equals("")){
					rightSide = translation;
				}else{
					rightSide = FreeIdentVar;
					FreeIdentVar = "";
				}
				translation = tmp + translation;
				translation += ")";
				//Checks if either left or right side visits a ID
				if (expression.getRight().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", leftSide);
					}
				}else if(expression.getLeft().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", rightSide);
					}
				}
			}			
			break;
		case Formula.RANSUB:
			Print("RANSUB");
			if (predTrans || assignmentTrans){
				String tmp = translation;
				translation = "";
				FreeIdentVar = "";
				String leftSide = "";
				String rightSide = "";
				expression.getLeft().accept(this);

				tmp_mapstoType = new Stack<JmlType>();
				if (mapstoType.size() != 0){
					tmp_mapstoType = clone(mapstoType);
					mapstoType.clear();
				}


				tmp_setExtType = new Stack<JmlType>();
				//SetExtType needs just the left part
				if (setExtType.size() != 0){
					tmp_setExtType = clone(setExtType);
					setExtType.clear();
				}

				if (FreeIdentVar.equals("")){
					leftSide = translation;
				}else{
					leftSide = FreeIdentVar;
					FreeIdentVar = "";
				}
				translation = tmp + translation + ".rangeSubtraction(";
				tmp = translation;
				translation = "";
				FreeIdentVar = "";
				expression.getRight().accept(this);

				//returns back the values of setExtType 
				if (tmp_setExtType.size() == 0){
					setExtType.clear();
				}else{
					setExtType = clone(tmp_setExtType);
				}

				if (tmp_mapstoType.size() != 0){
					mapstoType = clone(tmp_mapstoType);
				}

				if (FreeIdentVar.equals("")){
					rightSide = translation;
				}else{
					rightSide = FreeIdentVar;
					FreeIdentVar = "";
				}
				translation = tmp + translation;
				translation += ")";
				//Checks if either left or right side visits an ID
				//TODO I think this was the previous way to update the set extension type
				if (expression.getRight().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", leftSide);
					}
				}else if(expression.getLeft().getTag() == Formula.KID_GEN){
					if (translation.contains("??")){
						translation = translation.replace("??", rightSide);
					}
				}
			}			
			break;
		case Formula.UPTO:  
			Print("UPTO");
			if (predTrans || assignmentTrans){
				tmp_mapstoType = new Stack<JmlType>();
				if (mapstoType.size() != 0){
					mapstoType.peek().update(new JmlType("",JmlType.intT));
					tmp_mapstoType = clone(mapstoType);
					mapstoType.clear();
				}


				tmp_setExtType = new Stack<JmlType>();
				//Update SetExtension type if needed
				if (setExtType.size() != 0){
					setExtType.peek().update(new JmlType("",JmlType.intT));
					tmp_setExtType = clone(setExtType);
					setExtType.clear();
				}
				translation += "new Enumerated(";
				expression.getLeft().accept(this);
				translation += ",";
				expression.getRight().accept(this);
				translation += ")";
				if (tmp_setExtType.size() == 0){
					setExtType.clear();
				}else{
					setExtType = clone(tmp_setExtType);
				}

				if (tmp_mapstoType.size() != 0){
					mapstoType = clone(tmp_mapstoType);
				}
			}
			break;
		case Formula.RELIMAGE:
			Print("RELIMAGE");
			String tmp1 = translation;
			translation = "";
			expression.getLeft().accept(this);
			translation = tmp1 + translation + ".image(";
			expression.getRight().accept(this);
			translation += ")";
			break;
		case Formula.CPROD:
			Print("CPROD");
			if (gettingType){
				if (brel){ //coming from POW
					//It is a set of pairs which is translated as BRelation
					currentType.updateSetPairs();
				}else{
					if (currentType.d){
						//the type hasn't been created
						currentType = new JmlType(JmlType.pairT);
					}else{
						//the type was created, it is needed to update the values
						currentType.update(new JmlType("",JmlType.pairT));
					}
				}
				
				brel = false;
				expression.getLeft().accept(this);
				//translation += ",";
				expression.getRight().accept(this);
				//translation += ">";
				//nestedType = false;
			}else if(predTrans || assignmentTrans){
				//(12.11)VR: Bug Type
				//		It is missing to update both setType and mapstoType variables
				
				
				if (mapstoType.size() != 0){
					mapstoType.add(new JmlType("",JmlType.relT));
				}
				//Update SetExtension type if needed
				if (setExtType.size() != 0){
					setExtType.add(new JmlType("",JmlType.relT));
				}
				
				translation += "BRelation.cross(";
				expression.getLeft().accept(this);
				translation += ",";
				expression.getRight().accept(this);
				translation += ")";
				
				//Update Mapsto 
				if (mapstoType.size() > 1){
					JmlType TT = mapstoType.pop();
					
					JmlType prj1 = TT.getDomainType();
					JmlType prj2 = TT.getRangeType();
					
					//check whether prj1 or prj2 contains sets
					// TODO: this can be easily managed by JmlType class
					if (prj1.getInternalType().equals(JmlType.setT)) {
						prj1 = new JmlType (prj1.getSetType());
					}else if (prj1.getInternalType().equals(JmlType.relT)) {
						JmlType p1 = prj1.getDomainType();
						JmlType p2 = prj1.getRangeType();
						prj1 = new JmlType (new JmlType ("",JmlType.pairT));
						prj1.update(p1);
						prj1.update(p2);
					}
					if (prj2.getInternalType().equals(JmlType.setT)) {
						prj2 = new JmlType (prj2.getSetType());
					}else if (prj2.getInternalType().equals(JmlType.relT)) {
						JmlType p1 = prj2.getDomainType();
						JmlType p2 = prj2.getRangeType();
						prj2 = new JmlType (new JmlType ("",JmlType.pairT));
						prj2.update(p1);
						prj2.update(p2);
					}
					mapstoType.peek().update(new JmlType("",JmlType.relT));
					mapstoType.peek().update(new JmlType(prj1));
					mapstoType.peek().update(new JmlType(prj2));
				}
				//Update setExtType
				if (setExtType.size() != 0){
					JmlType TT = setExtType.pop();
					
					JmlType prj1 = TT.getDomainType();
					JmlType prj2 = TT.getRangeType();
					
					//check whether prj1 or prj2 contains sets
					// TODO: this can be easily managed by JmlType class
					if (prj1.getInternalType().equals(JmlType.setT)) {
						prj1 = new JmlType (prj1.getSetType());
					}else if (prj1.getInternalType().equals(JmlType.relT)) {
						JmlType p1 = prj1.getDomainType();
						JmlType p2 = prj1.getRangeType();
						prj1 = new JmlType (new JmlType ("",JmlType.pairT));
						prj1.update(p1);
						prj1.update(p2);
					}
					if (prj2.getInternalType().equals(JmlType.setT)) {
						prj2 = new JmlType (prj2.getSetType());
					}else if (prj2.getInternalType().equals(JmlType.relT)) {
						JmlType p1 = prj2.getDomainType();
						JmlType p2 = prj2.getRangeType();
						prj2 = new JmlType (new JmlType ("",JmlType.pairT));
						prj2.update(p1);
						prj2.update(p2);
					}
					setExtType.peek().update(new JmlType("",JmlType.relT));
					setExtType.peek().update(new JmlType(prj1));
					setExtType.peek().update(new JmlType(prj2));
				}
			}else{
				expression.getLeft().accept(this);
				expression.getRight().accept(this);
			}
			break;	
		case Formula.DPROD:
			Print("DPROD");
			if (predTrans || assignmentTrans){
				String tmp = translation;
				translation = "";
				expression.getLeft().accept(this);
				translation = tmp + translation + ".directProd(";
				expression.getRight().accept(this);
				translation += tmp + ")";
			}
			break;
		case Formula.PPROD:
			Print("PPROD");
			if (predTrans || assignmentTrans){
				String tmp = translation;
				translation = "";
				expression.getLeft().accept(this);
				translation = tmp + translation + ".parallel(";
				expression.getRight().accept(this);
				translation += tmp + ")";
			}
			break;
		case Formula.FUNIMAGE:
			Print("FUNIMAGE");
			//there is a special case: PRED and SUCC
			//since Rodin generates AST for those constructs 
			//using the FUNIMAGE, we need to take care of that case

			if (expression.getLeft().getTag() == Formula.KSUCC ||
					expression.getLeft().getTag() == Formula.KPRED
					){
				String tmp = translation;
				translation = "";
				
				tmp_mapstoType = new Stack<JmlType>();
				if (mapstoType.size() != 0){
					mapstoType.peek().update(new JmlType("",JmlType.intT));
					tmp_mapstoType = clone(mapstoType);
					mapstoType.clear();
				}
				
				tmp_setExtType = new Stack<JmlType>();
				if (setExtType.size() != 0){
					setExtType.peek().update(new JmlType("",JmlType.intT));
					tmp_setExtType = clone(setExtType);
					setExtType.clear();
				}
				
				expression.getRight().accept(this);
				
				//giving back the values for setExtType and mapstoType
				if (tmp_mapstoType.size() != 0){
					mapstoType = clone(tmp_mapstoType);
				}
				if (tmp_setExtType.size() != 0){
					setExtType = clone(tmp_setExtType);
				}
				Integer d = 3;
				if (expression.getLeft().getTag() == Formula.KSUCC){
					translation = tmp + "new Integer(" + translation + " + 1)";
				}else{
					translation = tmp + "new Integer(" + translation + " - 1)";
				}
			}else{

				String tmp = translation;
				translation = "";

				tmp_setExtType = new Stack<JmlType>();
				if (setExtType.size() != 0){
					tmp_setExtType = clone(setExtType);
					setExtType.clear();
					setExtType.add(new JmlType("",JmlType.noT));
				}

				tmp_mapstoType = new Stack<JmlType>();
				if (mapstoType.size() != 0){
					tmp_mapstoType = clone(mapstoType);
					mapstoType.clear();
					mapstoType.add(new JmlType("",JmlType.noT));
				}

				expression.getLeft().accept(this);

				if (mapstoType.size() != 0){
					tmp_mapstoType.peek().update(mapstoType.peek().getRangeType());
					mapstoType.clear();
					mapstoType.add(new JmlType("",JmlType.noT));
				}


				if (setExtType.size() != 0){
					if (setExtType.peek().getInternalType().equals(JmlType.pairT)){
						tmp_setExtType.peek().update(setExtType.peek().prj1());
					}else{
						tmp_setExtType.peek().update(setExtType.peek().getRangeType());
					}

					setExtType.clear();
					//setExtType.add(new JmlType("",JmlType.noT));
				} 


				if (expression.getLeft().getTag() == Formula.KPRJ1_GEN || expression.getLeft().getTag() == Formula.KPRJ2_GEN){
					translation = "";
					expression.getRight().accept(this);
					if (expression.getLeft().getTag() == Formula.KPRJ1_GEN){
						translation = tmp + translation + ".fst()";
					}else{
						translation = tmp + translation + ".snd()";
					}
				}else{
					translation = tmp + translation + ".apply(";
					expression.getRight().accept(this);
				}

				if (tmp_setExtType.size() == 0){
					setExtType.clear();
				}else{
					setExtType = clone(tmp_setExtType);
				}

				if (tmp_mapstoType.size() != 0){				
					mapstoType = clone(tmp_mapstoType);
				}

				if (!(expression.getLeft().getTag() == Formula.KPRJ1_GEN || expression.getLeft().getTag() == Formula.KPRJ2_GEN)){
					translation += ")";
				}
			}
			break;
		}
	}

	@Override
	public void visitBoolExpression(BoolExpression expression) {
		Print("\n visitBoolExpression "); //bool(predicate)

		Stack<JmlType> tmp_mapstoType = new Stack<JmlType>();
		if (mapstoType.size() != 0){
			mapstoType.peek().update(new JmlType(JmlType.boolT));
			tmp_mapstoType = clone(mapstoType);
			mapstoType.clear();
		}


		Stack<JmlType> tmp_setExtType = new Stack<JmlType>();
		//SetExtType is an Boolean
		if (setExtType.size() != 0){
			setExtType.peek().update(new JmlType("",JmlType.boolT));
			tmp_setExtType = clone(setExtType);
			setExtType.clear();
		}
		boolExp = true;
		translation += "(";
		expression.getPredicate().accept(this);
		translation += ")";

		//returns back the values of setExtType 
		if (tmp_setExtType.size() == 0){
			setExtType.clear();
		}else{
			setExtType = clone(tmp_setExtType);
		}

		if (tmp_mapstoType.size() != 0){
			mapstoType = clone(tmp_mapstoType);
		}
	}

	@Override
	public void visitIntegerLiteral(IntegerLiteral expression) {
		Print("\n visitIntegerLiteral: " + expression.getValue());
		// Integet Literal (check)
		// 0..9


		if (mapstoType.size() != 0){
			mapstoType.peek().update(new JmlType(JmlType.intT));
		}
		//Update SetExtension type if needed
		if (setExtType.size() != 0){
			setExtType.peek().update(new JmlType("",JmlType.intT));
		}

		if (predTrans){
			translation += "new Integer("+expression.getValue()+")";
		}else if(assignmentTrans){
			if (assignamentType.equals("")){
				assignamentType = JmlType.NATIVE;
			}
			translation += expression.getValue();
		}
	}

	@Override
	public void visitQuantifiedExpression(QuantifiedExpression expression) {
		Print("\n visitQuantifiedExpression");
		// {union (bigU), intersection (bign)} varlist . predicate | expression
		//QuantifiedExpressions are being translated only to JML. We are not generating 
		//Java code since in the general way it could be infinite sets. e.g. {x¦ x > 10}

		ArrayList<String> boundedVars = new ArrayList<String>();

		//TODO it is missing QUNION QINTER
		switch (expression.getTag()){
		case 	Formula.QUNION:
			Print("QUNION");
			break;
		case 	Formula.QINTER:
			Print("QINTER");
			break;
		}
		
		/*if (varType.containsKey(FreeIdentVar)){
			type = varType.get(FreeIdentVar).getJmlType();
		}*/ //Note 07.11.17: type cannot be found in Free Identifier. It should be Bound Ident

		String vars = "";
		String tmp = translation;
		translation = "";
		
		HashMap<Integer, String> becomes_vars_tmp = new HashMap<Integer, String>();
		for (Integer k: becomes_vars.keySet()) {
			becomes_vars_tmp.put(k, becomes_vars.get(k));
		}
		becomes_vars = new HashMap<Integer, String>();
		int i = 0;
		for (BoundIdentDecl var: expression.getBoundIdentDecls()){
			Print("var: " + var.getName());
			//translation += var.toString();

			var.accept(this);
			// Translation variable contains the bound variable name

			//Once the set comprehension is over, the bounded variables should be deleted from varType
			String typeUnbounded = getvariableType(var.getName(), var.getType().toString(),true);
			boundedVars.add(var.getName());
			becomes_vars.put(i, var.getName());
			
			vars += typeUnbounded + " " + var.getName();

		}
		//do not take into account the value of translation since it contains the bounded variables

		translation = "";
		expression.getExpression().accept(this);
		String exp = translation;
		translation = "";
		
		expression.getPredicate().accept(this);
		String pred = translation;

		String type = "error";

		for (String v: boundedVars){
			if (type.contains("error")){
				if (varType.containsKey(v))
					type = varType.get(v).getInternalType();
			}
			varType.remove(v);
		}
		
		//(07.11)VR: Bug SetComprehension
		//		Type was not being translated correctly
		
		translation = tmp + 
				"new Best<" + type + ">(new JMLObjectSet {"
				+ vars + " | " +
				"(\\exists " + type + " e; (" + pred + "); e.equals(" + exp + "))})";
		
		int numBoundIdent = expression.getBoundIdentDecls().length;
		boundIdentifiers.subList(0, numBoundIdent).clear();
		
		// it is not translated to Java. Only to JML
		assignamentType = "QuantifiedExpression";
		
		becomes_vars = new HashMap<Integer, String>();
		for (Integer k: becomes_vars.keySet()) {
			becomes_vars.put(k, becomes_vars_tmp.get(k));
		}
	}


	@Override
	public void visitSetExtension(SetExtension expression) {
		Print("\n visitSetExtension");
		// Set Extension
		// '{' list_expression '}'
		if (predTrans || assignmentTrans){
			if (assignmentTrans && assignamentType.equals("")){
				assignamentType = JmlType.SET;
			}

			String tmp = translation;

			translation = "new ?SetExtType?(";
			setExtType.add(new JmlType(JmlType.setT));

			for (int exp=0; exp < expression.getMembers().length;exp++){


				if (exp == 0 && mapstoType.size() != 0){
					mapstoType.peek().update(new JmlType(JmlType.setT));
				}

				//Parse each expression.
				expression.getMembers()[exp].accept(this);
				if (exp < expression.getMembers().length-1){
					translation += ",";
				}else{
					translation += ")";
				}

				//Update the type of the BSet/BRel
				if (setExtType.size() != 0 && exp ==0){
					JmlType TT = setExtType.pop();
					translation = translation.replace("?SetExtType?",TT.getJmlType());
					if (setExtType.size() != 0){
						setExtType.peek().update(TT);
					}
				}
			}
			translation = tmp + translation;
		}else{
			translation += "{";
			for (Expression exp: expression.getMembers()){
				//Parse each expression.
				exp.accept(this);
				translation += ",";
			}
			translation += "}";
		}

	}

	@Override
	public void visitUnaryExpression(UnaryExpression expression) {
		// Unary Expressions
		//	Ident(expression)

		Print("\n visitUnaryExpression");
		switch (expression.getTag()){
		case 	Formula.UNMINUS:
			Print("UNMINUS");
			expression.getChild().accept(this);
			break;
		case 	Formula.KCARD:
			Print("KCARD");
			Stack<JmlType> tmp_setExtType = new Stack<JmlType>();
			//Update SetExtension type if needed
			if (setExtType.size() != 0){
				setExtType.peek().update(new JmlType("",JmlType.intT));
				tmp_setExtType = clone(setExtType);
				setExtType.clear();
			}
			translation += "new Integer(";
			expression.getChild().accept(this);

			if (tmp_setExtType.size() == 0){
				setExtType.clear();
			}else{
				setExtType = clone(tmp_setExtType);
			}

			translation += ".size())";
			break;
		case 	Formula.POW:
			Print("POW");
			if (predTrans){
				translation += "((";
				expression.getChild().accept(this);
				translation += ").pow())";
			}else if (gettingType){
				brel = true;
				if (currentType.d){
					//the type hasn't been created
					currentType = new JmlType(JmlType.setT);
				}else{
					//the type was created, it is needed to update the values
					currentType.update(new JmlType("",JmlType.setT));
				}

				//translation += "?VarType?";
				expression.getChild().accept(this);

			}else if (assignmentTrans){
				//translation = translation.replace("Type", "BSet<Type>");
				//translation += "(new BSet<Type>(";
				translation += "(";
				expression.getChild().accept(this);
				translation += ").pow()";
			}
			break;
		case 	Formula.POW1:
			Print("POW1");
			expression.getChild().accept(this);
			break;
		case 	Formula.KUNION:
			Print("KUNION");
			expression.getChild().accept(this);
			break;
		case 	Formula.KINTER:
			Print("KINTER");
			expression.getChild().accept(this);
			break;
		case 	Formula.KDOM:
			Print("KDOM");
			if (predTrans || assignmentTrans){
				String tmp = translation;
				translation = "";
				tmp_setExtType = new Stack<JmlType>();
				
								
				if (setExtType.size() != 0) {
					tmp_setExtType = clone(setExtType);
					setExtType.clear();
					setExtType.add(new JmlType("",JmlType.noT));
				}

				expression.getChild().accept(this);

				translation = tmp + translation + ".domain()";
				
				//Update SetExtension type if needed
				if (setExtType.size() != 0){
					tmp_setExtType.peek().update(setExtType.peek().getDomainType());
					//It is needed just the domain of the function/Relation
				}
				if (tmp_setExtType.size() == 0){
					setExtType.clear();
				}else{
					setExtType = clone(tmp_setExtType);
				}
				
				
			}else{
				expression.getChild().accept(this);
			}
			break;
		case 	Formula.KRAN:
			Print("KRAN");
			if (predTrans || assignmentTrans){
				String d = translation;
				translation = "";
				tmp_setExtType = new Stack<JmlType>();

				if (setExtType.size() != 0){
					tmp_setExtType = clone(setExtType);
					setExtType.clear();
					setExtType.add(new JmlType("",JmlType.noT));
				}

				expression.getChild().accept(this);
				//Update SetExtension type if needed
				if (setExtType.size() != 0){
					tmp_setExtType.peek().update(setExtType.peek().getRangeType());
					//It is needed just the range of the function/Relation
					//setExtType.set(setExtType.size()-1, setExtType.peek().replace("BRelation<", "").replace(">", "").split(",")[1]);
				}
				if (tmp_setExtType.size() == 0){
					setExtType.clear();
				}else{
					setExtType = clone(tmp_setExtType);
				}

				translation = d + translation + ".range()";
			}else{
				expression.getChild().accept(this);
			}
			break;
		case 	Formula.KMIN:
			Print("KMIN");
			if (predTrans || assignmentTrans){
				String tmp = translation;
				translation = "";
				expression.getChild().accept(this);
				translation = tmp + translation + ".min()";
			}else{
				expression.getChild().accept(this);
			}
			break;
		case 	Formula.KMAX:
			Print("KMAX");
			if (predTrans || assignmentTrans){
				String tmp = translation;
				translation = "";
				expression.getChild().accept(this);
				translation = tmp + translation + ".max()";
			}
			break;
		case 	Formula.CONVERSE:
			Print("CONVERSE");
			if (predTrans || assignmentTrans){
				String tmp = translation;
				translation = "";
				expression.getChild().accept(this);
				translation = tmp + translation + ".inverse()";
			}
			break;
		case 	Formula.KPRJ1_GEN:
			Print("KPRJ1_GEN");
			expression.getChild().accept(this);
			break;
		case 	Formula.KPRJ2_GEN:
			Print("KPRJ2_GEN");
			expression.getChild().accept(this);
			break;
		case 	Formula.KID_GEN:
			Print("KID_GEN");
			expression.getChild().accept(this);
			break;
		}
	}


	@Override
	public void visitBoundIdentifier(BoundIdentifier identifierExpression) {
		Print("\n visitBoundIdentifier");

		if (predTrans){
			Print("-- " + boundIdentifiers.get(identifierExpression.getBoundIndex()));
			translation += boundIdentifiers.get(identifierExpression.getBoundIndex());
			if (mapstoType.size() != 0){
				mapstoType.peek().update(varType.get(boundIdentifiers.get(identifierExpression.getBoundIndex())));
			}
			//Update SetExtension type if needed
			if (setExtType.size() != 0){
				setExtType.peek().update(varType.get(boundIdentifiers.get(identifierExpression.getBoundIndex())));
			}
		}else if(assignmentTrans){
			translation += becomes_vars.get(identifierExpression.getBoundIndex());
		}
	}

	@Override
	public void visitFreeIdentifier(FreeIdentifier identifierExpression) {
		Print("\n visitFreeIdentifier");
		Print("\t -- " + identifierExpression.getName());
		FreeIdentVar = identifierExpression.getName();
		JmlType tt = null;
		if (varType.containsKey(identifierExpression.getName())){
			tt = new JmlType(varType.get(identifierExpression.getName()));
		}

		if (gettingType) {
			Type type = identifierExpression.getType();
			String tmp;
			if (type == null){
				tmp = JmlType.intT;
			}else{
				tmp = type.toString();
			}
			if (currentType.d){
				currentType = new JmlType(currentVar,tmp);
			}else{
				currentType.update(new JmlType("",tmp));
			}
			translation += tmp;
		}else{
			if (predTrans){
				translation += "machine."+identifierExpression.getName();
			}else if (assignmentTrans){
				if (assignamentType.equals("")){
					String name = identifierExpression.getName();
					if (varType.get(name).getInternalType().equals(JmlType.intT)){
						assignamentType = JmlType.NATIVE;
					}else{
						assignamentType = JmlType.SET;
					}
				}
				translation += "machine."+identifierExpression.getName();
			}
		}

		//Update SetExtension type if needed
		if (setExtType.size() != 0){

			if (predTrans || assignmentTrans){
				tt.changeRel();
			}
			setExtType.peek().update(tt);
		}
		if (mapstoType.size() != 0){
			if (predTrans || assignmentTrans){
				tt.changeRel();
			}
			mapstoType.peek().update(tt);
		}
	}

	@Override
	public void visitAssociativePredicate(AssociativePredicate predicate) {
		Print("\n visitAssociativePredicate");
		// check again!
		switch (predicate.getTag()){
		case Formula.LAND:
			Print("LAND");
			int i = predicate.getChildren().length;
			for (Predicate pre : predicate.getChildren()){
				pre.accept(this);
				if (i > 1){
					translation += " && ";
				}i--;
			}
			break;
		case Formula.LOR:
			Print("LOR");
			int i2 = predicate.getChildren().length;
			for (Predicate pre : predicate.getChildren()){
				pre.accept(this);
				if (i2 > 1){
					translation += " || ";
				}i2--;
			}
			break;
		}
	}

	@Override
	public void visitBinaryPredicate(BinaryPredicate predicate) {
		Print("\n visitBinaryPredicate");
		//Binary predicate
		//	predicate {or/and/impl/equiv} predicate

		switch (predicate.getTag()){
		case Formula.LIMP:
			Print("LIMP");
			if (predTrans||assignmentTrans){
				if (predicate_guard){
					String tmp = translation + "BOOL.implication(";
					translation = "";
					predicate.getLeft().accept(this);
					translation += ",";
					String tmp2 = translation;
					translation = "";
					predicate.getRight().accept(this);
					translation = tmp2 + translation;
					translation = tmp + translation +  ")";
				}else{
					String tmp = translation + "((";
					translation = "";
					predicate.getLeft().accept(this);
					translation += ") ==> (";
					String tmp2 = translation;
					translation = "";
					predicate.getRight().accept(this);
					translation = tmp2 + translation;
					translation = tmp + translation +  "))";
				}
			}
			break;
		case Formula.LEQV:
			Print("LEQV");
			if (predTrans||assignmentTrans){
				if (predicate_guard){
					String tmp = translation + "BOOL.bi_implication(";
					translation = "";
					predicate.getLeft().accept(this);
					translation += ",";
					String tmp2 = translation;
					translation = "";
					predicate.getRight().accept(this);
					translation = tmp2 + translation;
					translation = tmp + translation +  ")";
				}else{
					String tmp = translation + "((";
					translation = "";
					predicate.getLeft().accept(this);
					translation += ") <==> (";
					String tmp2 = translation;
					translation = "";
					predicate.getRight().accept(this);
					translation = tmp2 + translation;
					translation = tmp + translation +  "))";
				}
			}
			break;
		}

	}

	@Override
	public void visitLiteralPredicate(LiteralPredicate predicate) {
		Print("\n visitLiteralPredicate");
		switch (predicate.getTag()){
		case 	Formula.BTRUE:
			Print("BTRUE");
			translation += "true";
			break;
		case 	Formula.BFALSE:
			Print("BFALSE");
			translation += "false";
			break;
		}
	}

	@Override
	public void visitMultiplePredicate(MultiplePredicate predicate) {
		Print("\n visitMultiplePredicate");
		//partition(a_1,a_2,...,a_n)
		translation += "BSet.partition(";
		int i = predicate.getChildCount();
		for (Expression child:  predicate.getChildren()){
			child.accept(this);
			if (i > 1){
				translation += ",";
			}
			i--;
		}
		translation += ")";
	}

	@Override
	public void visitQuantifiedPredicate(QuantifiedPredicate predicate) {
		Print("\n visitQuantifiedPredicate");
		// Quantified Predicate
		//	{forall/exists} varlist . predicate
		String par = "";
		ArrayList<String> boundedVars = new ArrayList<String>();
		switch (predicate.getTag()){
		case	Formula.FORALL:
			Print("FORALL");
			if (predTrans){
				String tmp = translation;
				translation = "";
				//translation += "(\\forall ";
				int i = predicate.getBoundIdentDecls().length;
				for (BoundIdentDecl var : predicate.getBoundIdentDecls()){
					//Has to get the bound variable and its type
					String tmp2 = "";
					tmp2 = translation;
					translation = "";
					var.accept(this);
					// Translation variable contains the bound variable name

					//Once the forall is over, the bounded variables should be deleted from varType
					String typeUnbounded = getvariableType(var.getName(), var.getType().toString(),true);
					boundedVars.add(var.getName());

					if (typeUnbounded.equals("NAT1.instance") ||
							typeUnbounded.equals("NAT.instance") ||
							typeUnbounded.equals("INT.instance") ||
							typeUnbounded.equals("Integer")){
						typeUnbounded = "Integer";
					}else if (typeUnbounded.equals("BOOL.instance")){
						typeUnbounded = "Boolean";
					}

					translation = tmp2 + " (\\forall " + typeUnbounded + " " + translation;
					par += ")";
					if (i > 1){
						translation += "; ";
					}i--;
				}
				translation = tmp + translation + ";";
			}
			break;
		case	Formula.EXISTS:
			Print("EXISTS");
			if (predTrans){
				String tmp = translation;
				translation = "";
				//translation += "(\\exists ";

				int i = predicate.getBoundIdentDecls().length;
				for (BoundIdentDecl var : predicate.getBoundIdentDecls()){
					//Has to get the bound variable and its type
					String tmp2 = "";
					tmp2 = translation;
					translation = "";
					var.accept(this);
					// Translation variable contains the bound variable name
					String typeUnbounded = getvariableType(var.getName(), var.getType().toString(),true);
					boundedVars.add(var.getName());
					if (typeUnbounded.equals("NAT1.instance") ||
							typeUnbounded.equals("NAT.instance") ||
							typeUnbounded.equals("INT.instance") ||
							typeUnbounded.equals("Integer")){
						typeUnbounded = "Integer";
					}else if (typeUnbounded.equals("BOOL.instance")){
						typeUnbounded = "Boolean";
					}

					translation = tmp2 + " (\\exists " + typeUnbounded + " " + translation;
					par += ")";
					if (i > 1){
						translation += ";";
					}i--;
				}
				translation = tmp + translation + ";";
			}
			break;
		}
		predicate.getPredicate().accept(this);
		//Once the forall/exists is over, the bounded variables should be deleted from varType
		for (String v: boundedVars){
			varType.remove(v);
		}
		translation += par;
		int numBoundIdent = predicate.getBoundIdentDecls().length;
		boundIdentifiers.subList(0, numBoundIdent).clear();
	}
	String current_name_test = "no_val";
	@Override
	public void visitRelationalPredicate(RelationalPredicate predicate) {
		Print("\n visitRelationalPredicate");
		// Relational Predicate
		// expression	{= / <= / < / >= / > / : (belongs)} expression
		switch (predicate.getTag()){
		case 	Formula.EQUAL:
			Print("EQUAL");
			if (predicate.getRight().getTag() == Formula.CSET){ //equality change if user is using a set comprehension
				String tmp = translation;
				translation = "";
				FreeIdentVar = "";
				predicate.getLeft().accept(this);
				translation = tmp + translation + ".isSubset(";
				predicate.getRight().accept(this);
				translation += ")";

				/*String ident = "";
				if (FreeIdentVar.equals("")){
					ident = translation;
				}else{
					ident = FreeIdentVar;
				}
				if (varType.containsKey(ident)){
					String tt = varType.get(ident).getInternalType();
					translation = tmp + translation + ".isSubset(";
					predicate.getRight().accept(this);
					translation += ")";*/
			}else
				if (assignmentTrans || predTrans || boolExp){
					String tmp = translation;
					translation = "";
					FreeIdentVar = "";
					predicate.getLeft().accept(this);
					String ident = "";
					if (FreeIdentVar.equals("")){
						ident = translation;
					}else{
						ident = FreeIdentVar;
					}
					translation = tmp + translation + ".equals(";
					predicate.getRight().accept(this);
					translation += ")";
				}
			break;
		case 	Formula.NOTEQUAL:
			Print("NOTEQUAL");
			if (predTrans){
				String tmp = translation;
				translation = "";
				FreeIdentVar = "";
				predicate.getLeft().accept(this);
				String ident = "";

				if (FreeIdentVar.equals("")){
					ident = translation;
				}else{
					ident = FreeIdentVar; 
				}
				translation = tmp + "!" + translation + ".equals(";
				predicate.getRight().accept(this);
				translation += ")";
			}
			break;
		case	Formula.LT:
			Print("LT");
			if (predTrans || assignmentTrans){
				translation += "(";
				predicate.getLeft().accept(this);
				translation += ").compareTo(";
				predicate.getRight().accept(this);
				translation += ") < 0";
			}else{
				predicate.getLeft().accept(this);
				predicate.getRight().accept(this);
			}
			break;
		case	Formula.LE:
			Print("LE");
			if (predTrans || assignmentTrans){
				translation += "(";
				predicate.getLeft().accept(this);
				translation += ").compareTo(";
				predicate.getRight().accept(this);
				translation += ") <= 0";
			}else{
				predicate.getLeft().accept(this);
				predicate.getRight().accept(this);
			}
			break;
		case	Formula.GT:
			Print("GT");
			if (predTrans || assignmentTrans){
				translation += "(";
				predicate.getLeft().accept(this);
				translation += ").compareTo(";
				predicate.getRight().accept(this);
				translation += ") > 0";
			}else{
				predicate.getLeft().accept(this);
				predicate.getRight().accept(this);
			}
			break;
		case	Formula.GE:
			Print("GE");
			if (predTrans || assignmentTrans){
				translation += "(";
				predicate.getLeft().accept(this);
				translation += ").compareTo(";
				predicate.getRight().accept(this);
				translation += ") >= 0";
			}else{
				predicate.getLeft().accept(this);
				predicate.getRight().accept(this);
			}
			break;
		case 	Formula.IN:
			Print("IN");
			if (predTrans || assignmentTrans){
				String tmp = translation;
				translation = "";
				FreeIdentVar = "";
				varFuncRel.push("??");

				predicate.getLeft().accept(this);


				inv_type_func_rel = null;

				/*if (predicate.getLeft().getTag() == Formula.FREE_IDENT &&
						isFunRel(predicate.getRight().getTag())){
					//there is some information already stored that can be used
					String vn = translation.replaceAll("machine.", "");
					if (varType.containsKey(vn)){
						inv_type_func_rel = varType.get(vn);
					}
				}*/

				String n = translation;
				tmp += "?left_info?" + ".has(" + n +")"; 
				translation = "";
				predicate.getRight().accept(this);

				translation = tmp.replaceAll("\\?left_info\\?", translation.replaceAll("\\?left_info\\?", n));
			}else if (boolExp){
				String tmp = translation;
				String tmp_currentVar = currentVar;
				translation = "";
				predicate.getRight().accept(this);
				translation = tmp + translation + ".has(";
				predicate.getLeft().accept(this);
				translation += ")";
				varFuncRel.clear();
				currentVar = tmp_currentVar;
			}
			break;
		case 	Formula.NOTIN:
			Print("NOTIN");
			if (predTrans || assignmentTrans){
				String tmp = translation;
				translation = "";
				FreeIdentVar = "";
				varFuncRel.push("??");

				predicate.getLeft().accept(this);


				inv_type_func_rel = null;
				/*if (predicate.getLeft().getTag() == Formula.FREE_IDENT &&
						isFunRel(predicate.getRight().getTag())){
					//there is some information already stored that can be used
					String vn = translation.replaceAll("machine.", "");
					if (varType.containsKey(vn)){
						inv_type_func_rel = varType.get(vn);
					}
				}*/

				String n = translation;
				tmp += "!?left_info?" + ".has(" + n +")"; 
				translation = "";
				predicate.getRight().accept(this);

				translation = tmp.replaceAll("\\?left_info\\?", translation.replaceAll("\\?left_info\\?", n));
			}else if (boolExp){
				String tmp = translation;
				String tmp_currentVar = currentVar;
				translation = "";
				predicate.getRight().accept(this);
				translation = tmp + "!" +translation + ".has(";
				predicate.getLeft().accept(this);
				translation += ")";
				varFuncRel.clear();
				currentVar = tmp_currentVar;
			}
			break;
		case 	Formula.SUBSET:
			Print("SUBSET");
			if (predTrans || assignmentTrans){
				String tmp = translation;
				String tmp_currentVar = currentVar;
				translation = "";
				varFuncRel.push("??");
				isaSubset = true;
				predicate.getLeft().accept(this);
				String v = translation;
				if (isFunRel(predicate.getRight().getTag())){
					translation = "";
					predicate.getRight().accept(this);
					translation = translation.replace("??", v);
					varFuncRel.clear();
				}else{
					translation = tmp + translation + ".isProperSubset(";
					predicate.getRight().accept(this);
					translation = translation.replace("??", v);
					translation += ")";
					varFuncRel.clear();
				}
				isaSubset = false;
				currentVar = tmp_currentVar;
			}else{
				predicate.getLeft().accept(this);
				predicate.getRight().accept(this);
			}
			break;
		case 	Formula.NOTSUBSET:
			Print("NOTSUBSET");
			if (predTrans || assignmentTrans){
				String tmp = translation;
				String tmp_currentVar = currentVar;
				translation = "";
				varFuncRel.push("??");
				isaSubset = true;
				translation += "!";
				predicate.getLeft().accept(this);
				String v = translation;
				if (isFunRel(predicate.getRight().getTag())){
					translation = "";
					predicate.getRight().accept(this);
					translation = translation.replace("??", v);
					varFuncRel.clear();
				}else{
					translation = tmp + translation + ".isProperSubset(";
					predicate.getRight().accept(this);
					translation = translation.replace("??", v);
					translation += ")";
					varFuncRel.clear();
				}
				isaSubset = false;
				currentVar = tmp_currentVar;
			}else{
				predicate.getLeft().accept(this);
				predicate.getRight().accept(this);
			}
			break;
		case 	Formula.SUBSETEQ:
			Print("SUBSETEQ");
			if (predTrans || assignmentTrans){
				String tmp = translation;
				String tmp_currentVar = currentVar;
				translation = "";
				varFuncRel.push("??");
				isaSubset = true;
				predicate.getLeft().accept(this);
				String v = translation;				
				if (isFunRel(predicate.getRight().getTag())){
					translation = "";
					predicate.getRight().accept(this);
					translation = translation.replace("??", v);
					varFuncRel.clear();
				}else{
					translation = tmp + translation + ".isSubset(";
					predicate.getRight().accept(this);
					translation = translation.replace("??", v);
					translation += ")";
					varFuncRel.clear();
				}
				isaSubset = false;
				currentVar = tmp_currentVar;
			}else{
				predicate.getLeft().accept(this);
				predicate.getRight().accept(this);
			}
			break;
		case 	Formula.NOTSUBSETEQ:
			Print("NOTSUBSETEQ");
			if (predTrans || assignmentTrans){
				String tmp = translation;
				String tmp_currentVar = currentVar;
				translation = "";
				varFuncRel.push("??");
				isaSubset = true;
				translation += "!";
				predicate.getLeft().accept(this);
				String v = translation;
				if (isFunRel(predicate.getRight().getTag())){
					translation = "";
					predicate.getRight().accept(this);
					translation = translation.replace("??", v);
					varFuncRel.clear();
				}else{
					translation = tmp + translation + ".isSubset(";
					predicate.getRight().accept(this);
					translation = translation.replace("??", v);
					translation += ")";
					varFuncRel.clear();
				}
				isaSubset = false;
				currentVar = tmp_currentVar;
			}else{
				predicate.getLeft().accept(this);
				predicate.getRight().accept(this);
			}
			break;
		}
	}

	@Override
	public void visitSimplePredicate(SimplePredicate predicate) {
		Print("\n visitSimplePredicate");
		switch (predicate.getTag()){
		case 	Formula.KFINITE:
			Print("KFINITE");
			String tmp = translation;
			translation = "";
			predicate.getExpression().accept(this);
			translation = tmp + translation + ".finite()";
			break;
		}
	}

	@Override
	public void visitUnaryPredicate(UnaryPredicate predicate) {
		Print("\n visitUnaryPredicate");
		translation += "!(";
		predicate.getChild().accept(this);
		translation += ")";

	}

	@Override
	public void visitExtendedExpression(ExtendedExpression expression) {
		Print("\n visitExtendedExpression");
		// source file: @noextend This class is not intended to be subclassed by clients.
	}

	@Override
	public void visitExtendedPredicate(ExtendedPredicate predicate) {
		Print("\n visitExtendedPredicate");
		// source file: @noextend This class is not intended to be subclassed by clients.
	}

	@Override
	public void visitPredicateVariable(PredicateVariable predVar) {
		Print("\n visitPredicateVariable");
		// source file: @noextend This class is not intended to be subclassed by clients.
	}

	public boolean as = false;
	public void Print(String s){
		if (as){
			System.out.println(s);
		}
	}

	public void Print(int s){
		if (as){
			System.out.println(s);
		}
	}

	public void PrintVarT(HashMap<String, ArrayList<String>> v){
		for (String var: v.keySet()){
			System.out.print(var + ": \n      ");
			for (String sv: v.get(var)){
				System.out.print(sv + "  -");
			}
			System.out.println();
			System.out.println();
			System.out.println();
		}
	}

	public void PrintVarT2(HashMap<String, JmlType> v){
		for (String var: v.keySet()){
			System.out.print(var + ": \n      ");
			System.out.println(v.get(var).getJmlType());
		}
		System.out.println();
		System.out.println();
		System.out.println();
	}

	public boolean isFunRel(int tag){
		return tag == Formula.REL ||
				tag == Formula.TREL ||
				tag == Formula.SREL ||
				tag == Formula.STREL ||
				tag == Formula.PFUN ||
				tag == Formula.TFUN ||
				tag == Formula.PINJ ||
				tag == Formula.TINJ ||
				tag == Formula.PSUR ||
				tag == Formula.TSUR ||
				tag == Formula.TBIJ; 
	}

	public Stack<JmlType> clone(Stack<JmlType> q){
		Stack<JmlType> res = new Stack<JmlType>();
		for (int t=0;t<q.size();t++){
			res.add(t,new JmlType(q.get(t)));
		}
		return res;
	}
}