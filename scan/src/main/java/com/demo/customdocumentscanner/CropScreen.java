package com.demo.customdocumentscanner;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.myapps.documentscanner.data.OpenCVUtils;
import com.myapps.documentscanner.data.PolygonView;
import com.myapps.documentscanner.helpers.AppUtils;

import org.opencv.core.Point;

import java.io.IOException;
import java.util.Map;

public class CropScreen extends AppCompatActivity {
    public Uri fileUri, tempFileUri;
    private TextView scanButton, backButton;
    private ImageView sourceImageView;
    private FrameLayout sourceFrame;
    private PolygonView polygonView;
    private View view;
    private Bitmap original;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop_screen);
        init();
    }

    private void init() {
        sourceImageView = findViewById(R.id.sourceImageView);
        scanButton = findViewById(R.id.tv_crop);
        sourceFrame = findViewById(R.id.sourceFrame);
        polygonView = findViewById(R.id.polygonView);
        sourceFrame.post(new Runnable() {
            @Override
            public void run() {
                original = getBitmap();
                if (original != null) {
                    setBitmap(original);
                }
            }
        });

        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap croppedBitmap = OpenCVUtils.Companion.cropReceiptByFourPoints(original, polygonView.getListPoint(), sourceImageView.getWidth(), sourceImageView.getHeight());
                String savedPath = AppUtils.Companion.saveBitmapToFile(croppedBitmap);
                Intent intent =new Intent();
                intent.putExtra("path", savedPath);
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }

    private Bitmap getBitmap() {
        Uri uri = getUri();
        try {
            Bitmap bitmap = AppUtils.Companion.getBitmap(uri, this);
            bitmap.setDensity(Bitmap.DENSITY_NONE);
            if (bitmap.getWidth() > bitmap.getHeight()) {
                bitmap = OpenCVUtils.Companion.rotate(bitmap, 90);
            }
            return bitmap;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void setBitmap(Bitmap original) {
        Bitmap scaledBitmap =   Bitmap.createScaledBitmap(original, sourceFrame.getWidth(), sourceFrame.getHeight(), false);
        sourceImageView.setImageBitmap(scaledBitmap);
        Bitmap tempBitmap = ((BitmapDrawable) sourceImageView.getDrawable()).getBitmap();
        Map<Integer, Point> pointFs =  OpenCVUtils.Companion.getEdgePoints(scaledBitmap, polygonView);
        if(pointFs.get(0).x < 10.0 || pointFs.get(1).x == 900 || pointFs.get(2).x == 0.0 || pointFs.get(3).x == 900) {
            pointFs.put(0, new Point(9.0, 16.0));
            pointFs.put(1, new Point(500.0, 16.0));
            pointFs.put(2, new Point(9.0, 500.0));
            pointFs.put(3, new Point(500.0, 500.0));
            polygonView.setPoints(pointFs);

        } else {
            polygonView.setPoints(pointFs);
        }
        polygonView.setVisibility(View.VISIBLE);
        int padding = (int) getResources().getDimension(R.dimen.scanPadding);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(tempBitmap.getWidth() , tempBitmap.getHeight());
        layoutParams.gravity = Gravity.CENTER;
        polygonView.setLayoutParams(layoutParams);
    }


    private Uri getUri() {
        Uri uri = getIntent().getParcelableExtra("path");

        return uri;
    }

    private Bitmap scaledBitmap(Bitmap bitmap, int width, int height) {
        Matrix m = new Matrix();
        m.setRectToRect(new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight()), new RectF(0, 0, width, height), Matrix.ScaleToFit.CENTER);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
    }

}
