/**
 *
 * Copyright 2003-2006 Jive Software.
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
package org.jivesoftware.smackx.filetransfer;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.filter.OrFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.si.packet.StreamInitiation;
import org.jxmpp.jid.Jid;


/**
 * The fault tolerant negotiator takes two stream negotiators, the primary and the secondary
 * negotiator. If the primary negotiator fails during the stream negotiaton process, the second
 * negotiator is used.
 */
public class FaultTolerantNegotiator extends StreamNegotiator {

    private final StreamNegotiator primaryNegotiator;
    private final StreamNegotiator secondaryNegotiator;
    private final XMPPConnection connection;
    private PacketFilter primaryFilter;
    private PacketFilter secondaryFilter;

    public FaultTolerantNegotiator(XMPPConnection connection, StreamNegotiator primary,
            StreamNegotiator secondary) {
        this.primaryNegotiator = primary;
        this.secondaryNegotiator = secondary;
        this.connection = connection;
    }

    public PacketFilter getInitiationPacketFilter(Jid from, String streamID) {
        if (primaryFilter == null || secondaryFilter == null) {
            primaryFilter = primaryNegotiator.getInitiationPacketFilter(from, streamID);
            secondaryFilter = secondaryNegotiator.getInitiationPacketFilter(from, streamID);
        }
        return new OrFilter(primaryFilter, secondaryFilter);
    }

    InputStream negotiateIncomingStream(Stanza streamInitiation) {
        throw new UnsupportedOperationException("Negotiation only handled by create incoming " +
                "stream method.");
    }

    final Stanza initiateIncomingStream(XMPPConnection connection, StreamInitiation initiation) {
        throw new UnsupportedOperationException("Initiation handled by createIncomingStream " +
                "method");
    }

    public InputStream createIncomingStream(StreamInitiation initiation) throws SmackException, InterruptedException {
        PacketCollector collector = connection.createPacketCollectorAndSend(
                        getInitiationPacketFilter(initiation.getFrom(), initiation.getSessionID()),
                        super.createInitiationAccept(initiation, getNamespaces()));

        ExecutorService threadPoolExecutor = Executors.newFixedThreadPool(2);
        CompletionService<InputStream> service
                = new ExecutorCompletionService<InputStream>(threadPoolExecutor);
        List<Future<InputStream>> futures = new ArrayList<Future<InputStream>>();
        InputStream stream = null;
        SmackException exception = null;
        try {
            futures.add(service.submit(new NegotiatorService(collector)));
            futures.add(service.submit(new NegotiatorService(collector)));

            int i = 0;
            while (stream == null && i < futures.size()) {
                Future<InputStream> future;
                try {
                    i++;
                    future = service.poll(connection.getPacketReplyTimeout(), TimeUnit.MILLISECONDS);
                }
                catch (InterruptedException e) {
                    continue;
                }

                if (future == null) {
                    continue;
                }

                try {
                    stream = future.get();
                }
                catch (InterruptedException e) {
                    /* Do Nothing */
                }
                catch (ExecutionException e) {
                    exception = new SmackException(e.getCause());
                }
            }
        }
        finally {
            for (Future<InputStream> future : futures) {
                future.cancel(true);
            }
            collector.cancel();
            threadPoolExecutor.shutdownNow();
        }
        if (stream == null) {
            if (exception != null) {
                throw exception;
            }
            else {
                throw new SmackException("File transfer negotiation failed.");
            }
        }

        return stream;
    }

    private StreamNegotiator determineNegotiator(Stanza streamInitiation) {
        return primaryFilter.accept(streamInitiation) ? primaryNegotiator : secondaryNegotiator;
    }

    public OutputStream createOutgoingStream(String streamID, Jid initiator, Jid target)
                    throws SmackException, XMPPException, InterruptedException {
        OutputStream stream;
        try {
            stream = primaryNegotiator.createOutgoingStream(streamID, initiator, target);
        }
        catch (Exception ex) {
            stream = secondaryNegotiator.createOutgoingStream(streamID, initiator, target);
        }

        return stream;
    }

    public String[] getNamespaces() {
        String[] primary = primaryNegotiator.getNamespaces();
        String[] secondary = secondaryNegotiator.getNamespaces();

        String[] namespaces = new String[primary.length + secondary.length];
        System.arraycopy(primary, 0, namespaces, 0, primary.length);
        System.arraycopy(secondary, 0, namespaces, primary.length, secondary.length);

        return namespaces;
    }

    private class NegotiatorService implements Callable<InputStream> {

        private PacketCollector collector;

        NegotiatorService(PacketCollector collector) {
            this.collector = collector;
        }

        public InputStream call() throws XMPPErrorException, InterruptedException, NoResponseException, SmackException {
            Stanza streamInitiation = collector.nextResultOrThrow();
            StreamNegotiator negotiator = determineNegotiator(streamInitiation);
            return negotiator.negotiateIncomingStream(streamInitiation);
        }
    }
}
