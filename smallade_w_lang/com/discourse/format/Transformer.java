package com.discourse.format;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.Predicate;
import com.Symbol;
import com.Term;

public class Transformer {

    // Transforms a temporal logic formula into a quantifier free first order logic
    // Assumes there is a starting time
	
    // Conventions for temporal and non-temporal operators
	
    // '&' - and 
    // '|' - or
    // '-' - not
    // '<>' - eventually
    // '[]' - always
    // '()' - next
    // '^' - until
    // '#' - exist a path
    // '*' - all paths
	
	
    //Finds the current maximum time within an expression
    private String max_time(String formula){
	String max = "0";
		
	int index = formula.indexOf(",t");
		
	while(index > 0){
	    String c = formula.substring(index+2, index + 3);
	    if(c.charAt(0) > max.charAt(0)){
		max = c;
	    }
	    formula = formula.substring(index + 1);
	    index = formula.indexOf(",t");
	}
		
	return max;
    }

    //Updates the current maximal time within the formula to the new value
    private String update(String formula, String t){
	String result = "";
	String max = max_time(formula);		
	result = formula.replaceAll(",t" + max, ",t" + t);
	return result;
    }
	
    //This assumes 'perfect' brackets around all binary connectives
    private String transform_formula(String formula, int t, int always){
	String result = "";
	String left_f = "", right_f = "";
	String operator = "";
		
		
	if(formula.startsWith("(")){
	    int count = 0;
	    //find the corresponding bracket
	    for(int i = 0; i < formula.length(); i++){
		if(formula.charAt(i) == '(') count++;
		if(formula.charAt(i) == ')') count--;
		if(count == 0){
		    left_f = formula.substring(1, i);
		    // Operator after (...) cannot be anything else but 'and', 'or', 'until'.
		    right_f = formula.substring(i+3, formula.length() - 1);
		    operator = formula.substring(i+1, i+2);
		    break;
		}				
	    }
			
	    //Parse accordingly
			
	    if(operator.equals("&") || operator.equals("|")){
		if(always == 1 || left_f.startsWith("[]") || right_f.startsWith("[]")){
		    String resultl = transform_formula(left_f, t, always);
		    String resultr = transform_formula(right_f, t, always);
					
		    String maxl = max_time(resultl);
		    String maxr = max_time(resultr);
					
		    if(maxl.charAt(0) >= maxr.charAt(0)){
			return resultl + " " + operator + " " + update(resultr,maxl);
		    }else{
			return update(resultl,maxr) + " " + operator + " " + resultr;
		    }
					
		}else{
		    return transform_formula(left_f, t, always) + " " + operator + " " + transform_formula(right_f, t, always);
		}
	    }
	    if(operator.equals("^")){
		if(always == 1){
		    return transform_formula(left_f, t,1) + " & " + transform_formula(right_f, t+1,1) + " & " + "t" + t + "<" + "t" + (t+1) + "&" +  transform_formula(right_f, t,1);
		}else{
		    return transform_formula(left_f, t,0) + " & " + transform_formula(right_f, t+1,0) + " & " + "t" + t + "<" + "t" + (t+1);
		}
	    }
			
	}	
		
	// We are starting with one of the other operators 
	if(formula.startsWith("-")){
	    if(formula.startsWith("(", 1)){
		formula = formula.substring(1, formula.length() - 1);
	    }
	    return  "-" + transform_formula(formula.substring(1), t, always);
	}
	if(formula.startsWith("<>")){
	    if(formula.startsWith("(", 2)){
		formula = formula.substring(1, formula.length() - 1);
	    }
	    return  transform_formula(formula.substring(2), t+1, always) + " & t" + t + "<" + "t" + (t+1);
	}

	if(formula.startsWith("()")){
	    if(formula.startsWith("(", 2)){
		formula = formula.substring(1, formula.length() - 1);
	    }
	    return  transform_formula(formula.substring(2), t+1, always) + " & next(t"+ t + ",t" + (t+1) + ")";
	}

	if(formula.startsWith("[]")){
	    if(formula.startsWith("(", 2)){
		formula = formula.substring(1, formula.length() - 1);
	    }
	    //Check if we have anymore temporal connectives
	    if(formula.indexOf("#") < 1 && formula.indexOf("*") < 1 && formula.indexOf("<>") < 1 && formula.indexOf("^") < 1 && formula.indexOf("()") < 1){
		return transform_formula(formula.substring(2), t, 1);
	    }else{				
		return transform_formula(formula.substring(2), t, 1);				
	    }
			
	}
	if(formula.startsWith("#")){
	    if(formula.startsWith("(", 1)){
		formula = formula.substring(1, formula.length() - 1);
	    }
			
	    //We need to differentiate in between state formulas and path formulas
	    //Currently, there is no 'set' method, we assume all formulas are STATE formulas
	    //This will be updated accordingly later on
	    if(always == 1){
		return  transform_formula(formula.substring(1), t+1, 1) + "&" + transform_formula(formula.substring(2), t+1,1) + "t" + t + "<" + "t" + (t+1);
	    }else{
		return  transform_formula(formula.substring(1), t+1,1) + " & t" + t + "<" + "t" + (t+1);
	    }
	}
	if(formula.startsWith("*")){
	    if(formula.startsWith("(", 1)){
		formula = formula.substring(1, formula.length() - 1);
	    }
	    //We need to find all the possible paths
	    //This is again not possible without the domain/external sources
	    //Hence this is treated as 'there' exist a path
	    if(always == 1){
		return  transform_formula(formula.substring(2), t+1, 1) + "&" + transform_formula(formula.substring(2), t+1,1) + "t" + t + "<" + "t" + (t+1);
	    }else{
		return  transform_formula(formula.substring(2), t+1,1) + " & t" + t + "<" + "t" + (t+1);
	    }
	}
		
	//This is a predicate, adjust it accordingly
		
	String newf = formula;
	String tt = "";
	if(always == 1){
	    tt = "(t" + t + ",t" + t +  ")";
	}else{
	    tt = "t" + t;
	}
		
	if(newf.indexOf("(") > 0){
	    if(newf.endsWith("!")){
		newf = newf.substring(0, newf.lastIndexOf(",")) + "," + tt + newf.substring(newf.lastIndexOf(","));
	    }else{
		newf = newf.substring(0, newf.lastIndexOf(")")) + "," + tt + newf.substring(newf.lastIndexOf(")")) + "!";
	    }
	}else{
	    if(!tt.startsWith("(")){
		tt = "(" + tt + ")";
	    }
	    if(newf.endsWith("!")){
		newf = newf + tt;
	    }else{
		newf = newf + tt + "!";
	    }
	}
		
	result = newf;
		
	return result;
    }

