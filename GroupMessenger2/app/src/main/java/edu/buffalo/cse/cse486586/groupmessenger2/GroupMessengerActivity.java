package edu.buffalo.cse.cse486586.groupmessenger2;


import android.app.Activity;
import android.os.Bundle;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.widget.Button;
import android.view.View;
import android.widget.TextView;
import android.content.ContentResolver;
import android.widget.EditText;
import android.util.Log;
import android.os.AsyncTask;
import android.database.Cursor;
import android.content.Context;
import android.view.KeyEvent;
import android.view.View.OnKeyListener;
import android.telephony.TelephonyManager;
import android.net.Uri;
import android.content.ContentValues;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.io.*;
import java.util.*;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    static final String TAG = GroupMessengerProvider.class.getSimpleName();

    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    static final int SERVER_PORT = 10000;
    int count = 0;
    int max = 0;
    int client_proposal = 0;
    String[] portsArray = {REMOTE_PORT0, REMOTE_PORT1, REMOTE_PORT2, REMOTE_PORT3, REMOTE_PORT4};
    String port;
    Uri providerUri = Uri.parse("content://edu.buffalo.cse.cse486586.groupmessenger2.provider");
    int pid;
    HashMap<String, Double> hmap = new HashMap<String, Double>();
    LinkedList<Messagaes> detforseq = new LinkedList<Messagaes>();
    LinkedList<Messagaes> detfordelivery = new LinkedList<Messagaes>();


    class Messagaes  {
        public String msg;
        public double number;

        // A parameterized constructor
        public Messagaes(String msg, double number) {

            this.msg = msg;
            this.number = number;
        }

        public Double getseq() {
            return number;
        }
        //public String toString(){ return receivedmsg+ " "+ sequence;}

        public String getmsg() {
            return msg;
        }


        public boolean equals(Object o){
            Messagaes newobject =  (Messagaes)(o);

            if((this.msg.equals(newobject.msg)) && (this.number == newobject.number)){
                return  true;

            }
            return false;
        }

    }

    class SequenceComparator implements Comparator<Messagaes>{

        public int compare(Messagaes m1, Messagaes m2) {
            if (m1.number > m2.number)
                return 1;
            else if (m1.number < m2.number)
                return -1;
            return 0;
        }


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        System.out.println("Printing my port: " + myPort);
        port = myPort;


        if (myPort.equals(REMOTE_PORT0)) {
            pid = 1;
        }

        if (myPort.equals(REMOTE_PORT1)) {
            pid = 2;
        }

        if (myPort.equals(REMOTE_PORT2)) {
            pid = 3;
        }

        if (myPort.equals(REMOTE_PORT3)) {
            pid = 4;
        }
        if (myPort.equals(REMOTE_PORT4)) {
            pid = 5;
        }


        final TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        final EditText et = (EditText) findViewById(R.id.editText1);
        et.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {

                    String msg = et.getText().toString() + "\n";
                    et.setText("");
                    tv.append("\t" + msg);
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
                    return true;
                }
                return false;
            }
        });
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        Button button = (Button) findViewById(R.id.button4);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String msg = et.getText().toString() + "\n";
                et.setText("");
                tv.append("\t" + msg);
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);

            }
        });
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {

            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }





    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];

            int server_proposal = 0;
            double proposal = 0, f_Proposal;
            String message, realmessage;



            try {

                while (true) {
                    System.out.println("---------------------------------------------------------------------------------------------------------------------------------------");


                    Socket socket = serverSocket.accept();
                    System.out.println("Socket connection established.");
                    ObjectInputStream oiStream = new ObjectInputStream(socket.getInputStream());
                    message = (String) oiStream.readObject();

                    Log.e(TAG, "Server Received message and now sending proposal");


                    if (!(message.substring(0, 4)).equals("#:#:")) {

                        server_proposal++;
                        //Server proposal creation
                        proposal = 0.1 * pid + server_proposal;
                        System.out.println("Hmap size: " + hmap.size() + ", for pid: " + pid);

                        Messagaes obj = new Messagaes(message, proposal);
                        detforseq.add(obj);
                        hmap.put(message, proposal);


                        System.out.println("Server Proposal : " + server_proposal);
                        ObjectOutputStream ooStream = new ObjectOutputStream(socket.getOutputStream());
                        System.out.println("Server sent: " + proposal);
                        System.out.println("---------------------------------------------------------------------------------------------------------------------------------------");
                        ooStream.writeObject(proposal);
                        ooStream.flush();
                    }


                    //------------------------------------------------------------------------------------------------------------------------------------------------------------------


                    else if ((message.substring(0, 4)).equals("#:#:")) {

                        System.out.println("---------------------------------------------------------------------------------------------------------------------------------------");
                        Log.e(TAG, "Server Received final proposal and will now store it in file");
                        System.out.println("Server Received final proposal and will now store it in file");
                        realmessage = message.substring(4);
                        String[] arrOfString = realmessage.split(":::::");

                        realmessage = arrOfString[0];
                        f_Proposal = Double.parseDouble(arrOfString[1]);
                        server_proposal=Math.max(server_proposal,(int)Math.round(f_Proposal));
                        System.out.println("Server received: " +realmessage);

                        Double initial_seq = hmap.get(realmessage);
                        System.out.println("Initial sequence for: " + initial_seq +" is " +realmessage);

                        Messagaes obj1 = new Messagaes(realmessage, initial_seq);
                        System.out.println("Object created:" + obj1);

                        boolean isremoved = detforseq.remove(obj1);
                        System.out.println("Return value after remove: "+ isremoved);

                        Messagaes newobj = new Messagaes(realmessage, f_Proposal);
                        detforseq.add(newobj);
                        detfordelivery.add(newobj);

                        System.out.println("Initial LL size: "+ detforseq.size() +" Final LL size: " + detfordelivery.size());

                        if(detforseq.size() > 0 && detfordelivery.size() > 0 ){

                            System.out.println("In if loop.");
                            Collections.sort(detforseq, new SequenceComparator());
                            Collections.sort(detfordelivery, new SequenceComparator());

                            for(Messagaes e:detforseq){
                                System.out.println("Sorted initial LL: "+ e.getmsg());
                            }

                            for(Messagaes e:detfordelivery){
                                System.out.println("Sorted final LL: "+ e.getmsg());
                            }

                            System.out.println("Sorting completed");
                            int i=0;
                            System.out.println("detforseq: "+detforseq.get(i));
                            System.out.println("detfordelivery: "+detfordelivery.get(i));
                            System.out.println("first element: "+ detforseq.getFirst());
                            System.out.println("delivery list first element: "+ detfordelivery.getFirst());
                            System.out.println("Message in list: "+ detforseq.get(i).getmsg());
                            System.out.println("Message in delivery: "+ detfordelivery.get(i).getmsg());
                            System.out.println("List1 seq: "+ detforseq.get(i).getseq());
                            System.out.println("Delivery seq: "+ detfordelivery.get(i).getseq());

                            //Messagaes object_final = null;
                            System.out.println("detforseq.size(): "+detforseq.size() + " "+i);
                            System.out.println("detfordelivery.size() "+detfordelivery.size()+ " "+i);

                            while(i <detforseq.size() && i <detfordelivery.size() && detforseq.get(i).equals(detfordelivery.get(i)) ){
                                System.out.println("final loop");
                                Messagaes object_final = detfordelivery.get(i);

                                String msg_publish = object_final.getmsg();
                                publishProgress(msg_publish);
                                boolean isremoved_final = detforseq.remove(object_final);
                                boolean isremoved_final2 = detfordelivery.remove(object_final);
                                System.out.println("Return value after remove from final loop: " + isremoved_final);
                                System.out.println("Return value after remove from final loop: " + isremoved_final2);

                            }

                        }

                             /*
                                      Send Acknowledgement message to client.

                             */

                        ObjectOutputStream ooStream = new ObjectOutputStream(socket.getOutputStream());

                        System.out.println("---------------------------------------------------------------------------------------------------------------------------------------");
                        ooStream.writeObject("ACK");
                        System.out.println("Sending Acknowledgement.");
                        ooStream.flush();


                    }


                }

            } catch (OptionalDataException e1) {
                e1.printStackTrace();
            } catch (StreamCorruptedException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            } catch (ClassNotFoundException e1) {
                e1.printStackTrace();
            }


            return null;


        }
        protected void onProgressUpdate(String... strings) {


            System.out.println("Insert finished file created :" + count);
            TextView localTextView = (TextView) findViewById(R.id.textView1);
            localTextView.append(String.valueOf(count)+" "+strings[0]);
            localTextView.append("\n");


            ContentValues keyValueToInsert = new ContentValues();
            keyValueToInsert.put("key", Integer.toString(count));
            keyValueToInsert.put("value", strings[0]);
            getContentResolver().insert(
                    providerUri,
                    keyValueToInsert


            );
            count++;


        }
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {

            try {
                String msgToSend = msgs[0];
                System.out.println("New Entered message in client is:" + msgToSend);

                msgToSend = msgToSend.replace("\n", "").replace("\r", "");
                System.out.println("Msg: " + msgToSend);
                ArrayList<Double> allproposals = new ArrayList<Double>();
                double recproposal = 0;
                ObjectOutputStream ooStream = null;
                HashMap<String, Double> hmap = new HashMap<String, Double>();
                HashMap<String, Double> newhmap = new HashMap<String, Double>();

                Log.e(TAG, "Client will now start sending messages.");

                for (int i = 0; i < portsArray.length; i++) {


                    System.out.println("---------------------------------------------------------------------------------------------------------------------------------------");

                    System.out.println("Msg: " + msgToSend);
                    //Creation of sockets
                    byte[] ipAddr = new byte[]{10, 0, 2, 2};
                    System.out.println(ipAddr);
                    InetAddress addr = InetAddress.getByAddress(ipAddr);
                    Socket socket = new Socket(addr, Integer.parseInt(portsArray[i]));
                    //  socket.setSoTimeout(500);
                    System.out.println("Socket object created");
                    ooStream = new ObjectOutputStream(socket.getOutputStream());
                    System.out.println("OOS object created");
                    //Generating unique message:
                    String process_id = portsArray[i];

                    //   String client = Integer.toString(client_proposal) + "." + process_id;
                    System.out.println("Client's proposal is: " + msgToSend);

                    System.out.print("Client " + port + " is sending: " + msgToSend + " to server " + process_id);

                    ooStream.writeObject(msgToSend);
                    ooStream.flush();


                    System.out.println("---------------------------------------------------------------------------------------------------------------------------------------");


                    InputStream inputStream = socket.getInputStream();
                    ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);

                    if (objectInputStream != null) {

                        recproposal = (Double) objectInputStream.readObject();


                        if (recproposal != 0) {
                            System.out.println("Client received proposal: " + recproposal);

                            allproposals.add(recproposal);                                    //Client is storing it in sortedset
                            System.out.println("All proposals are: " + allproposals + ", For message: "+ msgToSend);


                            //Collections.sort(allproposals);
                            hmap.put(msgToSend, Double.valueOf(Collections.max(allproposals)));


                        }

                        Set<Map.Entry<String, Double>> set = hmap.entrySet();
                        for (Map.Entry<String, Double> me : set) {

                            System.out.println("For message: " + me.getKey() + ", Maximum proposal is: " + me.getValue());

                        }

                    }


                    Log.e(TAG, "1. Server Sent the acknowledgement to close the socket.");

                    System.out.println("Acknowledgement received from socket.");
                    socket.close();
                    ooStream.close();
                    System.out.println("Socket 1 is closed now.");
                }


                System.out.println("---------------------------------------------------------------------------------------------------------------------------------------");

                for (int i = 0; i < portsArray.length; i++) {

                    System.out.println("In for loop for the 2nd time");
                    byte[] ipAddr = new byte[]{10, 0, 2, 2};
                    System.out.println(ipAddr);
                    InetAddress addr = InetAddress.getByAddress(ipAddr);
                    Socket socket = new Socket(addr, Integer.parseInt(portsArray[i]));
                    //  socket.setSoTimeout(500);
                    System.out.println("Socket object created");
                    ooStream = new ObjectOutputStream(socket.getOutputStream());
                    System.out.println("OOS object created");

                    Log.e(TAG, "Client has sorted the proposals and will now send the maximum proposal");

                    System.out.println("Sorted Proposal size: " + allproposals.size());
                    System.out.println("Hash map size: / Total number of messages of which we know max proposal of:" + hmap.size());

                    Set<Map.Entry<String, Double>> nset = hmap.entrySet();
                    for (Map.Entry<String, Double> me : nset) {

                        System.out.println("For key: " + me.getKey() + ", Max proposed Value is: " + me.getValue());
                        System.out.println("Now client will send the final seq number to servers");
                        newhmap.put(me.getKey(), me.getValue());

                    }

                    Set<Map.Entry<String, Double>> newset = newhmap.entrySet();
                    for (Map.Entry<String, Double> me : newset) {

                        //Client sending largest proposalHmap size
                        String sendagain = "#:#:" + me.getKey() + ":::::" + me.getValue();
                        System.out.println("Real message to send from client side is: " + sendagain);
                        ooStream.writeObject(sendagain);
                        ooStream.flush();

                    }
                    System.out.println("---------------------------------------------------------------------------------------------------------------------------------------");

                    Thread.sleep(10);


                    ObjectInputStream oino = new ObjectInputStream(socket.getInputStream());

                    if(oino!=null) {

                        String ack = (String) oino.readObject();

                        Log.e(TAG, "2. Server Sent the acknowledgement to close the socket.");

                        if (ack.equals("ACK")) {
                            System.out.println("Acknowledgement received from socket.");
                            socket.close();
                            ooStream.close();
                            System.out.println("Socket is closed now.");
                        }
                    }
                }


                Log.e(TAG, "Done!");

                return null;
            } catch (OptionalDataException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (SocketTimeoutException e) {
                e.printStackTrace();
            }  catch (SocketException e) {
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (StreamCorruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                Log.e(TAG, "IO Exception has occurred.");
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return null;
        }
    }
}