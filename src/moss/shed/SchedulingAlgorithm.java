package moss.shed;// Run() is called from main.Scheduling.main() and is where
// the scheduling algorithm written by the user resides.
// User modification should occur within the Run() function.

import java.util.Comparator;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Vector;
import java.io.*;

public class SchedulingAlgorithm {

  private SchedulingAlgorithm() {}

  public static String processActionInfo(sProcess process, String action, int curTime) {
    return String.format("at %dms Process: %s %s (%s)", curTime, process.id, action, process.processInfo());
  }

  public static Results run(int runtime, Vector<sProcess> processVector, Results result, double agingCoef) {
    int curTime = 0;
    sProcess curProcess = null;
    int numProcesses = processVector.size();
    int numCompletedProcesses = 0;
    int curProcessTime = 0;
    boolean scheduleNextProcess = true;
    String resultsFile = "Processes Results";
    result.schedulingType = "Interactive (Nonpreemptive)";
    result.schedulingName = "Shortest process next, agingCoef = " + agingCoef;

    try (PrintStream out = new PrintStream(new FileOutputStream(resultsFile))) {
      PriorityQueue<sProcess> processesHeap = new PriorityQueue<>(Comparator.comparingDouble(process -> process.timeEstimate));

      while (curTime < runtime) {
        checkAndAddArrivalOrUnblock(processVector, curTime, processesHeap, out);

        if (scheduleNextProcess && !processesHeap.isEmpty()) {
          curProcess = processesHeap.poll();
          scheduleNextProcess = false;
          out.println(processActionInfo(curProcess, "registered", curTime));
        }

        if (curProcess != null) {
          curTime++;
          executeProcess(curProcess, out);

          if (curProcess.cpuDone == curProcess.cpuTime) {
            completeProcess(curProcess, out);
            numCompletedProcesses++;
            if (numCompletedProcesses == numProcesses) break;
            curProcessTime = 0;
            curProcess = null;
            scheduleNextProcess = true;
          } else if (curProcess.runTimeBeforeBlocking == curProcess.ioNext) {
            blockForIO(curProcess, agingCoef, curProcessTime, curTime, out);
            curProcessTime = 0;
            scheduleNextProcess = true;
          }
        } else {
          out.println("at " + curTime + "ms waiting for any process to get ready");
          curTime++;
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    result.compuTime = curTime;
    return result;
  }

  private static void checkAndAddArrivalOrUnblock(Vector<sProcess> processVector, int curTime, PriorityQueue<sProcess> processesHeap, PrintStream out) {
    for (int processIndex = 0; processIndex < processVector.size(); processIndex++) {
      boolean addToScheduling = false;
      sProcess checkedProcess = processVector.elementAt(processIndex);

      if (checkedProcess.arrivalTime == curTime) {
        addToScheduling = true;
        out.println(processActionInfo(checkedProcess, "arrived and added to scheduling", curTime));
      }

      if (checkedProcess.isBlocked && curTime - checkedProcess.lastTimeExecuted == checkedProcess.blockDuration) {
        checkedProcess.isBlocked = false;
        addToScheduling = true;
        out.println(processActionInfo(checkedProcess, "unblocked and added to scheduling", curTime));
      }

      if (addToScheduling) {
        processesHeap.add(checkedProcess);
      }
    }
  }

  private static void executeProcess(sProcess curProcess, PrintStream out) {
    curProcess.cpuDone++;
    if (!curProcess.isBlocked && curProcess.runTimeBeforeBlocking > 0) {
      curProcess.ioNext++;
    }
  }

  private static void completeProcess(sProcess curProcess, PrintStream out) {
    out.println(processActionInfo(curProcess, "completed", curProcess.cpuDone));
  }

  private static void blockForIO(sProcess curProcess, double agingCoef, int curProcessTime, int curTime, PrintStream out) {
    curProcess.numBlocked++;
    curProcess.isBlocked = true;
    curProcess.ioNext = 0;
    curProcess.lastTimeExecuted = curTime;
    out.println(processActionInfo(curProcess, "I/O blocked", curTime));

    curProcess.timeEstimate = agingCoef * curProcess.timeEstimate + (1 - agingCoef) * curProcessTime;
    out.println("Process " + curProcess.id + " new time estimate: " + curProcess.timeEstimate);
  }
}
