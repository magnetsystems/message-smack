Magnet Smack
============

Magnet Smack is based on Smack v4.0.7 with enhancements from Magnet Inc.
Please refer to README.md for Smack.  The enhancements done by Magnet are:

  - Improve performance for writing packets larger than 1MB in mobile devices.
    The original PacketWriter constructed a single buffer for the entire packet
    and its performance was bad especially in aSmack.  It fragmented the heap
    and upset the GC.  The enhancement is to write XML fragments directly to
    socket using an improved buffering.

  - Provide a mechanism to dispose XML fragment if it is back by a file after the
    packet was sent successfully or unable to be sent.

  - Provide additional callbacks when a packet was sent successfully or unable
    to be sent.
