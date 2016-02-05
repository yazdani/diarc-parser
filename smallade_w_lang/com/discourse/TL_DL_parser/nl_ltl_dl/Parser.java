package com.discourse.TL_DL_parser.nl_ltl_dl;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import com.discourse.TL_DL_parser.core.Dictionary;
import com.discourse.TL_DL_parser.core.Entry;
import com.discourse.TL_DL_parser.core.Node;
import com.discourse.TL_DL_parser.core.Tree;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.discourse.TL_DL_parser.lambda.Conversions;

public class Parser {

    private Tree rec_parse(Tree t, List<Node> n, Dictionary d, String sen){
        if(n.size() == 1){
            t = new Tree(n.get(0), sen);
            return t;
        }else{
            //Try pairing up the nodes using forward and backward application
            //Other rules are not included in this version of the parser

            for(int i = 0; i < n.size() - 1; i++){
                Node n1 = n.get(i);
                Node n2 = n.get(i+1);
                String c1 = n1.getCategory();
                String c2 = n2.getCategory();

                String cat1 = "", cat2 = "";

                //Get the right categories

                //Category 1
                int count = 0;
                for(int j = 0; j < c1.length(); j++){
                    if(c1.charAt(j) == '('){
                        count++;
                    }
                    if(c1.charAt(j) == ')'){
                        count--;
                    }

                    if(count == 0){
                        if(c1.length() == 1){
                            cat1 = c1;
                            break;
                        }
                        if(j == c1.length() - 1){
                            c1 = c1.substring(1, j);
                            if(c1.indexOf("/") > 0){
                                cat1 = c1.substring(c1.indexOf("/"), c1.length());
                            }else{
                                cat1 = c1;
                            }
                            break;
                        }
                        if(j > 0){
                            cat1 = c1.substring(j+1);
                            break;
                        }else{
                            if(c1.indexOf("/") > 0){
                                cat1 = c1.substring(c1.indexOf("/"));
                            }else{
                                cat1 = c1;
                            }
                            break;
                        }
                    }
                }

                //Category 2
                count = 0;
                for(int j = 0; j < c2.length(); j++){
                    if(c2.charAt(j) == '('){
                        count++;
                    }
                    if(c2.charAt(j) == ')'){
                        count--;
                    }

                    if(count == 0){
                        if(c2.length() == 1){
                            cat2 = c2;
                            break;
                        }
                        if(j == c2.length() - 1){
                            c2 = c2.substring(1, j);
                            if(c2.indexOf("|") > 0){
                                cat2 = c2.substring(c2.indexOf("|"), c2.length());
                            }else{
                                cat2 = c2;
                            }
                            break;
                        }
                        if(j > 0){
                            cat2 = c2.substring(j+1);
                            break;
                        }else{
                            if(c2.indexOf("|") > 0){
                                cat2 = c2.substring(c2.indexOf("|"));
                            }else{
                                cat2 = c2;
                            }
                            break;
                        }
                    }
                }


                //Check combinations

                //Brackets can cause issues, so make sure they are around every category
                //Need to to it for both categories

                if(cat1.length() > 1 && cat1.charAt(0) != '('){
                    if(cat1.charAt(1) != '(' && cat1.indexOf("/") > -1){
                        cat1 = cat1.substring(0,1) + "(" + cat1.substring(1) + ")";
                    }else{
                        if(cat1.charAt(1) != '('){
                            cat1 = "(" + cat1 + ")";
                        }
                    }
                }

                if(cat2.length() > 1 && cat2.charAt(0) != '('){
                    if(cat2.charAt(1) != '('&& cat2.indexOf("|") > -1){
                        cat2 = cat2.substring(0,1) + "(" + cat2.substring(1) + ")";
                    }else{
                        if(cat2.charAt(1) != '('){
                            cat2 = "(" + cat2 + ")";
                        }
                    }
                }

                if(c1.charAt(0) != '('){
                    c1 = "(" + c1 + ")";
                }

                if(c2.charAt(0) != '('){
                    c2 = "(" + c2 + ")";
                }

                //Try Forward application
                if(cat1.charAt(0) == '/' && cat1.substring(1).equals(c2)){
                    //Recurse

                    //Take care of brackest again
                    if(c1.charAt(0) == '(' && c1.charAt(c1.length() - 1) == ')'){
                        c1 = c1.substring(0, c1.lastIndexOf('('))+ c1.substring(c1.lastIndexOf('(')+1,c1.length() - 1);
                    }

                    if(c1.charAt(0) == '(' && c1.charAt(c1.length() - 1) == ')'){
                        c1 = c1.substring(0, c1.lastIndexOf('('))+ c1.substring(c1.lastIndexOf('(')+1,c1.length() - 1);
                    }

                    if(c1.charAt(c1.length() - 1) != ')'){
                        cat1 = "/" + cat1.substring(2, cat1.length() - 1);
                    }

                    //Create new node, depends on if we have paralled lambdas or not
                    Node nn = null;
                    if(n1.getPar_lambda().length() > 1){
                        nn = new Node(n1.getWord()+ " " + n2.getWord(), c1.substring(0, c1.lastIndexOf(cat1)), "", Conversions.beta(n1.getLambda(), n2.getLambda()), Conversions.beta(n1.getPar_lambda(), n2.getPar_lambda()));
                    }else{
                        /**
                         * when there's no lambda expressions, need to "guess" from the syntax
                         * */
                        if(n1.getLambda().length()==0) {
                            String genpos = genPosition(n1).replaceAll(" ", "");
                            n1.setLambda(genpos);//correct: # x. x @ hallway wrong: # x. hallway(x)
                            n1.setDictlambda(genpos);
                        }
                        if(n2.getLambda().length()==0) {
                            String genpos = genPosition(n2).replaceAll(" ", "");
                            n2.setLambda(genpos);
                            n2.setDictlambda(genpos);
                        }
                        nn = new Node(n1.getWord()+ " " + n2.getWord(), c1.substring(0, c1.lastIndexOf(cat1)), "", Conversions.beta(n1.getLambda(), n2.getLambda()));
                    }
                    nn.setLchild(n1);
                    nn.setRchild(n2);
                    n1.setParent(nn);
                    n2.setParent(nn);

                    List<Node> new_n = new ArrayList<Node>(n);
                    new_n.remove(i);
                    new_n.remove(i);
                    new_n.add(i, nn);
                    String current = Conversions.update(new_n.get(i).getLambda());
                    //System.out.println("CURRENT: " + current);

                    Tree t1 = rec_parse(t,new_n,d,sen);
                    if(t1 != null){
                        return t1;
                    }
                }

                //Try Backward
                if(cat2.charAt(0) == '|' && cat2.substring(1).equals(c1)){
                    //Recurse

                    //Swap nodes
                    n2 = n.get(i);
                    n1 = n.get(i+1);
                    c1 = n1.getCategory();
                    c2 = n2.getCategory();


                    //Take care of brackest again
                    if(c1.charAt(0) == '(' && c1.charAt(c1.length() - 1) == ')'){
                        c1 = c1.substring(0, c1.lastIndexOf('('))+ c1.substring(c1.lastIndexOf('(')+1,c1.length() - 1);
                    }

                    if(c1.charAt(0) == '(' && c1.charAt(c1.length() - 1) == ')'){
                        c1 = c1.substring(0, c1.lastIndexOf('('))+ c1.substring(c1.lastIndexOf('(')+1,c1.length() - 1);
                    }

                    if(c1.charAt(c1.length() - 1) != ')'){
                        cat2 = "|" + cat2.substring(2, cat2.length() - 1);
                    }

                    //Create new node
                    //Create new node, depends on if we have paralled lambdas or not
                    Node nn = null;
                    if(n1.getPar_lambda().length() > 1){
                        nn = new Node(n2.getWord()+ " " + n1.getWord(), c1.substring(0, c1.lastIndexOf(cat2)), "", Conversions.beta(n1.getLambda(), n2.getLambda()), Conversions.beta(n1.getPar_lambda(), n2.getPar_lambda()));
                    }else{
                        /**
                         * when there's no lambda expressions, need to "guess" from the syntax
                         * */
                        if(n1.getLambda().length()==0) {
                            String genpos = genPosition(n1).replaceAll(" ", "");
                            n1.setLambda(genpos);//correct: # x. x @ hallway wrong: # x. hallway(x)
                            n1.setDictlambda(genpos);
                        }
                        if(n2.getLambda().length()==0) {
                            String genpos = genPosition(n2).replaceAll(" ", "");
                            n2.setLambda(genpos);
                            n2.setDictlambda(genpos);
                        }
                        nn = new Node(n2.getWord()+ " " + n1.getWord(), c1.substring(0, c1.lastIndexOf(cat2)), "", Conversions.beta(n1.getLambda(), n2.getLambda()));
                    }
                    nn.setLchild(n1);
                    nn.setRchild(n2);
                    n1.setParent(nn);
                    n2.setParent(nn);

                    List<Node> new_n = new ArrayList<Node>(n);
                    new_n.remove(i);
                    new_n.remove(i);
                    new_n.add(i, nn);

                    String current = Conversions.update(new_n.get(i).getLambda());
                    //System.out.println("CURRENT: " + current);

                    Tree t1 = rec_parse(t,new_n,d,sen);
                    if(t1 != null){
                        return t1;
                    }
                }
            }
        }

        return null;
    }

