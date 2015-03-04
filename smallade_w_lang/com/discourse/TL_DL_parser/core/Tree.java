package com.discourse.TL_DL_parser.core;

public class Tree {

    Node root;
    String sentence;

    public Tree(String s){
        this.root = null;
        this.sentence = s;
    }

    public Tree(Node r, String s){
        this.root = r;
        this.sentence = s;
    }

    public Node getRoot() {
        return root;
    }

    public void setRoot(Node root) {
        this.root = root;
    }

    public String getSentence() {
        return sentence;
    }

    public void setSentence(String sentence) {
        this.sentence = sentence;
    }

}
