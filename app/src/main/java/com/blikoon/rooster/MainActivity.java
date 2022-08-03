package com.blikoon.rooster;

import android.os.AsyncTask;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jxmpp.stringprep.XmppStringprepException;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MyLoginTask task = new MyLoginTask();
        task.execute("");
    }
    private class MyLoginTask extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... params) {
            // Create a connection to the jabber.org server.
            XMPPTCPConnectionConfiguration config = null;
            try {
                config = XMPPTCPConnectionConfiguration.builder()

                        .setUsernameAndPassword("888", "888")
                        .setHost("jitsi.indahonline.com")
                        .setResource("stork")
                        .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
                       // .setServiceName("localhost")
                       // .setPort(5222)
                        .setDebuggerEnabled(true) // to view what's happening in detail
                        .build();
            } catch (XmppStringprepException e) {
                e.printStackTrace();
            }

            AbstractXMPPConnection conn1 = new XMPPTCPConnection(config);
            try {
                conn1.connect();
                if(conn1.isConnected()) {
                    Log.w("app", "conn done");
                }
                conn1.login();

                if(conn1.isAuthenticated()) {
                    Log.w("app", "Auth done");
                }
            }
            catch (Exception e) {
                Log.w("app", e.toString());
            }

            return "";
        }


        @Override
        protected void onPostExecute(String result) {
        }

    }
}