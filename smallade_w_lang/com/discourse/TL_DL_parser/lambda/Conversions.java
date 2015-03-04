package com.discourse.TL_DL_parser.lambda;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Conversions {
    
    public static String alpha(String e, String v1, String v2){
        return e.replaceAll(v1, v2);
    }
    
    public static String beta(String e1, String e2){
        String s = "";
        
        
        if(e1.length() < 1){
            return e2;
        }
        
        if(e2.length() < 1){
            return e1;
        }
        
        //Locate the symbol to replace, # - lambda
        //Get rid of extra spaces
        e1 = e1.replace(" ", "");
        /*
         * System.out.println("e1 = " + e1);
         * //Get lambda symbol out and get the variable to replace
         * int lambdaloc = e1.indexOf("#");
         * System.out.println("lambdaloc = " + lambdaloc);
         * s = e1.substring(lambdaloc+1,lambdaloc+2);
         * System.out.println("s = " + s);
         * e1 = e1.substring(0,lambdaloc)+e1.substring(lambdaloc+2,e1.length());
         * System.out.println("e1 = " + e1);
         */
        
        e1 = e1.substring(1, e1.length());
        //System.out.println("e1 = " + e1);
        s = e1.substring(0, 1);
        //System.out.println("s = " + s);
        e1 = e1.substring(1, e1.length());
        //System.out.println("e1 = " + e1);
        
        //Replace
        try{
            //if(!s.equals(")")){ //}
            //System.out.println("e1: " + e1 + " e2: "+e2 + " s: "+s);
            /*if there is an variable at the end of the definition, add an non-word charactor at the end and del it after the replacement*/
            if(e1.matches(".*\\W"+s)) {
                e1 = e1+"#";
                Pattern p = Pattern.compile("([\\W])" + s + "([\\W])");
                String replacem = "$1"+e2+"$2";
                Matcher m = p.matcher(e1);
                e1 = m.replaceAll(replacem);
                e1 = e1.substring(0, e1.length()-1);
            } else {
                Pattern p = Pattern.compile("([\\W])" + s + "([\\W])");
                String replacem = "$1"+e2+"$2";
                Matcher m = p.matcher(e1);
                e1 = m.replaceAll(replacem);
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        
        //Get rid of extra stuff
        if(e1.charAt(0) == '.'){
            e1 = e1.substring(1);
        }
        
        if(e1.charAt(0) == '('){
            e1 = e1.substring(1);
            if(e1.charAt(e1.length() - 1) == ')'){
                e1 = e1.substring(0, e1.length() - 1);
            }
        }
        
        return e1;
        
    }
    
    
    /** Performs the final application step for lambda formulas
     *
     * @param e1
     * @return
     */
    public static String update(String e1){
        String s = "";
        String ex = "";
        String var = "";
        String t = e1;
        
        int i =0;
        while(i < e1.length()){
            //for(int i = 0; i < e1.length(); i++){ //}
            if(t.indexOf('#') < 0){
                if(i < 1){
                    s = e1;
                }
                if(s.indexOf('#') > -1){
                    s = update(s.replaceAll("@", "@;"));
                    s = s.replaceAll(";", "");
                }
                break;
            }else{
                if(e1.charAt(i) == '#'){
                    //Look for @
                    String s1 = e1.substring(i);
                    int j = s1.indexOf('@');
                    if(j > -1){
                        if(s1.charAt(j+1)== '('){
                            ex = s1.substring(j+1, s1.indexOf(')'));
                        }else{
                            if(s1.charAt(j+1)== '#' || s1.charAt(j+1)== ';'){
                                String ss = s1.substring(j+1);
                                if(ss.indexOf(')') > 0){
                                    ex = ss.substring(0, ss.indexOf(")"));
                                }else{
                                    ex = s1.substring(j+1, s1.length());
                                }
                            }else{
                                ex = s1.substring(j+1, j+2);
                            }
                        }
                    }
                    
                    int k = s1.indexOf('.');
                    if(k > -1){
                        var = s1.substring(1,k);
                    }
                    
                    if(t.charAt(0) != '#'){
                        s = s + t.substring(0, t.indexOf("#"));
                    }
                    
                    if(t.indexOf("@" + ex) + ex.length() + 1 < t.length()){
                        t = t.substring(t.indexOf("@" + ex) + ex.length() + 1);
                    }else{
                        
                        t = "";
                    }
                    
                    if (j>-1) {
                        //					System.out.println("k+1 = " + k+1 + ", j = " + j);
                        s1 = s1.substring(k+1,j);
                    }
                    else
                        s1=s1.substring(k+1);
                    
                    if(s.indexOf(".") > 0){
                        //s = s.substring(0, s.length() - s1.length() + 1);
                        s = s.substring(0, s.indexOf("#"));
                        s = s + s1.replaceAll(var, ex);
                    }else{
                        s = s + s1.replaceAll(var, ex);
                    }
                    
                    int l = e1.length();
                    e1 = s + t;
                    i -= l - e1.length();
                    
                    if(i < 0){
                        i = 0;
                    }
                }
            }
            i++;
        }
        
        String mul = s + t;
        
        /* handle *: multiply function */
        //Pattern p = Pattern.compile("[([\\d]+)\\*]+([\\d]+)");
        Pattern p = Pattern.compile("[-?[\\d]+\\*]+-?[\\d]+");
        Matcher m = p.matcher(e1);
        while(m.find()){
            String occur = m.group();
            int start = m.start();
            int end = m.end();
            
            //System.out.println(occur + " which is " + mul.substring(start, end) + " " +occur.indexOf("*"));
            //System.out.println(start + " " + end);
            /*simple split by * */
            String[] muls = occur.split("\\*");
            int result = 1;
            for(int j = 0; j<muls.length; j++) {
                try{
                    //System.out.println(muls[j] + " " + result);
                    int cur = Integer.parseInt(muls[j]);
                    result = result * cur;
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
            //System.out.println("replace " + mul + "\n" + occur + " with " + Integer.toString(result));
            mul = mul.substring(0, start)+  Integer.toString(result) + mul.substring(end, mul.length());
        }
        
        return mul;
    }

}
