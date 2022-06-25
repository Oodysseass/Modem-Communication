import ithakimodem.*;
import java.io.*;
import java.util.*;

public class UserApplication {
    public static void main(String[] param) {

        UserApplication session = new UserApplication();
        int k;
        // establishing communication with Ithaki Server
        Modem modem = new Modem(80000);
        modem.setTimeout(2000);
        modem.open("ithaki");

        // reading welcome message only once
        for(;;) {

            try {
                
                k = modem.read();
                if (k == -1) {
                    break;
                }
                System.out.print((char)k);

            }catch(Exception x){
                break;
            }

        }

        // apply corresponding code on every session
        //session.echoRequest("E7713\r", modem);
        //session.noErrorImageRequest("M0316\r", modem);
        //session.errorImageRequest("G7620\r", modem);
        //session.gpsRequest("P4172", modem);
        session.arqRequest("Q5450\r", "R6252\r", modem);

        modem.close();
    }

    public void echoRequest(String code, Modem modem) {
        int k;

        // requesting packets for 5 minutes and printing them 
        long start = System.currentTimeMillis();
        long end, startOfPacket;
        do {

            // count duration of each packet
            startOfPacket = System.currentTimeMillis();
            modem.write(code.getBytes());

            for(;;) {

                try {
                    
                    k = modem.read();
                    if (k == -1) {
                        break;
                    }
                    System.out.print((char)k);

                }catch(Exception x){
                    break;
                }

            }

            end = System.currentTimeMillis();
            System.out.println();
            System.out.println("Duration of packet: " + (end - startOfPacket) + "ms.");
        } while((end - start) / 60000 < 5);

    }

    public void noErrorImageRequest(String code, Modem modem) {
        int k;

        // requesting image and saving binary data to jpg
        try{

            FileOutputStream image = new FileOutputStream("okayImage.jpg");
            long start = System.currentTimeMillis();
            modem.write(code.getBytes());
            
            for(;;) {

                try {
                        
                    k = modem.read();
                    image.write((char)k);
                    if (k == -1) {
                        break;
                    }
    
                }catch(Exception x){
                    break;
                }
            }

            long end = System.currentTimeMillis();
            System.out.println("Duration of no error image: " + (end - start) + "ms.");
            image.close();

        }catch(FileNotFoundException e) {
			e.printStackTrace();
        }catch(IOException e){
            e.printStackTrace();
        }

    }

    public void errorImageRequest(String code, Modem modem) {
        int k;

        // requesting image and saving binary data to jpg
        try{

            FileOutputStream image = new FileOutputStream("errorImage.jpg");
            long start = System.currentTimeMillis();
            modem.write(code.getBytes());
            
            for(;;) {

                try {
                        
                    k = modem.read();
                    image.write((char)k);
                    if (k == -1) {
                        break;
                    }
    
                }catch(Exception x){
                    break;
                }
            }

            long end = System.currentTimeMillis();
            System.out.println("Duration of faulty image: " + (end - start) + "ms.");
            image.close();

        }catch(FileNotFoundException e) {
			e.printStackTrace();
        }catch(IOException e){
            e.printStackTrace();
        }

    }

    public void gpsRequest(String code, Modem modem) {
        int k, lineCounter = 0, tCounter = 0;
        ArrayList<Character> packet = new ArrayList<Character>();
        double seconds = 0;
        // changing gpdCode for appropriate X and number of packets
        modem.write((code + "R=1000199\r").getBytes());

        for(;;) {

            try {
                        
                k = modem.read();
                if (lineCounter % 10 == 1){     // saving packet we will use below for gps coordinates
                    packet.add((char)k);
                }

                if (k == -1) {
                    break;
                }

                System.out.print((char)k);

                if ((char) k == '\n') {
                    lineCounter++;
                    if (lineCounter % 10 == 2 && tCounter < 10) {   // just a custom way to skip some packets
                        System.out.println(" Packet chosen");
                        code += "T=";
                        for (int i = 31; i < 35; i++){
                            code = code + packet.get(i);        // getting longtitude
                        }
                        for (int i = 36; i < 40; i++){
                            seconds = seconds + Math.pow(10, 35 - i) * Character.getNumericValue(packet.get(i));
                        }
                        seconds = seconds * 60;                 // getting .mm and converting it to seconds
                        code = code + (int) seconds;
                        seconds = 0;
                        for (int i = 18; i < 22; i++){
                            code = code + packet.get(i);        // getting latitude
                        }
                        for (int i = 23; i < 27; i++){
                            seconds = seconds + Math.pow(10, 22 - i) * Character.getNumericValue(packet.get(i));
                        }
                        seconds = seconds * 60;                 // getting .mm and converting it to seconds
                        code = code + (int) seconds;
                        seconds = 0;
                        tCounter++;
                        packet.clear();
                    }
                }

            }catch(Exception x){
                break;
            }

        }

        try{

            // requesting image and saving binary data to jpg
            FileOutputStream image = new FileOutputStream("gpsImage.jpg");
            long start = System.currentTimeMillis();
            modem.write((code + "\r").getBytes());
            
            for(;;) {

                try {
                        
                    k = modem.read();
                    image.write((char)k);
                    if (k == -1) {
                        break;
                    }
    
                }catch(Exception x){
                    break;
                }
            }

            long end = System.currentTimeMillis();
            System.out.println("Duration of gps image: " + (end - start) + "ms.");
            image.close();

        }catch(FileNotFoundException e) {
			e.printStackTrace();
        }catch(IOException e){
            e.printStackTrace();
        }

    }

    public void arqRequest(String ack, String nack, Modem modem) {
        int k, realFCS, guessedFCS, requests;

        long start = System.currentTimeMillis();
        long end, startOfPacket;

        // implemantation of ARQ mechanism for 5 mins
        do{

            // initializing for new packet
            requests = 0;
            startOfPacket = System.currentTimeMillis();
            modem.write(ack.getBytes());

            for(;;){
                requests++;                                     // we will obviously need at least on request even if the packet is correct from the first time
                realFCS = 0;
                guessedFCS = 0;
                for(;;) {

                    try {
                                
                        k = modem.read();
                        if (k == -1) {
                            break;
                        }
                        System.out.print((char)k);

                        if ((char)k == '<') {
                            for (int i = 0; i < 16; i++) {
                                k = modem.read();               // reading the 16 characters of the message 
                                guessedFCS = guessedFCS ^ k;    // and calculating FCS
                                System.out.print((char)k);
                            }
                        }

                        else if ((char)k == '>') {
                            k = modem.read();
                            System.out.print((char)k);
                            for (int i = 0; i < 3; i++){
                                k = modem.read();               // reading actual FCS of the message
                                realFCS = realFCS + (int) Math.pow(10, 2 - i) * Character.getNumericValue((char) k);
                                System.out.print((char)k);
                            }
                        }

                    }catch(Exception x){
                        break;
                    }

                }

                System.out.println();
                
                if(realFCS != guessedFCS){
                    modem.write(nack.getBytes());               // sending NACK till calculated FCS is equal to the right one
                }

                else break;

            }

            end = System.currentTimeMillis();
            System.out.println("Requests needed for packet: " + requests);
            System.out.println("Time needed: " + (end - startOfPacket) + "ms");
            System.out.println();

        }while((end - start) / 60000 < 5);
        
    }

}
