package com.example.embeddedvis_arcore;

import android.media.Image;
import android.os.StrictMode;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ServerConnectionMain {
    private final String TAG = "server_A";

    private ImageUtil imageUtil;
    private Socket clientSocket;

    private PrintWriter socketOut1;
    private DataOutputStream socketOut2;
    private InputStream dataInputStream;
    private byte[] dataArr;
    private String [] hitPos;
    public int readByteCount;
    private String inputData = "";
    private String posData = "";
    public float posX, posY, boxX, boxY;
    public String name, gnLink, ytID;

    //private final String ip = "10.20.16.212"; // GPU3
    //private final String ip = "10.20.16.174"; // ivader
    private final String ip = "10.20.12.115";
    private final int port = 3330;

    // ar fragment의 frame 이미지를 jpg로 디코딩하고 소켓통신으로 전송
    public void yoloAndOCR(Image received_img, int rotation) {
        long start = System.currentTimeMillis();
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        imageUtil = new ImageUtil(); // jpg로 디코딩 하기 위한 코드
        String data = imageUtil.YUV2Base64(received_img, rotation);

        try {
            clientSocket = new Socket(ip, port);
            clientSocket.getTcpNoDelay();
            Log.d(TAG, "Connected A");

            socketOut1 = new PrintWriter(clientSocket.getOutputStream(), true);
            socketOut2 = new DataOutputStream(clientSocket.getOutputStream());

            String data_len = String.valueOf(data.length());
            socketOut1.println(String.format("%-16s", data_len));
            socketOut2.writeBytes(data);

            dataArr = new byte[2048];
            dataInputStream = clientSocket.getInputStream();
            readByteCount = dataInputStream.read(dataArr); //bytes

            if (readByteCount != -1) {
                inputData = new String(dataArr, 0, readByteCount, StandardCharsets.UTF_8);
                Log.d(TAG, "Port 3330 : " + inputData);

                if (!inputData.substring(0, 7).equals("No data")) {
                    Log.i(TAG, inputData);

                    int posDataEnd = inputData.indexOf("]posend");
                    posData = inputData.substring(1, posDataEnd);
                    posData = posData.trim();
                    posData = posData.replace("   ", " ");
                    posData = posData.replace("  ", " ");

                    int nameEnd = inputData.indexOf("objlabelend");
                    name = inputData.substring(posDataEnd + 8, nameEnd);

                    int gnLinkStart = inputData.indexOf("GNLINK=");
                    int gnLinkEnd = inputData.indexOf("yt");
                    gnLink = inputData.substring(gnLinkStart + 7, gnLinkEnd - 5);

                    int ytIDStart = inputData.indexOf("YTID=(");
                    if (!inputData.substring(ytIDStart + 6, ytIDStart + 10).equals("null")) {
                        ytID = inputData.substring(ytIDStart + 6, ytIDStart + 17);
                    } else {
                        ytID = "null";
                    }

                    hitPos = posData.split(" ");
                    posX = Float.parseFloat(hitPos[0]);
                    posY = Float.parseFloat(hitPos[1]);
                    boxX = Float.parseFloat(hitPos[2]);
                    boxY = Float.parseFloat(hitPos[3]);

                }
            }

            dataInputStream.close();
            socketOut2.close();
            socketOut1.close();
            clientSocket.close();
        } catch (ConnectException e) {
            // 연결 실패일때 예외처리
            //e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        long end = System.currentTimeMillis();
        Log.d(TAG, "entire time : " + (end - start));
    }

}
