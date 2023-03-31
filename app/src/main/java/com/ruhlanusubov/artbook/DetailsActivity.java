package com.ruhlanusubov.artbook;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.ruhlanusubov.artbook.databinding.ActivityDetailsBinding;
import com.ruhlanusubov.artbook.databinding.ActivityMainBinding;

import java.io.ByteArrayOutputStream;

public class DetailsActivity extends AppCompatActivity {

    private ActivityDetailsBinding binding;
    ActivityResultLauncher<Intent> activityResultLauncher;
    ActivityResultLauncher<String> permissionLauncher;
    Intent intenttoGallery;
    Bitmap selection;

    SQLiteDatabase database;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetailsBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        database=this.openOrCreateDatabase("Arts",MODE_PRIVATE,null);
        registerLauncher();

        Intent intent=getIntent();
         String info=intent.getStringExtra("info");
         if(info.equals("new")){
             //new art
             binding.nameText.setText("");
             binding.artistText.setText("");
             binding.yearText.setText("");
             binding.button.setVisibility(View.VISIBLE);
             binding.selectedimage.setImageResource(R.drawable.select);
         }
         else{

        int artId=intent.getIntExtra("artId",0);
        binding.button.setVisibility(View.INVISIBLE);

        try {

            Cursor cursor=database.rawQuery("SELECT * FROM arts WHERE id=?",new String[] {String.valueOf(artId)});
            int artNameIx=cursor.getColumnIndex("artname");
            int painterNameIx=cursor.getColumnIndex("paintername");
            int yearIx=cursor.getColumnIndex("year");
            int imageIx=cursor.getColumnIndex("image");

            while(cursor.moveToNext()){
                binding.nameText.setText(cursor.getString(artNameIx));
                binding.artistText.setText(cursor.getString(painterNameIx));
                binding.yearText.setText(cursor.getString(yearIx));

                byte[] bytes=cursor.getBlob(imageIx);
                Bitmap bitmap= BitmapFactory.decodeByteArray(bytes,0,bytes.length);

                binding.selectedimage.setImageBitmap(bitmap);
            }
            cursor.close();

        }
        catch (Exception e){
            e.printStackTrace();
        }
         }
    }


    public void savebutton(View view){

            String name=binding.nameText.getText().toString();
            String artistname=binding.artistText.getText().toString();
            String year=binding.yearText.getText().toString();

            Bitmap smallImage=makeSmallerimage(selection,300);

        ByteArrayOutputStream outputStream=new ByteArrayOutputStream();
        smallImage.compress(Bitmap.CompressFormat.PNG,50,outputStream);
        byte[] byteArray=outputStream.toByteArray();


        //database

        try{


            database.execSQL("CREATE TABLE IF NOT EXISTS arts (id INTEGER PRIMARY KEY,artname VARCHAR,paintername VARCHAR,year VARCHAR,image BLOB)");
            String sqlString="INSERT INTO arts (artname,paintername,year,image) VALUES(?,?,?,?)";
            SQLiteStatement sqLiteStatement=database.compileStatement(sqlString);
            sqLiteStatement.bindString(1,name);
            sqLiteStatement.bindString(2,artistname);
            sqLiteStatement.bindString(3,year);
            sqLiteStatement.bindBlob(4,byteArray);
            sqLiteStatement.execute();


        }
        catch (Exception e){
            e.printStackTrace();

        }
        //finish();//1-ci yol
    Intent intent=new Intent(DetailsActivity.this,MainActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);//2-ci yol butun activityni bagliyir sonrakina kecir
        startActivity(intent);




    }

    public Bitmap makeSmallerimage(Bitmap image,int maxsize){
        int width=image.getWidth();
        int height=image.getHeight();
        float bitmapRatio=(float)width/(float) height;
        if(bitmapRatio>1){
            //landscape image
            width=maxsize;
            height=(int)(width/bitmapRatio);
        }
        else{
            height=maxsize;
            width=(int)(width*bitmapRatio);

        }
        return image.createScaledBitmap(image,width,height,true);
    }
    public void selectedimage(View view) {

        //for onlu api 33+->READ_MEDIA_IMAGES
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_MEDIA_IMAGES)) {

                    Snackbar.make(view, "Permission needed for gallery!", Snackbar.LENGTH_INDEFINITE).setAction("Give permission", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //request permission
                            permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);

                        }
                    }).show();
                } else {
                    //request permission
                    permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);


                }

            } else {
                //gallery
                intenttoGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                activityResultLauncher.launch(intenttoGallery);


            }



        } else { //READ_EXTERNAL_STORAGE


            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {

                    Snackbar.make(view, "Permission needed for gallery!", Snackbar.LENGTH_INDEFINITE).setAction("Give permission", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //request permission
                            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);

                        }
                    }).show();
                } else {
                    //request permission
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);


                }

            } else {
                //gallery
                intenttoGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                activityResultLauncher.launch(intenttoGallery);


            }

        }
    }
    private void registerLauncher(){

        activityResultLauncher=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if(result.getResultCode()==RESULT_OK){
                    Intent intentfromResult=result.getData();
                    if(intentfromResult!=null){
                        Uri imagedata= intentfromResult.getData();
                        //binding.selectedimage.setImageURI(imagedata);// alternative,seklin datasi lazmdi deye bitmapa ceviririk,database de save olacagi ucun

                        try {
                            if(Build.VERSION.SDK_INT>=28) {
                                ImageDecoder.Source source = ImageDecoder.createSource(DetailsActivity.this.getContentResolver(), imagedata);
                                selection = ImageDecoder.decodeBitmap(source);
                                binding.selectedimage.setImageBitmap(selection);
                            }
                            else{
                                selection=MediaStore.Images.Media.getBitmap(DetailsActivity.this.getContentResolver(),imagedata);
                                binding.selectedimage.setImageBitmap(selection);
                            }
                        }
                        catch (Exception e){
                            e.printStackTrace();
                        }


                    }
                }
            }
        });
        permissionLauncher=registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) {
                if(result){
                    //permission granted
                    intenttoGallery=new Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    activityResultLauncher.launch(intenttoGallery);
                }
                else{
                    //permission denied
                    Toast.makeText(DetailsActivity.this,"Permission denied!",Toast.LENGTH_LONG).show();
                }
            }
        });

    }
}