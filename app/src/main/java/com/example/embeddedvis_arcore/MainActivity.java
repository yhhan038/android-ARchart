package com.example.embeddedvis_arcore;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.media.Image;
import android.os.Bundle;

import com.github.mikephil.charting.charts.BarChart;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.Display;
import android.view.DragEvent;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;
import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapView;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.util.FusedLocationSource;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity
implements OnMapReadyCallback {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;
    private final int PERMISSIONS_ACCESS_FINE_LOCATION = 1000;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private FusedLocationSource locationSource;

    String[] REQUIRED_PERMISSIONS  = {Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION};

    // 통신
    private ServerConnectionMain serverConnectionMain;
    private boolean sendFlag = true;
    private Runnable task = new yoloAndOCRRunnable();
    private Thread yoloAndOCRWorker = new Thread(task);

    // 트래픽
    private TextView serverRx, processedData;
    public float serverDataSize = 0;
    public float processedDataSize = 0;
    private ProgressBar progressBar;
    private Runnable progressRunnable = new ProgressBarRunnable();
    private Thread progressWorker = new Thread(progressRunnable);
    private float maxValue = 0;
    private float currentValue = 0;
    private float progress = 0;

    // Border box
    private Point size = new Point();
    private Display display;
    private DrawView drawBox;
    private int width, height;
    private float hitW, hitH, boxW, boxH;
    private boolean hitFlag = false;
    private Runnable boxMovement = new BoxRunnable();
    private Thread boxWorker = new Thread(boxMovement);

    // 서버 전송 이미지
    public Image img;
    private long lastSend = 0;
    private long lastUpdate = 0;

    // 서버 수신 값 처리
    public String ytIDTemp, ytIDMain, gnLinkTemp, gnLinkMain;
    public CharSequence objLabelTemp, objLabelMain;
    private View nameHeader;
    public TextView[] viewObjectName = new TextView[5];
    public String[] objYtID = new String[5];
    public String[] objGnLink = new String[5];
    public WebView webView;
    public View chartView;
    private int touchedObject = 0;

    // AR Fragment
    private int displayrotation;
    private ArFragment arFragment; // Sceneform 설명 doc의 ArFragment 부분 참고
    private FragmentManager fragManager = getSupportFragmentManager(); // 팝업 띄울때 씀
    private ViewRenderable[] labelRenderable = new ViewRenderable[5];

    // 터치
    private GestureDetector gestureDetector;
    private ScaleGestureDetector mScaleDetector;
    private float mScaleFactor = 1.f;
    private float scaleConst = 0.f;

    // 센서
    private SensorControl sensorControl;
    private boolean sensorFlag;
    public TextView gyroValue;

    // 다섯개 표시를 위한 큐
    private final Queue<AnchorNode> labelAnchorNodeQueue = new LinkedList<>();
    private int objectCount = 0; // for object contents
    private int MAX_NUM = 4;
    private int num_object = 0; // for anchor node queue

    // UI
    private FloatingActionButton clearButton, sensorButton, statusButton, settingButton; // 옵션 버튼들
    private Boolean isVideoMode = false;
    private Boolean isWebMode = true; // 기본값으로 설정
    private Boolean isSettingOpen = false;
    private Boolean isShowingStatus = true;
    private int MinMaxMode = 1;
    private Animation settingOpen, settingClose;
    private TextView planeType, clearText, sensorText, statusText;
    private LinearLayout statusLayout;

    private MapView mapView;

    /*
    // 3D asset test
    private static final String GLTF_ASSET =
            "https://github.com/KhronosGroup/glTF-Sample-Models/raw/master/2.0/Duck/glTF/Duck.gltf";
    private ModelRenderable objRenderable, duckRenderable;
    */

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ux);

        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }

        int cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int locationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);

        if (cameraPermission != PackageManager.PERMISSION_GRANTED || locationPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, 0 );
            return;
        }

        sendFlag = true;
        sensorFlag = true;

        settingOpen = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.button_open);
        settingClose = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.button_close);

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

        display = getWindowManager().getDefaultDisplay();
        display.getSize(size);
        width = size.x;
        height = size.y;

        // 버튼 이름
        planeType = findViewById(R.id.planeType); //plane이 vertical인지 horizontal인지 표시
        clearText = findViewById(R.id.clearAllText);
        sensorText = findViewById(R.id.sensorText);
        statusText = findViewById(R.id.statusText);
        statusText.setText(R.string.hidestat);
        statusLayout = findViewById(R.id.statusLayout);

        // 버튼
        settingButton = findViewById(R.id.settingButton);
        clearButton = findViewById(R.id.clearButton);
        sensorButton = findViewById(R.id.sensorButton);
        statusButton = findViewById(R.id.statusButton);

        sensorButton.setEnabled(true);
        settingButton.setOnClickListener(this::buttonOnClick);
        clearButton.setOnClickListener(this::deleteObject);
        sensorButton.setOnClickListener(v -> {
            if (sensorFlag) {
                sensorControl.unregisterSensor();
                sensorFlag = false;
                Toast.makeText(this, "Sensor deactivated", Toast.LENGTH_LONG).show();
            }
            else {
                sensorControl.registerSensor();
                sensorFlag = true;
                Toast.makeText(this, "Sensor activated", Toast.LENGTH_LONG).show();
            }
        });
        statusButton.setOnClickListener(v -> {
            if (isShowingStatus) {
                isShowingStatus = false;
                statusLayout.setVisibility(View.INVISIBLE);
                statusText.setText(R.string.showstat);
                Toast.makeText(this, "Hide status", Toast.LENGTH_LONG).show();
            }
            else {
                isShowingStatus = true;
                statusLayout.setVisibility(View.VISIBLE);
                statusText.setText(R.string.hidestat);
                Toast.makeText(this, "Show status", Toast.LENGTH_LONG).show();
            }
        });

        // 센서
        sensorControl = new SensorControl(this);
        gyroValue = findViewById(R.id.gyro);
        sensorControl.setSensorListener();

        // Traffic
        serverRx = findViewById(R.id.serverRx);
        processedData = findViewById(R.id.processedData);
        progressBar = findViewById(R.id.networkProgressBar);
        progressBar.setProgress(0);

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout inflatedLayout = (LinearLayout) inflater.inflate(R.layout.webbox, null);

        TabLayout tabLayout = inflatedLayout.findViewById(R.id.contentsTabs);

        objYtID[0] = "7SwZUNDsWaM";
        objGnLink[0] = "https://news.google.com";

        webView = inflatedLayout.findViewById(R.id.web2DView);
        webView.loadUrl("https://news.google.com"); // 초기 페이지 만들기
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());

        addContentView(inflatedLayout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        inflatedLayout.setX(width / (float)1.53);
        inflatedLayout.setY(height / (float)2.05);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int pos = tab.getPosition();

                switch(pos) {
                    case 0 : // News
                        webView.setVisibility(View.VISIBLE);
                        webView.loadUrl(objGnLink[touchedObject]);
                        isWebMode = true;
                        isVideoMode = false;
                        break;
                    case 1 : // YouTube
                        webView.setVisibility(View.VISIBLE);
                        if (!objYtID[touchedObject].equals("null")) {
                            webView.loadUrl("https://www.youtube.com/embed/" + objYtID[touchedObject]);
                        } else {
                            webView.loadUrl("https://www.youtube.com"); // html 파일 만들어서 에러페이지 같은거 로딩하기
                        }
                        isVideoMode = true;
                        isWebMode = false;
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        // Chart
        FrameLayout.LayoutParams chart_size = new FrameLayout.LayoutParams(900, 600 );


        // Bar
        LayoutInflater bar_inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        RelativeLayout barLayout = (RelativeLayout) bar_inflater.inflate(R.layout.activity_barchart, null);
        addContentView(barLayout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        myBarChart barChart = new myBarChart(this);
        barChart.onCreateView(this, barLayout);

        barLayout.setX(width / (float)1.53);
        barLayout.setY(10);
        //barLayout.setBackgroundColor(Color.argb(180,255,255,255));
        barLayout.setLayoutParams(chart_size);

        barLayout.findViewById(R.id.bar_title).setOnTouchListener(this::onTouch);
        barLayout.getRootView().setOnDragListener(this::onDrag);
        barLayout.findViewById(R.id.bar_minmax).setOnClickListener(this::onMinusClick);
        barLayout.findViewById(R.id.bar_plus).setOnClickListener(this::onPlusClick);

        // Line
        LayoutInflater line_inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        RelativeLayout lineLayout = (RelativeLayout) line_inflater.inflate(R.layout.activity_linechart, null);
        addContentView(lineLayout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        myLineChart lineChart = new myLineChart(this);
        lineChart.onCreateView(this, lineLayout);

        lineLayout.setX(width / (float)1.53 - 1100);
        lineLayout.setY(10);
        //lineLayout.setBackgroundColor(Color.argb(180,255,255,255));
        lineLayout.setLayoutParams(chart_size);

        lineLayout.findViewById(R.id.line_title).setOnTouchListener(this::onTouch);
        lineLayout.getRootView().setOnDragListener(this::onDrag);
        lineLayout.findViewById(R.id.line_minmax).setOnClickListener(this::onMinusClick);
        lineLayout.findViewById(R.id.line_plus).setOnClickListener(this::onPlusClick);

        // Scatter
        LayoutInflater scatter_inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        RelativeLayout scatterLayout = (RelativeLayout) scatter_inflater.inflate(R.layout.activity_scatterchart, null);
        addContentView(scatterLayout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        myScatterChart scatterChart = new myScatterChart(this);
        scatterChart.onCreateView(this, scatterLayout);

        scatterLayout.setX(width / (float)1.53 - 1100);
        scatterLayout.setY(height / (float)2.05);
        //scatterLayout.setBackgroundColor(Color.argb(180,255,255,255));
        scatterLayout.setLayoutParams(chart_size);

        scatterLayout.findViewById(R.id.scatter_title).setOnTouchListener(this::onTouch);
        scatterLayout.getRootView().setOnDragListener(this::onDrag);
        scatterLayout.findViewById(R.id.scatter_minmax).setOnClickListener(this::onMinusClick);
        scatterLayout.findViewById(R.id.scatter_plus).setOnClickListener(this::onPlusClick);

        LayoutInflater map_inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout mapLayout = (LinearLayout) map_inflater.inflate(R.layout.mapview, null);

        mapView = mapLayout.findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        addContentView(mapLayout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        mapLayout.setX(0);
        mapLayout.setY(0);
        mapLayout.bringToFront();

        locationSource =
                new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);




        // 빨간색 box 그리기 - 어떤 오브젝트나 장소가 detect되면 그부분을 표시해주기 위한 box
        drawBox = new DrawView(this);
        addContentView(drawBox, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // View Renderable을 이용해서 그려주는 object는 text, 3d model, image
        // 각각 source를 가지고와서 view renderable object를 만들어줌
        nameHeader = getLayoutInflater().inflate(R.layout.object_name, null, false);
        for (int nameIndex = 0 ; nameIndex < 5 ; nameIndex++) {
            viewObjectName[nameIndex] = nameHeader.findViewById(R.id.name);
        }

        CompletableFuture<ViewRenderable> objectName1 =
                ViewRenderable.builder().setView(this, R.layout.object_name).build();
        CompletableFuture<ViewRenderable> objectName2 =
                ViewRenderable.builder().setView(this, R.layout.object_name).build();
        CompletableFuture<ViewRenderable> objectName3 =
                ViewRenderable.builder().setView(this, R.layout.object_name).build();
        CompletableFuture<ViewRenderable> objectName4 =
                ViewRenderable.builder().setView(this, R.layout.object_name).build();
        CompletableFuture<ViewRenderable> objectName5 =
                ViewRenderable.builder().setView(this, R.layout.object_name).build();

        CompletableFuture.allOf(objectName1, objectName2, objectName3, objectName4, objectName5).handle(
                (notUsed, throwable) -> {
                    if (throwable != null) {
                        DemoUtils.displayError(this, "Unable to load renderable", throwable);
                        return null;
                    }
                    // 각각 Renderable과 연결
                    try {
                        labelRenderable[0] = objectName1.get();
                        labelRenderable[1] = objectName2.get();
                        labelRenderable[2] = objectName3.get();
                        labelRenderable[3] = objectName4.get();
                        labelRenderable[4] = objectName5.get();
                    } catch (InterruptedException | ExecutionException ex) {
                        DemoUtils.displayError(this, "Unable to load rederable", ex);
                    }
                    return null;
                });


/*
        gestureDetector =
                new GestureDetector(
                        this,
                        new GestureDetector.SimpleOnGestureListener() {
                            @Override
                            public boolean onDown(MotionEvent e) {
                                return true;
                            }
                            @Override
                            public boolean onSingleTapUp(MotionEvent e) {
                                return true;
                            }
                        });


        // 카메라 fragment 부분의 터치 리스너
        arFragment.getArSceneView().getScene().setOnTouchListener(
                (HitTestResult hitTestResult, MotionEvent motionEvent) -> {
                    if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                        // 코드 넣으면 터치시 동작함
                    }
                    return gestureDetector.onTouchEvent(motionEvent);
                });
*/

        // 매번 업데이트 되는 listener
        // 여기서 매번 업데이트 되는 frame data를 가져옴
        arFragment.getArSceneView().getScene().addOnUpdateListener(new Scene.OnUpdateListener() {
            @Override
            public void onUpdate(FrameTime frameTime) {

                Frame frame = arFragment.getArSceneView().getArFrame();
                if (frame == null) {
                    return;
                }

                if (frame.getCamera().getTrackingState() != TrackingState.TRACKING) {
                    return;
                }

                long now = System.currentTimeMillis();

                for (Plane plane : frame.getUpdatedTrackables(Plane.class)) { // plane이 detection 되는지 검사하는 부분, !!!!!!!잘 안되는것 같음.

                    if (hitFlag) {
                        objLabelMain = objLabelTemp;
                        ytIDMain = ytIDTemp;
                        gnLinkMain = gnLinkTemp;

                        viewObjectName[objectCount].setText(objLabelMain);
                        viewObjectName[objectCount] = (TextView) labelRenderable[objectCount].getView();

                        addObject(objLabelMain, gnLinkMain, ytIDMain);
                        hitFlag = false;
                        break;
                    }
                }

                displayrotation = display.getRotation();

                if (now - lastSend > 0) {
                    try {
                        img = frame.acquireCameraImage(); // frame data를 이미지로 가져옴
                    } catch (Exception e) {
                        Log.e(TAG, "Can't acquire CameraImage");
                    }
                }
            }
        });

        /*
        arFragment.setOnTapArPlaneListener(((hitResult, plane, motionEvent) -> {
            place3DModel(hitResult.createAnchor());
        }));
        */
        //Thread webboxThread = new Thread(new LayoutPositionRunnable(inflatedLayout));
        //webboxThread.start();

        //Thread webbox_positionThread = new Thread(new LayoutPositionRunnable(inflatedLayout));
        //webbox_positionThread.start();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,  @NonNull int[] grantResults) {
        if (locationSource.onRequestPermissionsResult(
                requestCode, permissions, grantResults)) {
            return;
        }
        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        naverMap.setLocationSource(locationSource);
        naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);
    }


    /*
    private void place3DModel(Anchor anchor) {
        // 3D asset test
        ModelRenderable.builder()
                .setSource(this, RenderableSource.builder().setSource(
                        this, Uri.parse(GLTF_ASSET),
                        RenderableSource.SourceType.GLTF2)
                        .setScale(0.2f).setRecenterMode(RenderableSource.RecenterMode.ROOT).build()
                ).setRegistryId(GLTF_ASSET).build()
                .thenAccept(renderable -> add3DModelToScene(renderable, anchor))
                .exceptionally(
                        throwable -> {
                            Toast toast = Toast.makeText(this, "Unable to load renderable " + GLTF_ASSET, Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            return null;
                        });
    }

    private void add3DModelToScene(ModelRenderable renderable, Anchor anchor) {
        AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.setRenderable(renderable);
        arFragment.getArSceneView().getScene().addChild(anchorNode);
    }
    */

    private void addObject(CharSequence label, String addr, String ytID) {
        Frame frame = arFragment.getArSceneView().getArFrame();

        if (frame != null) {
            List<HitResult> result = frame.hitTest(hitW, (hitH+boxH));

            for (HitResult hit : result) {
                Trackable trackable = hit.getTrackable();
                if (trackable instanceof Plane && (((Plane) trackable).isPoseInPolygon(hit.getHitPose()))) {

                    lastUpdate = System.currentTimeMillis();
                    if (num_object > MAX_NUM) {
                        Objects.requireNonNull(labelAnchorNodeQueue.poll().getAnchor()).detach();
                        num_object--;
                    }

                    Anchor anchor = hit.createAnchor();
                    AnchorNode anchorNode = new AnchorNode(anchor);
                    anchorNode.setParent(arFragment.getArSceneView().getScene());

                    planeType.setText(R.string.horizontal);

                    if (trackable instanceof Plane) {
                        //Log.i("label", label + "");
                        anchorNode.addChild(createLabelView(objectCount, label.toString(), addr, ytID)); // anchor에 node들 달아놓음 (트리와 같은 구조라고 생각하면 됨)
                        Toast.makeText(getApplicationContext(), "Object Added " + objectCount, Toast.LENGTH_SHORT).show();
                        objectCount++;
                    }

                    if(((Plane) trackable).getType() == Plane.Type.VERTICAL) {
                        planeType.setText(R.string.vertical);
                        //viewSystem.setLookDirection(Vector3.forward());
                    }

                    /*
                    anchorNode.setOnTapListener((hitTestResult, motionEvent) -> {
                        if (isWebMode) {
                            PopupWebview webdialog = new PopupWebview();
                            webdialog.setWebURL(gnLinkMain);
                            webdialog.show(fragManager, "Web");
                        } else if (isVideoMode) {
                            PopupVideo videodialog = new PopupVideo();
                            videodialog.setVideoID(ytIDMain);
                            videodialog.show(fragManager, "Video");
                        }
                    });
                    */

                    num_object++;
                    labelAnchorNodeQueue.offer(anchorNode);
                    if (objectCount == 5) {
                        objectCount = 0;
                    }
                }
            }
        }

    }

    private void deleteObject(View view) {
        Toast.makeText(getApplicationContext(), "Delete all objects", Toast.LENGTH_SHORT).show();
        List<Node> nodeList = new ArrayList<>(arFragment.getArSceneView().getScene().getChildren());
        for(Node childNode : nodeList) {
            if (childNode instanceof AnchorNode) {
                if(((AnchorNode) childNode).getAnchor() != null) {
                    Objects.requireNonNull(((AnchorNode) childNode).getAnchor()).detach();
                    childNode.setParent(null);
                }
            }
        }
    }

    private Node createLabelView(int count, String label, String addr, String ytID) {
        Node name = new Node();
        name.setRenderable(labelRenderable[count]); // text box 의  node
        //name.setLocalPosition(new Vector3(0f, 0f, 0f));

        objYtID[count] = ytID;
        objGnLink[count] = addr;

        name.setOnTapListener((hitTestResult, motionEvent) -> {
            touchedObject = count;

            if (isWebMode) {
                webView.loadUrl(objGnLink[count]);
            } else if (isVideoMode) {
                webView.loadUrl("https://www.youtube.com/embed/" + objYtID[count]);
            }

            Toast.makeText(getApplicationContext(), "No. " + touchedObject + " " + label + "selected", Toast.LENGTH_SHORT).show();
        });

        return name;
    }

    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
        String openGlVersionString =
                ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                    .show();
            activity.finish();
            return false;
        }
        return true;
    }

    public void buttonAnimation() {
        if (isSettingOpen) {
            clearButton.startAnimation(settingClose);
            sensorButton.startAnimation(settingClose);
            statusButton.startAnimation(settingClose);
            clearText.startAnimation(settingClose);
            sensorText.startAnimation(settingClose);
            statusText.startAnimation(settingClose);
            clearButton.setClickable(false);
            sensorButton.setClickable(false);
            statusButton.setClickable(false);
            isSettingOpen = false;
        } else {
            clearButton.startAnimation(settingOpen);
            sensorButton.startAnimation(settingOpen);
            statusButton.startAnimation(settingOpen);
            clearText.startAnimation(settingOpen);
            sensorText.startAnimation(settingOpen);
            statusText.startAnimation(settingOpen);
            clearButton.setClickable(true);
            sensorButton.setClickable(true);
            statusButton.setClickable(true);
            isSettingOpen = true;
        }
    }

    public void buttonOnClick(View v) {
        buttonAnimation();
    }


    public boolean onTouch (View view, MotionEvent motionEvent) {

        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            View parent = (View) view.getParent().getParent();
            View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(parent);
            parent.startDragAndDrop(null, shadowBuilder, parent, 0);
            parent.setVisibility(View.INVISIBLE);
            parent.bringToFront();
            return true;
        } else
            return false;
    }

    public boolean onDrag(View v, DragEvent event) {
        switch (event.getAction()) {
            case DragEvent.ACTION_DROP:

                float x = event.getX();
                float y = event.getY();

                View view = (View) event.getLocalState();
                view.setX(x - view.getWidth()/2);
                view.setY(y);
                view.setVisibility(View.VISIBLE);

            default:
                break;
        }
        return true;
    }

    public void onClick(View view) {
        RelativeLayout parent = (RelativeLayout) view.getParent().getParent();
        View child = (View) parent.getChildAt(1);

        FrameLayout.LayoutParams max_size = new FrameLayout.LayoutParams(1500, 1000 );
        FrameLayout.LayoutParams chart_size = new FrameLayout.LayoutParams(900, 600 );

        switch (MinMaxMode) {
            case -1:
                child.setVisibility(View.INVISIBLE);
                parent.setLayoutParams(chart_size);

                MinMaxMode ++;
                break;

            case 0:

                child.setVisibility(View.VISIBLE);
                MinMaxMode ++;
                break;

            case 1:

                Display disp = getWindowManager().getDefaultDisplay();
                Point size = new Point();
                disp.getSize(size);

                parent.setLayoutParams(max_size);

                parent.setX(size.x/2 - 1500/2);
                parent.setY(size.y/2 - 1000/2);

                parent.bringToFront();;
                MinMaxMode = -1;

                break;

            default:
                break;
        }

    }


    public void onPlusClick (View view) {

        RelativeLayout parent = (RelativeLayout) view.getParent().getParent();
        View child = (View) parent.getChildAt(1);

        float width = parent.getWidth();
        float height = parent.getHeight();

        FrameLayout.LayoutParams chart_size = new FrameLayout.LayoutParams((int) (width * 1.1f), (int) (height * 1.1f));
        parent.setLayoutParams(chart_size);
        //scaleConst = scaleConst + 0.02f;

        parent.bringToFront();

    }

    public void onMinusClick (View view) {

        RelativeLayout parent = (RelativeLayout) view.getParent().getParent();
        View child = (View) parent.getChildAt(1);

        float width = parent.getWidth();
        float height = parent.getHeight();

        FrameLayout.LayoutParams chart_size = new FrameLayout.LayoutParams((int) (width * 0.9f), (int) (height * 0.9f));
        parent.setLayoutParams(chart_size);
        //scaleConst = scaleConst - 0.02f;

        parent.bringToFront();

    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();

        int cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int locationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);

        if (cameraPermission == PackageManager.PERMISSION_GRANTED || locationPermission == PackageManager.PERMISSION_GRANTED) {
            arFragment.onResume();
            sensorControl.registerSensor();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, 0 );
        }

        sendFlag = true;

        //yoloAndOCRWorker.setDaemon(true);
        //yoloAndOCRWorker.start();

        //progressWorker.setDaemon(true);
        //progressWorker.start();

       // boxWorker.setDaemon(true);
       // boxWorker.start();

    }

    @Override
    protected void onPause() {
        super.onPause();
        arFragment.onPause();
        mapView.onPause();
        sensorControl.unregisterSensor();
        sendFlag = false;

      //  yoloAndOCRWorker.interrupt();
       // progressWorker.interrupt();

      //  boxWorker.interrupt();
    }

    @Override
    protected void onStop() {
        super.onStop();
        arFragment.onStop();
        mapView.onStop();
       // yoloAndOCRWorker.interrupt();
       // progressWorker.interrupt();
       // boxWorker.interrupt();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        arFragment.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);

    }

    public class BoxRunnable extends Thread implements Runnable {
        @Override
        public void run() {
            while (true) {
                if (sensorControl.aX > 0.4 || sensorControl.aX < -0.4 || sensorControl.aY > 0.4 || sensorControl.aY < -0.4) {
                    drawBox.setPosition(0,0,0,0);
                }
            }
        }
    }

    public class yoloAndOCRRunnable extends Thread implements Runnable {
        @Override
        public void run() {
            while(sendFlag) {
                try {
                    serverConnectionMain = new ServerConnectionMain();

                    if(sensorControl.aX > -0.02 && sensorControl.aX < 0.02 && sensorControl.aY > -0.02 && sensorControl.aY < 0.02) {
                        serverConnectionMain.yoloAndOCR(img, displayrotation);
                        Log.d(TAG, "Send Frame");
                    }
                    lastSend = System.currentTimeMillis();
                    serverDataSize += serverConnectionMain.readByteCount;
                    serverRx.setText("Received: " + (serverDataSize / 1024) + "KB");

                    img.close();

                    hitW = serverConnectionMain.posX * width;
                    hitH = serverConnectionMain.posY * height;
                    boxW = serverConnectionMain.boxX / 640 * width;
                    boxH = serverConnectionMain.boxY / 360 * height;

                    // 좌표값 변경되면 box 위치와 크기 변경해줌
                    if(hitW != 0 && hitH != 0) {
                        objLabelTemp = serverConnectionMain.name;
                        ytIDTemp = serverConnectionMain.ytID;
                        gnLinkTemp = serverConnectionMain.gnLink;

                        drawBox.setPosition(hitW, hitH, boxW, boxH);

                        hitFlag = true;
                    }

                    processedDataSize += serverConnectionMain.readByteCount;
                    processedData.setText("Processed : " + (processedDataSize / 1024) + "KB");

                } catch (Exception e) {
                    //e.printStackTrace();
                    //Log.e(TAG, "Can't close image");
                }
            }
        }
    }

    public class ProgressBarRunnable extends Thread implements Runnable {
        @Override
        public void run() {
            while(true) {
                maxValue = serverDataSize;
                currentValue = processedDataSize;
                if (maxValue != 0) {
                    progress = (currentValue / maxValue) * 100;
                }
                progressBar.setProgress((int) progress);

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public class LayoutPositionRunnable implements Runnable {

        public int x = 0;
        public int y = 0;
        LinearLayout linearLayout;
        RelativeLayout relativeLayout;

        public LayoutPositionRunnable (LinearLayout layout) {
            this.linearLayout = layout;
        }
        public LayoutPositionRunnable (RelativeLayout layout) {
            this.relativeLayout = layout;
        }

        public void run() {

            while (true) {
                x = (int) (Math.random() * 2000);
                y = (int) (Math.random() * 1000);


                linearLayout.setX(x);
                linearLayout.setY(y);

                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
