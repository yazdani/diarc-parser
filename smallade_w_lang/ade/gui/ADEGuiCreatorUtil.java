/**
 * Agent Development Environment (ADE)
 *
 * @version 1.0
 * @author Matthias Scheutz
 *
 * Copyright 1997-2013 Matthias Scheutz and the HRILab Development Team
 * All rights reserved.  For information or questions, please contact
 * the director of the HRILab, Matthias Scheutz, at mscheutz@gmail.com
 * 
 * Redistribution and use of all files of the ADE package, in source and
 * binary forms with or without modification, are permitted provided that
 * (1) they retain the above copyright notice, this list of conditions
 * and the following disclaimer, and (2) redistributions in binary form
 * reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials
 * provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDER "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT HOLDER OR ANY
 * OF THE CONTRIBUTORS TO THE ADE PROJECT BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING
 * IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.

 * Note: This license is equivalent to the FreeBSD license.
 */
package ade.gui;

import ade.ADEComponent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * A utility class just to simplify the creation of GUI visualizations
 */
public class ADEGuiCreatorUtil {

    public static ADEGuiPanel createGuiPanel(ADEGuiCallHelper guiCallHelper,
            ADEGuiVisualizationSpecs.Item aSpecItem) {
        // The ADEGuiPanel's constructor accepts a single ADEGuiCallHelper,
        //     parameter, followed by any number of arguments.  
        try {
            Object[] allParams = concatenateArrays(
                    new Object[]{guiCallHelper}, aSpecItem.additionalArguments);
            Constructor<?> constructor = ReflectionUtil.findConstructor(aSpecItem.classType, allParams);

            // create the panel based on the constructor.  if exceptions during creation, don't bother continuing:
            return (ADEGuiPanel) constructor.newInstance(allParams);

        } catch (Exception e) {
            System.err.println("Could not create visualization due to the following exception:  \n" + e);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * creates a GUICallHelper, for use by a GUI panel.
     *
     * @param component
     * @return
     */
    public static ADEGuiCallHelper createCallHelper(final ADEComponent component) {
        ADEGuiCallHelper helper = new ADEGuiCallHelper() {
            private static final long serialVersionUID = 1L;

            // create a new private reference object for this specific gui component...
            public Object call(String methodName, Object... args) throws Exception {
                Method meth = ReflectionUtil.findMethod(methodName, args, component.getClass().getMethods());
                Object callResult = meth.invoke(component, args);

                ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                ObjectOutputStream objectOut = new ObjectOutputStream(byteOut);
                objectOut.writeObject(callResult);
                byte[] bytes = byteOut.toByteArray();
                ByteArrayInputStream byteIn = new ByteArrayInputStream(bytes);
                ObjectInputStream objectIn = new ObjectInputStream(byteIn);
                Object clonedResult = objectIn.readObject();

                return clonedResult;
            }
        };

        return helper;
    }

    private static <T> T[] concatenateArrays(T[] first, T[] second) {
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
}
