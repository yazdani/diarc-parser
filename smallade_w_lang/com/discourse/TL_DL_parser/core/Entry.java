package com.discourse.TL_DL_parser.core;

import java.util.ArrayList;
import java.util.List;

public class Entry {

    String word;
    List<String> categories;
    List<String> lambda;
    List<String> par_lambda;

    public void print() {
        System.out.print(getWord() + ": ");
        for (int n=0; n<categories.size()-1; n++)
            System.out.print(categories.get(n) + ", ");
        System.out.print(categories.get(categories.size()-1) + " : ");
        for (int n=0; n<lambda.size()-1; n++)
            System.out.print(lambda.get(n) + ", ");
        System.out.println(lambda.get(lambda.size()-1));
    }

    public Entry(){
        word = "";
        categories = new ArrayList<String>();
        lambda = new ArrayList<String>();
        par_lambda = new ArrayList<String>();
    }

    // unused
    public Entry(String w, List<String> c, List<String> l){
        this.word = w;
        this.categories = c;
        this.lambda = l;
        this.par_lambda = null;
    }

    // unused
    public Entry(String w, List<String> c, List<String> l, List<String> p){
        this.word = w;
        this.categories = c;
        this.lambda = l;
        this.par_lambda = p;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public List<String> getLambda() {
        return lambda;
    }

    public void setLambda(List<String> lambda) {
        this.lambda = lambda;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public List<String> getPar_lambda() {
        return par_lambda;
    }

    public void setPar_lambda(List<String> par_lambda) {
        this.par_lambda = par_lambda;
    }

    public void addCategory(String s){
        this.categories.add(s);
    }

    public void addLambda(String s){
        this.lambda.add(s);
    }

    public void addParLambda(String s){
        this.par_lambda.add(s);
    }

}
