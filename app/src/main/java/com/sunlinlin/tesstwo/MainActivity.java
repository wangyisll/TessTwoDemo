package com.sunlinlin.tesstwo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private Button btn;
    private TextView tv;

    private ViewPager vp;
    private MyPagerAdapter adapter;
    private List<ImageView> list;
    private int[] ids = new int[]{R.drawable.chaxun, R.drawable.fan, R.drawable.fengying,
            R.drawable.meishi, R.drawable.mj, R.drawable.mjn, R.drawable.quanbu, R.drawable.tupian, R.drawable.yingyu, R.drawable.zheng};

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn = (Button) findViewById(R.id.btn);
        tv = (TextView) findViewById(R.id.tv);
        vp = (ViewPager) findViewById(R.id.vp);
        initList();
        adapter = new MyPagerAdapter();
        vp.setAdapter(adapter);




        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            }
        }

        //Android6.0之前安装时就能复制，6.0之后要先请求权限，所以6.0以上的这个方法无用。
        copyToSD(LANGUAGE_PATH, DEFAULT_LANGUAGE_NAME);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        long startTime = System.currentTimeMillis();
                        Log.i(TAG, "run: kaishi " + startTime);

                        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), ids[vp.getCurrentItem()]);
                        Log.i(TAG, "run: bitmap " + System.currentTimeMillis());

                        TessBaseAPI tessBaseAPI = new TessBaseAPI();

                        tessBaseAPI.init(DATAPATH, DEFAULT_LANGUAGE);

                        tessBaseAPI.setImage(bitmap);
                        String text = tessBaseAPI.getUTF8Text();
                        long finishTime = System.currentTimeMillis();
                        Log.i(TAG, "run: jieshu " + finishTime);
                        Log.i(TAG, "run: text "+text);
                        text = text +"\r\n"+" 耗时"+(finishTime-startTime)+"毫秒";
                        final String finalText = text;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tv.setText(finalText);
                            }
                        });

                        tessBaseAPI.end();
                    }
                }).start();
            }
        });

    }

    private void initList() {
        list = new ArrayList<>();
        for (int i = 0; i < ids.length; i++) {
            ImageView imageView = new ImageView(this);
            imageView.setImageResource(ids[i]);
            list.add(imageView);
        }

    }

    /**
     * 将assets中的识别库复制到SD卡中
     *
     * @param path 要存放在SD卡中的 完整的文件名。这里是"/storage/emulated/0//tessdata/chi_sim.traineddata"
     * @param name assets中的文件名 这里是 "chi_sim.traineddata"
     */
    public void copyToSD(String path, String name) {
        Log.i(TAG, "copyToSD: " + path);
        Log.i(TAG, "copyToSD: " + name);

        //如果存在就删掉
        File f = new File(path);
        if (f.exists()) {
            f.delete();
        }
        if (!f.exists()) {
            File p = new File(f.getParent());
            if (!p.exists()) {
                p.mkdirs();
            }
            try {
                f.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        InputStream is = null;
        OutputStream os = null;
        try {
            is = this.getAssets().open(name);
            File file = new File(path);
            os = new FileOutputStream(file);
            byte[] bytes = new byte[2048];
            int len = 0;
            while ((len = is.read(bytes)) != -1) {
                os.write(bytes, 0, len);
            }
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null)
                    is.close();
                if (os != null)
                    os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
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
                    copyToSD(LANGUAGE_PATH, DEFAULT_LANGUAGE_NAME);
                }
                break;
            default:
                break;
        }
    }

    class MyPagerAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public ImageView instantiateItem(ViewGroup container, int position) {
            container.addView(list.get(position));
            return list.get(position);
        }

    }
}
