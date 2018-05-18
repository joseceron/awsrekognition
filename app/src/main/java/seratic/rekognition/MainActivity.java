package seratic.rekognition;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.AWSStartupHandler;
import com.amazonaws.mobile.client.AWSStartupResult;
import com.amazonaws.mobileconnectors.cognito.CognitoSyncManager;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.polly.AmazonPollyPresigningClient;
import com.amazonaws.services.polly.model.OutputFormat;
import com.amazonaws.services.polly.model.SynthesizeSpeechPresignRequest;
import com.amazonaws.services.polly.model.Voice;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClient;
import com.amazonaws.services.rekognition.model.DetectLabelsRequest;
import com.amazonaws.services.rekognition.model.DetectLabelsResult;
import com.amazonaws.services.rekognition.model.FaceMatch;
import com.amazonaws.services.rekognition.model.FaceRecord;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.IndexFacesRequest;
import com.amazonaws.services.rekognition.model.IndexFacesResult;
import com.amazonaws.services.rekognition.model.Label;
import com.amazonaws.services.rekognition.model.SearchFacesByImageRequest;
import com.amazonaws.services.rekognition.model.SearchFacesByImageResult;
import com.amazonaws.util.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final String COGNITO_POOL_ID = "us-east-1:8ac024db-0ea3-4c53-abfa-c2a6d0d82321";

    /*
     * Region of your Cognito identity pool ID.
     */
    public static final String COGNITO_POOL_REGION = "us-east-1";

    /*
     * Note, you must first create a bucket using the S3 console before running
     * the sample (https://console.aws.amazon.com/s3/). After creating a bucket,
     * put it's name in the field below.
     */
    public static final String BUCKET_NAME = "fotos1103";

    /*
     * Region of your bucket.
     */
    public static final String BUCKET_REGION = "us-east-1";

    static SimpleDateFormat simpleDateFormat;

    private static final int CAMERA_REQUEST = 1888;
    private ImageView imageView;
    private static final int MY_CAMERA_PERMISSION_CODE = 100;
    private InputStream inputStream = null;
    Button compararButton,guardarButton,photoButton;
    EditText etNombreFoto;
    Integer tiempo;

    //Nombre de la colección creada donde se almacenan las fotos de referencia.
    public static final String collectionId = "exampleCollection";
    public static final String bucket = "photo-label-detect";
    public static final String fileName = "Mario";
    public String strNombreFoto;
    public String saludo;

    MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.imageView = (ImageView) this.findViewById(R.id.imageView1);
        photoButton = (Button) this.findViewById(R.id.button1);
        compararButton = (Button) this.findViewById(R.id.button2);
        guardarButton = (Button) this.findViewById(R.id.button3);
        etNombreFoto = (EditText)this.findViewById(R.id.str_nombre);

        setHorario();
        setupNewMediaPlayer();




        photoButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (checkSelfPermission(Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.CAMERA},
                            MY_CAMERA_PERMISSION_CODE);
                } else {
                    Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, CAMERA_REQUEST);
                }
            }
        });

        compararButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (inputStream != null) {
                    try {
                        Thread thread = new Thread(new Runnable() {

                            @Override
                            public void run() {
                                try {
                                    //Your code goes here
                                    searchImage(inputStream);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });

                        thread.start();

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });


            guardarButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    if (inputStream != null) {


                        try {
                            Thread thread = new Thread(new Runnable() {

                                @Override
                                public void run() {
                                    try {
                                        //Your code goes here
                                        //Your code goes here
                                        strNombreFoto = etNombreFoto.getText().toString();
                                        if(strNombreFoto.equals("")){
                                            Toast.makeText(MainActivity.this,"Debe insertar un nombre para la foto",Toast.LENGTH_SHORT);
                                        }
                                        else {

                                                guardarImagenCollection(inputStream);

                                        }

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            });

                            thread.start();

                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }

                    }
                    else{
                        Toast.makeText(MainActivity.this,"Debe tomar la foto",Toast.LENGTH_SHORT);
                    }
                }
            });


    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
                Intent cameraIntent = new
                        Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            } else {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }

        }


    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            imageView.setImageBitmap(photo);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            photo.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
            byte[] bitmapdata = bos.toByteArray();
            ByteArrayInputStream bs = new ByteArrayInputStream(bitmapdata);
            inputStream = bs;
        }

    }

    public void searchImage(InputStream inputStream) throws Exception {
        //String photo="/Users/myapps/Documents/images/Karla.jpeg";

        String name = "";
        String texto;
        ByteBuffer imageBytes = null;
        try {
            imageBytes = ByteBuffer.wrap(IOUtils.toByteArray(inputStream));
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                COGNITO_POOL_ID,
                Regions.fromName(COGNITO_POOL_REGION)
        );

        CognitoSyncManager syncClient = new CognitoSyncManager(
              getApplicationContext(),
            Regions.US_EAST_1, // Region
           credentialsProvider);
        AmazonPollyPresigningClient client = new AmazonPollyPresigningClient(credentialsProvider);

        AmazonRekognition amazonRekognition = new AmazonRekognitionClient(credentialsProvider);
        amazonRekognition.setRegion(Region.getRegion(Regions.fromName(COGNITO_POOL_REGION)));

        // Search collection for faces similar to the largest face in the image.
        SearchFacesByImageRequest searchFacesByImageRequest = new SearchFacesByImageRequest()
                .withCollectionId(collectionId)
                .withImage(new Image().withBytes(imageBytes))
                .withFaceMatchThreshold(50F)
                .withMaxFaces(1);

        //Buscar la foto enviada en la colección
        SearchFacesByImageResult searchFacesByImageResult =
                amazonRekognition.searchFacesByImage(searchFacesByImageRequest);

        System.out.println("Faces matching largest face in image from" + fileName);

        List<FaceMatch> faceImageMatches = searchFacesByImageResult.getFaceMatches();
        for (FaceMatch face : faceImageMatches) {

            name = face.getFace().getExternalImageId();
            System.out.println("La cara es: " + face.getFace().getExternalImageId());
            System.out.println();

        }

        //------o-----

        //Establecer características para Label
        DetectLabelsRequest detectLabelsRequest = new DetectLabelsRequest()
                .withImage(new Image().withBytes(imageBytes))
                .withMaxLabels(20)
                .withMinConfidence(50F);
        //Detectar label de la foto enviada
        DetectLabelsResult searchFacesByImageResult2 =
                amazonRekognition.detectLabels(detectLabelsRequest);

        List<Label> label = amazonRekognition.detectLabels(detectLabelsRequest).getLabels();

        String strLabel="";
        Integer n=1;
        for (Label labels : label){
            System.out.println("Label : "+n+": "+ labels.getName());
            n=n+1;
        }

        if(name.equals("") ){
            texto = "Sujeto no identificado, exterminar";
      }else{
            texto = saludo+name;
        }

        // Create speech synthesis request.
        SynthesizeSpeechPresignRequest synthesizeSpeechPresignRequest =
                new SynthesizeSpeechPresignRequest()
                        // Set text to synthesize.
                        .withText(texto)
                        // Set voice selected by the user.
                        // .withVoiceId(selectedVoice.getId())
                        .withVoiceId("Miguel")
                        // Set format to MP3.
                        .withOutputFormat(OutputFormat.Mp3);

        // Get the presigned URL for synthesized speech audio stream.
        URL presignedSynthesizeSpeechUrl =
                client.getPresignedSynthesizeSpeechUrl(synthesizeSpeechPresignRequest);


        // Create a media player to play the synthesized audio stream.
        if (mediaPlayer.isPlaying()) {
            setupNewMediaPlayer();
        }
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        try {
            // Set media player's data source to previously obtained URL.
            mediaPlayer.setDataSource(presignedSynthesizeSpeechUrl.toString());
        } catch (IOException e) {
        }

        // Start the playback asynchronously (since the data source is a network stream).
        mediaPlayer.prepareAsync();



    }



    public void guardarImagenCollection(InputStream inputStream) throws Exception {

        ByteBuffer imageBytes = null;
        try {
            //crea el objeto imagen
            imageBytes = ByteBuffer.wrap(IOUtils.toByteArray(inputStream));
            Image img = new Image().withBytes(imageBytes);

            //Se establece la petición: indico en el noombre de la colección, la imagen a crear con el nombre strNombreFoto
            IndexFacesRequest indexFacesRequest = new IndexFacesRequest()
                        .withImage(img)
                        .withCollectionId(collectionId)
                        .withExternalImageId(strNombreFoto)
                        .withDetectionAttributes("ALL");

            //Registro en aws
            AWSCredentials credentials;
            CognitoCachingCredentialsProvider credentialsProvider2 = new CognitoCachingCredentialsProvider(
                        getApplicationContext(),
                        COGNITO_POOL_ID,
                        Regions.fromName(COGNITO_POOL_REGION)
                );

            AmazonRekognition amazonRekognition = new AmazonRekognitionClient(credentialsProvider2);
            amazonRekognition.setRegion(Region.getRegion(Regions.fromName(COGNITO_POOL_REGION)));

            //Guardo la foto en la colección de aws con la función index faces
            IndexFacesResult indexFacesResult=amazonRekognition.indexFaces(indexFacesRequest);

            System.out.println(strNombreFoto + " Creada");
            Toast.makeText(MainActivity.this,"Foto de "+strNombreFoto+ " creada",Toast.LENGTH_SHORT);

            //Consulto e imprimo para validar que la foto fue creada
            List <FaceRecord> faceRecords = indexFacesResult.getFaceRecords();
            for (FaceRecord faceRecord: faceRecords) {

                 System.out.println("Face detected: Faceid is " +
                         faceRecord.getFace().getFaceId());
            }


        } catch (Exception ex) {
            ex.printStackTrace();

        }




    }
    void setHorario ()
    {
        simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
        Calendar calendar = Calendar.getInstance();
        System.out.println(simpleDateFormat.format(calendar.getTime()));


        Integer Ihorario= compararHora(calendar);

        if(Ihorario == 0){
            saludo ="Buenos días ";
        }
        else if(Ihorario == 1){
            saludo ="Buenos tardes ";
        }
        else{
            saludo ="Buenos noches ";
        }

        System.out.println(simpleDateFormat.format(calendar.getTime()));
        System.out.println(tiempo);


    }

    public Integer compararHora(Calendar fecha) {


        Calendar manana = Calendar.getInstance();
        manana.set(Calendar.HOUR_OF_DAY, 12);
        manana.set(Calendar.MINUTE, 00);
        manana.set(Calendar.SECOND, 00);

        Calendar noche = Calendar.getInstance();
        noche.set(Calendar.HOUR_OF_DAY, 19);
        noche.set(Calendar.MINUTE, 00);
        noche.set(Calendar.SECOND, 00);




        if(fecha.compareTo(manana)<=0 )
        {
            tiempo=0;
        }
        else if (fecha.compareTo(manana)>0 && fecha.compareTo(noche)<0){
            tiempo=1;
        }
        else if (fecha.compareTo(noche)>=0){

            tiempo=2;
        }

        return tiempo;
    }

    void setupNewMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.release();
                setupNewMediaPlayer();
            }
        });
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.start();
                compararButton.setEnabled(true);
            }
        });
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                compararButton.setEnabled(true);
                return false;
            }
        });
    }

}