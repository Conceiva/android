package com.handwerkcloud.client;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
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
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.os.SystemClock;
import android.print.PrintAttributes;
import android.print.pdf.PrintedPdfDocument;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.barteksc.pdfviewer.PDFView;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.owncloud.android.R;
import com.owncloud.android.ui.activity.UploadFilesActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;

public class OCRActivity extends Activity {
    final boolean DEVELOPER_MODE = false;
    static final String TAG = "OCRActivity";
    private CameraSource mCameraSource;
    private int requestPermissionID = 1;
    private CameraOverlaySurfaceView mCameraView;
    ImageButton capture = null;
    public static final int PERMISSION_CODE = 42042;
    String mFilename;
    String mPreviewFilename;
    PDFView pdfView;
    private final int STATE_CAPTURE = 0;
    private final int STATE_PREVIEW = 1;
    int mState = STATE_CAPTURE;

    ProgressBar progressBar;
    ImageButton cancelBtn;
    ImageButton acceptBtn;
    ImageButton flashCameraButton;
    private TextRecognizer textRecognizer;
    private int cameraWidth = 1600;
    int cameraHeight = 1200;
    private int rotation;
    private boolean flashmode;
    private ImageButton textHighlightButton;

    //AsyncTask<Params, Progress, Result>
    //Params: type passed in the execute() call, and received in the doInBackground method
    //Progress: type of object passed in publishProgress calls
    //Result: object type returned by the doInBackground method, and received by onPostExecute()
    private class CaptureTask extends AsyncTask<byte[], Integer, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
            mCameraSource.stop();
        }

        @Override
        protected String doInBackground(byte[]... params) {
            String filename = null;
            byte[] bytes = params[0];
            try {
                // convert byte array into bitmap
                Bitmap bitmap = null;
                bitmap = BitmapFactory.decodeByteArray(bytes, 0,
                    bytes.length);
                rotation = Exif.getOrientation(bytes);

                // rotate Image
                if (rotation != 0) {
                    Matrix rotateMatrix = new Matrix();
                    rotateMatrix.postRotate(rotation);
                    Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0,
                        bitmap.getWidth(), bitmap.getHeight(),
                        rotateMatrix, false);
                    bitmap = rotatedBitmap;
                }

                SparseArray<TextBlock> items = textRecognizer.detect(new Frame.Builder().setBitmap(bitmap).build());
                filename = savePDF(items, bitmap);
                if (filename == null) {
                    return filename;
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

                    return filename;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return filename;
        }

        @Override
        protected void onPostExecute(String filename) {
            super.onPostExecute(filename);
            mPreviewFilename = filename.replace("/scan", "/Highlightscan");
            displayPreview(mPreviewFilename);
        }
    }

    void displayPreview(String filename) {
        mCameraView.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        if (filename != null) {
            pdfView.fromFile(new File(filename)).load();
        }
        mState = STATE_PREVIEW;
        pdfView.setVisibility(View.VISIBLE);
        cancelBtn.setVisibility(View.VISIBLE);
        acceptBtn.setVisibility(View.VISIBLE);
        textHighlightButton.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mState = savedInstanceState.getInt("SCAN_STATE");
        mFilename = savedInstanceState.getString("FILENAME");
        mPreviewFilename = savedInstanceState.getString("PREVIEWFILENAME");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("SCAN_STATE", mState);
        outState.putString("FILENAME", mFilename);
        outState.putString("PREVIEWFILENAME", mPreviewFilename);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (DEVELOPER_MODE) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()   // or .detectAll() for all detectable problems
                .penaltyLog()
                .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .penaltyDeath()
                .build());
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ocr_activity);
        mCameraView = findViewById(R.id.surfaceView);
        pdfView = findViewById(R.id.pdfView);
        capture = findViewById(R.id.capture);
        capture.setEnabled(false);
        cancelBtn = findViewById(R.id.cancelBtn);
        acceptBtn = findViewById(R.id.acceptBtn);
        progressBar = findViewById(R.id.pBar);
        textHighlightButton = findViewById(R.id.textHighlightBtn);

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
        textHighlightButton.setVisibility(View.GONE);

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
                File previewFile = new File(mPreviewFilename);
                previewFile.delete();
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

        textHighlightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean visible = mPreviewFilename.contains("Highlightscan");
                if (!visible) {
                    mPreviewFilename = mFilename.replace("/scan", "/Highlightscan");
                    textHighlightButton.setColorFilter(Color.WHITE);
                }
                else {
                    mPreviewFilename = mFilename;
                    textHighlightButton.setColorFilter(Color.LTGRAY);
                }
                pdfView.fromFile(new File(mPreviewFilename)).load();
            }
        });
        if (!getBaseContext().getPackageManager().hasSystemFeature(
            PackageManager.FEATURE_CAMERA_FLASH)) {
            flashCameraButton.setVisibility(View.GONE);
        }
        startCameraSource();
        if (savedInstanceState != null) {
            mState = savedInstanceState.getInt("SCAN_STATE");
            mFilename = savedInstanceState.getString("FILENAME");
            mPreviewFilename = savedInstanceState.getString("PREVIEWFILENAME");
            if (mState == STATE_PREVIEW) {
                if (mPreviewFilename.contains("Highlightscan")) {
                    textHighlightButton.setColorFilter(Color.WHITE);
                }
                else {
                    textHighlightButton.setColorFilter(Color.LTGRAY);
                }
                displayPreview(mPreviewFilename);
            }
        }
    }

    void returnToCamera() {
        pdfView.setVisibility(View.GONE);
        startCameraSource();
        mCameraView.setVisibility(View.VISIBLE);
        cancelBtn.setVisibility(View.GONE);
        acceptBtn.setVisibility(View.GONE);
        textHighlightButton.setVisibility(View.GONE);
        textHighlightButton.setColorFilter(Color.WHITE);
        flashCameraButton.setImageResource(R.drawable.ic_flash_off);
        mState = STATE_CAPTURE;
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
                new CaptureTask().execute(bytes);
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
            if (ratioMax > ratioBitmap) {
                finalWidth = (int) ((float) maxHeight * ratioBitmap);
            } else {
                finalHeight = (int) ((float) maxWidth / ratioBitmap);
            }
            image.setHasAlpha(true);
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
                mPreviewFilename = mFilename.replace("/scan", "/Highlightscan");
                pdfView.fromFile(new File(mPreviewFilename)).load();
                pdfView.setVisibility(View.VISIBLE);
            }
        }
        else if (requestCode == requestPermissionID) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCameraSource();
            } else {
                // permission denied, boo! Disable the
                // functionality that depends on this permission.
            }
        }
    }

    public static <C> List<C> ConvertToList(SparseArray<C> sparseArray) {
        if (sparseArray == null) return null;
        List<C> arrayList = new ArrayList<C>(sparseArray.size());

        for (int i = 0; i < sparseArray.size(); i++)
            arrayList.add(sparseArray.valueAt(i));
        return arrayList;
    }

    public static Comparator<TextBlock> TextBlockComparator
        = new Comparator<TextBlock>() {
        public int compare(TextBlock textBlock1, TextBlock textBlock2) {
            return textBlock1.getBoundingBox().top - textBlock2.getBoundingBox().top;
        }
    };

    private String savePDF(SparseArray<TextBlock> items, Bitmap bitmap) {
        if (items == null) {
            return null;
        }

        List<TextBlock> itemsArray = ConvertToList(items);
        Collections.sort(itemsArray, TextBlockComparator);

        // open a new document
        PrintedPdfDocument document = null;
        PrintedPdfDocument previewDocument = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            PrintAttributes printAttributes = new PrintAttributes.Builder().
                setMediaSize(PrintAttributes.MediaSize.ISO_A4).
                setMinMargins(PrintAttributes.Margins.NO_MARGINS).
                build();
            document = new PrintedPdfDocument(this,
                printAttributes);
            previewDocument = new PrintedPdfDocument(this,
                printAttributes);

            // start a page
            PdfDocument.Page page = document.startPage(0);
            PdfDocument.Page previewPage = previewDocument.startPage(0);

            // draw something on the page
            Canvas canvas = page.getCanvas();
            Canvas highlightCanvas = previewPage.getCanvas();
            int origBitmapWidth = bitmap.getWidth();
            int origBitmapHeight = bitmap.getHeight();
            bitmap = resize(bitmap, canvas.getWidth(), canvas.getHeight());
            float ratiowidth = (float)bitmap.getWidth() / origBitmapWidth;
            float ratioheight = (float)bitmap.getHeight() / origBitmapHeight;

            int xOffset = canvas.getWidth() == bitmap.getWidth() ? 0 : (canvas.getWidth() - bitmap.getWidth()) / 2;
            int yOffset = canvas.getHeight() == bitmap.getHeight() ? 0 : (canvas.getHeight() - bitmap.getHeight()) / 2;

            Paint paint = new Paint();
            paint.setColor(Color.BLACK);

            paint.setAntiAlias(true);
            paint.setFilterBitmap(true);
            paint.setDither(true);
            paint.setColor(Color.BLACK);
            highlightCanvas.drawBitmap(bitmap, xOffset, yOffset, paint);

            for (int i = 0; i < itemsArray.size(); i++) {

                for (int j = 0; j < itemsArray.get(i).getComponents().size(); j++) {
                    String text = itemsArray.get(i).getComponents().get(j).getValue();
                    Rect rect = itemsArray.get(i).getComponents().get(j).getBoundingBox();
                    rect.left = (int) (rect.left * ratiowidth) + xOffset;
                    rect.right = (int) (rect.right * ratiowidth) + xOffset;
                    rect.top = (int) (rect.top * ratioheight) + yOffset;
                    rect.bottom = (int) (rect.bottom * ratioheight) + yOffset;

                    float textSize = 8;

                    paint.setStyle(Paint.Style.STROKE);
                    paint.setColor(Color.RED);
                    highlightCanvas.drawRect(rect, paint);
                    //paint.setStyle(Paint.Style.FILL);

                    canvas.save();
                    TextPaint textpaint = new TextPaint();
                    textSize = calculateMaxTextSize(text, textpaint, (rect.right - rect.left), (rect.bottom - rect.top));
                    textpaint.setTextSize(textSize);

                    StaticLayout mTextLayout = new StaticLayout(text, textpaint, (rect.right - rect.left) + 1, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                    canvas.translate(rect.left, rect.top/* - (int)(textSize / 8)*/); // Adjust top position of the text based upon the font size
                    mTextLayout.draw(canvas);
                    canvas.restore();

                    highlightCanvas.save();
                    textpaint.setColor(Color.YELLOW);
                    highlightCanvas.translate(rect.left, rect.top/* - (int)(textSize / 8)*/); // Adjust top position of the text based upon the font size
                    mTextLayout.draw(highlightCanvas);
                    highlightCanvas.restore();
                }
            }

            canvas.drawBitmap(bitmap, xOffset, yOffset, paint);

            // finish the page
            document.finishPage(page);
            previewDocument.finishPage(previewPage);

            String dir = Environment.getExternalStorageDirectory()+File.separator+"HandwerkCloud";
            //create folder
            File folder = new File(dir); //folder name
            folder.mkdirs();

            //create file
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String FileName = "scan" + timeStamp + ".pdf";
            String PreviewFileName = "Highlightscan" + timeStamp + ".pdf";
            File file = new File(dir, FileName);
            File previewFile = new File(dir, PreviewFileName);

            try {
                FileOutputStream oFile = new FileOutputStream(file);
                // write the document content
                document.writeTo(oFile);
                oFile.close();

                FileOutputStream oPreviewFile = new FileOutputStream(previewFile);
                // write the document content
                previewDocument.writeTo(oPreviewFile);
                oPreviewFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            //close the document
            document.close();
            previewDocument.close();

            return file.getAbsolutePath();
        }
        return null;
    }

    public static float calculateMaxTextSize(String text, TextPaint paint, int maxWidth, int maxHeight) {
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

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static float calculateLetterSpacing(String text, TextPaint paint, int maxWidth) {
        if (text == null || paint == null) return 0;
        Rect bound = new Rect();
        float size = 0.01f;
        float step= 0.01f;
        paint.setLetterSpacing(size);
        while (true) {
            paint.getTextBounds(text, 0, text.length(), bound);
            if (bound.width() < maxWidth && bound.width() != 0) {
                size += step;
                if (size == 0.06f) {
                    return size;
                }
                paint.setLetterSpacing(size);
            } else {
                return size - step;
            }
        }
    }

    private void startCameraSource() {
        if (ActivityCompat.checkSelfPermission(getApplicationContext(),
            Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(OCRActivity.this,
                new String[]{Manifest.permission.CAMERA},
                requestPermissionID);
            return;
        }

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

        try {
            if (mCameraView.getHolder().getSurface().isValid()) {
                mCameraSource.start(mCameraView.getHolder());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
