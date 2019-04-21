/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alipay.sofa.jraft.entity.codec.v2;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alipay.sofa.jraft.JRaftUtils;
import com.alipay.sofa.jraft.entity.LogEntry;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.entity.codec.LogEntryDecoder;
import com.alipay.sofa.jraft.entity.codec.v2.LogOutter.PBLogEntry;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.ZeroByteStringHelper;

/**
 * V2 log entry decoder based on protobuf, see src/main/resources/log.proto
 * @author boyan(boyan@antfin.com)
 *
 */
public class V2Decoder implements LogEntryDecoder {

    private V2Decoder() {

    }

    private static final Logger   LOG      = LoggerFactory.getLogger(V2Decoder.class);
    public static final V2Decoder INSTANCE = new V2Decoder();

    @Override
    public LogEntry decode(final byte[] bs) {

        if (bs == null || bs.length < LogEntryV2CodecFactory.HEADER_SIZE) {
            return null;
        }

        int i = 0;
        for (; i < LogEntryV2CodecFactory.MAGIC_BYTES.length; i++) {
            if (bs[i] != LogEntryV2CodecFactory.MAGIC_BYTES[i]) {
                return null;
            }
        }

        if (bs[i++] != LogEntryV2CodecFactory.VERSION) {
            return null;
        }
        // Ignored reserved
        i += LogEntryV2CodecFactory.RESERVED.length;
        try {

            PBLogEntry entry = PBLogEntry.parseFrom(ZeroByteStringHelper.wrap(bs, i, bs.length - i));

            LogEntry log = new LogEntry();
            log.setType(entry.getType());
            log.getId().setIndex(entry.getIndex());
            log.getId().setTerm(entry.getTerm());

            if (entry.hasChecksum()) {
                log.setChecksum(entry.getChecksum());
            }
            if (entry.getPeersCount() > 0) {
                List<PeerId> peers = new ArrayList<>(entry.getPeersCount());
                for (String s : entry.getPeersList()) {
                    peers.add(JRaftUtils.getPeerId(s));
                }
                log.setPeers(peers);
            }
            if (entry.getOldPeersCount() > 0) {
                List<PeerId> peers = new ArrayList<>(entry.getOldPeersCount());
                for (String s : entry.getOldPeersList()) {
                    peers.add(JRaftUtils.getPeerId(s));
                }
                log.setOldPeers(peers);
            }

            if (!entry.getData().isEmpty()) {
                log.setData(ByteBuffer.wrap(entry.getData().toByteArray()));
            }

            return log;
        } catch (InvalidProtocolBufferException e) {
            LOG.error("Fail to decode pb log entry", e);
            return null;
        }
    }

}
