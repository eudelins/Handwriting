package com.example.handwriting;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import org.tensorflow.lite.Interpreter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.app.Activity.RESULT_OK;

public class FirstFragment extends Fragment {
    private Context context;

    private ImageView imageView;
    private Button buttonCamera;
    private Button buttonImport;
    private Button buttonReadNumber;
    private Button rotateImageButton;

    private Interpreter tfLiteModel;
    private ProgressDialog mProgressDialog;
    private TextView textViewNumber;
    private String textNumber;

    String currentPhotoPath;
    private Bitmap currentPhotoBitmap;
    private final static int REQUEST_IMAGE_CAPTURE = 1;
    private final static int RESULT_LOAD_IMG = 1;

    private PyObject pythonScript;
    private byte[] inputData;


    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false);
    }


    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        context = getActivity().getApplicationContext();

        try {
            AssetFileDescriptor fileDescriptor = getActivity().getAssets().openFd("model.tflite");
            FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();
            ByteBuffer tfLiteFile = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
            tfLiteModel = new Interpreter(tfLiteFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (!Python.isStarted()) Python.start(new AndroidPlatform(getActivity()));
        Python py = Python.getInstance();
        pythonScript = py.getModule("recup_pixels");

        textViewNumber = view.findViewById(R.id.textNumber);
        imageView = (ImageView) view.findViewById(R.id.imageView);
        createCameraButton(view);
        createImportButton(view);
        createButtonReadNumber(view);
        createRotateImageButton(view);
    }

    private void createButtonReadNumber(View view) {
        buttonReadNumber = view.findViewById(R.id.buttonReadNumber);
        buttonReadNumber.setEnabled(false);
        buttonReadNumber.setVisibility(View.INVISIBLE);
        buttonReadNumber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                readNumber();
            }
        });
    }

    private void createRotateImageButton(View view) {
        rotateImageButton = view.findViewById(R.id.rotateImageButton);
        rotateImageButton.setEnabled(false);
        rotateImageButton.setVisibility(View.INVISIBLE);
        rotateImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rotateBitmap((float) 90.0);
                imageView.setImageBitmap(currentPhotoBitmap);
            }
        });
    }


    /**
     * Save an image in the right directory
     * @return the file created for the image
     * @throws IOException creation failed
     */
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }


    void rotateBitmap(float x)
    {
        // create new matrix
        Matrix matrix = new Matrix();

        // setup rotation degree
        matrix.postRotate(x);
        Bitmap bmp = Bitmap.createBitmap(currentPhotoBitmap, 0, 0, currentPhotoBitmap.getWidth(), currentPhotoBitmap.getHeight(), matrix, true);
        currentPhotoBitmap = bmp;
    }


    /**
     * Display the picture taken in the imageView
     */
    private void setPic() {
        currentPhotoBitmap = BitmapFactory.decodeFile(currentPhotoPath);

        try {
            InputStream imageStream = new FileInputStream(currentPhotoPath);
            inputData = getBytes(imageStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


        rotateBitmap((float) 90.0);
        imageView.setImageBitmap(currentPhotoBitmap);

        // delete the temporary file that contains the image
        File imageFile = new File(currentPhotoPath);
        if (imageFile.exists()) {
            if (imageFile.delete()) {
                System.out.println("file Deleted :" + currentPhotoPath);
            } else {
                System.out.println("file not Deleted :" + currentPhotoPath);
            }
        }
    }

    /**
     * Method which is called when a picture is taken
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data == null && resultCode == RESULT_OK) {
            setPic();
        } else if (resultCode == RESULT_OK) {
            try {
                final Uri imageUri = data.getData();
                final InputStream imageStream = context.getContentResolver().openInputStream(imageUri);
                inputData = getBytes(context.getContentResolver().openInputStream(imageUri));
                final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                currentPhotoBitmap =  selectedImage;
                rotateBitmap((float) 90.0);
                imageView.setImageBitmap(currentPhotoBitmap);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(context, "Une erreur s'est produite", Toast.LENGTH_LONG).show();
            }
        }

        buttonReadNumber.setEnabled(true);
        buttonReadNumber.setVisibility(View.VISIBLE);
        rotateImageButton.setEnabled(true);
        rotateImageButton.setVisibility(View.VISIBLE);
    }

    public void createCameraButton(View view) {
        buttonCamera = (Button) view.findViewById(R.id.buttonCamera);
        buttonCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //prepare intent
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                // Ensure that there's a camera activity to handle the intent
                if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                    // Create the File where the photo should go
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                    } catch (IOException ex) {
                        Toast.makeText(context, "errorFileCreate", Toast.LENGTH_SHORT).show();
                        Log.i("File error", ex.toString());
                    }
                    // Continue only if the File was successfully created
                    if (photoFile != null) {
                        Uri photoURI = FileProvider.getUriForFile(getActivity(),
                                "com.example.handwriting.fileprovider",
                                photoFile);
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                    }


                }
            }
        });
    }


    public void createImportButton(View view) {
        buttonImport = (Button) view.findViewById(R.id.buttonImport);
        buttonImport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, RESULT_LOAD_IMG);
            }
        });
    }


    private void readNumber() {
        mProgressDialog = new ProgressDialog(getActivity());
        mProgressDialog.setTitle("Reading process dialog");
        mProgressDialog.setMessage("The Network is reading...");
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();

        new Thread(new Runnable() {
            public void run() {
                PyObject pix = pythonScript.callAttr("get_pixels_from_data", inputData);
                float[][][][] pixels = pix.toJava(float[][][][].class);
                float[][] output = new float[1][10];

                tfLiteModel.run(pixels, output);
                textNumber = Integer.toString(argmax(output[0]));

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (textNumber != null && !textNumber.equals("")) {
                            textViewNumber.setText(textNumber);
                        }

                        mProgressDialog.dismiss();
                    }
                });
            }
        }).start();
    }

    private int argmax(float[] array) {
        if (array.length == 0) return 0;

        float maxVal = array[0];
        int indiceMax = 0;
        for (int i = 0; i < array.length; i++) {
            if (array[i] > maxVal) {
                maxVal = array[i];
                indiceMax = i;
            }
        }
        return indiceMax;
    }

    public byte[] getBytes(InputStream inputStream) {
        try {
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];

            int len = 0;
            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            return byteBuffer.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}