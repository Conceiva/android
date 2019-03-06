package com.handwerkcloud.client;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.pdf.PdfDocument;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.os.Environment;
import android.os.SystemClock;
import android.print.PrintAttributes;
import android.print.pdf.PrintedPdfDocument;
import android.provider.MediaStore;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.util.SparseArray;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.github.barteksc.pdfviewer.PDFView;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.owncloud.android.R;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.ui.activity.UploadFilesActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;

public class OCRActivity extends Activity {
    static final String TAG = "OCRActivity";
    private CameraSource mCameraSource;
    private int requestPermissionID = 1;
    private CameraOverlaySurfaceView mCameraView;
    private TextView mTextView;
    ImageButton capture = null;
    public static final int PERMISSION_CODE = 42042;
    String mFilename;
    PDFView pdfView;

    Button textPreview;
    Button textOutline;
    ImageButton cancelBtn;
    ImageButton acceptBtn;
    ImageButton flashCameraButton;
    private TextRecognizer textRecognizer;
    private int cameraWidth = 1600;
    int cameraHeight = 1200;
    private int rotation;
    private boolean flashmode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ocr_activity);
        mTextView = findViewById(R.id.text_view);
        mCameraView = findViewById(R.id.surfaceView);
        pdfView = findViewById(R.id.pdfView);
        capture = findViewById(R.id.capture);
        capture.setEnabled(false);
        cancelBtn = findViewById(R.id.cancelBtn);
        acceptBtn = findViewById(R.id.acceptBtn);
        textPreview = findViewById(R.id.textPreview);
        textOutline = findViewById(R.id.textOutline);
        textPreview.setVisibility(View.GONE);
        textOutline.setVisibility(View.GONE);

        flashCameraButton = (ImageButton) findViewById(R.id.flash);
        capture.setOnClickListener(new View.OnClickListener() {
            private long mLastClickTime;

            @Override
            public void onClick(View view) {

                // mis-clicking prevention, using threshold of 1000 ms
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000){
                    return;
                }
                mLastClickTime = SystemClock.elapsedRealtime();
                takePicture();
            }
        });

        cancelBtn.setVisibility(View.GONE);
        acceptBtn.setVisibility(View.GONE);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                returnToCamera();
            }
        });

        acceptBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra(UploadFilesActivity.EXTRA_CHOSEN_FILES, new String[]{mFilename});
                setResult(UploadFilesActivity.RESULT_OK_AND_MOVE, resultIntent);
                finish();
            }
        });

        flashCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (flashmode) {
                    flashCameraButton.setImageResource(R.drawable.ic_flash_off);
                    mCameraSource.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                } else {
                    flashCameraButton.setImageResource(R.drawable.ic_flash_on);
                    mCameraSource.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                }

                flashmode = !flashmode;

            }
        });

        textPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCameraView.setDrawText(!mCameraView.getDrawText());
            }
        });

        textOutline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCameraView.setDrawOutline(!mCameraView.getDrawOutline());
            }
        });

        if (!getBaseContext().getPackageManager().hasSystemFeature(
            PackageManager.FEATURE_CAMERA_FLASH)) {
            flashCameraButton.setVisibility(View.GONE);
        }
        startCameraSource();
    }

    void returnToCamera() {
        //textPreview.setVisibility(View.VISIBLE);
        //textOutline.setVisibility(View.VISIBLE);
        pdfView.setVisibility(View.GONE);
        startCameraSource();
        mCameraView.setVisibility(View.VISIBLE);
        cancelBtn.setVisibility(View.GONE);
        acceptBtn.setVisibility(View.GONE);
        flashCameraButton.setImageResource(R.drawable.ic_flash_off);
    }

    @Override
    public void onBackPressed(){
        if (cancelBtn.getVisibility() == View.VISIBLE) {
            returnToCamera();
        }
        else {
            super.onBackPressed();
        }
    }

    private void showFlashButton(Camera.Parameters params) {
        boolean showFlash = (getPackageManager().hasSystemFeature(
            PackageManager.FEATURE_CAMERA_FLASH) && params.getFlashMode() != null)
            && params.getSupportedFlashModes() != null
            && params.getSupportedFocusModes().size() > 1;

        flashCameraButton.setVisibility(showFlash ? View.VISIBLE
            : View.INVISIBLE);

    }

    void takePicture() {
        mCameraSource.takePicture(null, new CameraSource.PictureCallback() {

            private File imageFile;
            @Override
            public void onPictureTaken(byte[] bytes) {
                try {
                    // convert byte array into bitmap
                    Bitmap bitmap = null;
                    bitmap = BitmapFactory.decodeByteArray(bytes, 0,
                        bytes.length);
                    
                    // rotate Image
                    Matrix rotateMatrix = new Matrix();
                    rotateMatrix.postRotate(rotation);
                    Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0,
                        bitmap.getWidth(), bitmap.getHeight(),
                        rotateMatrix, false);
                    bitmap = rotatedBitmap;
                    SparseArray<TextBlock> items = textRecognizer.detect(new Frame.Builder().setBitmap(bitmap).build());
                    mCameraView.setVisibility(View.GONE);
                    String filename = savePDF(items, bitmap);
                    if (filename == null) {
                        return;
                    }

                    mFilename = filename;

                    int permissionCheck = ContextCompat.checkSelfPermission(OCRActivity.this,
                        READ_EXTERNAL_STORAGE);

                    if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(
                            OCRActivity.this,
                            new String[]{READ_EXTERNAL_STORAGE},
                            PERMISSION_CODE
                        );

                        return;
                    }

                    textPreview.setVisibility(View.GONE);
                    textOutline.setVisibility(View.GONE);
                    pdfView.fromFile(new File(filename)).load();
                    pdfView.setVisibility(View.VISIBLE);
                    cancelBtn.setVisibility(View.VISIBLE);
                    acceptBtn.setVisibility(View.VISIBLE);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }

    private Bitmap resize(Bitmap image, int maxWidth, int maxHeight) {
        if (maxHeight > 0 && maxWidth > 0) {
            int width = image.getWidth();
            int height = image.getHeight();
            float ratioBitmap = (float) width / (float) height;
            float ratioMax = (float) maxWidth / (float) maxHeight;

            int finalWidth = maxWidth;
            int finalHeight = maxHeight;
            if (ratioMax > 1) {
                finalWidth = (int) ((float) maxHeight * ratioBitmap);
            } else {
                finalHeight = (int) ((float) maxWidth / ratioBitmap);
            }
            image = Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true);
            return image;
        } else {
            return image;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                PDFView pdfView = findViewById(R.id.pdfView);
                pdfView.fromFile(new File(mFilename)).load();
                pdfView.setVisibility(View.VISIBLE);
            }
        }
    }

    private String savePDF(SparseArray<TextBlock> items, Bitmap bitmap) {
        if (items == null) {
            return null;
        }

        // open a new document
        PrintedPdfDocument document = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            PrintAttributes printAttributes = new PrintAttributes.Builder().
                setMediaSize(PrintAttributes.MediaSize.ISO_A4).
                setMinMargins(PrintAttributes.Margins.NO_MARGINS).
                build();
            document = new PrintedPdfDocument(this,
                printAttributes);

            // start a page
            PdfDocument.Page page = document.startPage(0);
            // draw something on the page
            Canvas canvas = page.getCanvas();

            Paint paint = new Paint();
            paint.setColor(Color.BLACK);
            float ratiowidth = (float)canvas.getWidth() / bitmap.getWidth();
            float ratioheight = (float)canvas.getHeight() / bitmap.getHeight();
            for (int i = 0; i < items.size(); i++) {

                for (int j = 0; j < items.valueAt(i).getComponents().size(); j++) {
                    String text = items.valueAt(i).getComponents().get(j).getValue();
                    Rect rect = items.valueAt(i).getComponents().get(j).getBoundingBox();
                    rect.left = (int) (rect.left * ratiowidth);
                    rect.right = (int) (rect.right * ratiowidth);
                    rect.top = (int) (rect.top * ratioheight);
                    rect.bottom = (int) (rect.bottom * ratioheight);

                    float textSize = 8;

                    paint.setStyle(Paint.Style.STROKE);
                    //canvas.drawRect(rect, paint);
                    //paint.setStyle(Paint.Style.FILL);

                    canvas.save();
                    TextPaint textpaint = new TextPaint();
                    textSize = calculateMaxTextSize(text, textpaint, (rect.right - rect.left) - 5, (rect.bottom - rect.top) - 1);

                    textpaint.setTextSize(textSize);

                    StaticLayout mTextLayout = new StaticLayout(text, textpaint, rect.right - rect.left, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                    canvas.translate(rect.left, rect.top);
                    Log.d(TAG, text + " left: " + rect.left);
                    mTextLayout.draw(canvas);
                    canvas.restore();
                }
            }


            // finish the page
            document.finishPage(page);

            page = document.startPage(1);
            // draw something on the page
            canvas = page.getCanvas();
            Rect bitmapRect = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
            canvas.drawBitmap(bitmap, null, bitmapRect, null);
            // finish the page
            document.finishPage(page);

            String dir = Environment.getExternalStorageDirectory()+File.separator+"HandwerkCloud";
            //create folder
            File folder = new File(dir); //folder name
            folder.mkdirs();

            //create file
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String FileName = "capture" + timeStamp + ".pdf";
            File file = new File(dir, FileName);

            try {
                FileOutputStream oFile = new FileOutputStream(file);
                // write the document content
                document.writeTo(oFile);
                //oFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            //close the document
            document.close();

            return file.getAbsolutePath();
        }
        return null;
    }

    public static float calculateMaxTextSize(String text, Paint paint, int maxWidth, int maxHeight) {
        if (text == null || paint == null) return 0;
        Rect bound = new Rect();
        float size = 1.0f;
        float step= 1.0f;
        paint.setTextSize(size);
        while (true) {
            paint.getTextBounds(text, 0, text.length(), bound);
            if (bound.width() < maxWidth && bound.height() < maxHeight) {
                size += step;
                paint.setTextSize(size);
            } else {
                return size - step;
            }
        }
    }

    private void startCameraSource() {

        //Create the TextRecognizer
        textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();

        if (!textRecognizer.isOperational()) {
            Log.w(TAG, "Detector dependencies not loaded yet");
        } else {
            int numCameras=Camera.getNumberOfCameras();
            for (int i=0;i<numCameras;i++)
            {
                Camera.CameraInfo cameraInfo=new Camera.CameraInfo();
                Camera.getCameraInfo(i,cameraInfo);
                if (cameraInfo.facing== Camera.CameraInfo.CAMERA_FACING_BACK)
                {
                    Camera camera= Camera.open(i);
                    Camera.Parameters cameraParams=camera.getParameters();
                    List<Camera.Size> sizes= cameraParams.getSupportedPreviewSizes();
                    cameraWidth=sizes.get(0).width;
                    cameraHeight=sizes.get(0).height;
                    camera.release();
                }
            }
            //Initialize camerasource to use high resolution and set Autofocus on.
            mCameraSource = new CameraSource.Builder(getApplicationContext(), textRecognizer)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedPreviewSize(cameraWidth, cameraHeight)
                .setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)
                .setRequestedFps(2.0f)
                .build();

            /**
             * Add call back to SurfaceView and check if camera permission is granted.
             * If permission is granted we can start our cameraSource and pass it to surfaceView
             */
            mCameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    try {

                        if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                            Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

                            ActivityCompat.requestPermissions(OCRActivity.this,
                                new String[]{Manifest.permission.CAMERA},
                                requestPermissionID);
                            return;
                        }
                        mCameraSource.start(mCameraView.getHolder());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                }

                /**
                 * Release resources for cameraSource
                 */
                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    mCameraSource.stop();
                }
            });

            //Set the TextRecognizer's Processor.
            textRecognizer.setProcessor(new Detector.Processor<TextBlock>() {
                @Override
                public void release() {
                }

                /**
                 * Detect all the text from camera using TextBlock and the values into a stringBuilder
                 * which will then be set to the textView.
                 * */
                @Override
                public void receiveDetections(Detector.Detections<TextBlock> detections) {
                    final SparseArray<TextBlock> items = detections.getDetectedItems();
                    capture.setEnabled(true);
                    mCameraView.setItems(items.clone());
                }
            });
        }
    }
}
