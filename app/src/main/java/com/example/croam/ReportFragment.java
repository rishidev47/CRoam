package com.example.croam;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.icu.text.SimpleDateFormat;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReportFragment extends Fragment {

    static final int REQUEST_TAKE_PHOTO = 1;
    static final int REQUEST_VIDEO_CAPTURE = 2;
    EditText description;
    Button video;

    //    VideoView videoView;
    Uri videoUri;
    ImageView reportImage;
    LinearLayout bg;
    ProgressBar mProgressBar;
    CoordinatorLayout mCoordinatorLayout;
    private String currentPhotoPath = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        final View view = inflater.inflate(R.layout.fragment_report, container, false);

        video = view.findViewById(R.id.video);
        reportImage = view.findViewById(R.id.reportImage);
        bg = view.findViewById(R.id.bg);
        mProgressBar = view.findViewById(R.id.progress_circular);
        mCoordinatorLayout = getActivity().findViewById(R.id.mainroot);

        final Button captureButton = view.findViewById(R.id.capture);
        description = view.findViewById(R.id.description);

        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currentPhotoPath = null;
                videoUri = null;
                dispatchTakePictureIntent();
            }
        });

        video.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currentPhotoPath = null;
                videoUri = null;
                dispatchTakeVideoIntent();
            }
        });

        Button report = view.findViewById(R.id.report);

        report.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (description.getText().toString().equals("")) {
                    description.setError("Please enter description");
                }
                if (currentPhotoPath != null) {
                    if (!description.getText().toString().equals("")) {
                        uploadFile(Uri.parse(currentPhotoPath), REQUEST_TAKE_PHOTO);
                    }

                } else if (videoUri != null) {
                    if (!description.getText().toString().equals("")) {
                        uploadFile(videoUri, REQUEST_VIDEO_CAPTURE);
                    }

                } else {

                    captureButton.setError("Please upload an image or video!");
                    video.setError("Please upload an image or video!");
                }
            }
        });


        return view;
    }

    private void dispatchTakeVideoIntent() {
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        takeVideoIntent.putExtra(android.provider.MediaStore.EXTRA_VIDEO_QUALITY, 0);
        takeVideoIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 20);
