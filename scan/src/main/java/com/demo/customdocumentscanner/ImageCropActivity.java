package com.demo.customdocumentscanner;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.demo.customdocumentscanner.base.CropperErrorType;
import com.demo.customdocumentscanner.base.DocumentScanActivity;
import com.demo.customdocumentscanner.base.libraries.PolygonView;
import com.demo.customdocumentscanner.helpers.ScannerConstants;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Callable;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class ImageCropActivity extends DocumentScanActivity {

    private FrameLayout holderImageCrop;
    private ImageView imageView;
    private PolygonView polygonView;
    private boolean isInverted;
    private ProgressBar progressBar;
    private Bitmap cropImage;
    private OnClickListener btnImageEnhanceClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            showProgressBar();
            disposable.add(
                    Observable.fromCallable(new Callable<Object>() {
                        @Override
                        public Object call() throws Exception {
                            cropImage = getCroppedImage();
                            if (cropImage == null)
                                return false;
                            if (ScannerConstants.saveStorage)
                                saveToInternalStorage(cropImage);
                            return false;
                        }
                    })
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Consumer<Object>() {
                                @Override
                                public void accept(Object result) throws Exception {
                                    hideProgressBar();
                                    if (cropImage != null) {
                                        ScannerConstants.selectedImageBitmap = cropImage;
                                        saveToInternalStorage(cropImage);
                                        setResult(RESULT_OK);
                                        finish();
                                    }
                                }
                            })
            );
        }
    };
    private OnClickListener btnRebase = new OnClickListener() {
        @Override
        public void onClick(View v) {
            cropImage = ScannerConstants.selectedImageBitmap.copy(ScannerConstants.selectedImageBitmap.getConfig(), true);
            isInverted = false;
            ImageCropActivity.this.startCropping();
        }
    };
    private OnClickListener btnCloseClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            ImageCropActivity.this.finish();
        }
    };
    private OnClickListener btnInvertColor = new OnClickListener() {
        @Override
        public void onClick(View v) {
            showProgressBar();
            disposable.add(
                    Observable.fromCallable(new Callable<Object>() {
                        @Override
                        public Object call() throws Exception {
                            invertColor();
                            return false;
                        }
                    })
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Consumer<Object>() {
                                @Override
                                public void accept(Object result) throws Exception {
                                    hideProgressBar();
                                    Bitmap scaledBitmap = scaledBitmap(cropImage, holderImageCrop.getWidth(), holderImageCrop.getHeight());
                                    imageView.setImageBitmap(scaledBitmap);
                                    cropImage = scaledBitmap;
                                }
                            })
            );
        }
    };
    private OnClickListener onRotateClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            showProgressBar();
            disposable.add(
                    Observable.fromCallable(new Callable<Object>() {
                        @Override
                        public Object call() throws Exception {
                            if (isInverted)
                                invertColor();
                            cropImage = rotateBitmap(cropImage, 90);
                            return false;
                        }
                    })
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Consumer<Object>() {
                                @Override
                                public void accept(Object result) throws Exception {
                                    hideProgressBar();
                                    startCropping();
                                }
                            })
            );
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_crop);
        cropImage = ScannerConstants.selectedImageBitmap;
        isInverted = false;
        if (ScannerConstants.selectedImageBitmap != null)
            initView();
        else {
            Toast.makeText(this, ScannerConstants.imageError, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected FrameLayout getHolderImageCrop() {
        return holderImageCrop;
    }

    @Override
    protected ImageView getImageView() {
        return imageView;
    }

    @Override
    protected PolygonView getPolygonView() {
        return polygonView;
    }

    @Override
    protected void showProgressBar() {
        RelativeLayout rlContainer = findViewById(R.id.rlContainer);
        setViewInteract(rlContainer, false);
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    protected void hideProgressBar() {
        RelativeLayout rlContainer = findViewById(R.id.rlContainer);
        setViewInteract(rlContainer, true);
        progressBar.setVisibility(View.GONE);
    }

    @Override
    protected void showError(CropperErrorType errorType) {
        switch (errorType) {
            case CROP_ERROR:
                Toast.makeText(this, ScannerConstants.cropError, Toast.LENGTH_LONG).show();
                break;
        }
    }

    @Override
    protected Bitmap getBitmapImage() {
        return cropImage;
    }

    private void setViewInteract(View view, boolean canDo) {
        view.setEnabled(canDo);
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                setViewInteract(((ViewGroup) view).getChildAt(i), canDo);
            }
        }
    }

    private void initView() {
        TextView btnImageCrop = findViewById(R.id.btnImageCrop);
        TextView btnClose = findViewById(R.id.btnClose);
        holderImageCrop = findViewById(R.id.holderImageCrop);
        imageView = findViewById(R.id.imageView);
        ImageView ivRotate = findViewById(R.id.ivRotate);
        ImageView ivInvert = findViewById(R.id.ivInvert);
        ImageView ivRebase = findViewById(R.id.ivRebase);
        ImageView ivBack = findViewById(R.id.iv_back);
        //btnImageCrop.setText(ScannerConstants.cropText);
        //btnClose.setText(ScannerConstants.backText);
        polygonView = findViewById(R.id.polygonView);
        progressBar = findViewById(R.id.progressBar);
        if (progressBar.getIndeterminateDrawable() != null && ScannerConstants.progressColor != null)
            progressBar.getIndeterminateDrawable().setColorFilter(Color.parseColor(ScannerConstants.progressColor), android.graphics.PorterDuff.Mode.MULTIPLY);
        else if (progressBar.getProgressDrawable() != null && ScannerConstants.progressColor != null)
            progressBar.getProgressDrawable().setColorFilter(Color.parseColor(ScannerConstants.progressColor), android.graphics.PorterDuff.Mode.MULTIPLY);
        //btnImageCrop.setBackgroundColor(Color.parseColor(ScannerConstants.cropColor));
        //btnClose.setBackgroundColor(Color.parseColor(ScannerConstants.backColor));
        btnImageCrop.setOnClickListener(btnImageEnhanceClick);
        btnClose.setOnClickListener(btnCloseClick);
        ivBack.setOnClickListener(btnCloseClick);
        ivRotate.setOnClickListener(onRotateClick);
        ivInvert.setOnClickListener(btnInvertColor);
        ivRebase.setOnClickListener(btnRebase);
        startCropping();
    }

    private void invertColor() {
        if (!isInverted) {
            Bitmap bmpMonochrome = Bitmap.createBitmap(cropImage.getWidth(), cropImage.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmpMonochrome);
            ColorMatrix ma = new ColorMatrix();
            ma.setSaturation(0);
            Paint paint = new Paint();
            paint.setColorFilter(new ColorMatrixColorFilter(ma));
            canvas.drawBitmap(cropImage, 0, 0, paint);
            cropImage = bmpMonochrome.copy(bmpMonochrome.getConfig(), true);
        } else {
            cropImage = cropImage.copy(cropImage.getConfig(), true);
        }
        isInverted = !isInverted;
    }

    private String saveToInternalStorage(Bitmap bitmapImage) {
        //File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/LiscioScanner");
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "cropped_" + timeStamp + ".jpeg";
        File mypath = new File(directory, imageFileName);
        ScannerConstants.filePath = String.valueOf(mypath);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            bitmapImage.compress(Bitmap.CompressFormat.JPEG, 70, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return directory.getAbsolutePath();
    }
}
