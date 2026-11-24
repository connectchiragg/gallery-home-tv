package com.chirag.home;

import android.content.Context;
import android.util.Base64;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.Cipher;

/* JADX INFO: loaded from: classes.dex */
final class SelfAdb {
    private static final int AUTH_RSAPUBLICKEY = 3;
    private static final int AUTH_SIGNATURE = 2;
    private static final int AUTH_TOKEN = 1;
    private static final int A_AUTH = 1213486401;
    private static final int A_CLSE = 1163086915;
    private static final int A_CNXN = 1314410051;
    private static final int A_OKAY = 1497451343;
    private static final int A_OPEN = 1313165391;
    private static final int A_WRTE = 1163154007;
    private static final int MAX_PAYLOAD = 262144;
    static final int PORT = 5555;
    private static final byte[] SHA1_PREFIX = {48, 33, 48, 9, 6, 5, 43, 14, 3, 2, 26, 5, 0, 4, 20};
    static final String TAG = "SelfAdb";
    private static final int VERSION = 16777217;

    SelfAdb() {
    }

    private static final class Msg {
        int a0;
        int a1;
        int cmd;
        byte[] data;

        private Msg() {
            this.data = new byte[0];
        }
    }

    static synchronized String shell(Context context, String str, int i) {
        Msg msg;
        try {
            Socket socket = new Socket();
            try {
                socket.connect(new InetSocketAddress("127.0.0.1", PORT), 3000);
                socket.setSoTimeout(i);
                DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                OutputStream outputStream = socket.getOutputStream();
                KeyPair keyPairKeys = keys(context);
                send(outputStream, A_CNXN, VERSION, MAX_PAYLOAD, "host::\u0000".getBytes("UTF-8"));
                int i2 = 0;
                boolean z = false;
                boolean z2 = false;
                while (true) {
                    Msg msg2 = read(dataInputStream);
                    if (msg2.cmd != A_CNXN) {
                        if (msg2.cmd == A_AUTH && msg2.a0 == AUTH_TOKEN) {
                            if (!z) {
                                send(outputStream, A_AUTH, AUTH_SIGNATURE, 0, sign(keyPairKeys.getPrivate(), msg2.data));
                                z = true;
                            } else if (!z2) {
                                Log.i(TAG, "asking the TV to allow the launcher's debugging key");
                                send(outputStream, A_AUTH, AUTH_RSAPUBLICKEY, 0, publicKey((RSAPublicKey) keyPairKeys.getPublic()));
                                z2 = true;
                            } else {
                                socket.close();
                                return null;
                            }
                        }
                    } else {
                        socket.setSoTimeout(15000);
                        send(outputStream, A_OPEN, AUTH_TOKEN, 0, ("shell:" + str + "\u0000").getBytes("UTF-8"));
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        while (true) {
                            msg = read(dataInputStream);
                            if (msg.cmd == A_OKAY) {
                                i2 = msg.a0;
                            } else if (msg.cmd == A_WRTE) {
                                byteArrayOutputStream.write(msg.data);
                                send(outputStream, A_OKAY, AUTH_TOKEN, msg.a0, null);
                            } else if (msg.cmd == A_CLSE) {
                                break;
                            }
                        }
                        if (i2 == 0) {
                            i2 = msg.a0;
                        }
                        send(outputStream, A_CLSE, AUTH_TOKEN, i2, null);
                        String string = byteArrayOutputStream.toString("UTF-8");
                        socket.close();
                        return string;
                    }
                }
            } catch (Throwable th) {
                try {
                    socket.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        } catch (Exception e) {
            Log.w(TAG, "shell failed: " + e);
            return null;
        }
    }

    private static void send(OutputStream outputStream, int i, int i2, int i3, byte[] bArr) throws IOException {
        int i4 = 0;
        int length = bArr == null ? 0 : bArr.length;
        if (bArr != null) {
            int length2 = bArr.length;
            int i5 = 0;
            while (i4 < length2) {
                i5 += bArr[i4] & 255;
                i4 += AUTH_TOKEN;
            }
            i4 = i5;
        }
        ByteBuffer byteBufferOrder = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN);
        byteBufferOrder.putInt(i).putInt(i2).putInt(i3).putInt(length).putInt(i4).putInt(~i);
        outputStream.write(byteBufferOrder.array());
        if (length > 0) {
            outputStream.write(bArr);
        }
        outputStream.flush();
    }

    private static Msg read(DataInputStream dataInputStream) throws IOException {
        byte[] bArr = new byte[24];
        dataInputStream.readFully(bArr);
        ByteBuffer byteBufferOrder = ByteBuffer.wrap(bArr).order(ByteOrder.LITTLE_ENDIAN);
        Msg msg = new Msg();
        msg.cmd = byteBufferOrder.getInt();
        msg.a0 = byteBufferOrder.getInt();
        msg.a1 = byteBufferOrder.getInt();
        int i = byteBufferOrder.getInt();
        if (i < 0 || i > 1048576) {
            throw new IOException("bad length " + i);
        }
        msg.data = new byte[i];
        dataInputStream.readFully(msg.data);
        return msg;
    }

    private static byte[] sign(PrivateKey privateKey, byte[] bArr) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(AUTH_TOKEN, privateKey);
        byte[] bArr2 = new byte[SHA1_PREFIX.length + bArr.length];
        System.arraycopy(SHA1_PREFIX, 0, bArr2, 0, SHA1_PREFIX.length);
        System.arraycopy(bArr, 0, bArr2, SHA1_PREFIX.length, bArr.length);
        return cipher.doFinal(bArr2);
    }

