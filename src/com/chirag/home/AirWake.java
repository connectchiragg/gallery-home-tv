package com.chirag.home;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* JADX INFO: loaded from: classes.dex */
final class AirWake {
    static final String AIRSCREEN = "com.ionitech.airscreen";
    static final int DEFAULT_BACK_PORT = 57000;
    static final int FRONT_PORT = 47000;
    static final String NAME = "Hall's TV";
    static final String TAG = "AirWake";
    private static AirWake inst;
    static volatile boolean weLaunched;
    volatile boolean announced;
    volatile String backName;
    final Context ctx;
    volatile long lastClosed;
    volatile long launchedAt;
    final AtomicInteger live = new AtomicInteger();
    final Handler main = new Handler(Looper.getMainLooper());
    boolean pendingReregister;
    final SharedPreferences prefs;
    NsdManager.RegistrationListener reg;
    static final String[][] DEFAULT_TXT = {new String[]{"deviceid", "6F:D2:DD:90:9A:E3"}, new String[]{"features", "0x5A7FFFF7,0xE"}, new String[]{"model", "AppleTV3,1"}, new String[]{"srcvers", "220.68"}, new String[]{"vv", "2"}, new String[]{"pi", "5e66cf9b-0a39-4e0c-9d32-081a8ce63231"}, new String[]{"pk", "ee31f83c88a71219a6fcf9bee0da9bc22620588f5a15a6145553504df9649e5c"}, new String[]{"flags", "0x44"}};
    static final Pattern PORT_KEY = Pattern.compile("(<key>(dataPort|eventPort|controlPort)</key>\\s*<integer>)(\\d+)(</integer>)");

    static synchronized void start(Context context) {
        if (inst == null) {
            inst = new AirWake(context.getApplicationContext());
            inst.run();
        }
    }

    private AirWake(Context context) {
        this.ctx = context;
        this.prefs = context.getSharedPreferences("airwake", 0);
    }

