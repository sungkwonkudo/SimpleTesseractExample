package com.imperialsoupgmail.tesseractexample;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telecom.Call;
import android.util.Log;
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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
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
    private TextView mTextView;
    private Button cameraButton;
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
        cameraButton = (Button)findViewById(R.id.OCRbutton);
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

    class processImage implements Callable{
        String OCRresult;
        @Override
        public String call() throws Exception {
            mTess.setImage(image);
            OCRresult = mTess.getUTF8Text();
            return OCRresult;
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
        } catch (FileNotFoundException e) {
            e.printStackTrace();
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

    class getImage implements Callable{
        Context cont;

        getImage(Context c) {
            this.cont = c;
        }
        @Override
        public Bitmap call() throws Exception {
            Drawable d = ContextCompat.getDrawable(cont, R.drawable.test1);
            image = ((BitmapDrawable)d).getBitmap();

            return image;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == TAKE_PICTURE && resultCode == RESULT_OK) {
            // Thread pool manager
            ExecutorService executor = Executors.newFixedThreadPool(8);

            // Code commented out to test for threading speeds.
            //Bundle extras = intent.getExtras();
            //image = (Bitmap) extras.get("data");


            // Test image, hard coded into getImage class
            Future<Bitmap> iFuture = executor.submit(new getImage(this));
            try {
                image = iFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            mImageView.setImageBitmap(image);

            // OCR
            Future<String> tFuture = executor.submit(new processImage());
            String result = null;
            try {
                result = tFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }


            Future<String> kFuture = executor.submit(new defineKanji(result));

            String definition = null;
            try {
                definition = kFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }

            TextView OCRTextView = (TextView) findViewById(R.id.OCRTextView);
            OCRTextView.setText(definition);
        }
        else {
            Toast.makeText(getApplicationContext(), "Error getting image", Toast.LENGTH_LONG).show();
        }
    }

    class defineKanji implements Callable{
        String input;
        defineKanji(String input){
            this.input = input;
        }

        @Override
        public String call() throws Exception {
            String holder = "";
            SQLiteDatabase kdatabase = db.getFirstKanjiResult();

            // Check for punctuations that could potentially crash the code and SQLite
            final String punctuations = ".,<>:;\'\")(*&^%$#@!+_-=\\|[]{}?/~`";

            for (Token token : tokenizer.tokenize(input)) {
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
                    Query qKanji = new Query(strToken, kdatabase);

                    holder += token.getSurface() + " " + qKanji.call() + "\n";
                }
            }
            return holder;
        }

    }


}