    private static byte[] publicKey(RSAPublicKey rSAPublicKey) throws Exception {
        BigInteger modulus = rSAPublicKey.getModulus();
        BigInteger bigIntegerShiftLeft = BigInteger.ONE.shiftLeft(32);
        BigInteger bigIntegerMod = modulus.mod(bigIntegerShiftLeft).modInverse(bigIntegerShiftLeft).negate().mod(bigIntegerShiftLeft);
        BigInteger bigIntegerMod2 = BigInteger.ONE.shiftLeft(4096).mod(modulus);
        ByteBuffer byteBufferOrder = ByteBuffer.allocate(524).order(ByteOrder.LITTLE_ENDIAN);
        byteBufferOrder.putInt(64).putInt(bigIntegerMod.intValue());
        for (int i = 0; i < 64; i += AUTH_TOKEN) {
            byteBufferOrder.putInt(modulus.shiftRight(i * 32).mod(bigIntegerShiftLeft).intValue());
        }
        for (int i2 = 0; i2 < 64; i2 += AUTH_TOKEN) {
            byteBufferOrder.putInt(bigIntegerMod2.shiftRight(i2 * 32).mod(bigIntegerShiftLeft).intValue());
        }
        byteBufferOrder.putInt(rSAPublicKey.getPublicExponent().intValue());
        return (Base64.encodeToString(byteBufferOrder.array(), AUTH_SIGNATURE) + " home-launcher@halls-tv\u0000").getBytes("UTF-8");
    }

    private static KeyPair keys(Context context) throws Exception {
        File file = new File(context.getFilesDir(), "adbkey");
        File file2 = new File(file, "priv");
        File file3 = new File(file, "pub");
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        if (file2.isFile() && file3.isFile()) {
            return new KeyPair(keyFactory.generatePublic(new X509EncodedKeySpec(readAll(file3))), keyFactory.generatePrivate(new PKCS8EncodedKeySpec(readAll(file2))));
        }
        file.mkdirs();
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPairGenerateKeyPair = keyPairGenerator.generateKeyPair();
        try (FileOutputStream fileOutputStream = new FileOutputStream(file2)) {
            fileOutputStream.write(keyPairGenerateKeyPair.getPrivate().getEncoded());
        }
        try (FileOutputStream fileOutputStream2 = new FileOutputStream(file3)) {
            fileOutputStream2.write(keyPairGenerateKeyPair.getPublic().getEncoded());
        }
        return keyPairGenerateKeyPair;
    }

    private static byte[] readAll(File file) throws IOException {
        int length = (int) file.length();
        byte[] bArr = new byte[length];
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            int i = 0;
            while (i < length) {
                int i2 = fileInputStream.read(bArr, i, length - i);
                if (i2 < 0) {
                    break;
                }
                i += i2;
            }
        }
        return bArr;
    }
}
