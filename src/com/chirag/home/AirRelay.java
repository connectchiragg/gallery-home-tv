package com.chirag.home;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
final class AirRelay {
    volatile InetAddress airscreenAddr;
    final DatagramSocket ext;
    volatile int in;
    volatile int out;
    final int port;
    final InetAddress sender;
    volatile int airscreenPort = -1;
    volatile boolean open = true;
    final DatagramSocket local = new DatagramSocket((SocketAddress) null);

    AirRelay(final int i, final InetAddress inetAddress) throws Exception {
        this.port = i;
        this.sender = inetAddress;
        this.local.setReuseAddress(true);
        this.local.bind(new InetSocketAddress(i));
        this.ext = new DatagramSocket();
        new Thread(new Runnable() { // from class: com.chirag.home.AirRelay$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                m5lambda$new$0$comchiraghomeAirRelay(inetAddress, i);
            }
        }, "airrelay-out").start();
        new Thread(new Runnable() { // from class: com.chirag.home.AirRelay$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                m6lambda$new$1$comchiraghomeAirRelay();
            }
        }, "airrelay-in").start();
    }

    /* JADX INFO: renamed from: lambda$new$0$com-chirag-home-AirRelay, reason: not valid java name */
    /* synthetic */ void m5lambda$new$0$comchiraghomeAirRelay(InetAddress inetAddress, int i) {
        byte[] bArr = new byte[2048];
        while (this.open) {
            try {
                DatagramPacket datagramPacket = new DatagramPacket(bArr, 2048);
                this.local.receive(datagramPacket);
                if (datagramPacket.getAddress().equals(inetAddress)) {
                    if (this.airscreenPort > 0) {
                        this.local.send(new DatagramPacket(bArr, datagramPacket.getLength(), this.airscreenAddr, this.airscreenPort));
                        this.in++;
                    }
                } else {
                    this.airscreenPort = datagramPacket.getPort();
                    this.airscreenAddr = datagramPacket.getAddress();
                    this.ext.send(new DatagramPacket(bArr, datagramPacket.getLength(), inetAddress, i));
                    this.out++;
                }
            } catch (Exception e) {
                if (!this.open) {
                    return;
                }
            }
        }
    }

    /* JADX INFO: renamed from: lambda$new$1$com-chirag-home-AirRelay, reason: not valid java name */
    /* synthetic */ void m6lambda$new$1$comchiraghomeAirRelay() {
        byte[] bArr = new byte[2048];
        try {
            InetAddress.getByName("127.0.0.1");
            while (this.open) {
                try {
                    DatagramPacket datagramPacket = new DatagramPacket(bArr, 2048);
                    this.ext.receive(datagramPacket);
                    if (this.airscreenPort > 0) {
                        this.local.send(new DatagramPacket(bArr, datagramPacket.getLength(), this.airscreenAddr, this.airscreenPort));
                        this.in++;
                    }
                } catch (Exception e) {
                    if (!this.open) {
                        return;
                    }
                }
            }
        } catch (Exception e2) {
        }
    }

    void close() {
        this.open = false;
        try {
            this.local.close();
        } catch (Exception e) {
        }
        try {
            this.ext.close();
        } catch (Exception e2) {
        }
    }

    static List<Integer> ports(byte[] bArr) {
        ArrayList arrayList = new ArrayList();
        if (bArr.length < 40 || bArr[0] != 98 || bArr[1] != 112) {
            return arrayList;
        }
        int length = bArr.length - 32;
        int i = bArr[length + 6] & 255;
        int i2 = bArr[length + 7] & 255;
        long jBe = be(bArr, length + 8, 8);
        long jBe2 = be(bArr, length + 16, 8);
        long jBe3 = be(bArr, length + 24, 8);
        if (jBe > 4096) {
            return arrayList;
        }
        walk(bArr, (int) jBe2, i, i2, (int) jBe3, arrayList, 0, null);
        return arrayList;
    }

    static String dump(byte[] bArr) {
        int length = bArr.length - 32;
        int i = bArr[length + 6] & 255;
        int i2 = bArr[length + 7] & 255;
        StringBuilder sb = new StringBuilder();
        dumpObj(bArr, (int) be(bArr, length + 16, 8), i, i2, (int) be(bArr, length + 24, 8), sb, 0);
        return sb.length() > 900 ? sb.substring(0, 900) : sb.toString();
    }

    static void dumpObj(byte[] bArr, int i, int i2, int i3, int i4, StringBuilder sb, int i5) {
        String str;
        if (i5 > 6 || sb.length() > 900) {
            return;
        }
        int iObjOff = objOff(bArr, i, i2, i4);
        int i6 = bArr[iObjOff] & 255;
        int i7 = i6 >> 4;
        int i8 = i6 & 15;
        if (i7 == 0) {
            if (i8 == 9) {
                str = "true";
            } else {
                str = i8 == 8 ? "false" : "null";
            }
            sb.append(str);
            return;
        }
        if (i7 == 1) {
            sb.append(be(bArr, iObjOff + 1, 1 << i8));
            return;
        }
        if (i7 == 2) {
            sb.append("real");
            return;
        }
        int i9 = iObjOff + 1;
        long jBe = i8;
        if (i8 == 15) {
            int i10 = 1 << ((bArr[i9] & 255) & 15);
            jBe = be(bArr, i9 + 1, i10);
            i9 += i10 + 1;
        }
        long j = jBe;
        int i11 = i9;
        long j2 = j;
        if (i7 == 5) {
            sb.append(new String(bArr, i11, (int) Math.min(j2, 40L), StandardCharsets.US_ASCII));
            return;
        }
        if (i7 == 6) {
            sb.append(new String(bArr, i11, ((int) Math.min(j2, 20L)) * 2, StandardCharsets.UTF_16BE));
            return;
        }
        if (i7 == 4) {
            sb.append("<").append(j2).append("b>");
            return;
        }
        if (i7 == 10) {
            sb.append("[");
            int i12 = 0;
            while (i12 < j2) {
                if (i12 > 0) {
                    sb.append(",");
                }
                dumpObj(bArr, (int) be(bArr, (i12 * i3) + i11, i3), i2, i3, i4, sb, i5 + 1);
                i12++;
                j2 = j2;
            }
            sb.append("]");
            return;
        }
        if (i7 != 13) {
            sb.append("?");
            return;
        }
        sb.append("{");
        int i13 = 0;
        while (true) {
            long j3 = i13;
            if (j3 >= j2) {
                sb.append("}");
                return;
            }
            if (i13 > 0) {
                sb.append(",");
            }
            int i14 = i5 + 1;
            dumpObj(bArr, (int) be(bArr, (i13 * i3) + i11, i3), i2, i3, i4, sb, i14);
            sb.append("=");
            dumpObj(bArr, (int) be(bArr, (((int) (j2 + j3)) * i3) + i11, i3), i2, i3, i4, sb, i14);
            i13++;
        }
    }

    static long be(byte[] bArr, int i, int i2) {
        long j = 0;
        for (int i3 = 0; i3 < i2; i3++) {
            j = (j << 8) | ((long) (bArr[i + i3] & 255));
        }
        return j;
    }

    static int objOff(byte[] bArr, int i, int i2, int i3) {
        return (int) be(bArr, i3 + (i * i2), i2);
    }

    static Object walk(byte[] bArr, int i, int i2, int i3, int i4, List<Integer> list, int i5, String str) {
        long jBe;
        int i6;
        if (i5 > 8) {
            return null;
        }
        int iObjOff = objOff(bArr, i, i2, i4);
        int i7 = bArr[iObjOff] & 255;
        int i8 = i7 >> 4;
        int i9 = i7 & 15;
        if (i8 == 1) {
            long jBe2 = be(bArr, iObjOff + 1, 1 << i9);
            if (str != null && ((str.equals("timingPort") || str.equals("controlPort")) && jBe2 > 0 && jBe2 < 65536)) {
                list.add(Integer.valueOf((int) jBe2));
            }
            return Long.valueOf(jBe2);
        }
        int i10 = iObjOff + 1;
        long j = i9;
        if (i9 == 15 && (i8 == 5 || i8 == 6 || i8 == 10 || i8 == 13 || i8 == 4)) {
            int i11 = 1 << ((bArr[i10] & 255) & 15);
            jBe = be(bArr, i10 + 1, i11);
            i6 = i10 + i11 + 1;
        } else {
            jBe = j;
            i6 = i10;
        }
        if (i8 == 5) {
            return new String(bArr, i6, (int) jBe, StandardCharsets.US_ASCII);
        }
        if (i8 == 6) {
            return new String(bArr, i6, ((int) jBe) * 2, StandardCharsets.UTF_16BE);
        }
        if (i8 == 10) {
            int i12 = 0;
            while (i12 < jBe) {
                walk(bArr, (int) be(bArr, (i12 * i3) + i6, i3), i2, i3, i4, list, i5 + 1, null);
                i12++;
                jBe = jBe;
                i6 = i6;
            }
            return null;
        }
        long j2 = jBe;
        int i13 = i6;
        if (i8 == 13) {
            int i14 = 0;
            while (true) {
                long j3 = i14;
                if (j3 < j2) {
                    int i15 = i5 + 1;
                    Object objWalk = walk(bArr, (int) be(bArr, i13 + (i14 * i3), i3), i2, i3, i4, list, i15, null);
                    walk(bArr, (int) be(bArr, i13 + (((int) (j2 + j3)) * i3), i3), i2, i3, i4, list, i15, objWalk instanceof String ? (String) objWalk : null);
                    i14++;
                } else {
                    return null;
                }
            }
        } else {
            return null;
        }
    }
}
