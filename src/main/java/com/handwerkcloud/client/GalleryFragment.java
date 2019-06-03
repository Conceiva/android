package com.handwerkcloud.client;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.CircularProgressDrawable;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableResource;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.esafirm.imagepicker.features.ImagePicker;
import com.esafirm.imagepicker.model.Image;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.android.material.button.MaterialButton;
import com.owncloud.android.R;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.media.ExifInterface.ORIENTATION_ROTATE_180;
import static android.media.ExifInterface.ORIENTATION_ROTATE_270;
import static android.media.ExifInterface.ORIENTATION_ROTATE_90;

/**
 * A placeholder fragment containing a simple view.
 */
public class GalleryFragment extends Fragment implements OCRActivity.OnCaptureEventListener, LoaderManager.LoaderCallbacks<Cursor>, GalleryAdapter.ImageSelectedListener {

    private static final int GALLERY_LOADER_ID = 100;
    private static final int ACTIVITY_REQUEST_CHOOSE_FILE = 1;
    String sortOrder = MediaStore.Images.ImageColumns.DATE_ADDED + " DESC";

    //Create the TextRecognizer
    WeakReference<TextRecognizer> textRecognizer;
    AppCompatSpinner sortOrderSpinner;
    MaterialButton openFileButton;
    private RecyclerView recyclerView;
    private GridLayoutManager gridLayoutManager;
    private GalleryAdapter galleryAdapter;
    private boolean scanInProgress = false;
    private Loader<Cursor> galleryLoader() {
        Uri contactsUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI; // The content URI of the phone contacts

        String[] projection = {                                  // The columns to return for each row
            MediaStore.Images.Media.DATA
        } ;

        String selection = null;                                 //Selection criteria
        String[] selectionArgs = {};                             //Selection criteria
                                         //The sort order for the returned rows

        return new CursorLoader(
            getActivity().getApplicationContext(),
            contactsUri,
            projection,
            selection,
            selectionArgs,
            sortOrder);
    }

    @Override
    public void onInitialize() {

    }

    @Override
    public void onCaptureStarted() {

    }

    @Override
    public void onDisplayPreview() {

    }

    @Override
    public void onPreviewCancel() {

    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        return galleryLoader();
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        galleryAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        galleryAdapter.swapCursor(null);
    }

    @Override
    public void imageSelected(String path) {
        if (scanInProgress) {
            return;
        }
        new ImageTask(getActivity(), this, textRecognizer.get()).execute(path);
    }

    //AsyncTask<Params, Progress, Result>
    //Params: type passed in the execute() call, and received in the doInBackground method
    //Progress: type of object passed in publishProgress calls
    //Result: object type returned by the doInBackground method, and received by onPostExecute()
    private static class ImageTask extends AsyncTask<String, Integer, String> {

        private int rotation;
        static private WeakReference<Activity> mActivity = null;
        WeakReference<TextRecognizer> mTextRecognizer;
        WeakReference<GalleryFragment> mFragment = null;

        ImageTask(Activity activity, GalleryFragment fragment, TextRecognizer textRecognizer) {
            mActivity = new WeakReference<>(activity);
            mFragment = new WeakReference<>(fragment);
            mTextRecognizer = new WeakReference<>(textRecognizer);
        }

        public static void setActivity(Activity activity) {
            mActivity = new WeakReference<>(activity);
        }

