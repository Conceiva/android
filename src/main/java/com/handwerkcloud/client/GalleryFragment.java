package com.handwerkcloud.client;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.owncloud.android.R;

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

    //Create the TextRecognizer
    WeakReference<TextRecognizer> textRecognizer;
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
        String sortOrder = null;                                 //The sort order for the returned rows

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
        new ImageTask().execute(path);
    }

    //AsyncTask<Params, Progress, Result>
    //Params: type passed in the execute() call, and received in the doInBackground method
    //Progress: type of object passed in publishProgress calls
    //Result: object type returned by the doInBackground method, and received by onPostExecute()
    private class ImageTask extends AsyncTask<String, Integer, String> {

        private int rotation;

        @Override
        protected void onPreExecute() {
            scanInProgress = true;
            super.onPreExecute();
            Intent i = new Intent(getActivity(), OCRActivity.class);
            i.setAction(OCRActivity.ACTION_CAPTURE_STARTED);
            startActivity(i);
        }

        @Override
        protected String doInBackground(String... params) {
            String filename = null;
            String path = params[0];
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                Bitmap bitmap = BitmapFactory.decodeFile(path, options);
                if (bitmap == null) {
                    return null;
                }

                rotation = 0;
                ExifInterface exif = new ExifInterface(path);
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

                SparseArray<TextBlock> items = textRecognizer.get().detect(new Frame.Builder().setBitmap(bitmap).build());
                filename = OCRActivity.savePDF(items, bitmap, getActivity().getApplicationContext());
                if (filename == null) {
                    return filename;
                }

                int permissionCheck = ContextCompat.checkSelfPermission(getActivity(),
                    READ_EXTERNAL_STORAGE);

                if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                        getActivity(),
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
            scanInProgress = false;
            if (getActivity() == null) {
                return;
            }

            if (filename != null) {
                String previewFilename = filename.substring(0, filename.lastIndexOf('/') + 1) + "/Highlightscan.pdf";
                Intent i = new Intent(getActivity(), OCRActivity.class);
                i.setAction(OCRActivity.ACTION_DISPLAY_PREVIEW);
                i.putExtra(OCRActivity.EXTRA_FILENAME, filename);
                i.putExtra(OCRActivity.EXTRA_PREVIEW_FILENAME, previewFilename);
                startActivity(i);
            }
            else {
                Intent i = new Intent(getActivity(), OCRActivity.class);
                i.setAction(OCRActivity.ACTION_SCAN_FAILED);
                startActivity(i);
            }
        }
    }

    public GalleryFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        ((OCRActivity)getActivity()).setCaptureEventListener(this);
        textRecognizer = new WeakReference<TextRecognizer>(((OCRActivity)getActivity()).getTextRecognizer());
        View view = inflater.inflate(R.layout.fragment_gallery, container, false);

        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        gridLayoutManager = new GridLayoutManager(getActivity().getApplicationContext(), 3);
        recyclerView.setLayoutManager(gridLayoutManager);

        galleryAdapter = new GalleryAdapter(getActivity().getApplicationContext(), null);
        galleryAdapter.setImageSelectedListener(this);
        recyclerView.setAdapter(galleryAdapter);
        LoaderManager.getInstance(this).initLoader(GALLERY_LOADER_ID, null, this);
        return view;
    }

}
