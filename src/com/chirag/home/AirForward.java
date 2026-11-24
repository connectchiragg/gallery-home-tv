package com.chirag.home;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/* JADX INFO: loaded from: classes.dex */
final class AirForward {
    volatile int conns;
    final int real;
    volatile InetAddress senderAddr;
    volatile long tcpBytes;
    volatile int udpIn;
    volatile int udpOut;
    volatile boolean open = true;
    volatile int senderPort = -1;
    final ServerSocket tcp = new ServerSocket(0);
    final int port = this.tcp.getLocalPort();
    final DatagramSocket udp = new DatagramSocket(this.port);
    final DatagramSocket toBack = new DatagramSocket();

    AirForward(final int i) throws Exception {
        this.real = i;
        final InetAddress byName = InetAddress.getByName("127.0.0.1");
        new Thread(new Runnable() { // from class: com.chirag.home.AirForward$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                m1lambda$new$1$comchiraghomeAirForward();
            }
        }, "airfwd-tcp").start();
        new Thread(new Runnable() { // from class: com.chirag.home.AirForward$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                m2lambda$new$2$comchiraghomeAirForward(byName, i);
            }
        }, "airfwd-udp-in").start();
        new Thread(new Runnable() { // from class: com.chirag.home.AirForward$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                m3lambda$new$3$comchiraghomeAirForward();
            }
        }, "airfwd-udp-out").start();
    }

    /* JADX INFO: renamed from: lambda$new$1$com-chirag-home-AirForward, reason: not valid java name */
    /* synthetic */ void m1lambda$new$1$comchiraghomeAirForward() {
        while (this.open) {
            try {
                final Socket socketAccept = this.tcp.accept();
                this.conns++;
                new Thread(new Runnable() { // from class: com.chirag.home.AirForward$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        serve(socketAccept);
                    }
                }).start();
            } catch (Exception e) {
                if (!this.open) {
                    return;
                }
            }
        }
    }

    /* JADX INFO: renamed from: lambda$new$2$com-chirag-home-AirForward, reason: not valid java name */
    /* synthetic */ void m2lambda$new$2$comchiraghomeAirForward(InetAddress inetAddress, int i) {
        byte[] bArr = new byte[65536];
        while (this.open) {
            try {
                DatagramPacket datagramPacket = new DatagramPacket(bArr, 65536);
                this.udp.receive(datagramPacket);
                this.senderAddr = datagramPacket.getAddress();
                this.senderPort = datagramPacket.getPort();
                this.toBack.send(new DatagramPacket(bArr, datagramPacket.getLength(), inetAddress, i));
                this.udpIn++;
            } catch (Exception e) {
                if (!this.open) {
                    return;
                }
            }
        }
    }

    /* JADX INFO: renamed from: lambda$new$3$com-chirag-home-AirForward, reason: not valid java name */
    /* synthetic */ void m3lambda$new$3$comchiraghomeAirForward() {
        byte[] bArr = new byte[65536];
        while (this.open) {
            try {
                DatagramPacket datagramPacket = new DatagramPacket(bArr, 65536);
                this.toBack.receive(datagramPacket);
                if (this.senderPort > 0) {
                    this.udp.send(new DatagramPacket(bArr, datagramPacket.getLength(), this.senderAddr, this.senderPort));
                    this.udpOut++;
                }
            } catch (Exception e) {
                if (!this.open) {
                    return;
                }
            }
        }
    }

    public void serve(final Socket socket) {
        try {
            try {
                final Socket socket2 = new Socket();
                try {
                    socket2.connect(new InetSocketAddress("127.0.0.1", this.real), 2000);
                    socket.setTcpNoDelay(true);
                    socket2.setTcpNoDelay(true);
                    Thread thread = new Thread(new Runnable() { // from class: com.chirag.home.AirForward$$ExternalSyntheticLambda4
                        @Override // java.lang.Runnable
                        public final void run() {
                            m4lambda$serve$4$comchiraghomeAirForward(socket2, socket);
                        }
                    });
                    thread.start();
                    copy(socket, socket2, true);
                    thread.join(2000L);
                    socket2.close();
                    if (socket != null) {
                        socket.close();
                    }
                } catch (Throwable th) {
                    try {
                        socket2.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                    throw th;
                }
            } catch (Throwable th3) {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (Throwable th4) {
                        th3.addSuppressed(th4);
                    }
                }
                throw th3;
            }
        } catch (Exception e) {
        }
    }

    /* JADX INFO: renamed from: lambda$serve$4$com-chirag-home-AirForward, reason: not valid java name */
    /* synthetic */ void m4lambda$serve$4$comchiraghomeAirForward(Socket socket, Socket socket2) {
        copy(socket, socket2, false);
    }

    void copy(Socket socket, Socket socket2, boolean z) {
        byte[] bArr = new byte[65536];
        try {
            try {
                InputStream inputStream = socket.getInputStream();
                OutputStream outputStream = socket2.getOutputStream();
                while (true) {
                    int i = inputStream.read(bArr);
                    if (i < 0) {
                        socket2.shutdownOutput();
                        return;
                    }
                    outputStream.write(bArr, 0, i);
                    outputStream.flush();
                    if (z) {
                        this.tcpBytes += (long) i;
                    }
                }
            } catch (Exception e) {
                socket2.shutdownOutput();
            } catch (Throwable th) {
                try {
                    socket2.shutdownOutput();
                } catch (Exception e2) {
                }
                throw th;
            }
        } catch (Exception e3) {
        }
    }

    String stats() {
        return "forward " + this.port + "->" + this.real + " conns=" + this.conns + " tcpBytes=" + this.tcpBytes + " udpIn=" + this.udpIn + " udpOut=" + this.udpOut;
    }

    void close() {
        this.open = false;
        try {
            this.tcp.close();
        } catch (Exception e) {
        }
        try {
            this.udp.close();
        } catch (Exception e2) {
        }
        try {
            this.toBack.close();
        } catch (Exception e3) {
        }
    }
}
