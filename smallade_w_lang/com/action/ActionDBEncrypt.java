/**
 * ADE 1.0 
 * Copyright 1997-2012 HRILab (http://hrilab.org/) 
 * 
 * All rights reserved.  Do not copy and use without permission. 
 * For questions contact Matthias Scheutz at mscheutz@cs.tufts.edu
 *
 * ActionDBEncrypt.java
 *
 * Last update: October 2012
 *
 * @author Paul Schermerhorn
 *
 */

package com.action;

import javax.crypto.*;
import javax.crypto.spec.*;
import java.io.*;
import java.security.*;
import java.security.spec.*;

class ActionDBEncrypt {
    private static String ifile = "com/action/db/actioncore.xml";
    private static String ofile = "com/action/db/actioncore";
    private static char[] kbuf = {(char)0x21,(char)0x65,(char)0x72,(char)0x6f,(char)0x6d,
        (char)0x6f,(char)0x6e,(char)0x73,(char)0x69,(char)0x62,(char)0x61,
        (char)0x6c,(char)0x6f,(char)0x72,(char)0x69,(char)0x61};
    private static String alg = "PBEWithMD5AndDES";
    private static boolean encrypt = true;

    public static void decrypt() {
        File file = new File(ifile);
        InputStream is = null;
        String newfile;
        String kstr = new String(kbuf);
        int b = 0;

        PBEKeySpec keySpec = new PBEKeySpec(kstr.toCharArray());
        SecretKeyFactory keyFactory = null;
        try {
            keyFactory = SecretKeyFactory.getInstance(alg);
        } catch (NoSuchAlgorithmException nsae) {
            System.err.println("nsae: " + nsae);
            System.exit(1);
        }
        SecretKey key = null;
        try {
            key = keyFactory.generateSecret(keySpec);
        } catch (InvalidKeySpecException ikse) {
            System.err.println("ikse: " + ikse);
            System.exit(1);
        }
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException nsae) {
            System.err.println("nsae: " + nsae);
            System.exit(1);
        }
        md.update("input".getBytes());
        byte[] dig = md.digest();
        byte[] buf = new byte[8];
        System.arraycopy(dig, 0, buf, 0, 8);
        PBEParameterSpec paramSpec = new PBEParameterSpec(buf, 20);
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance(alg);
        } catch (NoSuchAlgorithmException nsae) {
            System.err.println("nsae: " + nsae);
            System.exit(1);
        } catch (NoSuchPaddingException nspe) {
            System.err.println("nspe: " + nspe);
            System.exit(1);
        }
        try {
            cipher.init(Cipher.DECRYPT_MODE, key, paramSpec);
        } catch (InvalidKeyException ike) {
            System.err.println("ike: " + ike);
            System.exit(1);
        } catch (InvalidAlgorithmParameterException iape) {
            System.err.println("iape: " + iape);
            System.exit(1);
        }
        if (file.exists()) {
            try {
                is = new FileInputStream(ifile);
            } catch (FileNotFoundException fnfe) {
                System.err.println("fnfe: " + fnfe);
                System.exit(1);
            }
        }
        CipherInputStream in = null;
        in = new CipherInputStream(is, cipher);
        BufferedOutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(ofile));
        } catch (FileNotFoundException fnfe) {
            System.err.println("fnfe: " + fnfe);
            System.exit(1);
        }
        try {
            do {
                b = in.read(buf);
                if (b > 0) {
                    out.write(buf, 0, b);
                }
            } while (b > 0);
            out.flush();
            out.close();
        } catch (IOException ioe) {
            System.err.println("ioe: " + ioe);
            System.exit(1);
        }
    }
        
    public static void encrypt() {
        char[] kbuf = {(char)0x21,(char)0x65,(char)0x72,(char)0x6f,(char)0x6d,
            (char)0x6f,(char)0x6e,(char)0x73,(char)0x69,(char)0x62,(char)0x61,
            (char)0x6c,(char)0x6f,(char)0x72,(char)0x69,(char)0x61};
        String kstr = new String(kbuf);
        int b = 0;

        PBEKeySpec keySpec = new PBEKeySpec(kstr.toCharArray());
        SecretKeyFactory keyFactory = null;
        try {
            keyFactory = SecretKeyFactory.getInstance(alg);
        } catch (NoSuchAlgorithmException nsae) {
            System.err.println("nsae: " + nsae);
            System.exit(1);
        }
        SecretKey key = null;
        try {
            key = keyFactory.generateSecret(keySpec);
        } catch (InvalidKeySpecException ikse) {
            System.err.println("ikse: " + ikse);
            System.exit(1);
        }
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException nsae) {
            System.err.println("nsae: " + nsae);
            System.exit(1);
        }
        md.update("input".getBytes());
        byte[] dig = md.digest();
        byte[] buf = new byte[8];
        System.arraycopy(dig, 0, buf, 0, 8);
        PBEParameterSpec paramSpec = new PBEParameterSpec(buf, 20);
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance(alg);
        } catch (NoSuchAlgorithmException nsae) {
            System.err.println("nsae: " + nsae);
            System.exit(1);
        } catch (NoSuchPaddingException nspe) {
            System.err.println("nspe: " + nspe);
            System.exit(1);
        }
        try {
            cipher.init(Cipher.ENCRYPT_MODE, key, paramSpec);
        } catch (InvalidKeyException ike) {
            System.err.println("ike: " + ike);
            System.exit(1);
        } catch (InvalidAlgorithmParameterException iape) {
            System.err.println("iape: " + iape);
            System.exit(1);
        }

        CipherInputStream in = null;
        try {
            in = new CipherInputStream(new FileInputStream(ifile), cipher);
        } catch (FileNotFoundException fnfe) {
            System.err.println("fnfe: " + fnfe);
            System.exit(1);
        }
        BufferedOutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(ofile));
        } catch (FileNotFoundException fnfe) {
            System.err.println("fnfe: " + fnfe);
            System.exit(1);
        }
        try {
            do {
                b = in.read(buf);
                if (b > 0) {
                    out.write(buf, 0, b);
                }
            } while (b > 0);
            out.flush();
            out.close();
        } catch (IOException ioe) {
            System.err.println("ioe: " + ioe);
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-in")) {
                ifile = args[++i];
            } else if (args[i].equalsIgnoreCase("-out")) {
                ofile = args[++i];
            } else if (args[i].equalsIgnoreCase("-decrypt")) {
                encrypt = false;
            } else {
                System.err.println("Unrecognized option: " + args[i]);
            }
        }

        if (encrypt) {
            encrypt();
        } else {
            decrypt();
        }
    }
}
