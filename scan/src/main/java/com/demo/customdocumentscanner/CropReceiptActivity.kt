package com.demo.customdocumentscanner;

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.demo.customdocumentscanner.R
import com.myapps.documentscanner.data.OpenCVUtils
import com.myapps.documentscanner.data.PolygonView
import com.myapps.documentscanner.helpers.AppUtils
import org.opencv.android.OpenCVLoader
import java.io.IOException

class CropReceiptActivity : AppCompatActivity(), View.OnClickListener {
    override fun onClick(v: View?) {
        if(v!!.id == R.id.scanButton){
            sourceFrame!!.post{
                val croppedBitmap = OpenCVUtils.cropReceiptByFourPoints(bitmap!!, polygonView!!.getListPoint(), sourceImageView.width, sourceImageView.height)
                val savedPath = AppUtils.saveBitmapToFile(croppedBitmap!!)
                val intent = Intent()
                intent.putExtra("path", savedPath)
                setResult(RESULT_OK, intent);
                finish();
            }
        }
    }

    init {
        //here goes static initializer code
        if (!OpenCVLoader.initDebug()) {
            Log.e("Scan", "OpenCVLoader.initDebug() = FALSE")
        } else {
            Log.e("Scan", "OpenCVLoader.initDebug() = TRUE")
        }
    }

    private var scanButton: Button? = null
    private lateinit var sourceImageView: ImageView
    private var sourceFrame: FrameLayout? = null
    private var polygonView: PolygonView? = null
    //    private var progressDialogFragment: ProgressDialogFragment? = null
//    private var scanner: IScanner? = null
    private var original: Bitmap? = null

    private var bitmap: Bitmap?
        get() {
            val uri = uri
            try {
                var bitmap = AppUtils.getBitmap(uri, this)
                bitmap!!.setDensity(Bitmap.DENSITY_NONE)
                if (bitmap.getWidth() > bitmap.getHeight()) {
                    bitmap = OpenCVUtils.rotate(bitmap, 90)
                }
//                this.contentResolver.delete(uri!!, null, null)
                return bitmap
            } catch (e: IOException) {
                e.printStackTrace()
            }

            return null
        }
        set(original) {
            val scaledBitmap = original?.let { Bitmap.createScaledBitmap(it, sourceImageView.width, sourceImageView.height, false) };
            sourceImageView.setImageBitmap(scaledBitmap)
            val pointFs = scaledBitmap?.let { OpenCVUtils.getEdgePoints(it, polygonView!!) }
            polygonView!!.points = pointFs!!
            polygonView!!.visibility = View.VISIBLE

            /*val layoutParams = FrameLayout.LayoutParams(sourceImageView.width, sourceImageView.height)
            layoutParams.gravity = Gravity.CENTER
            polygonView!!.layoutParams = layoutParams*/
        }

    private val uri: Uri?
        get() = intent.getParcelableExtra<Uri>("path")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crop_receipt)
       // getSupportActionBar()!!.setDisplayHomeAsUpEnabled(true)
        init()
    }

    private fun init() {
        sourceImageView = findViewById(R.id.sourceImageView)
        scanButton = findViewById(R.id.scanButton)
        scanButton!!.setOnClickListener(this)
        sourceFrame = findViewById(R.id.sourceFrame)
        polygonView = findViewById(R.id.polygonView)
        sourceFrame!!.post {
            original = bitmap
            if (original != null) {
                bitmap = original
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.getItemId()) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

}