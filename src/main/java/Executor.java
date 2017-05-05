import com.sun.istack.internal.Nullable;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.*;

/**
 * Created by obada on 2016-10-27.
 */
class Executor {
    static Process p;
    static PrintWriter pw;
    private final String USR_DIR = System.getProperty("user.dir");
    private final String P1 = (System.getProperty("os.name").charAt(0) == 'W' ? "cmd.exe" : "bash");
    final String P2 = (System.getProperty("os.name").charAt(0) == 'W' ? "/k" : "-C");

    //................................testing...........................................................
    static int len;
    static byte[] buffer = new byte[1024 * 1024];

Executor()
{
    len = -1;
    //open up a process builder
    ProcessBuilder pb = new ProcessBuilder(P1, P2);
    //Shell directory
    pb.directory(new File(USR_DIR));
    p = null;
    try
    {
        p = pb.start();
    } catch (Exception e)
    {
        e.printStackTrace();
    }
    pw = new PrintWriter(new OutputStreamWriter(p.getOutputStream()), true);
}
private void close() throws InterruptedException {
    //this will make main thread wait till process (console) will finish (will be closed)
    p.waitFor();
}
boolean changePWD(String wd)
{
    //open up a process builder
    ProcessBuilder pb = new ProcessBuilder(P1, P2);
    //Shell directory
    pb.directory(new File(wd));

    try {
        p = pb.start();
        pw = new PrintWriter(new OutputStreamWriter(p.getOutputStream()), true);
        return true;
    } catch (IOException e1) {

        e1.printStackTrace();
        // Change back to the user_dir
        if(!wd.equals(USR_DIR))
            changePWD(USR_DIR);
        return false;
    }
}
byte[] Execute(String command)
{
    if(System.getProperty("os.name").charAt(0) == 'L' &&  command.contains("cd"))
    {
        System.out.println("the whole command I got: "+ command.substring(command.indexOf('d') + 1, command.length()));
        if(changePWD(command.substring(command.indexOf('d') + 2, command.length())))
            return "done".getBytes();
        else
            return "Failed to change dir".getBytes();
    }
    new Thread(() -> pw.println(command)).start();
    // Wait for a possible running reading processes to read the process input/error stream
    try {
        Thread.sleep(100);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
    // if some previous process read any values then just return it
    if(len!=-1)
    {
        int tmp = len;
        // Reset the flag to -1
        len = -1;
        // Return the read value
        return Arrays.copyOfRange(buffer, 0, tmp);
    }
    // start a new reading process
    ExecutorService executor = Executors.newSingleThreadExecutor();

    Future<String> future = executor.submit(new ReadInputStream());
    try
    {
        // A timeout for the process but this won't stop the process more or less move the pointer beyond it
        future.get(1, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
        // Cancel the inputStream reading process
        future.cancel(true);
        executor.shutdown();
        executor = Executors.newSingleThreadExecutor();
        // Read from the error stream
        future = executor.submit(new ReadErrorStream());
        try
        {
            // A timeout for the error stream
            future.get(1, TimeUnit.SECONDS);
        }catch (Exception e1)
        {
            // Cancel it
            future.cancel(true);
        }
    } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
    }
    executor.shutdownNow();
    // Read if we have any values
    if(len!=-1)
    {
        int tmp = len;
        len = -1;
        return Arrays.copyOfRange(buffer, 0, tmp);
    }
    else
        return null;
}

    static class ReadInputStream implements Callable<String> {
        @Override
        public String call() throws Exception
        {
            // Even if this process is canceled it'll still be running in the background
            len = Executor.p.getInputStream().read(buffer);
            return "Done";
        }
    }
    static class ReadErrorStream implements Callable<String> {
        @Override
        public String call() throws Exception
        {
            len = Executor.p.getErrorStream().read(buffer);
            return "Done";
        }
    }
}