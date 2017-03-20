package com.sunlinlin.tesstwo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.oginotihiro.cropview.CropView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.sunlinlin.tesstwo.SDUtils.assets2SD;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    private Button recBtn;
    private TextView resultTv;
    private CropView cropView;

    private ViewPager vp;
    private MyPagerAdapter adapter;
    private List<ImageView> list;
    private int[] ids = new int[]{R.drawable.chaxun, R.drawable.fan, R.drawable.fengying,
            R.drawable.meishi, R.drawable.mj, R.drawable.mjn, R.drawable.quanbu, R.drawable.tupian, R.drawable.yingyu, R.drawable.zheng,
            R.drawable.xingming, R.drawable.minzu};

    private Button pickBtn;
    private Button widthBtn, heightBtn, cropRecBtn;
    private ImageView resultIv;
    private ScrollView scrollView;
    /**
     * TessBaseAPI初始化用到的第一个参数，是个目录。
     */
    private static final String DATAPATH = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator;
    /**
     * 在DATAPATH中新建这个目录，TessBaseAPI初始化要求必须有这个目录。
     */
    private static final String tessdata = DATAPATH + File.separator + "tessdata";
    /**
     * TessBaseAPI初始化测第二个参数，就是识别库的名字不要后缀名。
     */
    private static final String DEFAULT_LANGUAGE = "chi_sim";
    /**
     * assets中的文件名
     */
    private static final String DEFAULT_LANGUAGE_NAME = DEFAULT_LANGUAGE + ".traineddata";
    /**
     * 保存到SD卡中的完整文件名
     */
    private static final String LANGUAGE_PATH = tessdata + File.separator + DEFAULT_LANGUAGE_NAME;

    /**
     * 权限请求值
     */
    private static final int PERMISSION_REQUEST_CODE = 0;

    private static final int PICK_REQUEST_CODE = 10;

    /**
     * 截图框的高
     */
    private int cropHeight;
    /**
     * 截图框的宽
     */
    private int cropWidth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        scrollView = (ScrollView) findViewById(R.id.sv);
        recBtn = (Button) findViewById(R.id.btn_rec);
        pickBtn = (Button) findViewById(R.id.btn_pick);
        widthBtn = (Button) findViewById(R.id.btn_width);
        heightBtn = (Button) findViewById(R.id.btn_height);
        cropRecBtn = (Button) findViewById(R.id.btn_crop_rec);
        resultTv = (TextView) findViewById(R.id.tv);
        cropView = (CropView) findViewById(R.id.iv);
        resultIv = (ImageView) findViewById(R.id.iv_result);
        vp = (ViewPager) findViewById(R.id.vp);

        recBtn.setOnClickListener(this);
        pickBtn.setOnClickListener(this);
        widthBtn.setOnClickListener(this);
        heightBtn.setOnClickListener(this);
        cropRecBtn.setOnClickListener(this);

        resultIv.setVisibility(View.INVISIBLE);
        cropView.setVisibility(View.INVISIBLE);

        initList();
        adapter = new MyPagerAdapter(list);
        vp.setAdapter(adapter);


        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            }
        }

        //Android6.0之前安装时就能复制，6.0之后要先请求权限，所以6.0以上的这个方法无用。

        assets2SD(getApplicationContext(), LANGUAGE_PATH, DEFAULT_LANGUAGE_NAME);

        cropView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    //允许ScrollView截断点击事件，ScrollView可滑动
                    scrollView.requestDisallowInterceptTouchEvent(false);
                } else {
                    //不允许ScrollView截断点击事件，点击事件由子View处理
                    scrollView.requestDisallowInterceptTouchEvent(true);
                }
                return false;
            }
        });
    }

    /**
     * 识别图像
     *
     * @param bitmap
     */
    private void recognition(final Bitmap bitmap) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                long startTime = System.currentTimeMillis();
                Log.i(TAG, "run: kaishi " + startTime);
                TessBaseAPI tessBaseAPI = new TessBaseAPI();
                tessBaseAPI.init(DATAPATH, DEFAULT_LANGUAGE);
                tessBaseAPI.setImage(bitmap);
                String text = tessBaseAPI.getUTF8Text();
                long finishTime = System.currentTimeMillis();
                Log.i(TAG, "run: jieshu " + finishTime);
                Log.i(TAG, "run: text " + text);
                text = text + "\r\n" + " 耗时" + (finishTime - startTime) + "毫秒";
                final String finalText = text;
                final Bitmap finalBitmap = bitmap;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        resultTv.setText(finalText);
                        cropView.setImageBitmap(finalBitmap);
                    }
                });
                tessBaseAPI.end();
            }
        }).start();
    }

    private void initList() {
        list = new ArrayList<>();
        for (int i = 0; i < ids.length; i++) {
            ImageView imageView = new ImageView(this);
            imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            imageView.setImageResource(ids[i]);
            list.add(imageView);
        }

    }

    /**
     * 请求到权限后在这里复制识别库
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionsResult: " + grantResults[0]);
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "onRequestPermissionsResult: copy");
                    assets2SD(getApplicationContext(), LANGUAGE_PATH, DEFAULT_LANGUAGE_NAME);
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_REQUEST_CODE) {
                Uri source = data.getData();
                cropHeight = 1;
                cropWidth = 1;
                cropView.setVisibility(View.VISIBLE);
                resultIv.setVisibility(View.INVISIBLE);
                cropView.of(source).withAspect(cropWidth, cropHeight).initialize(MainActivity.this);
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_rec:
                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), ids[vp.getCurrentItem()]);
                recognition(bitmap);
                break;
            case R.id.btn_pick:
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*");
                startActivityForResult(intent, PICK_REQUEST_CODE);
                break;
            case R.id.btn_height:
                cropHeight++;
                cropView.setHeight(cropHeight);
                break;
            case R.id.btn_width:
                cropWidth++;
                cropView.setWidth(cropWidth);
                break;
            case R.id.btn_crop_rec:
                if (cropView.getVisibility() != View.VISIBLE) {
                    Toast.makeText(getApplicationContext(), "先选一张图片", Toast.LENGTH_SHORT).show();
                    return;
                }
                Bitmap bt = cropView.getOutput();
                cropView.setVisibility(View.INVISIBLE);
                resultIv.setVisibility(View.VISIBLE);
                resultIv.setImageBitmap(bt);
                recognition(bt);
                break;

        }
    }
}
