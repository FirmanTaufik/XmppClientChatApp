package com.blikoon.rooster;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.chat2.IncomingChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Updated by gakwaya on Oct/08/2017.
 */
public class RoosterConnection implements ConnectionListener {
    private final static char[] DEFAULT_PASSWORD = "Tigase".toCharArray();

    private static final String TAG = "RoosterConnection";

    private  final Context mApplicationContext;
    private  final String mUsername;
    private  final String mPassword;
    private  final String mServiceName;
    private XMPPTCPConnection mConnection;
    private BroadcastReceiver uiThreadMessageReceiver;//Receives messages from the ui thread.
    private File keyStoreFile ;

    private KeyStore keyStore;
    public static enum ConnectionState
    {
        CONNECTED ,AUTHENTICATED, CONNECTING ,DISCONNECTING ,DISCONNECTED;
    }

    public static enum LoggedInState
    {
        LOGGED_IN , LOGGED_OUT;
    }


    public RoosterConnection( Context context)
    {
        Log.d(TAG,"RoosterConnection Constructor called.");
        mApplicationContext = context.getApplicationContext();
        String jid = PreferenceManager.getDefaultSharedPreferences(mApplicationContext)
                .getString("xmpp_jid",null);
        mPassword = PreferenceManager.getDefaultSharedPreferences(mApplicationContext)
                .getString("xmpp_password",null);

        if( jid != null)
        {
            mUsername = jid.split("@")[0];
            mServiceName = jid.split("@")[1];
        }else
        {
            mUsername ="";
            mServiceName="";
        }
    }


    public void connect() throws IOException,XMPPException,SmackException
    {
        Log.d(TAG, "Connecting to server " + mServiceName);
        HostnameVerifier hostnameVerifier = new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                HostnameVerifier hv =
                        HttpsURLConnection.getDefaultHostnameVerifier();
                return hv.verify(mServiceName, session);
            }
        };
        // Load CAs from an InputStream
// (could be from a resource or ByteArrayInputStream or ...)
//        CertificateFactory cf = null;
//        Certificate ca = null;
//        try {
//            cf = CertificateFactory.getInstance("X.509");
//        } catch (CertificateException e) {
//            e.printStackTrace();
//        }
//// From https://www.washington.edu/itconnect/security/ca/load-der.crt
//        InputStream caInput = new BufferedInputStream(new FileInputStream("load-der.crt"));
//        try {
//            try {
//                ca = cf.generateCertificate(caInput);
//                System.out.println("ca=" + ((X509Certificate) ca).getSubjectDN());
//            } catch (CertificateException e) {
//                e.printStackTrace();
//            }
//        } finally {
//            caInput.close();
//        }
//
//// Create a KeyStore containing our trusted CAs
//        String keyStoreType = KeyStore.getDefaultType();
//        KeyStore keyStore = null;
//        try {
//            keyStore = KeyStore.getInstance(keyStoreType);
//        } catch (KeyStoreException e) {
//            e.printStackTrace();
//        }
//        try {
//            keyStore.load(null, null);
//            try {
//                keyStore.setCertificateEntry("ca", ca);
//            } catch (KeyStoreException e) {
//                e.printStackTrace();
//            }
//        } catch (NoSuchAlgorithmException e) {
//            e.printStackTrace();
//        } catch (CertificateException e) {
//            e.printStackTrace();
//        }
//
//// Create a TrustManager that trusts the CAs in our KeyStore
//        String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
//        TrustManagerFactory tmf = null;
//        try {
//            tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
//            try {
//                tmf.init(keyStore);
//            } catch (KeyStoreException e) {
//                e.printStackTrace();
//            }
//        } catch (NoSuchAlgorithmException e) {
//            e.printStackTrace();
//        }
//
//// Create an SSLContext that uses our TrustManager
//        SSLContext context = null;
//        try {
//            context = SSLContext.getInstance("TLS");
//        } catch (NoSuchAlgorithmException e) {
//            e.printStackTrace();
//        }
//        try {
//            context.init(null, tmf.getTrustManagers(), null);
//        } catch (KeyManagementException e) {
//            e.printStackTrace();
//        }

        handleSSLHandshake();
        XMPPTCPConnectionConfiguration.Builder builder = XMPPTCPConnectionConfiguration.builder()
                .setXmppDomain(mServiceName)
                .setHost("jitsi.indahonline.com")
                .setHostnameVerifier(hostnameVerifier)
               // .setCustomX509TrustManager(getCustom())
                .setResource("stork")

                //Was facing this issue
                //https://discourse.igniterealtime.org/t/connection-with-ssl-fails-with-java-security-keystoreexception-jks-not-found/62566
          //      .setKeystoreType(null) //This line seems to get rid of the problem
        .setKeystoreType(null)
        .setDebuggerEnabled(true)

                .setSecurityMode(ConnectionConfiguration.SecurityMode.required)
                .setCompressionEnabled(true) ;
