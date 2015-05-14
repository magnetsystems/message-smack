Magnet Smack
============

Magnet Smack is based on Smack v4.0.7 with enhancements from Magnet Systems Inc.
Please refer to README.md for details on Smack.  The enhancements done by Magnet
Systems are:

  - Improve performance for writing packets larger than 1MB in mobile devices.
    The original PacketWriter constructed a single buffer for the entire packet
    and its performance was poor especially in aSmack.  It fragmented the heap
    and upset the GC.  The enhancement is to write XML fragments directly to
    socket using an improved buffering scheme.

  - Provide a mechanism to dispose XML fragment if it is backed by a file after
    the packet was sent successfully or unable to be sent.

  - Provide additional callbacks when a packet was sent successfully or unable
    to be sent.
