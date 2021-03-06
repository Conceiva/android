package com.handwerkcloud.client;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.android.material.snackbar.Snackbar;
import com.owncloud.android.R;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;

/**
 * A placeholder fragment containing a simple view.
 */
public class CaptureFragment extends Fragment implements OCRActivity.OnCaptureEventListener {

    private static final String TAG = CaptureFragment.class.getSimpleName();
    ImageButton capture = null;
    private CameraSource mCameraSource;
    private CameraOverlaySurfaceView mCameraView;
    ImageButton flashCameraButton;
    private boolean flashmode;
    private int cameraWidth = 1600;
    int cameraHeight = 1200;

    //Create the TextRecognizer
    WeakReference<TextRecognizer> textRecognizer;

    //AsyncTask<Params, Progress, Result>
    //Params: type passed in the execute() call, and received in the doInBackground method
    //Progress: type of object passed in publishProgress calls
    //Result: object type returned by the doInBackground method, and received by onPostExecute()
    public static class CaptureTask extends AsyncTask<byte[], Integer, String> {

        private int rotation;
        static private WeakReference<Activity> mActivity = null;
        WeakReference<TextRecognizer> mTextRecognizer;

        CaptureTask(Activity activity, TextRecognizer textRecognizer) {
            mActivity = new WeakReference<>(activity);
            mTextRecognizer = new WeakReference<>(textRecognizer);
        }

        public static void setActivity(Activity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Intent i = new Intent(mActivity.get(), OCRActivity.class);
            i.setAction(OCRActivity.ACTION_CAPTURE_STARTED);
            mActivity.get().startActivity(i);
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

                SparseArray<TextBlock> items = mTextRecognizer.get().detect(new Frame.Builder().setBitmap(bitmap).build());
                filename = OCRActivity.savePDF(items, bitmap, mActivity.get());
                if (filename == null) {
                    return filename;
                }

                int permissionCheck = ContextCompat.checkSelfPermission(mActivity.get(),
                    READ_EXTERNAL_STORAGE);

                if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                        mActivity.get(),
                        new String[]{READ_EXTERNAL_STORAGE},
                        OCRActivity.PERMISSION_CODE
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
            if (filename != null && mActivity.get() != null) {
                String previewFilename = filename.substring(0, filename.lastIndexOf('/') + 1) + "/Highlightscan.pdf";
                Intent i = new Intent(mActivity.get(), OCRActivity.class);
                i.setAction(OCRActivity.ACTION_DISPLAY_PREVIEW);
                i.putExtra(OCRActivity.EXTRA_FILENAME, filename);
                i.putExtra(OCRActivity.EXTRA_PREVIEW_FILENAME, previewFilename);
                mActivity.get().startActivity(i);
            }
            else {
                if (mActivity.get() != null) {
                    Intent i = new Intent(mActivity.get(), OCRActivity.class);
                    i.setAction(OCRActivity.ACTION_SCAN_FAILED);
                    mActivity.get().startActivity(i);
                }
            }
        }
    }

    public CaptureFragment() {

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Activity a;

        if (context instanceof Activity){
            CaptureTask.setActivity((Activity) context);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_capture, container, false);

        ((OCRActivity)getActivity()).setCaptureEventListener(this);
        textRecognizer = new WeakReference<TextRecognizer>(((OCRActivity)getActivity()).getTextRecognizer());
        mCameraView = view.findViewById(R.id.surfaceView);
        capture = view.findViewById(R.id.capture);

        flashCameraButton = (ImageButton) view.findViewById(R.id.flash);
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

        if (!getActivity().getBaseContext().getPackageManager().hasSystemFeature(
            PackageManager.FEATURE_CAMERA_FLASH)) {
            flashCameraButton.setVisibility(View.GONE);
        }

        startCameraSource();
        return view;
    }

    @Override
    public void onInitialize() {
        startCameraSource();
    }

    @Override
    public void onCaptureStarted() {
        mCameraSource.stop();
    }

    @Override
    public void onDisplayPreview() {
        mCameraView.setVisibility(View.GONE);
    }

    @Override
    public void onPreviewCancel() {

        startCameraSource();
        mCameraView.setVisibility(View.VISIBLE);
        flashCameraButton.setImageResource(R.drawable.ic_flash_off);
    }

    private void showFlashButton(Camera.Parameters params) {
        boolean showFlash = (getActivity().getPackageManager().hasSystemFeature(
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
                new CaptureTask(getActivity(), textRecognizer.get()).execute(bytes);
            }
        });

    }

    private void startCameraSource() {
        if (ActivityCompat.checkSelfPermission(getActivity().getApplicationContext(),
            Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(getActivity(),
                new String[]{Manifest.permission.CAMERA},
                OCRActivity.requestPermissionID);
            return;
        }

        if (!textRecognizer.get().isOperational()) {
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
            mCameraSource = new CameraSource.Builder(getActivity().getApplicationContext(), textRecognizer.get())
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

                        if (ActivityCompat.checkSelfPermission(getActivity().getApplicationContext(),
                            Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

                            ActivityCompat.requestPermissions(getActivity(),
                                new String[]{Manifest.permission.CAMERA},
                                OCRActivity.requestPermissionID);
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