//        try {
//              keyStore = configKeyStore(builder);
//            try {
//                configSSLContext(builder, keyStore);
//            } catch (NoSuchAlgorithmException e) {
//                Log.d(TAG, "connect: "+e.getMessage());
//                e.printStackTrace();
//            } catch (KeyManagementException e) {
//                Log.d(TAG, "connect: "+e.getMessage());
//                e.printStackTrace();
//            }
//
//        } catch (KeyStoreException e) {
//            e.printStackTrace();
//        }

        Log.d(TAG, "Username : "+mUsername);
        Log.d(TAG, "Password : "+mPassword);
        Log.d(TAG, "Server : "+mServiceName);


        //Set up the ui thread broadcast message receiver.
        setupUiThreadBroadCastMessageReceiver();

        mConnection = new XMPPTCPConnection(builder.build());
        mConnection.addConnectionListener(this);
        try {
            Log.d(TAG, "Calling connect() ");
            mConnection.connect();
            mConnection.login(mUsername,mPassword);
            Log.d(TAG, " login() Called ");
        } catch (InterruptedException e) {
            Log.d(TAG, "connect: "+e.getMessage());
            e.printStackTrace();
        }

        ChatManager.getInstanceFor(mConnection).addIncomingListener(new IncomingChatMessageListener() {
            @Override
            public void newIncomingMessage(EntityBareJid messageFrom, Message message, Chat chat) {
                ///ADDED
                Log.d(TAG,"message.getBody() :"+message.getBody());
                Log.d(TAG,"message.getFrom() :"+message.getFrom());

                String from = message.getFrom().toString();

                String contactJid="";
                if ( from.contains("/"))
                {
                    contactJid = from.split("/")[0];
                    Log.d(TAG,"The real jid is :" +contactJid);
                    Log.d(TAG,"The message is from :" +from);
                }else
                {
                    contactJid=from;
                }

                //Bundle up the intent and send the broadcast.
                Intent intent = new Intent(RoosterConnectionService.NEW_MESSAGE);
                intent.setPackage(mApplicationContext.getPackageName());
                intent.putExtra(RoosterConnectionService.BUNDLE_FROM_JID,contactJid);
                intent.putExtra(RoosterConnectionService.BUNDLE_MESSAGE_BODY,message.getBody());
                mApplicationContext.sendBroadcast(intent);
                Log.d(TAG,"Received message from :"+contactJid+" broadcast sent.");
                ///ADDED

            }
        });


        ReconnectionManager reconnectionManager = ReconnectionManager.getInstanceFor(mConnection);
        reconnectionManager.setEnabledPerDefault(true);
        reconnectionManager.enableAutomaticReconnection();

    }

    private X509TrustManager getCustom() {


        TrustManagerFactory tmf = null;
        try {
            tmf = TrustManagerFactory
                    .getInstance(TrustManagerFactory.getDefaultAlgorithm());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
// Using null here initialises the TMF with the default trust store.
        try {
            loadKeystore(mApplicationContext.getResources().openRawResource(R.raw.trust_store_bks), null);
            loadKeystore(System.getProperty("javax.net.ssl.trustStore"));
              keyStoreFile = new File(
                    mApplicationContext.getApplicationContext().getDir("TrustStore", Context.MODE_PRIVATE) + File.separator +
                            "TrustStore.bks");
            loadKeystore(keyStoreFile, DEFAULT_PASSWORD);

            tmf.init((KeyStore) null);
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }

// Get hold of the default trust manager
        X509TrustManager x509Tm = null;
        for (TrustManager tm : tmf.getTrustManagers()) {
            if (tm instanceof X509TrustManager) {
                x509Tm = (X509TrustManager) tm;
                break;
            }
        }

// Wrap it in your own class.
        final X509TrustManager finalTm = x509Tm;
        X509TrustManager customTm = new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return finalTm.getAcceptedIssuers();
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain,
                                           String authType) throws CertificateException {
                finalTm.checkServerTrusted(chain, authType);
            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain,
                                           String authType) throws CertificateException {
                finalTm.checkClientTrusted(chain, authType);
            }
        };

        return customTm;
    }

    private void loadKeystore(File file, char[] password) {
        try {
            Log.d(TAG, "Loading keystore from " + file);
            InputStream in = new FileInputStream(file);
            loadKeystore(in, password);
        } catch (Exception e1) {
            Log.w(TAG, "Can't load keystore from file " + file);
        }

    }

    private void loadKeystore(InputStream in, char[] password) {
        try {
            try {
                keyStore.load(in, password);
            } finally {
                in.close();
            }
        } catch (Exception e1) {
            Log.w(TAG, "Can't load keystore from stream");
        }

    }

    private void loadKeystore(String file) {
        try {
            loadKeystore(new File(file), null);
        } catch (NullPointerException e) {
            Log.w(TAG, "Can't load keystore from file " + file);
        }
    }

