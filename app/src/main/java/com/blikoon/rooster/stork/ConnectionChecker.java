package com.blikoon.rooster.stork;


import android.content.Context;
import android.util.Log;
import tigase.jaxmpp.android.Jaxmpp;
import tigase.jaxmpp.core.client.*;
import tigase.jaxmpp.core.client.connector.StreamError;
import tigase.jaxmpp.core.client.eventbus.Event;
import tigase.jaxmpp.core.client.eventbus.EventHandler;
import tigase.jaxmpp.core.client.eventbus.EventListener;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xmpp.modules.ResourceBinderModule;
import tigase.jaxmpp.core.client.xmpp.modules.auth.AuthModule;
import tigase.jaxmpp.core.client.xmpp.modules.auth.SaslModule;
import tigase.jaxmpp.core.client.xmpp.modules.registration.InBandRegistrationModule;
import tigase.jaxmpp.core.client.xmpp.stanzas.IQ;
import tigase.jaxmpp.j2se.connectors.socket.SocketConnector;

public class ConnectionChecker {

}
