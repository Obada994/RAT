import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * RAT Created by obada on 3/19/17.
 */
public class Tests {

    ServerSocket server;

@Before
public void init() throws IOException {
    server = new ServerSocket(1234);
    new Thread(){
        public void run()
        {
//          while(true)
//              try {/
//                  new Client(server.accept(), "serverSide",Client.MODE.STEALTH, "serverKey1234567", "thisKey123456789");
//              } catch (IOException e) {
//                  e.printStackTrace();
//              }
        }
    }.start();
}
@Test
public void sendFile()
{
//    try {
//        Client client = new Client(new Socket("localhost",1234), "clientSide", Client.MODE.NORMAL, "serverKey1234567", "thisKey123456789");
//        JSONObject json = new JSONObject();
//        json.put("type","request");
//        json.put("data","get");
//        json.put("path","/home/obada/Desktop/testy");
//    } catch (IOException e) {
//        e.printStackTrace();
//    } catch (JSONException e) {
//        e.printStackTrace();
//    }
}


}