//        takeVideoIntent.putExtra(MediaStore.EXTRA_SIZE_LIMIT,20);
        if (takeVideoIntent.resolveActivity(
                Objects.requireNonNull(getActivity()).getPackageManager()) != null) {
            startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
        }
    }


    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Objects.requireNonNull(getActivity()).getExternalFilesDir(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(
                Objects.requireNonNull(getActivity()).getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File

            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(Objects.requireNonNull(getContext()),
                        "com.example.croam.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_VIDEO_CAPTURE && resultCode == RESULT_OK) {
            videoUri = intent.getData();
            Log.e("viduri", videoUri.toString());
            String[] filePathColumn = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContext().getContentResolver().query(videoUri, filePathColumn, null,
                    null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(picturePath,
                    MediaStore.Video.Thumbnails.FULL_SCREEN_KIND);
            File file = persistImage(bitmap, "a");
            Picasso.get().load(file).resize(0, 1024).centerCrop().placeholder(
                    R.drawable.report).into(reportImage);
//            reportImage.setImageBitmap(bitmap);

        } else if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            if (currentPhotoPath != null) {
                File file = new File(Uri.parse(currentPhotoPath).getPath());
//                uploadFile(Uri.parse(currentPhotoPath), REQUEST_TAKE_PHOTO);
                Picasso.get().load(file).resize(0, 1024).centerCrop().placeholder(
                        R.drawable.report).into(reportImage);
//                reportImage.setImageURI(Uri.parse(currentPhotoPath));

            }

        }
    }

    private void uploadFile(final Uri fileUri, final int code) {
        // https://github.com/iPaulPro/aFileChooser/blob/master/aFileChooser/src/com/ipaulpro
        // /afilechooser/utils/FileUtils.java
        // use the FileUtils to get the actual file by uri
        mProgressBar.setVisibility(View.VISIBLE);
        Objects.requireNonNull(getActivity()).getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        bg.setVisibility(View.VISIBLE);
        File file= null;



        RequestBody requestFile = null;

        // create RequestBody instance from file
        if (code == REQUEST_VIDEO_CAPTURE) {
            file= new File(FileUtil.getPath(fileUri, getContext()));
            Log.e("File", file.toString());
            requestFile =
                    RequestBody.create(
                            MediaType.parse(Objects.requireNonNull(
                                    getActivity().getContentResolver().getType(fileUri))),
                            file
                    );
            Log.e("vieo", "vie");
        }
        if (code == REQUEST_TAKE_PHOTO) {
            file = new File(fileUri.getPath());
            Log.e("File", file.toString());
            requestFile = RequestBody.create(MediaType.parse("image/*"), file);
        }

        // MultipartBody.Part is used to send also the actual file name
        final MultipartBody.Part body = MultipartBody.Part.createFormData("file", Objects.requireNonNull(
                file).getName(),
                Objects.requireNonNull(requestFile));


        // add another part within the multipart request
        String descriptionString = description.getText().toString();
        final RequestBody descriptionBody =
                RequestBody.create(
                        okhttp3.MultipartBody.FORM, descriptionString);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                Objects.requireNonNull(getContext()).getApplicationContext());
        String token = prefs.getString("access_token", null);
        final Map<String, String> auth = new HashMap<>();
        auth.put("Authorization",
                "Token " + token);
        final ArrayList<Boolean> flag = new ArrayList<>();
        SingleShotLocationProvider.requestSingleUpdate(Objects.requireNonNull(getContext()),
                new SingleShotLocationProvider.LocationCallback() {
                    @Override
                    public void onNewLocationAvailable(
                            SingleShotLocationProvider.GPSCoordinates location) {
                        flag.add(true);
                        Log.e("lat", String.valueOf(location.latitude));
                        RequestBody latitude =
                                RequestBody.create(
                                        okhttp3.MultipartBody.FORM,
                                        String.valueOf(location.latitude));
                        RequestBody longitude =
                                RequestBody.create(
                                        okhttp3.MultipartBody.FORM,
                                        String.valueOf(location.longitude));
                        Log.e("long", longitude.toString());

                        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
                        List<Address> addresses = null;
                         RequestBody country = null;
                         RequestBody state = null;
                         RequestBody city = null;
                        try {
                            addresses = geocoder.getFromLocation(location.latitude,
                                        location.longitude, 1);
                            String cityName = addresses.get(0).getLocality();
                            String stateName = addresses.get(0).getAdminArea();
                            String countryName = addresses.get(0).getCountryName();

                           country =
                                    RequestBody.create(
                                            okhttp3.MultipartBody.FORM, countryName);
                            state =
                                    RequestBody.create(
                                            okhttp3.MultipartBody.FORM, stateName);
                            city =
                                    RequestBody.create(
                                            okhttp3.MultipartBody.FORM, cityName);


                        } catch (IOException e) {
                            e.printStackTrace();
                        }


                        // finally, execute the request
                        Call<ResponseBody> call = MyApi.Companion.invoke().upload(descriptionBody, body,
                                latitude, longitude, country, state, city, auth);
                        Objects.requireNonNull(call).enqueue(new Callback<ResponseBody>() {
                            @Override
                            public void onResponse(Call<ResponseBody> call,
                                    Response<ResponseBody> response) {
                                description.setText("");
                                mProgressBar.setVisibility(View.GONE);
                                bg.setVisibility(View.GONE);
                                currentPhotoPath = null;
                                videoUri = null;
                                reportImage.setImageResource(R.drawable.report);
                                Objects.requireNonNull(getActivity()).getWindow().clearFlags(
                                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                                Toast.makeText(getContext(), "SUCCESS", Toast.LENGTH_LONG).show();
                                Log.e("resprepo", response.toString());
                                try {
                                    if (response.body() != null) {
                                        Log.e("re", response.body().string());
                                    } else {
                                        if (response.errorBody() != null) {
                                            Log.e("re", response.errorBody().string());
                                        }
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onFailure(Call<ResponseBody> call, Throwable t) {
                                Log.e("resprepo", t.toString());
                                mProgressBar.setVisibility(View.GONE);
                                bg.setVisibility(View.GONE);
                                Objects.requireNonNull(getActivity()).getWindow().clearFlags(
                                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                                Snackbar snackbar = Snackbar.make(mCoordinatorLayout,
                                        t.getMessage(),
                                        Snackbar.LENGTH_LONG).setAction("RETRY",
                                        new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                uploadFile(fileUri, code);
                                            }
                                        });
                                snackbar.setActionTextColor(Color.RED);
                                snackbar.show();
                            }
                        });
                    }
                });

        if(flag.size()==0){Log.e("get","fvs");
            final LocationManager locationManager = (LocationManager) getContext().getSystemService(
                    Context.LOCATION_SERVICE);
            boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            if (ActivityCompat.checkSelfPermission(getContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    getContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED || !isGPSEnabled){
                mProgressBar.setVisibility(View.GONE);
                bg.setVisibility(View.GONE);
                Objects.requireNonNull(getActivity()).getWindow().clearFlags(
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                Snackbar snackbar = Snackbar.make(mCoordinatorLayout,
                        "Please give location access and turn on gps",
                        Snackbar.LENGTH_LONG).setAction("RETRY",
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                uploadFile(fileUri, code);
                            }
                        });
                snackbar.setActionTextColor(Color.RED);
                snackbar.show();
            }

        }


    }

    private File persistImage(Bitmap bitmap, String name) {
        File filesDir = Objects.requireNonNull(getContext()).getApplicationContext().getFilesDir();
        File imageFile = new File(filesDir, name + ".jpg");

        OutputStream os;
        try {
            os = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
            os.flush();
            os.close();
            return imageFile;
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), "Error writing bitmap", e);
            return null;
        }
    }
}
