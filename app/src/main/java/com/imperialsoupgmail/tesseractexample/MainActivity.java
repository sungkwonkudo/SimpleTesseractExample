package com.imperialsoupgmail.tesseractexample;


import android.content.Intent;
import android.content.res.AssetManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;

import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.atilika.kuromoji.ipadic.Tokenizer;
import com.atilika.kuromoji.ipadic.Token;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MainActivity extends AppCompatActivity {

    // Tesseract
    Bitmap image;
    private TessBaseAPI mTess;
    String datapath = "";
    String LANG = "jpn";

    // Camera
    private static int TAKE_PICTURE = 1;
    protected ImageView mImageView;
    private View.OnClickListener cameraListener = new View.OnClickListener() {
        public void onClick(View v) {
            takePhoto();
        }
    };

    // SQLite Database
    private Database db;

    // Kuromoji
    Tokenizer tokenizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Database initialization
        db = new Database(this);

        // Camera and image view
        Button cameraButton = (Button) findViewById(R.id.OCRbutton);
        assert cameraButton != null;
        cameraButton.setOnClickListener(cameraListener);
        mImageView = (ImageView) findViewById(R.id.imageView);

        // Tesseract initialization
        String language = LANG;
        datapath = getFilesDir()+ "/tesseract/";
        mTess = new TessBaseAPI();
        checkFile(new File(datapath + "tessdata/"));
        mTess.init(datapath, language);

        // Kuromoji
        tokenizer = new Tokenizer.Builder().build();
    }

    private class processImage implements Callable{
        Bitmap rawImage;

        processImage(Bitmap inputImage){
            this.rawImage = inputImage;
        }

        @Override
        public Object call() throws Exception {
            mTess.setImage(rawImage);
            return mTess.getUTF8Text();
        }
    }

    private void checkFile(File dir) {
        if (!dir.exists()&& dir.mkdirs()){
                copyFiles();
        }
        if(dir.exists()) {
            String datafilepath = datapath+ "/" + "tessdata/" + LANG + ".traineddata";
            File datafile = new File(datafilepath);

            if (!datafile.exists()) {
                copyFiles();
            }
        }
    }

    private void copyFiles() {
        try {
            String filepath = datapath + "/" + "tessdata/" + LANG + ".traineddata";
            AssetManager assetManager = getAssets();

            InputStream instream = assetManager.open("tessdata/" + LANG + ".traineddata");
            OutputStream outstream = new FileOutputStream(filepath);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = instream.read(buffer)) != -1) {
                outstream.write(buffer, 0, read);
            }


            outstream.flush();
            outstream.close();
            instream.close();

            File file = new File(filepath);
            if (!file.exists()) {
                throw new FileNotFoundException();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void takePhoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, TAKE_PICTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);


        if (requestCode == TAKE_PICTURE && resultCode == RESULT_OK) {
            ExecutorService OCRexecutor = Executors.newFixedThreadPool(8);

            Bundle extras = intent.getExtras();
            image = (Bitmap) extras.get("data");
            mImageView.setImageBitmap(image);

            // Do the OCR on a separate thread, use future to do in order.
            Future ocrFuture = OCRexecutor.submit(new processImage(image));

            String definition = "";
            try {
                definition = defineKanji((String) ocrFuture.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            } finally {
                db.close();
            }

            TextView OCRTextView = (TextView) findViewById(R.id.OCRTextView);
            assert OCRTextView != null;
            OCRTextView.setText(definition);
        }
        else {
            Toast.makeText(getApplicationContext(), "Error getting image", Toast.LENGTH_LONG).show();
        }
    }

    public String defineKanji(String input){
        String holder = "";
        SQLiteDatabase kDatabase = db.getDatabase();

        // Check for punctuations that could potentially crash the code and SQLite
        final String punctuations = ".,<>:;\'\")(*&^%$#@!+_-=\\|[]{}?/~`";

        ExecutorService executor = Executors.newFixedThreadPool(30);
        List<Future<String>> futureList = new ArrayList<>();
        List<String> tokenList = new ArrayList<>();
        List<Token> wordList = null;
        Future kuromojiFuture = executor.submit(new Kuromoji(input, tokenizer));
        try {
             wordList = (List<Token>) kuromojiFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        
        for (Token token : wordList) {
            // Get the whole word
            String strToken = token.getSurface();

            boolean safety = true;

            // Check if the 'word' contains punctuation
            for(int i=0; i<strToken.length(); i++){
                String str = Character.toString(strToken.charAt(i));

                // If it contains punctuations,
                // exit the loop and set safety indicator to false
                if (punctuations.contains(str)){
                    safety = false;
                    i=strToken.length();
                }
            }
            if(safety){
                tokenList.add(strToken);
                futureList.add(executor.submit(new Query(strToken, kDatabase)));

                //Query qKanji = new Query(strToken, kdatabase);
                //holder += token.getSurface() + " " + qKanji.call() + "\n";
            }
        }
        for(int i=0; i<tokenList.size() && i<futureList.size() ; i++){
            try {
                String def;
                if(futureList.get(i).get() == null){
                    def = " ";
                }else{
                    def = futureList.get(i).get();
                }
                holder += tokenList.get(i)  + " " + def + "\n";
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        return holder;
    }


}