    private Tree rec_assign(Tree t, List<Node> n, String s, Dictionary d, String sen){
        if(s.length() < 2){
            //All categories have been assigned, try parsing it
            return rec_parse(t,n,d,sen);
        }

        if(s.length() > 0){
            //Get the word
            String word = s.substring(0, s.indexOf(" "));


            //search the dictionary for categories and lambdas, and try each one
            for(int i = 0; i < d.getEntries().size(); i++){
                Entry e = d.getEntries().get(i);
                if(word.equalsIgnoreCase(e.getWord())){
                    //We have the word, now we need to try all categories for it.
                    //We pick them in the order given by lexicon
                    for(int j = 0; j < e.getCategories().size(); j++){
                        Node node = null;
                        if(e.getPar_lambda().size() > 0){
                            node = new Node(word, e.getCategories().get(j), e.getLambda().get(j), e.getLambda().get(j), e.getPar_lambda().get(j));
                        }else{
                            node = new Node(word, e.getCategories().get(j), e.getLambda().get(j), e.getLambda().get(j));
                        }
                        //Try this category
                        n.add(node);
                        Tree t1 = rec_assign(t,n,s.substring(s.indexOf(" ") + 1, s.length()),d,sen);
                        if(t1 == null){
                            n.remove(n.size() - 1);
                        }else{
                            return t1;
                        }
                    }

                    // PWS: want to check other definitions (e.g., for "go straight" vs. "go to room7")
                    //break;
                }
            }

        }

        return null;
    }


