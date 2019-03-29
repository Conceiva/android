package com.handwerkcloud.client;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
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
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.os.SystemClock;
import android.print.PrintAttributes;
import android.print.pdf.PrintedPdfDocument;
import android.text.Editable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.esafirm.imagepicker.features.ImagePicker;
import com.esafirm.imagepicker.model.Image;
import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
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
import java.util.Iterator;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.PagerTabStrip;
import androidx.viewpager.widget.ViewPager;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;

public class OCRActivity extends FragmentActivity {
    public static final String ACTION_CAPTURE_STARTED = "ACTION_CAPTURE_STARTED";
    public static final String ACTION_DISPLAY_PREVIEW = "DISPLAY_PREVIEW";
    public static final String EXTRA_FILENAME = "FILENAME";
    public static final String EXTRA_PREVIEW_FILENAME = "PREVIEW_FILENAME";
    public static final String ACTION_SCAN_FAILED = "ACTION_SCAN_FAILED";
    final boolean DEVELOPER_MODE = false;
    static final String TAG = "OCRActivity";
    public static int requestPermissionID = 1;
    public static final int PERMISSION_CODE = 42042;
    String mFilename;
    String mPreviewFilename;
    PDFView pdfView;
    private final int STATE_CAPTURE = 0;
    private final int STATE_CAPTURING = 1;
    private final int STATE_PREVIEW = 2;
    int mState = STATE_CAPTURE;
    private TextRecognizer textRecognizer;

    ProgressBar progressBar;
    ImageButton cancelBtn;
    ImageButton acceptBtn;
    private ImageButton textHighlightButton;
    private ImageButton galleryButton;
    private EditText filenameEdit;
    private ArrayList<OnCaptureEventListener> mCaptureEventListener = new ArrayList<>();

    /**
     * The number of pages (wizard steps) to show
     */
    private static final int NUM_PAGES = 2;

    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    private ViewPager mPager;

    /**
     * The pager adapter, which provides the pages to the view pager widget.
     */
    private PagerAdapter pagerAdapter;

    public TextRecognizer getTextRecognizer() {
        return textRecognizer;
    }

