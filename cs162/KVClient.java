/**
 * Client component for generating load for the KeyValue store. 
 * This is also used by the Master server to reach the slave nodes.
 * 
 * @author Mosharaf Chowdhury (http://www.mosharaf.com)
 * @author Prashanth Mohan (http://www.cs.berkeley.edu/~prmohan)
 * 
 * Copyright (c) 2012, University of California at Berkeley
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *  * Neither the name of University of California, Berkeley nor the
 *    names of its contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *    
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY
 *  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package cs162;

import java.net.Socket;
import java.net.UnknownHostException;
import java.io.*;


/**
 * This class is used to communicate with (appropriately marshalling and unmarshalling) 
 * objects implementing the {@link KeyValueInterface}.
 *
 * @param <K> Java Generic type for the Key
 * @param <V> Java Generic type for the Value
 */
public class KVClient {

    private String server = null;
    private int port = 0;
    
    /**
     * @param server is the DNS reference to the Key-Value server
     * @param port is the port on which the Key-Value server is listening
     */
    public KVClient(String server, int port) {
        this.server = server;
        this.port = port;
    }

    protected Socket newSocket(String server, int port) throws IOException {
        return new Socket(server, port);
    }
    
    private Socket connectHost() throws KVException {
        try {
            return newSocket(this.server, this.port);
        } catch (UnknownHostException ignored) {
            throw new KVException("Network Error: Could not connect");
        } catch (IOException ignored) {
            throw new KVException("Network Error: Could not create socket");
        }
    }
    
    private void closeHost(Socket sock) throws KVException {
        try {
            sock.close();
        } catch (IOException ignored) {
            throw new KVException("Unknown Error: Could not close connection to host");
        }
    }

    public static void sanitize(String key) throws KVException {
        if (key == null)
            throw new KVException("Unknown Error: key is null");

        if (key.length() == 0)
            throw new KVException("Unknown Error: 0-length key");

        if (key.length() > 256)
            throw new KVException("Oversized key");
    }

    public static void sanitize(String key, String value) throws KVException {
        if (value == null)
            throw new KVException("Unknown Error: value is null");

        if (value.length() == 0)
            throw new KVException("Unknown Error: 0-length value");

        sanitize(key);
        if (value.length() > 256 * 1024)
            throw new KVException("Oversized value");
    }
    
    protected KVMessage request(String type, String key, String value) throws KVException {
        if (value != null)
            sanitize(key, value);
        else
            sanitize(key);

        KVMessage request = new KVMessage(type);
        request.setKey(key);
        request.setValue(value);
        Socket sock = this.connectHost();
        request.sendMessage(sock);

        KVMessage response = null;
        try {
            response = new KVMessage(sock.getInputStream());
        } catch (IOException ignored) {
            System.out.println(ignored);
            throw new KVException("Unknown Error: Couldn't open input stream");
        }

        if (response.getMessage() != null
            && !response.getMessage().equals("Success")) {
            throw new KVException(response);
        }

        this.closeHost(sock);
        return response;
    }

    private KVMessage request(String type, String key) throws KVException {
        // note: depending on KVMessage chose to represent empty field as null
        return this.request(type, key, null);
    }

    public void put(String key, String value) throws KVException {
        this.request("putreq", key, value);
    }


    public String get(String key) throws KVException {
        // TODO: what to do if no value present in remote
        return this.request("getreq", key).getValue();
    }
    
    public void del(String key) throws KVException {
        this.request("delreq", key);
    }
}
