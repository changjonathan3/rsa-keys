/**
 * @author Jonathan Chang
 */

import java.util.Random;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

public class RsaKeyGen{
    public static void main(String[]args){
        System.out.println("Starting key gen");
        int len = 256;
        Random r = new Random();
        LargeInteger p = new LargeInteger(len,r);
        LargeInteger q = new LargeInteger(len, r);
        LargeInteger n = p.multiply(q);
        System.out.println("Got n");
        LargeInteger t = getTotient(p, q);
        System.out.println("Got totient");
        LargeInteger e = new LargeInteger(new byte[]{(byte) 1});
        LargeInteger [] a = getGCD(e, t);
        System.out.println("Got e and d");
        e = a[0];
        LargeInteger d = a[2];
        if(d.isNegative()){
            d =d.add(t);
        }


        System.out.println("Writing to files");
        write("pubkey.rsa", e, n);
        write("privkey.rsa", d, n);
        System.out.println("Done writing to files");
        }

     private static void write(String fileName, LargeInteger one, LargeInteger two){
         try (FileOutputStream fos = new FileOutputStream(fileName)) {
             ObjectOutputStream oos = new ObjectOutputStream(fos);
             oos.writeObject(one);
             oos.writeObject(two);
             oos.close();
         } catch (Exception ioe) {
             ioe.printStackTrace();
         }
     }

    private static boolean isOne(byte[] val){
        if(val[val.length-1] != 1){
            return false;
        }
        for(int i = 0; i < val.length-1; i++){
            if(val[i] != 0){
                return false;
            }
        }
        return true;
    }

    private static LargeInteger getTotient(LargeInteger p, LargeInteger q){
        LargeInteger pDec = p.subtract(new LargeInteger(new byte[]{(byte) 1}));
        LargeInteger qDec = q.subtract(new LargeInteger(new byte[]{(byte) 1}));
        return pDec.multiply(qDec);
    }

    private static LargeInteger [] getGCD(LargeInteger e, LargeInteger t){
        LargeInteger gcd = new LargeInteger(new byte[1]);
        byte[] two = {(byte) 2};
        LargeInteger[]arr=null;
        while(!isOne(gcd.getVal())){
            e = e.add(new LargeInteger(two));
            arr = t.XGCD(e);
            gcd = arr[0];
        }

        LargeInteger one = arr[1].multiply(t);
        LargeInteger sec = arr[2].multiply(e);
        LargeInteger sum = one.add(sec);

        return arr;
    }


}