        private Bitmap openContentStream(String path) {
            Uri photoUri = Uri.parse(path);
            Bitmap bitmap = null;
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            ContentResolver cr = mActivity.get().getContentResolver();
            InputStream input = null;
            InputStream input1 = null;
            try {
                input = cr.openInputStream(photoUri);
                BitmapFactory.decodeStream(input, null, bmOptions);
                if (input != null) {
                    input.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            int photoW = bmOptions.outWidth;
            int photoH = bmOptions.outHeight;
            try {
                input1 = cr.openInputStream(photoUri);
                bitmap = BitmapFactory.decodeStream(input1);
                if (input1 != null) {
                    input1.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return bitmap;
        }

        @Override
        protected void onPreExecute() {
            mFragment.get().scanInProgress = true;
            super.onPreExecute();
            Intent i = new Intent(mActivity.get(), OCRActivity.class);
            i.setAction(OCRActivity.ACTION_CAPTURE_STARTED);
            mActivity.get().startActivity(i);
        }

        @Override
        protected String doInBackground(String... params) {
            String filename = null;
            String path = params[0];
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                Bitmap bitmap = null;
                if (path.startsWith("content://")) {
                    bitmap = openContentStream(path);
                }
                else {
                    bitmap = BitmapFactory.decodeFile(path, options);
                }

                if (bitmap == null) {
                    return null;
                }

                rotation = 0;
                ExifInterface exif = null;
                if (path.startsWith("content://")) {
                    ContentResolver cr = mActivity.get().getContentResolver();
                    InputStream input = null;
                    Uri pathUri = Uri.parse(path);

                    input = cr.openInputStream(pathUri);
                    exif = new ExifInterface(input);
                    if (input != null) {
                        input.close();
                    }
                }
                else {
                    exif = new ExifInterface(path);
                }

                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                if (orientation == ORIENTATION_ROTATE_90) {
                    rotation =90;
                }
                else if (orientation == ORIENTATION_ROTATE_180) {
                    rotation = 180;
                }
                else if (orientation == ORIENTATION_ROTATE_270) {
                    rotation =270;
                }
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
                filename = OCRActivity.savePDF(items, bitmap, mActivity.get().getApplicationContext());
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
            if (mFragment.get() == null) {
                return;
            }
            mFragment.get().scanInProgress = false;
            if (mActivity.get() == null) {
                return;
            }

            if (filename != null) {
                String previewFilename = filename.substring(0, filename.lastIndexOf('/') + 1) + "/Highlightscan.pdf";
                Intent i = new Intent(mActivity.get(), OCRActivity.class);
                i.setAction(OCRActivity.ACTION_DISPLAY_PREVIEW);
                i.putExtra(OCRActivity.EXTRA_FILENAME, filename);
                i.putExtra(OCRActivity.EXTRA_PREVIEW_FILENAME, previewFilename);
                mActivity.get().startActivity(i);
            }
            else {
                Intent i = new Intent(mActivity.get(), OCRActivity.class);
                i.setAction(OCRActivity.ACTION_SCAN_FAILED);
                mActivity.get().startActivity(i);
            }
        }
    }

    public GalleryFragment() {

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Activity a;

        if (context instanceof Activity){
            ImageTask.setActivity((Activity) context);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        ((OCRActivity)getActivity()).setCaptureEventListener(this);
        textRecognizer = new WeakReference<TextRecognizer>(((OCRActivity)getActivity()).getTextRecognizer());
        View view = inflater.inflate(R.layout.fragment_gallery, container, false);

        sortOrderSpinner = (AppCompatSpinner) view.findViewById(R.id.sortOrder);
        initSortOrderSpinner(sortOrderSpinner);
        openFileButton = (MaterialButton) view.findViewById(R.id.openFile);
        openFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");

                startActivityForResult(Intent.createChooser(i, null), ACTIVITY_REQUEST_CHOOSE_FILE);
            }
        });

        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        gridLayoutManager = new GridLayoutManager(getActivity().getApplicationContext(), 3);
        recyclerView.setLayoutManager(gridLayoutManager);

        galleryAdapter = new GalleryAdapter(getActivity().getApplicationContext(), null);
        galleryAdapter.setImageSelectedListener(this);
        recyclerView.setAdapter(galleryAdapter);
        LoaderManager.getInstance(this).initLoader(GALLERY_LOADER_ID, null, this);
        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if( requestCode == ACTIVITY_REQUEST_CHOOSE_FILE && data != null) {
            imageSelected(data.getData().toString());
        }
    }

    void initSortOrderSpinner(AppCompatSpinner spinner) {
        List<String> values = new ArrayList<>();
        values.add(getString(R.string.sort_by_modification_date_ascending));
        values.add(getString(R.string.sort_by_modification_date_descending));
        ArrayAdapter adapter = new ArrayAdapter<>(getActivity(), R.layout.sort_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (i == 0) {
                    sortOrder = MediaStore.Images.ImageColumns.DATE_ADDED + " DESC";
                }
                else if (i == 1) {
                    sortOrder = MediaStore.Images.ImageColumns.DATE_ADDED + " ASC";
                }
                LoaderManager.getInstance(GalleryFragment.this).restartLoader(GALLERY_LOADER_ID, null, GalleryFragment.this);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }
}
