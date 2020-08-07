package com.example.embeddedvis_arcore;

import android.util.Log;

import java.io.InputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ServerConnectionC {
    private final String TAG = "server_C";

    private Socket clientSocket;
    private InputStream dataInputStream;
    private byte[] dataArr;
    private int readByteCount;
    private String inputData = "";

    private final String ip = "10.20.12.115";
    private final int port = 3350;

    public void receiveC() {
        try {
            clientSocket = new Socket(ip, port);
            clientSocket.getTcpNoDelay();
            Log.d(TAG, "Connected C");

            dataArr = new byte[2048];
            dataInputStream = clientSocket.getInputStream();
            readByteCount = dataInputStream.read(dataArr);

            if (readByteCount != -1) {
                inputData = new String(dataArr, 0, readByteCount, StandardCharsets.UTF_8);
                Log.d(TAG, "Port 3350 : " + inputData);
            }
        } catch (ConnectException e) {
            // 연결 실패일때 예외처리
            //e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
