
import mjtb49.hashreversals.ChunkRandomReverser;
import kaptainwutax.seedutils.mc.MCVersion;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class Main {
    static File file;
    static FileOutputStream fos = null;
    static int interval = 10000;
    public static ArrayList<Long> eyeseeds = new ArrayList<Long>();
    public static void main(String[] args) {
        int start = 0;
        int end = 0;
        int threads = 0;
        if(args.length > 0){
            for(int i = 0; i < args.length; i+=2){
                if(args[i].equals("--threads")){
                    threads = Integer.parseInt(args[i+1]);
                    continue;
                }
                else if(args[i].equals("--start")){
                    start = Integer.parseInt(args[i+1]);
                    continue;
                }
                else if(args[i].equals("--end")){
                    end = Integer.parseInt(args[i+1]);
                    continue;
                }
                else if(args[i].equals("--interval")){
                    interval = Integer.parseInt(args[i+1]);
                    continue;
                }
                else{
                    System.out.println("Unrecognized parameter!");
                    return;
                }
            }
        }
        else{
            System.out.println("Please specify --start, --end, and --threads.");
            return;
        }
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        InputStream is = cl.getResourceAsStream("all_11_eye_matt.txt");
        InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr);
        try{
            for(String line; (line = br.readLine()) != null;){
                eyeseeds.add(Long.parseLong(line));
            }
        }
        catch(IOException ex){
            System.out.println("Fuck");
            return;
        }
        if(end == 0)
            end = 500;
        try{
            file = new File(start + "-" + end + ".txt");
            fos = new FileOutputStream(file);

        }
        catch(FileNotFoundException ex){
            System.out.println("Fuck");
            return;
        }
        try{
            if(!file.exists()){
                file.createNewFile();
            }
            reversePopulationSeedPre13(start, end, threads);
        }
        catch(IOException ex){
            System.out.println("Fuck");
            return;
        }
    }
    static int counter = 0;
    static int total = 0;
    static long startTime = System.nanoTime();
    static long tempTime = 0;
    private static synchronized void print(List<Long> worldSeeds, int x, int z) {
            worldSeeds.forEach( worldSeed -> {
                try {
                    fos.write(String.format("%d %d %d\n", worldSeed, x, z).getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
    }
    static DecimalFormat percentformatter = (DecimalFormat) NumberFormat.getNumberInstance(Locale.US);
    static DecimalFormat secondsformatter = (DecimalFormat) NumberFormat.getNumberInstance(Locale.US);

    private static synchronized void increment(){
        counter++;
        if(counter % interval == 0){
            tempTime = System.nanoTime();
            double percentage = ((double)counter/(double)total);
            double secondsElapsed = ((double)tempTime - (double)startTime)/1000000000.0;
            double secondsRemaining = secondsElapsed/percentage - secondsElapsed;
            System.out.printf("Progress: %s%% done (%08d out of %08d)\t%s seconds elapsed\t%s seconds remaining\n",percentformatter.format(percentage*100),  counter, total, secondsformatter.format(secondsElapsed), secondsformatter.format(secondsRemaining));
        }
    }
    public static void reversePopulationSeedPre13(int start, int end, int threads) throws IOException {
        percentformatter.applyPattern("000.000000");
        secondsformatter.applyPattern(("0000.000000"));
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        int outerLimit = 63;
        int innerLimit = 23;
        System.out.printf("Doing chunkseeds from %d to %d out of %d total.\n", start, end, eyeseeds.size());
        ChunkRandomReverser device = new ChunkRandomReverser();
        long startTime = System.nanoTime();
        for(int i = start; i <= end && i < eyeseeds.size(); i++) {
            for (int x = -outerLimit; x <= outerLimit; x++) {
                int xTemp = x;
                long cseed = eyeseeds.get(i) ^ 25214903917L;
                for (int z = -outerLimit; z <= outerLimit; z++) {
                    int zTemp = z;
                    if (!(z < innerLimit && z > -innerLimit && x < innerLimit && x > -innerLimit)) {
                        total++;
                        executor.submit(() -> {
                            List<Long> worldSeeds = device.reversePopulationSeed(cseed, xTemp, zTemp, MCVersion.v1_12);
                            print(worldSeeds, xTemp, zTemp);
                            increment();

                        });
                    }
                }
            }
            if(i % 100 == 0){
                System.out.printf("Progress: %d out of %d\t%f%%\n", i, end, ((double)i/(double)end)*100);
            }
        }
        System.out.println("All work assigned out!");
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        long endTime = System.nanoTime();
        System.out.println("Time spent: " + (double)((endTime - startTime)/1000000000.0));
        fos.flush();
    }

}
