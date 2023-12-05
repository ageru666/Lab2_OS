package moss.shed;// This file contains the main() function for the main.Scheduling
// simulation.  Init() initializes most of the variables by
// reading from a provided file.  main.SchedulingAlgorithm.Run() is
// called from main() to run the simulation.  Summary-main.Results
// is where the summary results are written, and Summary-Processes
// is where the process scheduling summary is written.

// Created by Alexander Reeder, 2001 January 06

import java.io.*;
import java.time.LocalDateTime;
import java.util.StringTokenizer;
import java.util.Vector;

public class Scheduling {

  private static final String RESULTS_FILE = "Results";

  private static int processnum = 5;
  private static int meanDev = 1000;
  private static int standardDev = 100;
  private static int runtime = 1000;
  private static double agingCoef = 0.2;
  private static Vector<sProcess> processVector = new Vector<>();
  private static Results result = new Results("null", "null", 0);

  private static void parseAgingCoef(String string) {
    try {
      agingCoef = Double.parseDouble(string.trim());
    } catch (NumberFormatException nfe) {
      System.out.println("NumberFormatException: " + nfe.getMessage());
    }
    if (agingCoef < 0 || agingCoef > 1) {
      System.out.println("Invalid agingCoef: must be within [0,1]. Using default agingCoef = 0.5");
      agingCoef = 0.5;
    }
  }

  private static void Init(String file) throws IOException {
    File f = new File(file);
    String line;
    int processId = 1;
    try (DataInputStream in = new DataInputStream(new FileInputStream(f))) {
      while ((line = in.readLine()) != null) {
        if (line.startsWith("numprocess")) {
          StringTokenizer st = new StringTokenizer(line);
          st.nextToken();
          processnum = Common.s2i(st.nextToken());
        }
        if (line.startsWith("mean")) {
          StringTokenizer st = new StringTokenizer(line);
          st.nextToken();
          meanDev = Common.s2i(st.nextToken());
        }
        if (line.startsWith("standdev")) {
          StringTokenizer st = new StringTokenizer(line);
          st.nextToken();
          standardDev = Common.s2i(st.nextToken());
        }
        if (line.startsWith("process")) {
          StringTokenizer st = new StringTokenizer(line);
          st.nextToken();
          int runTimeBeforeBlocking = Common.s2i(st.nextToken());
          int blockDuration = Common.s2i(st.nextToken());
          int arrivalTime = Common.s2i(st.nextToken());
          double X = Common.R1();
          while (X == -1.0) {
            X = Common.R1();
          }
          X = X * standardDev;
          int cpuTime = (int) X + meanDev;
          processVector.add(new sProcess(processId, cpuTime, runTimeBeforeBlocking, blockDuration, arrivalTime));
          processId++;
        }
        if (line.startsWith("runtime")) {
          StringTokenizer st = new StringTokenizer(line);
          st.nextToken();
          runtime = Common.s2i(st.nextToken());
        }
        if (line.startsWith("agingCoef")) {
          StringTokenizer st = new StringTokenizer(line);
          st.nextToken();
          parseAgingCoef(st.nextToken());
        }
      }
    }
  }

  private static void debug() {
    System.out.println("processnum " + processnum);
    System.out.println("meandevm " + meanDev);
    System.out.println("standdev " + standardDev);
    for (sProcess process : processVector) {
      System.out.println("process " + process.id + " " + process.cpuTime + " " + process.runTimeBeforeBlocking + " " + process.cpuDone + " " + process.numBlocked);
    }
    System.out.println("runtime " + runtime);
  }

  public static void main(String[] args) {

    args = new String[1];
    args[0] = "src\\moss\\shed\\scheduling.conf";
    if (args.length != 1) {
      System.out.println("Usage: 'java Scheduling <INIT FILE>'");
      System.exit(-1);
    }

    File f = new File(args[0]);
    if (!f.exists()) {
      System.out.println("Scheduling: error, file '" + f.getName() + "' does not exist.");
      System.exit(-1);
    }
    if (!f.canRead()) {
      System.out.println("Scheduling: error, read of " + f.getName() + " failed.");
      System.exit(-1);
    }

    System.out.println("Working...");
    try {
      Init(args[0]);
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(-1);
    }

    if (processVector.size() < processnum) {
      for (int i = processVector.size() + 1; i <= processnum; i++) {
        double X = Common.R1();
        while (X == -1.0) {
          X = Common.R1();
        }
        X = X * standardDev;
        int cpuTime = (int) X + meanDev;
        processVector.add(new sProcess(i, cpuTime, i * 100, i * 10, 0));
      }
    }

    result = SchedulingAlgorithm.run(runtime, processVector, result, agingCoef);

    try (PrintStream out = new PrintStream(new FileOutputStream(RESULTS_FILE))) {
      out.println("DateTime: " + LocalDateTime.now());
      out.println("Scheduling Type: " + result.schedulingType);
      out.println("Scheduling Name: " + result.schedulingName);
      out.println("Simulation Run Time: " + result.compuTime);
      out.println("Mean: " + meanDev);
      out.println("Standard Deviation: " + standardDev);
      out.println("Process #\tCPU Time\tIO Blocking\tCPU Completed\tCPU Blocked");
      for (sProcess process : processVector) {
        out.print(process.id);
        if (process.id < 100) {
          out.print("\t\t");
        } else {
          out.print("\t");
        }
        out.print(process.cpuTime);
        if (process.cpuTime < 100) {
          out.print(" (ms)\t\t");
        } else {
          out.print(" (ms)\t");
        }
        out.print(process.runTimeBeforeBlocking);
        if (process.runTimeBeforeBlocking < 100) {
          out.print(" (ms)\t\t");
        } else {
          out.print(" (ms)\t");
        }
        out.print(process.cpuDone);
        if (process.cpuDone < 100) {
          out.print(" (ms)\t\t");
        } else {
          out.print(" (ms)\t");
        }
        out.println(process.numBlocked + " times");
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    System.out.println("Completed.");
  }
}
