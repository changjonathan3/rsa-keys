/**
 * @author Jonathan Chang
 */

import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.FileInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.security.MessageDigest;

public class RsaSign{

    public static void main(String [] args){
        if(args.length!=2){
            System.out.println("Invalid command args (not two)");
            System.exit(0);
        }
        char flag = args[0].charAt(0);
        if(flag!='v' && flag!='s'){
            System.out.println("Flag is not 's' or 'v' ");
            System.exit(0);
        }
        if(flag=='s'){
            System.out.println("Begin signing");
            byte[] hash = genHash(args[1]);
            LargeInteger signature = sign(new LargeInteger(hash));
            write(args[1], signature);
            System.out.println("End signing");
        }
        else{
            System.out.println("Begin verify");
            byte[] hash = genHash(args[1]);
            LargeInteger sigHash = read(args[1]);
            sigHash = encrypt(sigHash);
            verify(new LargeInteger(hash), sigHash);
            System.out.println("End verify");
        }
    }

    private static byte[] genHash(String file){
        byte[] digest = null;
        try {
            // read in the file to hash
            Path path = Paths.get(file);
            byte[] data = Files.readAllBytes(path);

            // create class instance to create SHA-256 hash
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            // process the file
            md.update(data);
            // generate a has of the file
            digest = md.digest();

        } catch(Exception e) {
            System.out.println(e.toString());
            System.exit(0);
        }
        return digest;
    }

    private static LargeInteger sign(LargeInteger hash){
        LargeInteger decrypt = null;
        try(FileInputStream fis = new FileInputStream("privkey.rsa")){
           ObjectInputStream ois = new ObjectInputStream(fis);
           LargeInteger d =  (LargeInteger) ois.readObject();
           LargeInteger n = (LargeInteger) ois.readObject();
           decrypt = hash.modularExp(d, n);
           ois.close();
        }catch(Exception e){
            e.printStackTrace();
            System.exit(0);
        }
        return decrypt;
    }

    private static void write(String fileName, LargeInteger one){
        try (FileOutputStream fos = new FileOutputStream(fileName + ".sig")) {
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(one);
            oos.close();
        } catch (Exception ioe) {
            ioe.printStackTrace();
        }
    }

    private static LargeInteger read(String fileName){
        LargeInteger sigHash = null;
        try(FileInputStream fis = new FileInputStream(fileName + ".sig")){
            ObjectInputStream ois = new ObjectInputStream(fis);
            sigHash = (LargeInteger) ois.readObject();
            ois.close();
        }catch(Exception e){
            e.printStackTrace();
            System.exit(0);
        }
        return sigHash;
    }

    private static LargeInteger encrypt(LargeInteger sig){
        LargeInteger encrypt = null;
        try(FileInputStream fis = new FileInputStream("pubkey.rsa")){
            ObjectInputStream ois = new ObjectInputStream(fis);
            LargeInteger e = (LargeInteger) ois.readObject();
            LargeInteger n = (LargeInteger) ois.readObject();
            encrypt = sig.modularExp(e, n);
            ois.close();
        }catch(Exception e){
            e.printStackTrace();
            System.exit(0);
        }
        return encrypt;
    }

    private static void verify(LargeInteger hash, LargeInteger sigHash){
        int check = hash.compare(sigHash);
        if(check==0){
            System.out.println("Signature is valid");
        }
        else{
            System.out.println("Signature is valid ");
        }
    }
}