//    private void storeKeystore(File file) {
//        try {
//            OutputStream out = new FileOutputStream(file);
//            try {
//                keyStore.store(out, DEFAULT_PASSWORD);
//            } finally {
//                out.close();
//            }
//        } catch (Exception e1) {
//            Log.w(TAG, "Can't store keystore to file " + file);
//        }
//
//    }

    private void setupUiThreadBroadCastMessageReceiver()
    {
        uiThreadMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //Check if the Intents purpose is to send the message.
                String action = intent.getAction();
                if( action.equals(RoosterConnectionService.SEND_MESSAGE))
                {
                    //Send the message.
                    sendMessage(intent.getStringExtra(RoosterConnectionService.BUNDLE_MESSAGE_BODY),
                            intent.getStringExtra(RoosterConnectionService.BUNDLE_TO));
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(RoosterConnectionService.SEND_MESSAGE);
        mApplicationContext.registerReceiver(uiThreadMessageReceiver,filter);

    }

    private void sendMessage ( String body ,String toJid)
    {
        Log.d(TAG,"Sending message to :"+ toJid);

        EntityBareJid jid = null;


        ChatManager chatManager = ChatManager.getInstanceFor(mConnection);

        try {
            jid = JidCreate.entityBareFrom(toJid);
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }
        Chat chat = chatManager.chatWith(jid);
        try {
            Message message = new Message(jid, Message.Type.chat);
            message.setBody(body);
            chat.send(message);

        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public void disconnect()
    {
        Log.d(TAG,"Disconnecting from serser "+ mServiceName);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mApplicationContext);
        prefs.edit().putBoolean("xmpp_logged_in",false).commit();


        if (mConnection != null)
        {
            mConnection.disconnect();
        }

        mConnection = null;
        // Unregister the message broadcast receiver.
        if( uiThreadMessageReceiver != null)
        {
            mApplicationContext.unregisterReceiver(uiThreadMessageReceiver);
            uiThreadMessageReceiver = null;
        }

    }


    @Override
    public void connected(XMPPConnection connection) {
        RoosterConnectionService.sConnectionState=ConnectionState.CONNECTED;
        Log.d(TAG,"Connected Successfully");

    }

    @Override
    public void authenticated(XMPPConnection connection, boolean resumed) {
        RoosterConnectionService.sConnectionState=ConnectionState.CONNECTED;
        Log.d(TAG,"Authenticated Successfully");
        showContactListActivityWhenAuthenticated();
    }


    @Override
    public void connectionClosed() {
        RoosterConnectionService.sConnectionState=ConnectionState.DISCONNECTED;
        Log.d(TAG,"Connectionclosed()");

    }

    @Override
    public void connectionClosedOnError(Exception e) {
        RoosterConnectionService.sConnectionState=ConnectionState.DISCONNECTED;
        Log.d(TAG,"ConnectionClosedOnError, error "+ e.toString());

    }

    @Override
    public void reconnectingIn(int seconds) {
        RoosterConnectionService.sConnectionState = ConnectionState.CONNECTING;
        Log.d(TAG,"ReconnectingIn() ");

    }

    @Override
    public void reconnectionSuccessful() {
        RoosterConnectionService.sConnectionState = ConnectionState.CONNECTED;
        Log.d(TAG,"ReconnectionSuccessful()");

    }

    @Override
    public void reconnectionFailed(Exception e) {
        RoosterConnectionService.sConnectionState = ConnectionState.DISCONNECTED;
        Log.d(TAG,"ReconnectionFailed()");

    }

    private void showContactListActivityWhenAuthenticated()
    {
        Intent i = new Intent(RoosterConnectionService.UI_AUTHENTICATED);
        i.setPackage(mApplicationContext.getPackageName());
        mApplicationContext.sendBroadcast(i);
        Log.d(TAG,"Sent the broadcast that we are authenticated");
    }

    private KeyStore configKeyStore(XMPPTCPConnectionConfiguration.Builder builder) throws KeyStoreException {
        KeyStore keyStore;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            builder.setKeystorePath(null);
            builder.setKeystoreType("AndroidCAStore");
            keyStore = KeyStore.getInstance("AndroidCAStore");
        } else {
            builder.setKeystoreType("BKS");
            keyStore = KeyStore.getInstance("BKS");

            String path = System.getProperty("javax.net.ssl.trustStore");
            if (path == null)
                path = System.getProperty("java.home") + File.separator + "etc"
                        + File.separator + "security" + File.separator
                        + "cacerts.bks";
            builder.setKeystorePath(path);
        }
        return keyStore;
    }

    private void configSSLContext(XMPPTCPConnectionConfiguration.Builder builder, KeyStore keyStore) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        TrustManagerFactory trustManagerFactory = TrustManagerFactory
                .getInstance(KeyManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());

        builder.setCustomSSLContext(sslContext);
    }
    public void handleSSLHandshake() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                @Override
                public void checkClientTrusted(X509Certificate[] certs, String authType) {

                }

                @Override
                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }};

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((arg0, arg1) -> true);
        } catch (Exception ignored) {
        }
    }

}
