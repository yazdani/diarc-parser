package com.discourse.TL_DL_parser.core;

import java.util.ArrayList;

public class Node {

    String word;
    String category;
    String lambda;
    String par_lambda;
    String dictlambda;
    Node parent = null;
    ArrayList<Node> children;
    int pos = 0;
    int head = 0;

    public Node getLchild() {
        try {
            if(children!=null && children.size()>0)
                return children.get(0);
            else
                return null;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setLchild(Node lchild) {
        children.add(0, lchild);
    }

    public Node getRchild() {
        try {
            if(children!=null && children.size()>1)
                return children.get(1);
            else
                return null;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setRchild(Node rchild) {
        children.add(1, rchild);
    }

    // unused
    public Node(String w) {
        word = w;
    }

    /**
     * in order to be compliant with the version for binary tree,
     * the two null's are added to stand for the left child and right child
     * @param w
     * @param p
     * @param h
     */
    // unused
    public Node(String w, int p, int h) {
        word = w;
        pos = p;
        head = h;
        children = new ArrayList<Node>();
        children.add(null);
        children.add(null);
    }

    /*add additional argument "dictl" as the lambda expression from dictionary defination for the "word",
     * which doesn't change through lambda conversion*/
    public Node(String w, String c, String dictl, String l){
        word = w;
        category = c;
        lambda = l;
        par_lambda = "";
        dictlambda = dictl;
        children = new ArrayList<Node>();
        children.add(null);
        children.add(null);
    }

    public Node(String w, String c, String dictl, String l, String p){
        word = w;
        category = c;
        lambda = l;
        par_lambda = p;
        dictlambda = dictl;
        children = new ArrayList<Node>();
        children.add(null);
        children.add(null);
    }

    public Node(String w, String c, String dictl, String l, int p, int h){
        word = w;
        category = c;
        lambda = l;
        par_lambda = "";
        dictlambda = dictl;
        pos = p;
        head = h;
        children = new ArrayList<Node>();
        children.add(null);
        children.add(null);
    }

    public Node(String w, String c, String dictl, String l, String p, int ip, int h){
        word = w;
        category = c;
        lambda = l;
        par_lambda = p;
        dictlambda = dictl;
        pos = ip;
        head = h;
        children = new ArrayList<Node>();
        children.add(null);
        children.add(null);
    }

    public int getPos() {
        return pos;
    }

    public void setPos(int i) {
        pos = i;
    }

    public int getHead() {
        return head;
    }

    public void setHead(int i) {
        head=i;
    }

    public Node getParent() {
        return parent;
    }

    public void setParent(Node parent) {
        this.parent = parent;
    }

    public String getLambda() {
        return lambda;
    }

    public void setLambda(String lambda) {
        this.lambda = lambda;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPar_lambda() {
        return par_lambda;
    }

    public void setPar_lambda(String par_lambda) {
        this.par_lambda = par_lambda;
    }

    public void add(Node n) {
        children.add(n);
    }

    public void println() {
        String output = word + " p: " + getPos() + " h: " + getHead();
        for (int n=0; n<children.size(); n++)
            output += " " + children.get(n).getWord();
        System.out.println(output);
    }

    public void print() {
        println();
        for (int n=0; n<children.size(); n++)
            children.get(n).print();
    }

    public String getDictlambda() {
        return dictlambda;
    }

    public void setDictlambda(String dl) {
        dictlambda = dl;
    }
}