    //Try parsing the provided sentences one by one
    public List<Tree> Parse(String filename, Dictionary d){
        try{
            String current_line = "";

            FileReader f = new FileReader(filename);
            BufferedReader in = new BufferedReader(f);
            current_line = in.readLine();

            List<Tree> trees = new ArrayList<Tree>();


            while(current_line != null){

                //Remove end period, we assume there are no other periods given
                String sen = current_line.replace(".", "");
                current_line = sen;
                Tree t = new Tree(current_line);

                List<Node> nodes = new ArrayList<Node>();

                //For each word create a node
                //Since we do not know the exact category yet, we need to try all
                //This is done recursively
                long starttime = System.currentTimeMillis();
                t = rec_assign(t, nodes, sen + " ", d, sen);
                long durtime = System.currentTimeMillis() -starttime;
                if (durtime>1000)
                    System.out.println(sen + "      time(ms): " + durtime );


                current_line = in.readLine();
                trees.add(t);
            }
            return trees;


        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }


    //Try parsing the provided sentence
    public List<Tree> ParseLine(String current_line, Dictionary d){
	//   try{
            List<Tree> trees = new ArrayList<Tree>();
	    System.out.println("ParseLine");
            //Remove end period, we assume there are no other periods given
            String sen = current_line.replace(".", "");
            current_line = sen;
            Tree t = new Tree(current_line);
	    System.out.println("ParseLine2");
            List<Node> nodes = new ArrayList<Node>();

            //For each word create a node
            //Since we do not know the exact category yet, we need to try all
            //This is done recursively

            t = rec_assign(t, nodes, sen + " ", d, sen);
	    System.out.println("ParseLine3");
            // CC: added for testing
            if(current_line.equalsIgnoreCase("test")){
                Node n = new Node("test");
                n.setLambda("#x.x@testedMoveTo(?actor)");
                n.setPar_lambda("#x.x@testMoveTo(?actor)");
                t = new Tree("test");
                t.setRoot(n);
            }

            trees.add(t);
            return trees;


	    //   }catch(Exception e){
	    //  e.printStackTrace();
	    //  return null;
	    // }
    }

    /**
     * create dumb lambda expressions for unknown words
     * @param n
     * @return
     */
    private String genPosition(Node n) {
        /**
         * number of arguments in the lambda expression =
         * the number of depending parts in the CCG tag
         */
        int argnum = countOccurence(n.getCategory(), "[//|/|]");
        String lambdaexp = createLambdaexp("zz", argnum);
        System.out.println(lambdaexp);
        return lambdaexp;
    }

    /**
     * count the occurence of the regular expression in INPUT string
     */
    private int countOccurence(String INPUT, String REGEX) {
        Pattern p = Pattern.compile(REGEX);
        Matcher m = p.matcher(INPUT); // get a matcher object
        int count = 0;
        while(m.find()) {
            count++;
        }
        return count;
    }

    /**
     * create lambda expression based on the name of predicate and number of arguments
     * @param name
     * @param argnum
     * @return
     */
    private String createLambdaexp(String name, int argnum) {
        String argstr = "";
        String funcstr = name + "(";
        for(int idx=1; idx<=argnum; idx++) {
            argstr += "#"+idx + ".";
            funcstr += "" + idx;
            if(idx!=argnum)
                funcstr +=",";
        }
        return argstr + funcstr + ")";
    }
}
