package com.discourse.TL_DL_parser.nl_ltl_dl;

import java.util.List;
import com.discourse.TL_DL_parser.core.Dictionary;
import com.discourse.TL_DL_parser.core.Tree;
import com.discourse.TL_DL_parser.lambda.Conversions;

public class Eval {
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        
        if(args.length != 2){
            System.out.println("Please provide an input file and dictionary!");
        }else{
            Dictionary d = new Dictionary();
            d.parse(args[0], d); // PWS: this will not search jars correctly (wrong path root)
            
            Parser p = new Parser();
            
            List<Tree> trees = p.Parse(args[1], d);
            
            for(int i=0; i < trees.size();i++){
                Tree t = trees.get(i);
                if(t != null){
                    String s = Conversions.update(t.getRoot().getLambda());
                    s = s.replaceAll("`", ",");
                    String ss = Conversions.update(t.getRoot().getPar_lambda());
                    ss = ss.replaceAll("`", ",");
                    System.out.println("TL: " + s + " DL: " + ss);
                }else{
                    int j = i + 1;
                    System.out.println("In Eval - Cannot parse sentence number " + j + "!");
                }
            }
            
            //We are done
            //System.out.println("Done!");
        }
    }

}
