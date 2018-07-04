package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.PriorityQueue;

import static android.R.attr.x;
import static android.R.id.message;
import static android.content.ContentValues.TAG;
import static java.lang.Float.parseFloat;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    static final int SERVER_PORT = 10000;
    int skipServerID= -1;
    int clientID=-1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        /*if(myPort.equals("11108"))
            clientID=0;
        else if(myPort.equals("11110"))
            clientID=1;
        else if(myPort.equals("11116"))
            clientID=2;
        else if(myPort.equals("11120"))
            clientID=3;
        else
            clientID=4;*/
        clientID = Integer.parseInt(myPort);
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e("Exception Thrown", "Exception Thrown");
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        findViewById(R.id.button1).setOnClickListener(new OnPTestClickListener(tv, getContentResolver()));
        final EditText editText = (EditText) findViewById(R.id.editText1);
        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = editText.getText().toString();
                editText.setText(""); // This is one way to reset the input box.
                TextView tv1 = (TextView) findViewById(R.id.textView1);
                //      tv1.append("\t" + msg + "\n"); // This is one way to display a string.
                Log.v("CT--doInBackground" ,"Sleeping for 4000 ms");
                int sleepTime=4000;
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException ie) {

                }
                Log.v("CT--onCreate", "Invoking new CT thread on port" + myPort);
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }
    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }



    class ServerTask extends AsyncTask<ServerSocket, String, Void> {
        int counter = 0;
        float seqNum = 0;
        float unSeqNum = 0;

        PriorityQueue<Float> initialPQ = new PriorityQueue<Float>();
        HashMap<Float,Integer> recvFromTag = new HashMap<Float,Integer>();
        HashMap<String,String> messageTag = new HashMap<String, String>();
        HashMap<String,Float> seqTag = new HashMap<String,Float>();
        HashMap<Float,String> seqTagRev = new HashMap<Float,String>();
        void printDataStructures(HashMap messageTag,HashMap seqTag,HashMap seqTagRev){
            HashMap<String,String> messageTag1 = messageTag;
            HashMap<String,Float> seqTag1 = seqTag;
            HashMap<Float,String> seqTagRev1 = seqTagRev;
            Log.v("--Print MsgTag DS--","---");
            for (Map.Entry<String,String> entry : messageTag1.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                Log.v("MessageTag","Key-"+key+"Value-"+value);
            }
            Log.v("--Print SeqTag DS--","---");
            for (Map.Entry<String,Float> entry : seqTag1.entrySet()) {
                String key = entry.getKey();
                String value = Float.toString(entry.getValue());
                Log.v("SeqTag","Key-"+key+"Value-"+value);
            }
            Log.v("--Print SeqTagRev DS--","---");
            for (Map.Entry<Float,String> entry : seqTagRev1.entrySet()) {
                String key = Float.toString(entry.getKey());
                String value = entry.getValue();
                Log.v("SeqTagRev","Key-"+key+"Value-"+value);
            }
            Log.v("-----Print DS-----","--End--");
        }
        void sendProposedSeqNum(DataOutputStream out,String Message,int recvFrom){
            try{
                seqNum++;
                unSeqNum++;
                Log.v("Insert in message Tag",Message);
                messageTag.put(Message, "N");
                seqTag.put(Message,unSeqNum);
                Log.v("ST--sendProposedSeqNum","Inserting "+seqNum+" in seqTagRev");
                seqTagRev.put(unSeqNum ,Message);
                Log.v("ST--sendProposedSeqNum","Inserting "+unSeqNum+" in Priority Queue");
                initialPQ.add(unSeqNum);
                recvFromTag.put(unSeqNum,recvFrom);
                String sendSeq = Integer.toString((int)Math.floor(seqNum));
                out.writeBytes(sendSeq+"\n");
                Log.v("ST--sendProposedSeqNum", "Sending propSeqNum for "+Message+" as "+sendSeq);
            }
            catch(IOException io){
                Log.e("Exception Thrown","Exception Thrown");
                Log.e("sendProposedSeqNum","Exception Thrown");
            }
        }
        void getSeqNum(String finalSeq,String Message){
                float prevSeqNum = seqTag.get(Message);
                int recvFromTemp =  recvFromTag.get(prevSeqNum);
                Float seqNum1 = Float.parseFloat(finalSeq);
                Log.v("ST--getSeqNum","Removing "+prevSeqNum+" from Priority Queue");
                initialPQ.remove(prevSeqNum);
                Log.v("ST--getSeqNum","Inserting "+seqNum1+" in Priority Queue");
                initialPQ.add(seqNum1);
                recvFromTag.remove(prevSeqNum);
                recvFromTag.put(seqNum1,recvFromTemp);
                Log.v("ST--getSeqNum","Removing "+Message+" from Priority Queue");
                seqTag.remove(Message);
                Log.v("ST--getSeqNum","Inserting "+seqNum1+" in seqTag");
                seqTag.put(Message,seqNum1);
                Log.v("ST--getSeqNum","Removing "+prevSeqNum+" in seqTagRev");
                seqTagRev.remove(prevSeqNum);
                Log.v("ST--getSeqNum","Inserting finalSeq"+seqNum1+" in seqTagRev for"+Message);
                seqTagRev.put(seqNum1,Message);
                Log.v("ST--getSeqNum","Recvd finalSeqNum for "+Message+" as "+seqNum1);
        }
        protected void deliverMessages(String message,int skipServerID1){
            Log.v("Remove from message Tag",message);
            messageTag.remove(message);
            Log.v("Insert in message Tag",message);
            messageTag.put(message,"Y");
            String pqMessage="";
//          Log.v("initialPQ.peek()",seqTagRev.get(initialPQ.peek()));
            printDataStructures(messageTag,seqTag,seqTagRev);
            Log.v("St--skipServerID", Integer.toString(skipServerID1));
            while(!initialPQ.isEmpty()) {
              Log.v("ST--deliverMessage","Not Empty size is "+initialPQ.size());
                //if(recvFromTag.containsKey(initialPQ.peek())){
                try {

                    if (recvFromTag.get(initialPQ.peek()) == skipServerID1) {
                        pqMessage = seqTagRev.get(initialPQ.poll());
                        Log.v("Remove from message Tag", message);
                        messageTag.remove(pqMessage);
                        Log.v("ST--NdeliverMessages", pqMessage);
                        //publishProgress(pqMessage);
                    }//}
                    else if (!(messageTag.get(seqTagRev.get(initialPQ.peek()))).equals("N")) {
                        pqMessage = seqTagRev.get(initialPQ.poll());
                        Log.v("Remove from message Tag", message);
                        messageTag.remove(pqMessage);
                        Log.v("ST--deliverMessages", pqMessage);
                        publishProgress(pqMessage);
                    } else
                        break;
                }
                catch(NullPointerException ne){
                    Log.v("recvFromTag","initialPQ.peek()"+" "+"skipServerID1");
                    Log.v(Integer.toString((recvFromTag.get(initialPQ.peek()))), initialPQ.peek()+" "+Integer.toString(skipServerID1));
                    Log.v("ST-deliverMessagesExcep","--");
                    ne.printStackTrace();
                }
            }
        }
        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            int skipServerID1 =-1;
            while(true) {
                try {
                    Socket serverS = serverSocket.accept();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(serverS.getInputStream()));
                    DataOutputStream out = new DataOutputStream(serverS.getOutputStream());
                    String receivedMessage=bufferedReader.readLine();
                    Log.v("------------","-----------");
                    Log.v("ST--doInBackground", "Received new msg as " + receivedMessage);
                    String delim ="aDel";
                    String messageType="",message = "",finalSeq="" ;
                    if(receivedMessage!=null && !receivedMessage.isEmpty()) {
                        String tokens[] = receivedMessage.split(delim);
                        messageType = tokens[0];
                        message = tokens[1];
                        int recvFrom = -1;
                        if(Integer.parseInt(tokens[3])!=-1)
                        skipServerID1 = Integer.parseInt(tokens[3]);

                        if (messageType.equals("sendProp")) {
                            recvFrom = Integer.parseInt(tokens[2]);

                            Log.v("ST--doInBackground", "Received message type as: " + messageType + "& msg as " + message);
                            if (!message.isEmpty()) {

                                sendProposedSeqNum(out, message, recvFrom);
                                printDataStructures(messageTag,seqTag,seqTagRev);
                            }
                        } else if (messageType.equals("sendFinalSeq")) {
                            finalSeq = tokens[2];
                            getSeqNum(finalSeq, message);
                          //  publishProgress(message);
                            deliverMessages(message,skipServerID1);
                        }
                    }
                    out.flush();
                    out.close();
                    bufferedReader.close();
                }
                    catch (IOException e) {
                        Log.e("Exception Thrown","Exception Thrown");
                    Log.e("Server Task","Server IO Exception"+ e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        protected void onProgressUpdate(String... strings) {
            String strReceived = strings[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(strReceived + "\t\n"+"\n");
            Log.v("ST--Publish Progress ","Publish Progress-1");
            ContentValues keyValueToInsert = new ContentValues();
            Uri uri = buildUri("content","edu.buffalo.cse.cse486586.groupmessenger2.provider");
            Log.v("ST--Publish Progress ","Publish Progress-2");
            keyValueToInsert.put("key", Integer.toString(counter));
            keyValueToInsert.put("value", strReceived);
            getContentResolver().insert(uri, keyValueToInsert);
            Log.v("ST--Publish Progress ","Publish Progress End");
            counter++;
            Log.v("------------","--------------");
        }
    }

    class ClientTask extends AsyncTask<String, Void, Void> {
        float maxSeqNum =0;
        void sendInitialMsg(DataOutputStream out,String msgToSend) {
            try {
                String delim = "aDel";
                String sendProp ="sendProp";
                out.writeBytes(sendProp+delim+msgToSend+delim+clientID+delim+skipServerID+"\n");
                out.flush();
                Log.v("CT--sendInitialMsg" ,"Sending msg: "+msgToSend+"cID "+clientID);
            } catch (IOException io) {
                Log.e("Exception Thrown","Exception Thrown");
                Log.e("CTdoInBackground" ,"sendMsg Exception Caught");

            }
        }
        void getProposedSeqNum(BufferedReader in,int serverID,int serverID1){
            float x=0;
            float id= 1+serverID1;
            try{
                Log.v("CT--getProposedSeqNum" ,"Receving Prop Seq from serverID: "+serverID);
                String propSeq =in.readLine();
                if(propSeq==null || propSeq.isEmpty()) {
                    skipServerID = serverID;
                    Log.v("Skipping Server ID ",Integer.toString(serverID));
                }
                else {
                    x = parseFloat(propSeq) + (id / 10);
                    Log.v("CT--getProposedSeqNum", "Received the Prop Seq from serverID: " + serverID + " as " + x);
                }
            }
            catch(IOException io){
                Log.e("Exception Thrown","Exception Thrown");
                Log.e("getProposedSeqNum","getProposedSeqNum");
            }
            if(x > maxSeqNum)
                maxSeqNum =x;
        }
        void sendFinalMsg(DataOutputStream out,String msgToSend){
            try {
                String sendProp ="sendFinalSeq"+"aDel"+msgToSend;
                String finalSeq ="aDel"+Float.toString(maxSeqNum+1);
                out.writeBytes(sendProp+finalSeq+"aDel"+skipServerID+"\n");
                out.flush();
                Log.v("CT--sendFinalMsg" ,"Sending finalSeq for "+msgToSend+" as "+finalSeq);
            } catch (IOException io) {
                io.printStackTrace();
                Log.e("Exception Thrown","Exception Thrown");
                Log.e("SendingFinalSeq","SendingFinalSeq");
            }
        }
        @Override
        protected Void doInBackground(String... msgs) {
            try {
                String remotePort[] = {REMOTE_PORT0,REMOTE_PORT1,REMOTE_PORT2,REMOTE_PORT3,REMOTE_PORT4};
                String msgToSend = msgs[0];
                OutputStream outToServer;
                InputStream inToClient;
                DataOutputStream out;
                BufferedReader in;
                for(int i=0;i<5 ;i++) {
                    if(Integer.parseInt(remotePort[i])!=skipServerID) {
                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(remotePort[i]));
                        outToServer = socket.getOutputStream();
                        out = new DataOutputStream(outToServer);
                        inToClient = socket.getInputStream();
                        in = new BufferedReader(new InputStreamReader(inToClient));
                        sendInitialMsg(out, msgToSend);
                        getProposedSeqNum(in, Integer.parseInt(remotePort[i]),i);
                        out.close();
                    }
                }
                for(int i=0;i<5 ;i++) {
                    if(Integer.parseInt(remotePort[i])!=skipServerID) {
                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(remotePort[i]));
                        outToServer = socket.getOutputStream();
                        out = new DataOutputStream(outToServer);
                        sendFinalMsg(out, msgToSend);
                        Log.v("CT--doInBackground", "Sleeping for 100 ms");
                        try {
                            Thread.sleep(400);
                            out.close();
                        } catch (InterruptedException ie) {

                      }
                    }
                }
            } catch (UnknownHostException e) {
                Log.e("Exception Thrown","Exception Thrown");
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e("Exception Thrown","Exception Thrown");
                Log.e(TAG, "ClientTask socket IOException");
                e.printStackTrace();
            }
            return null;
        }
    }

}