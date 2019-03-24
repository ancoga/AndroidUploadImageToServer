package com.delaroystudios.uploadimagetoserver;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;


public class MainActivity extends AppCompatActivity {
    /**
     * Direccion donde se subiran los archivos, que puede ser una direccion de un servidor
     * url where the files will be uploaded
     * @String URL
     * @access public static
     */
    public static String URL = "";
    /**
     * Carpeta donde se guardaran los archivos subidos
     * Folder where the uploaded files will be saved
     * @String IMAGE_CAPTURE_FOLDER
     * @access private static
     */
    private static final String IMAGE_CAPTURE_FOLDER = "uploadImg";
    /**
     * Constante númerica que sirve para saber si hemos seleccionado la foto que realizamos
     * Numeric constant used to know if we have selected the photo we made
     * @int CAMERA_PIC_REQUEST
     * @access private static
     */
    private static final int CAMERA_PIC_REQUEST = 1111;
    /**
     * Código númerico para saber si se ha seleccionado una imagen de la galeria de fotos
     * Numerical code to know if an image of the photo gallery has been selected
     * @int SELECT_PICTURE
     * @access private static
     */
    private final int SELECT_PICTURE = 200;
    private Button btnCamera;
    private static File file;
    private static Uri _imagefileUri;
    private String urlImg;
    private TextView resultText;
    private static String _bytes64Sting, _imageFileName;
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /**
         * Part we link the elements of the design part with the programming part.
         */
        btnCamera = (Button) findViewById(R.id.button);
        imageView = (ImageView) findViewById(R.id.imageView);
        /**
         * Share where we use the Picasso library
         * that allows you to load images without problems in your application, and what is better, the vast majority of the time with a single line of code!
         */
        Picasso.with(this).load(R.mipmap.ic_launcher).into(imageView);
        /**
         * Doesn't enable any leakage of the application's components.
         */
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        /**
         * Part for programming the camera button
         */
        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                final CharSequence[] options = {"Tomar foto", "Elegir de galeria", "Cancelar"};
                final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Elige una opción");
                builder.setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (options[which]=="Tomar foto") {
                            captureImage(view);
                        }else if(options[which]=="Elegir de galeria"){
                            selectedImage();
                        }else if(options[which]=="Cancelar"){
                            dialog.dismiss();
                        }
                    }
                });
                builder.show();
            }
        });
    }

    /**
     * Method to generate alphanumeric random code with the desired length
     * @param length
     * @return string
     */
    public static String randomString(int length){
        SecureRandom random = new SecureRandom();
        char[] chars = new char[length];
        for(int i=0;i<chars.length;i++)
        {
            int v = random.nextInt(10 + 26 + 26);
            char c;
            if (v < 10)
            {
                c = (char)('0' + v);
            }
            else if (v < 36)
            {
                c = (char)('a' - 10 + v);
            }
            else
            {
                c = (char)('A' - 36 + v);
            }
            chars[i] = c;
        }
        return new String(chars);
    }//End Method randomString
    /**
     * Method to select gallery image to later upload it to the server
     */
    private void selectedImage(){
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent.createChooser(intent, "Selecciona app de imagen"), SELECT_PICTURE);
    }//End Method selectedImage

    /**
     * Method that makes a photo with the camera of the mobile to later upload that photo to the server.
     * @param v receives the view
     */
    private void captureImage(View v) {
        Intent intent;
        _imagefileUri = Uri.fromFile(getFile());
        int id;
        id=v.getId();
        switch (id){
            case R.id.button:
                intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent,CAMERA_PIC_REQUEST);
                break;
        }
    }//End Method captureImage

    /**
     * Method that prepares the upload of the image either because we have taken a photo or taken from the gallery
     * @param requestCode response code
     * @param resultCode Resulting code
     * @param data The intent
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode){
                case CAMERA_PIC_REQUEST:
                    uploadImage(_imagefileUri.getPath(), data);
                break;
                case SELECT_PICTURE:
                    _imagefileUri = data.getData();
                    uploadImage(_imagefileUri.getPath(), data);
                    break;
            }
        } else if (resultCode == RESULT_CANCELED) {
            // user cancelled Image capture
            Toast.makeText(getApplicationContext(),
                    "User cancelled image capture", Toast.LENGTH_SHORT).show();
        } else {
            // failed to capture image
            Toast.makeText(getApplicationContext(),
                    "Sorry! Failed to capture image", Toast.LENGTH_SHORT).show();
        }
    }//End Method onActivityResult

    /**
     * Method that prepares the photo that we want to upload to the server and then calls the function that will upload the photo
     * @param picturePath photo route
     * @param data the intent
     */
    private void uploadImage(String picturePath, Intent data) {
        _imageFileName = randomString(11); // poner código aleatorio en el nombre
        Bitmap bm;
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        Bundle ext = data.getExtras();
        if(ext != null){
            bm = (Bitmap)ext.get("data");
            bm.compress(Bitmap.CompressFormat.JPEG, 100, bao);
        }else{
            //bm = BitmapFactory.decodeFile(picturePath);
            try{
                bm = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());
                bm.compress(Bitmap.CompressFormat.JPEG, 100, bao);
            }catch (IOException e){
                e.printStackTrace();
            }
        }
        byte[] byteArray = bao.toByteArray();
        _bytes64Sting = Base64.encodeBytes(byteArray);
        RequestPackage rp = new RequestPackage();
        rp.setMethod("POST");
        rp.setUri(URL);
        rp.setSingleParam("base64", _bytes64Sting);
        rp.setSingleParam("ImageName", _imageFileName + ".jpg");

        // Upload image to server
        new uploadToServer().execute(rp);
    }//End Method uploadImage

    /**
     * Class that uploads the photo, which we select or make, to the server.
     */
    public class uploadToServer extends AsyncTask<RequestPackage, Void, String> {

        private ProgressDialog pd = new ProgressDialog(MainActivity.this);

        /**
         * Method before executing the upload of the photo, where we create the name of the file that is going to upload
         */
        protected void onPreExecute() {
            super.onPreExecute();
            resultText = (TextView) findViewById(R.id.textView);
            resultText.setText("New file "+_imageFileName+".jpg created\n");
            pd.setMessage("Image uploading!, please wait..");
            pd.setCancelable(false);
            pd.show();
        }//End Method onPreExecute

        @Override
        protected String doInBackground(RequestPackage... params) {

            String content = MyHttpURLConnection.getData(params[0]);
            return content;

        }//End Method doInBackground

        /**
         * After the upload of the photo we collect what the server returns
         * we show in an imageView the photo that we just upload to the server
         * @param result returned by the server
         */
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            pd.hide();
            pd.dismiss();
            resultText.append(result);
            //Cargamos la imagen cuando se ha terminado de ejecutar y subir el archivo al servidor.
            urlImg = "http://DOMINIO/"+_imageFileName;
            Picasso.with(MainActivity.this).load(urlImg)
                    .error(R.mipmap.ic_launcher)
                    .into(imageView);
            showNotification("Titulo", "Imagen subida");
        }//End Method onPostExecute
    }//End Class uploadToServer

    /**
     * Metodo que muestra la notificación en nuestro movil
     * Method that shows the notification on our mobile
     * @param title Title of the notification
     * @param content Content of the notification
     */
    private void showNotification(String title, String content){
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("default","YOUR_CHANNEL_NAME", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("YOUR_NOTIFICATION_CHANNEL_DISCRIPTION");
            mNotificationManager.createNotificationChannel(channel);
        }
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext(), "default")
                .setSmallIcon(R.mipmap.ic_launcher) // notification icon
                .setContentTitle(title) // title for notification
                .setContentText(content)// message for notification
                //.setSound(alarmSound) // set alarm sound for notification
                .setAutoCancel(true); // clear notification after click
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pi);
        mNotificationManager.notify(0, mBuilder.build());
    }//End Method showNotification

    /**
     * Method that returns the photo that we just made with the photo camera of the mobile
     * @return File, image file with the capture that we just made.
     */
    private File getFile() {
        String filepath = Environment.getExternalStorageDirectory().getPath();
        file = new File(filepath, IMAGE_CAPTURE_FOLDER);
        if (!file.exists()) {
            file.mkdirs();
        }

        return new File(file + File.separator + _imageFileName
                + ".jpg");
    }//End Method getFile


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }//End Method onCreateOptionsMenu

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }//End Method onOptionsItemSelected

}//End Class MainActivity