    void run() {
        new Thread(new Runnable() { // from class: com.chirag.home.AirWake$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                serve();
            }
        }, "airwake").start();
        NsdManager nsdManager = (NsdManager) this.ctx.getSystemService("servicediscovery");
        register(nsdManager);
        this.main.postDelayed(new AnonymousClass1(this, nsdManager), 8000L);
        try {
            nsdManager.discoverServices("_airplay._tcp", 1, new AnonymousClass2(this, nsdManager));
        } catch (Exception e) {
            Log.w(TAG, "discover", e);
        }
    }

    /* JADX INFO: renamed from: com.chirag.home.AirWake$1, reason: invalid class name */
    class AnonymousClass1 implements Runnable {
        final /* synthetic */ AirWake this$0;
        final /* synthetic */ NsdManager val$nsd;

        AnonymousClass1(AirWake airWake, NsdManager nsdManager) {
            this.val$nsd = nsdManager;
            this.this$0 = airWake;
        }

        @Override // java.lang.Runnable
        public void run() {
            boolean z;
            synchronized (this.this$0) {
                z = this.this$0.reg == null || !this.this$0.announced;
            }
            if (z && SystemClock.elapsedRealtime() - this.this$0.launchedAt > 45000) {
                final NsdManager nsdManager = this.val$nsd;
                new Thread(new Runnable() { // from class: com.chirag.home.AirWake$1$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        m10lambda$run$1$comchiraghomeAirWake$1(nsdManager);
                    }
                }).start();
            }
            this.this$0.main.postDelayed(this, 12000L);
        }

        /* JADX INFO: renamed from: lambda$run$0$com-chirag-home-AirWake$1, reason: not valid java name */
        /* synthetic */ void m9lambda$run$0$comchiraghomeAirWake$1(NsdManager nsdManager) {
            this.this$0.register(nsdManager);
        }

        /* JADX INFO: renamed from: lambda$run$1$com-chirag-home-AirWake$1, reason: not valid java name */
        /* synthetic */ void m10lambda$run$1$comchiraghomeAirWake$1(final NsdManager nsdManager) {
            if (!this.this$0.backAlive()) {
                this.this$0.backName = null;
                this.this$0.main.postDelayed(new Runnable() { // from class: com.chirag.home.AirWake$1$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        m9lambda$run$0$comchiraghomeAirWake$1(nsdManager);
                    }
                }, 3000L);
            }
        }
    }

    /* JADX INFO: renamed from: com.chirag.home.AirWake$2, reason: invalid class name */
    class AnonymousClass2 implements NsdManager.DiscoveryListener {
        final /* synthetic */ AirWake this$0;
        final /* synthetic */ NsdManager val$nsd;

        AnonymousClass2(AirWake airWake, NsdManager nsdManager) {
            this.val$nsd = nsdManager;
            this.this$0 = airWake;
        }

        @Override // android.net.nsd.NsdManager.DiscoveryListener
        public void onServiceFound(NsdServiceInfo nsdServiceInfo) {
            this.val$nsd.resolveService(nsdServiceInfo, new NsdManager.ResolveListener() { // from class: com.chirag.home.AirWake.2.1
                @Override // android.net.nsd.NsdManager.ResolveListener
                public void onResolveFailed(NsdServiceInfo nsdServiceInfo2, int i) {
                }

                @Override // android.net.nsd.NsdManager.ResolveListener
                public void onServiceResolved(NsdServiceInfo nsdServiceInfo2) {
                    if (nsdServiceInfo2.getPort() == AirWake.FRONT_PORT || !AnonymousClass2.this.this$0.isThisDevice(nsdServiceInfo2)) {
                        return;
                    }
                    AnonymousClass2.this.this$0.backName = nsdServiceInfo2.getServiceName();
                    AnonymousClass2.this.this$0.learn(AnonymousClass2.this.val$nsd, nsdServiceInfo2);
                }
            });
        }

        @Override // android.net.nsd.NsdManager.DiscoveryListener
        public void onServiceLost(NsdServiceInfo nsdServiceInfo) {
            if (this.this$0.backName != null && this.this$0.backName.equals(nsdServiceInfo.getServiceName())) {
                this.this$0.backName = null;
                Handler handler = this.this$0.main;
                final NsdManager nsdManager = this.val$nsd;
                handler.postDelayed(new Runnable() { // from class: com.chirag.home.AirWake$2$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        m11lambda$onServiceLost$0$comchiraghomeAirWake$2(nsdManager);
                    }
                }, 6000L);
            }
        }

        /* JADX INFO: renamed from: lambda$onServiceLost$0$com-chirag-home-AirWake$2, reason: not valid java name */
        /* synthetic */ void m11lambda$onServiceLost$0$comchiraghomeAirWake$2(NsdManager nsdManager) {
            if (this.this$0.backName == null) {
                this.this$0.register(nsdManager);
            }
        }

        @Override // android.net.nsd.NsdManager.DiscoveryListener
        public void onDiscoveryStarted(String str) {
        }

        @Override // android.net.nsd.NsdManager.DiscoveryListener
        public void onDiscoveryStopped(String str) {
        }

        @Override // android.net.nsd.NsdManager.DiscoveryListener
        public void onStartDiscoveryFailed(String str, int i) {
        }

        @Override // android.net.nsd.NsdManager.DiscoveryListener
        public void onStopDiscoveryFailed(String str, int i) {
        }
    }

    synchronized void register(NsdManager nsdManager) {
        if (this.reg == null || !this.announced) {
            if (this.reg != null) {
                try {
                    nsdManager.unregisterService(this.reg);
                } catch (Exception e) {
                }
                this.reg = null;
                return;
            }
            try {
                NsdServiceInfo nsdServiceInfo = new NsdServiceInfo();
                nsdServiceInfo.setServiceName(NAME);
                nsdServiceInfo.setServiceType("_airplay._tcp");
                nsdServiceInfo.setPort(FRONT_PORT);
                for (String[] strArr : DEFAULT_TXT) {
                    nsdServiceInfo.setAttribute(strArr[0], this.prefs.getString("txt_" + strArr[0], strArr[1]));
                }
                this.reg = new NsdManager.RegistrationListener() { // from class: com.chirag.home.AirWake.3
                    @Override // android.net.nsd.NsdManager.RegistrationListener
                    public void onServiceRegistered(NsdServiceInfo nsdServiceInfo2) {
                        AirWake.this.announced = true;
                        Log.i(AirWake.TAG, "announced as " + nsdServiceInfo2.getServiceName());
                    }

                    @Override // android.net.nsd.NsdManager.RegistrationListener
                    public void onRegistrationFailed(NsdServiceInfo nsdServiceInfo2, int i) {
                        Log.w(AirWake.TAG, "announce failed " + i);
                        synchronized (AirWake.this) {
                            if (AirWake.this.reg == this) {
                                AirWake.this.reg = null;
                            }
                        }
                    }

                    @Override // android.net.nsd.NsdManager.RegistrationListener
                    public void onServiceUnregistered(NsdServiceInfo nsdServiceInfo2) {
                    }

                    @Override // android.net.nsd.NsdManager.RegistrationListener
                    public void onUnregistrationFailed(NsdServiceInfo nsdServiceInfo2, int i) {
                    }
                };
                this.announced = false;
                nsdManager.registerService(nsdServiceInfo, 1, this.reg);
            } catch (Exception e2) {
                Log.w(TAG, "register", e2);
            }
        }
    }

    boolean isThisDevice(NsdServiceInfo nsdServiceInfo) {
        try {
            InetAddress host = nsdServiceInfo.getHost();
            if (host != null) {
                return host.isLoopbackAddress() || NetworkInterface.getByInetAddress(host) != null;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    void learn(NsdManager nsdManager, NsdServiceInfo nsdServiceInfo) {
        SharedPreferences.Editor editorPutInt = this.prefs.edit().putInt("port", nsdServiceInfo.getPort());
        for (Map.Entry<String, byte[]> entry : nsdServiceInfo.getAttributes().entrySet()) {
            if (entry.getValue() != null) {
                editorPutInt.putString("txt_" + entry.getKey(), new String(entry.getValue()));
            }
        }
        editorPutInt.apply();
        synchronized (this) {
            if (this.reg != null) {
                try {
                    nsdManager.unregisterService(this.reg);
                } catch (Exception e) {
                }
                this.reg = null;
                this.announced = false;
            }
        }
    }

    void serve() {
        try {
            ServerSocket serverSocket = new ServerSocket();
            try {
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress(FRONT_PORT));
                while (true) {
                    final Socket socketAccept = serverSocket.accept();
                    new Thread(new Runnable() { // from class: com.chirag.home.AirWake$$ExternalSyntheticLambda1
                        @Override // java.lang.Runnable
                        public final void run() {
                            handle(socketAccept);
                        }
                    }, "airwake-conn").start();
                }
            } catch (Throwable th) {
                try {
                    serverSocket.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        } catch (Exception e) {
            Log.w(TAG, "serve", e);
        }
    }

    public void handle(final Socket socket) {
        this.live.incrementAndGet();
        ArrayList<AirRelay> arrayList = new ArrayList<>();
        final List<AirForward> listSynchronizedList = Collections.synchronizedList(new ArrayList<AirForward>());
        try (Socket socket2 = socket; final Socket socketConnectBack = connectBack()) {
            if (socketConnectBack == null) {
                return;
            }
            socket.setTcpNoDelay(true);
            socketConnectBack.setTcpNoDelay(true);
            Thread thread = new Thread(new Runnable() { // from class: com.chirag.home.AirWake$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    replyPipe(socketConnectBack, socket, listSynchronizedList);
                }
            }, "airwake-down");
            thread.start();
            tap(socket, socketConnectBack, arrayList);
            thread.join(2000L);
        } catch (Exception e) {
        } finally {
            Iterator<AirRelay> it = arrayList.iterator();
            while (it.hasNext()) {
                it.next().close();
            }
            synchronized (listSynchronizedList) {
                Iterator<AirForward> it2 = listSynchronizedList.iterator();
                while (it2.hasNext()) {
                    it2.next().close();
                }
            }
            this.live.decrementAndGet();
        }
    }

    void tap(Socket socket, Socket socket2, List<AirRelay> list) {
        byte[] bArr = new byte[16384];
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket2.getOutputStream();
            boolean z = false;
            while (true) {
                int i = inputStream.read(bArr);
                if (i >= 0) {
                    if (z) {
                        outputStream.write(bArr, 0, i);
                        outputStream.flush();
                    } else {
                        byteArrayOutputStream.write(bArr, 0, i);
                        byte[] byteArray = byteArrayOutputStream.toByteArray();
                        int i2 = 0;
                        while (true) {
                            int iIndexOf = indexOf(byteArray, i2, new byte[]{13, 10, 13, 10});
                            if (iIndexOf < 0) {
                                if (byteArray.length - i2 <= 65536) {
                                    break;
                                }
                                z = true;
                                break;
                            }
                            Matcher matcher = Pattern.compile("(?im)^content-length:\\s*(\\d+)").matcher(new String(byteArray, i2, iIndexOf - i2, "ISO-8859-1"));
                            int i3 = matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
                            int i4 = iIndexOf + 4;
                            int i5 = i4 + i3;
                            if (i5 > byteArray.length) {
                                break;
                            }
                            if (i3 > 8 && byteArray[i4] == 98 && byteArray[iIndexOf + 5] == 112) {
                                try {
                                    Iterator<Integer> it = AirRelay.ports(Arrays.copyOfRange(byteArray, i4, i5)).iterator();
                                    while (it.hasNext()) {
                                        list.add(new AirRelay(it.next().intValue(), socket.getInetAddress()));
                                    }
                                } catch (Exception e) {
                                    Log.w(TAG, "setup parse", e);
                                }
                            }
                            outputStream.write(byteArray, i2, i5 - i2);
                            outputStream.flush();
                            i2 = i5;
                        }
                        byteArrayOutputStream.reset();
                        if (z) {
                            outputStream.write(byteArray, i2, byteArray.length - i2);
                            outputStream.flush();
                        } else if (i2 < byteArray.length) {
                            byteArrayOutputStream.write(byteArray, i2, byteArray.length - i2);
                        }
                    }
                } else {
                    break;
                }
            }
        } catch (Exception e) {
        } finally {
            try {
                socket2.shutdownOutput();
            } catch (Exception e2) {
            }
        }
    }

    public void replyPipe(Socket socket, Socket socket2, List<AirForward> list) {
        byte[] bArr;
        byte[] bArr2 = new byte[16384];
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket2.getOutputStream();
            int i = 0;
            boolean z = false;
            while (true) {
                int i2 = inputStream.read(bArr2);
                if (i2 >= 0) {
                    if (z) {
                        outputStream.write(bArr2, i, i2);
                        outputStream.flush();
                    } else {
                        byteArrayOutputStream.write(bArr2, i, i2);
                        byte[] byteArray = byteArrayOutputStream.toByteArray();
                        int i3 = i;
                        while (true) {
                            int iIndexOf = indexOf(byteArray, i3, new byte[]{13, 10, 13, 10});
                            if (iIndexOf < 0) {
                                if (byteArray.length - i3 <= 65536) {
                                    break;
                                }
                                z = true;
                                break;
                            }
                            String str = new String(byteArray, i3, iIndexOf - i3, "ISO-8859-1");
                            Matcher matcher = Pattern.compile("(?im)^(content-length:\\s*)(\\d+)").matcher(str);
                            int i4 = matcher.find() ? Integer.parseInt(matcher.group(2)) : i;
                            int i5 = iIndexOf + 4;
                            int i6 = i5 + i4;
                            if (i6 > byteArray.length) {
                                break;
                            }
                            String str2 = new String(byteArray, i5, i4, "ISO-8859-1");
                            if (str2.startsWith("<?xml") && str2.contains("Port</key>")) {
                                Matcher matcher2 = PORT_KEY.matcher(str2);
                                StringBuffer stringBuffer = new StringBuffer();
                                while (matcher2.find()) {
                                    int i7 = Integer.parseInt(matcher2.group(3));
                                    try {
                                        AirForward airForward = new AirForward(i7);
                                        list.add(airForward);
                                        i7 = airForward.port;
                                    } catch (Exception e) {
                                        Log.w(TAG, "forward", e);
                                    }
                                    matcher2.appendReplacement(stringBuffer, Matcher.quoteReplacement(matcher2.group(1) + i7 + matcher2.group(4)));
                                    bArr2 = bArr2;
                                }
                                bArr = bArr2;
                                matcher2.appendTail(stringBuffer);
                                byte[] bytes = stringBuffer.toString().getBytes("ISO-8859-1");
                                if (matcher.reset().find()) {
                                    str = matcher.replaceFirst(Matcher.quoteReplacement(matcher.group(1) + bytes.length));
                                }
                                outputStream.write(str.getBytes("ISO-8859-1"));
                                outputStream.write(new byte[]{13, 10, 13, 10});
                                outputStream.write(bytes);
                            } else {
                                bArr = bArr2;
                                outputStream.write(byteArray, i3, i6 - i3);
                            }
                            outputStream.flush();
                            i3 = i6;
                            bArr2 = bArr;
                            i = 0;
                        }
                        byteArrayOutputStream.reset();
                        if (z) {
                            outputStream.write(byteArray, i3, byteArray.length - i3);
                            outputStream.flush();
                        } else if (i3 < byteArray.length) {
                            byteArrayOutputStream.write(byteArray, i3, byteArray.length - i3);
                        }
                        i = 0;
                    }
                } else {
                    break;
                }
            }
        } catch (Exception e) {
        } finally {
            try {
                socket2.shutdownOutput();
            } catch (Exception e2) {
            }
        }
    }

    static int indexOf(byte[] bArr, int i, byte[] bArr2) {
        while (i <= bArr.length - bArr2.length) {
            for (int i2 = 0; i2 < bArr2.length; i2++) {
                if (bArr[i + i2] != bArr2[i2]) {
                    i++;
                }
            }
            return i;
        }
        return -1;
    }

    Socket connectBack() {
        int i = this.prefs.getInt("port", DEFAULT_BACK_PORT);
        for (int i2 = 0; i2 < 50; i2++) {
            try {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress("127.0.0.1", i), 400);
                return socket;
            } catch (Exception e) {
                if (i2 == 0) {
                    launch();
                }
                try {
                    Thread.sleep(500L);
                } catch (InterruptedException e2) {
                    return null;
                }
            }
        }
        return null;
    }

    static void pipe(Socket socket, Socket socket2) {
        byte[] bArr = new byte[16384];
        try {
            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket2.getOutputStream();
            while (true) {
                int i = inputStream.read(bArr);
                if (i < 0) {
                    break;
                }
                outputStream.write(bArr, 0, i);
                outputStream.flush();
            }
        } catch (Exception e) {
        } finally {
            try {
                socket2.shutdownOutput();
            } catch (Exception e2) {
            }
        }
    }

    boolean backAlive() {
        try {
            Socket socket = new Socket();
            try {
                socket.connect(new InetSocketAddress("127.0.0.1", this.prefs.getInt("port", DEFAULT_BACK_PORT)), 400);
                socket.close();
                return true;
            } catch (Throwable th) {
                try {
                    socket.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        } catch (Exception e) {
            return false;
        }
    }

    void launch() {
        Intent intentLaunchIntent = Ui.launchIntent(this.ctx, AIRSCREEN);
        if (intentLaunchIntent == null) {
            return;
        }
        weLaunched = true;
        this.launchedAt = SystemClock.elapsedRealtime();
        NsdManager nsdManager = (NsdManager) this.ctx.getSystemService("servicediscovery");
        synchronized (this) {
            if (this.reg != null) {
                try {
                    nsdManager.unregisterService(this.reg);
                } catch (Exception e) {
                }
                this.reg = null;
                this.announced = false;
            }
        }
        try {
            (HomeKeyService.inst != null ? HomeKeyService.inst : this.ctx).startActivity(new Intent(intentLaunchIntent).addFlags(268435456));
        } catch (Exception e2) {
            Log.w(TAG, "launch", e2);
        }
    }
}
