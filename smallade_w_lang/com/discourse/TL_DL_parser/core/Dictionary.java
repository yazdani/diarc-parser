package com.discourse.TL_DL_parser.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import com.Predicate;

public class Dictionary {

    /**
     * the built-in predicates
     */
    List<Predicate> builtin = new ArrayList<Predicate>();

    List<Entry> entries;

    public String getEntry(String s) {
        for (int n=0; n<entries.size(); n++)
            if (entries.get(n).getWord() != null && s.compareTo(entries.get(n).getWord())==0)
                return entries.get(n).toString();
        return "Entry not found";
    }

    /**
     * prints text dictionary
     * @author Rehj
     */
    public void print() {
        for (Entry entry:entries)
            entry.print();
    }

    public Entry getEntryEntry(String s) {
        for (int n=0; n<entries.size(); n++)
            if (entries.get(n).getWord() != null && s.compareTo(entries.get(n).getWord())==0)
                return entries.get(n);
        return null;
    }

    public Dictionary() {
        this.entries = new ArrayList<Entry>();
        /**
         * add here if need to define predicates outside the dictionary file
         */
        //builtin.add(new Predicate("facing", 1));
    }

    public Dictionary(List<Entry> l) {
        this.entries = new ArrayList<Entry>(l);
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public void setEntries(List<Entry> entries) {
        this.entries = entries;
    }

    public List<Predicate> getBuiltin() {
        return builtin;
    }

    //Tries to fill the dictionary with the data from file filename, returns non zero on success
    public int parse(String filename, Object owner) {

        String current_line = "";
        String savename = filename;

        try {
            BufferedReader in;
            if ((new File(filename)).exists()) {
                FileReader f = new FileReader(filename);
                in = new BufferedReader(f);
            } else {
                InputStream is;
                String newfile;
                System.out.println("Trying " + filename);
                while ((is = owner.getClass().getResourceAsStream(filename)) == null) {
                    newfile = filename;
                    filename = filename.substring(filename.indexOf('/') + 1);
                    if (filename.equals(newfile)) {
                        System.err.println("ERROR: did not find dictionary file: "+savename);
                        break;
                    }
                    System.out.println("Trying " + filename);
                }
                InputStreamReader isr = new InputStreamReader(is);
                in = new BufferedReader(isr);
            }
            current_line = in.readLine();

            while (current_line != null) {

                String s = "";
                Entry e = new Entry();

                // the word
                s = current_line.substring(0, current_line.indexOf(":"));

                e.setWord(s);

                // Get rid of extra spaces
                current_line = current_line.replaceAll(" ", "");


                // Get the categories and lambdas and anything else, if anything runs out, keep adding the last entry
                current_line = current_line.substring( current_line.indexOf(":") + 1,  current_line.length());

                // the category (syntax type)
                String cat = current_line.substring(0,  current_line.indexOf(":"));
                // the lambda expressions, ends up being TL (goal) lambda
                String lam = current_line.substring(current_line.indexOf(":") + 1,  current_line.length());

                // DL (action) lambda
                String extra = "";

                //Check if we have more then 1 type of lambda expression
                //Atm we support up to 2 different types
                if (lam.indexOf(":") > 0) {
                    extra = lam.substring(lam.indexOf(":") + 1, lam.length());

                    lam = lam.substring(0, lam.indexOf(":"));
                }

                // We want to handle different number of lambdas for each category
                while (cat.length() > 0) {
                    // PWS: looks like trouble to me: if more than one category and more
                    // than one TL lambda, add the first category and (if there's more than
                    // one TL lambda) the first TL lambda, but ignore the DL lambda; only add
                    // DL lambda (even if it's a list of lambdas) if there's only one TL lambda,
                    // and note that the DL lambda is also processed below...
                    if (cat.indexOf(",") > 0) {
                        // add first category
                        e.addCategory(cat.substring(0, cat.indexOf(",")));
                        // trim first category
                        cat = cat.substring(cat.indexOf(",") + 1, cat.length());

                        if (lam.indexOf(",") > 0) {
                            // add first TL lambda
                            e.addLambda(lam.substring(0, lam.indexOf(",")));
                            // trim first TL lambda
                            lam = lam.substring(lam.indexOf(",") + 1, lam.length());
                        } else {
                            e.addLambda(lam);
                            /**
                             * don't add parLambda if there's none
                             */
                            if (!extra.isEmpty())
                                e.addParLambda(extra);
                        }
                        // PWS: again, trouble: this is the last category, so grab the first TL
                        // lambda, ignore the rest
                    } else {
                        if (lam.indexOf(",") > 0) {
                            e.addLambda(lam.substring(0, lam.indexOf(",")));
                            lam = lam.substring(lam.indexOf(",") + 1, lam.length());
                            e.addCategory(cat);
                        } else {
                            e.addLambda(lam);
                            e.addCategory(cat);
                            //Last entries
                            cat = "";
                        }

                    }

                }

                //Process extra lambda expressions if applicable
                // PWS: now just add all the rest of the DL lambdas
                while (extra.length() > 0) {
                    if (extra.indexOf(",") > 0) {
                        String ss = extra.substring(0, extra.indexOf(","));
                        e.addParLambda(ss);
                        extra = extra.substring(extra.indexOf(",")+1, extra.length());
                    } else {
                        e.addParLambda(extra);
                        extra = "";
                    }
                }

                this.entries.add(e);
                current_line = in.readLine();

            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }

        return 1;
    }

}
