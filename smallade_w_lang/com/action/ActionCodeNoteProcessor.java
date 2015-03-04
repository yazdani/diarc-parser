/**
 * ADE 1.0 
 * Copyright 1997-2012 HRILab (http://hrilab.org/) 
 * 
 * All rights reserved.  Do not copy and use without permission. 
 * For questions contact Matthias Scheutz at mscheutz@cs.tufts.edu
 *
 * ActionCodeNoteProcessor.java
 *
 * Last update: October 2012
 *
 * @author Paul Schermerhorn
 *
 */

package com.action;

import java.util.Set;
import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.*;

@SupportedAnnotationTypes("com.action.ActionCodeNote")
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class ActionCodeNoteProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) {
            for (TypeElement te : annotations) {
                final Set< ? extends Element> elts = roundEnv.getElementsAnnotatedWith(te);
                for (Element elt : elts) {
                    String aval = elt.getAnnotation(com.action.ActionCodeNote.class).value();
                    String cname = roundEnv.getRootElements().toArray()[0].toString();
                    String ename = elt.getSimpleName().toString();
                    System.err.println("Note: ("+cname+"."+ename+") "+aval);
                }
            }
        }
        return true;
    }

}
// vi:ai:smarttab:expandtab:ts=8 sw=4