    /**
     * A simple pager adapter that represents 5 ScreenSlidePageFragment objects, in
     * sequence.
     */
    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public CharSequence getPageTitle (int position) {
            if (position == 0) {
                return getString(R.string.camera);
            }
            else {
                return getString(R.string.gallery);
            }
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                CaptureFragment fragment = new CaptureFragment();
                return fragment;
            }
            else if (position == 1) {
                GalleryFragment fragment = new GalleryFragment();
                return fragment;
            }
            return null;
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }
    }
    ViewPager.OnPageChangeListener viewPagerPageChangeListener = new ViewPager.OnPageChangeListener() {

        @Override
        public void onPageSelected(int position) {
            //addBottomDots(position);
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {

        }

        @Override
        public void onPageScrollStateChanged(int arg0) {

        }
    };

    public interface OnCaptureEventListener {
        void onInitialize();
        void onCaptureStarted();
        void onDisplayPreview();
        void onPreviewCancel();
    }

    public void setCaptureEventListener(OnCaptureEventListener listener) {
        this.mCaptureEventListener.add(listener);
    }

    void displayPreview(String filename) {
        Iterator iter = mCaptureEventListener.iterator();
        while (iter.hasNext()) {
            OnCaptureEventListener listener = (OnCaptureEventListener) iter.next();
            listener.onDisplayPreview();
        }
        progressBar.setVisibility(View.GONE);
        if (filename != null) {
            pdfView.fromFile(new File(filename)).load();
        }
        mState = STATE_PREVIEW;
        pdfView.setVisibility(View.VISIBLE);
        filenameEdit.setVisibility(View.VISIBLE);
        cancelBtn.setVisibility(View.VISIBLE);
        acceptBtn.setVisibility(View.VISIBLE);
        textHighlightButton.setVisibility(View.VISIBLE);
    }


    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mState = savedInstanceState.getInt("SCAN_STATE");
        mFilename = savedInstanceState.getString("FILENAME");
        if (mFilename != null) {
            filenameEdit.setText(mFilename.substring(mFilename.lastIndexOf('/')));
        }
        mPreviewFilename = savedInstanceState.getString("PREVIEWFILENAME");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("SCAN_STATE", mState);
        outState.putString("FILENAME", mFilename);
        outState.putString("PREVIEWFILENAME", mPreviewFilename);
        super.onSaveInstanceState(outState);
    }

    boolean renamePdf() {
        File current = new File(mFilename);
        String originalName = current.getName();
        String editName = filenameEdit.getText().toString();
        if (editName.compareTo(originalName) != 0) {
            if (!isFilenameValid(editName)) {
                return false;
            }

            String newFilename = current.getParent();
            newFilename += "/" + filenameEdit.getText();
            if (!newFilename.endsWith(".pdf")) {
                newFilename += ".pdf";
            }
            File dest = new File(newFilename);
            current.renameTo(dest);
            mFilename = newFilename;
        }
        return true;
    }

    private String getFragmentTag(int viewPagerId, int fragmentPosition)
    {
        return "android:switcher:" + viewPagerId + ":" + fragmentPosition;
    }

    public static boolean isFilenameValid(String file) {
        File f = new File(file);
        try {
            f.getCanonicalPath();
            return true;
        } catch (IOException e) {
            return false;
        }
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
        pdfView = findViewById(R.id.pdfView);
        cancelBtn = findViewById(R.id.cancelBtn);
        acceptBtn = findViewById(R.id.acceptBtn);
        progressBar = findViewById(R.id.pBar);
        textHighlightButton = findViewById(R.id.textHighlightBtn);
        filenameEdit = findViewById(R.id.filenameEdit);

        // Instantiate a ViewPager and a PagerAdapter.
        mPager = (ViewPager) findViewById(R.id.pager);
        pagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(pagerAdapter);
        mPager.addOnPageChangeListener(viewPagerPageChangeListener);

        //Create the TextRecognizer
        textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();

        filenameEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_DONE) {

                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(textView.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });
        filenameEdit.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                if(start < s.length() && s.charAt(start) == '/') {
                    String edit = s.toString();
                    edit = edit.replace("/", "");
                    int pos = filenameEdit.getSelectionStart();
                    filenameEdit.setText(edit);
                    filenameEdit.setSelection(pos);
                }

                if (s.length() == 0) {
                    acceptBtn.setEnabled(false);
                }
                else {
                    acceptBtn.setEnabled(true);
                }
            }
        });
        filenameEdit.setVisibility(View.GONE);
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

                if (!renamePdf()) {
                    Toast.makeText(OCRActivity.this, R.string.invalid_filename, Toast.LENGTH_LONG).show();
                    return;
                }
                Intent resultIntent = new Intent();
                File previewFile = new File(mPreviewFilename);
                previewFile.delete();
                resultIntent.putExtra(UploadFilesActivity.EXTRA_CHOSEN_FILES, new String[]{mFilename});
                setResult(UploadFilesActivity.RESULT_OK_AND_MOVE, resultIntent);
                finish();
            }
        });

        textHighlightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean visible = mPreviewFilename.contains("Highlight");
                if (!visible) {
                    mPreviewFilename = mFilename.substring(0, mFilename.lastIndexOf('/') + 1) + "/Highlightscan.pdf";
                    textHighlightButton.setColorFilter(Color.WHITE);
                }
                else {
                    mPreviewFilename = mFilename;
                    textHighlightButton.setColorFilter(Color.LTGRAY);
                }
                float zoom = pdfView.getZoom();
                float x = pdfView.getCurrentXOffset();
                float y = pdfView.getCurrentYOffset();
                float positionOffset = pdfView.getPositionOffset();
                pdfView.fromFile(new File(mPreviewFilename)).onLoad(new OnLoadCompleteListener() {
                    @Override
                    public void loadComplete(int nbPages) {
                        pdfView.post(new Runnable() {
                            @Override
                            public void run() {
                                if (zoom != 1.0f) {
                                    pdfView.zoomTo(zoom);
                                    pdfView.moveTo(x, y);
                                    pdfView.setPositionOffset(positionOffset);
                                }
                            }
                        });
                    }
                }).load();
            }
        });

        if (savedInstanceState != null) {
            mState = savedInstanceState.getInt("SCAN_STATE");
            mFilename = savedInstanceState.getString("FILENAME");
            mPreviewFilename = savedInstanceState.getString("PREVIEWFILENAME");
            if (mState == STATE_CAPTURING) {
                progressBar.setVisibility(View.VISIBLE);
            } else if (mState == STATE_PREVIEW) {
                if (mPreviewFilename.contains("/Highlightscan.pdf")) {
                    textHighlightButton.setColorFilter(Color.WHITE);
                }
                else {
                    textHighlightButton.setColorFilter(Color.LTGRAY);
                }
                displayPreview(mPreviewFilename);
            }
        }

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent i) {
        handleIntent(i);
    }

    void handleIntent(Intent i) {
        if (i == null) {
            return;
        }

        if (i.getAction() == ACTION_DISPLAY_PREVIEW) {
            String filename = i.getStringExtra(EXTRA_FILENAME);
            mFilename = filename;
            String previewFilename = i.getStringExtra(EXTRA_PREVIEW_FILENAME);
            mPreviewFilename = previewFilename;
            filenameEdit.setText(filename.substring(filename.lastIndexOf('/') + 1));
            acceptBtn.setEnabled(true);
            displayPreview(previewFilename);
        }
        else if (i.getAction() == ACTION_SCAN_FAILED) {
            mState = STATE_CAPTURE;
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, R.string.scan_failed, Toast.LENGTH_LONG).show();
        }
        else if (i.getAction() == ACTION_CAPTURE_STARTED) {

            progressBar.setVisibility(View.VISIBLE);
            mState = STATE_CAPTURING;
            Iterator iter = mCaptureEventListener.iterator();
            while (iter.hasNext()) {
                OnCaptureEventListener listener = (OnCaptureEventListener) iter.next();
                listener.onCaptureStarted();
            }
        }
    }

    void returnToCamera() {
        File previewFile = new File(mPreviewFilename);
        previewFile.delete();
        File file = new File(mFilename);
        file.delete();
        pdfView.setVisibility(View.GONE);
        filenameEdit.setVisibility(View.GONE);
        cancelBtn.setVisibility(View.GONE);
        acceptBtn.setVisibility(View.GONE);
        textHighlightButton.setVisibility(View.GONE);
        textHighlightButton.setColorFilter(Color.WHITE);
        Iterator iter = mCaptureEventListener.iterator();
        while (iter.hasNext()) {
            OnCaptureEventListener listener = (OnCaptureEventListener) iter.next();
            listener.onPreviewCancel();
        }
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

    public static Bitmap resize(Bitmap image, int maxWidth, int maxHeight) {
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
                mPreviewFilename = mFilename.substring(0, mFilename.lastIndexOf('/') + 1) + "/Highlightscan.pdf";
                pdfView.fromFile(new File(mPreviewFilename)).load();
                pdfView.setVisibility(View.VISIBLE);
            }
        }
        else if (requestCode == requestPermissionID) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Iterator iter = mCaptureEventListener.iterator();
                while (iter.hasNext()) {
                    OnCaptureEventListener listener = (OnCaptureEventListener) iter.next();
                    listener.onInitialize();
                }
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

    public static String savePDF(SparseArray<TextBlock> items, Bitmap bitmap, Context context) {
        if (items == null) {
            return null;
        }

        List<TextBlock> itemsArray = ConvertToList(items);
        Collections.sort(itemsArray, TextBlockComparator);

        // open a new document
        PrintedPdfDocument document = null;
        PrintedPdfDocument previewDocument = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            PrintAttributes.MediaSize customSize = new
                PrintAttributes.MediaSize("HANDWERKCLOUD", "HANDWERKCLOUD", ((bitmap.getWidth() / 72) * 1000), ((bitmap.getHeight() / 72) * 1000));
            customSize.asPortrait();
            PrintAttributes printAttributes = new PrintAttributes.Builder().
                setMediaSize(customSize).
                setResolution(new PrintAttributes.Resolution("RESOLUTION_ID", "RESOLUTION_ID", 72, 72)).
                setMinMargins(PrintAttributes.Margins.NO_MARGINS).
                build();
            document = new PrintedPdfDocument(context,
                printAttributes);
            previewDocument = new PrintedPdfDocument(context,
                printAttributes);

            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(bitmap.getWidth(), bitmap.getHeight(), 0).create();
            // start a page
            PdfDocument.Page page = document.startPage(pageInfo);
            PdfDocument.Page previewPage = previewDocument.startPage(pageInfo);

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

                    canvas.save();
                    TextPaint textpaint = new TextPaint();
                    textSize = calculateMaxTextSize(text, textpaint, (rect.right - rect.left), (rect.bottom - rect.top));
                    textpaint.setTextSize(textSize);

                    StaticLayout mTextLayout = new StaticLayout(text, textpaint, (canvas.getWidth() - rect.left) + 1, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                    canvas.translate(rect.left, rect.top);
                    mTextLayout.draw(canvas);
                    canvas.restore();

                    highlightCanvas.save();
                    textpaint.setColor(Color.YELLOW);
                    highlightCanvas.translate(rect.left, rect.top);
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
            String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HHmmss").format(new Date());
            String FileName = "scan" + timeStamp + ".pdf";
            String PreviewFileName = "Highlightscan.pdf";
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

}
