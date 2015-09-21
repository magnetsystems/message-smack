/**
 *
 * Copyright (c) 2015 Magnet Systems, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jivesoftware.smack.sasl;
//package com.magnet.mmx.client.common;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslException;

import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.sasl.SASLMechanism;
import org.jivesoftware.smack.util.Base64;

/**
 * The implementation of Magnet Message Server OAuth.  It is a token based
 * authentication.
 */
public class MMXBFOAuthMechanism extends SASLMechanism {
  /**
   * This name needs to match exactly with what server is using.
   */
  public static final String NAME = "X-MMX_BF_OAUTH2";

  public MMXBFOAuthMechanism(SASLAuthentication saslAuthentication) {
    super(saslAuthentication);
  }

  @Override
  protected String getName() {
    return NAME;
  }

  @Override
  public void authenticate(String host, CallbackHandler cbh)
      throws IOException, SaslException,
      SmackException.NotConnectedException {
    String[] mechanisms = { getName() };
    Map<String,String> props = new HashMap<String,String>();
    sc = Sasl.createSaslClient(mechanisms, null, "xmpp", host, props, cbh);
    authenticate();
  }


  /**
   * Construct the auth packet.  Auth packet consists of:
   * base64("\u0000" + user_name + "\u0000" + oauth_token)
   * @throws IOException
   * @throws SaslException
   * @throws SmackException.NotConnectedException
   */
  @Override
  protected void authenticate()
      throws IOException, SaslException, SmackException.NotConnectedException {
    final StringBuilder stanza = new StringBuilder();
    byte response[] = null;
    stanza.append("<auth xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\""
        + "mechanism=\"" + NAME + "\"" + ">");

    String composedResponse = "\u0000" + authenticationId + "\u0000" + password;
    response = composedResponse.getBytes("UTF-8");
    String authenticationText = "";
    if (response != null) {
      authenticationText = Base64.encodeBytes(response,
          Base64.DONT_BREAK_LINES);
    }
    stanza.append(authenticationText);
    stanza.append("</auth>");

    Packet authPacket = new Packet() {

      @Override
      public String toXML() {
        return stanza.toString();
      }
    };
    getSASLAuthentication().send(authPacket);
  }
}