    public String transformLine(String current_line){

	String result = "";

	try{
	    if(current_line != null){
		    //Parse the formula

		    //Get rid of spaces
		    while(current_line.indexOf(" ") > 0){
			current_line = current_line.replaceAll(" ", "");
		    }
		    result = transform_formula(current_line, 0, 0).replaceAll("!", "");
	    }

	}catch(Exception e){
	    e.printStackTrace();
	}

	return result;
    }

    /**
     * only for single target, two parts: term and args
     */
    public List<Symbol> transforml2pred(String lambdaexp) {

        ArrayList<Symbol> preds = new ArrayList<Symbol>();
        /* term ( possible multiple args separated by , */
        Pattern p = Pattern.compile("([\\w|\\d|\\-|\\*]+)\\(([[\\w|\\d|\\(|\\)|\\-|\\*]+,]*[\\w|\\d|\\(|\\)|\\-|\\*]+)\\)"); //single predicate, can't deal with & multiple goals
        Matcher m = p.matcher(lambdaexp);
        
        Predicate pred=null;
        while(m.find()){
            if(m.groupCount()!=2)
                System.out.println("group count error on lv1: " + m.groupCount()
				   + "only deal with TERM(arg1,arg2,...) but input is " + lambdaexp);
            ArrayList<Symbol> args = getSymbol(m.group(2));
            pred = new Predicate(m.group(1), args);

            preds.add(pred);
        }

        if(pred==null)
            /* when there's no predicate matched, deal with symbol as string only, return predicate with no args*/
            preds.add(new Term(lambdaexp));

        return preds;
    }

    /**
     * input is args with format: aa(bbb(cc,ff)), ee(ddd)
     */
    public ArrayList<Symbol> getSymbol(String argstr) {

        String argstrclr = argstr.replaceAll(" ", "");
        ArrayList<String> args = new ArrayList<String>();

        int idx = -1;
        idx=argstrclr.indexOf(",");

        for(;idx>0;) {

            /* if it's not term*/
            if(argstrclr.substring(0, idx).indexOf("(")!=-1 && argstrclr.charAt(idx-1)!=')') {
                //System.out.println("not term: " + argstrclr.substring(0, idx));
                idx = argstrclr.indexOf(",", idx+1);
            } else {
                //System.out.println("add arg: " + argstrclr.substring(0,idx-1));
                args.add(argstrclr.substring(0,idx));
                argstrclr = argstrclr.substring(idx+1, argstrclr.length());
                idx=argstrclr.indexOf(",");
            }
        }
        args.add(argstrclr);

        ArrayList<Symbol> preds = new ArrayList<Symbol>();
        for(int i = 0; i<args.size(); i++) {
            List<Symbol> argpreds = transforml2pred(args.get(i));
            /* when there's no predicate matched, deal with symbol as string only*/
            if(argpreds.size()==0) {
                preds.add(new Symbol(args.get(i)));
            } else {
                if(argpreds.size()!=1) {
                    System.out.println("can't deal with args as mult-targets: " + argpreds.size());
                } else {
                    preds.add(argpreds.get(0));
                }
            }
        }
        
        return preds;
    }
    
    public static void drawPred(List<Symbol> preds, int level) {

        for(int i =0; i<preds.size(); i++) {
            for(int j = 0; j<level; j++){
                System.out.print("  ");
            }
            if(level>0)
                System.out.print("|_");
            System.out.println("(" + preds.get(i).getClass().getSimpleName() + ")" + preds.get(i).getName());
            if(preds.get(i) instanceof Predicate) {
                drawPred(((Predicate)preds.get(i)).getArgs(), level+1);
            }
        }
    }
}
