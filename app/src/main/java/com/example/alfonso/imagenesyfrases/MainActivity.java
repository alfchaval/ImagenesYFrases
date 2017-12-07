package com.example.alfonso.imagenesyfrases;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.icu.util.Calendar;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;
import com.squareup.picasso.Picasso;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Date;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity {

    private final static String ERRORES = "http://alumno.mobi/~alumno/superior/chamorro/errores.php";
    private final char IMAGEN = 'i';
    private final char FRASE = 'f';

    private EditText edt_imagenes, edt_frases;
    private Button btn_descargar;
    private ImageView imv_res;
    private TextView txv_res;

    private int indice_imagenes = -1;
    private int indice_frases = -1;
    private int intervalo;

    private String[] galeria_imagenes;
    private String[] galeria_frases;

    private CountDownTimer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        edt_imagenes = (EditText) findViewById(R.id.edt_imagenes);
        edt_frases = (EditText) findViewById(R.id.edt_frases);
        btn_descargar = (Button) findViewById(R.id.btn_descargar);
        imv_res = (ImageView) findViewById(R.id.imv_res);
        txv_res = (TextView) findViewById(R.id.txv_res);

        leerIntervalo();

        btn_descargar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isNetworkAvailable()) {
                    if (!edt_imagenes.getText().toString().isEmpty()) conexionAsincrona(edt_imagenes.getText().toString(), IMAGEN);
                    else Toast.makeText(MainActivity.this, "Falta la ruta a las imágenes", Toast.LENGTH_LONG).show();

                    if (!edt_frases.getText().toString().isEmpty()) conexionAsincrona(edt_frases.getText().toString(), FRASE);
                    else Toast.makeText(MainActivity.this, "Falta la ruta a las frases", Toast.LENGTH_LONG).show();

                    if(timer == null) crearContador();
                }
                else Toast.makeText(MainActivity.this, "No hay conexión a internet", Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    private void leerIntervalo(){
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(getResources().openRawResource(R.raw.intervalo)));
            String line = reader.readLine();
            intervalo = Integer.parseInt(line)*1000;
            reader.close();
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "Error al leer el intervalo, usando valor por defecto", Toast.LENGTH_SHORT).show();
            intervalo = 5000;
        }
    }

    private void conexionAsincrona(String url, final char dato) {
        AsyncHttpClient client = new AsyncHttpClient();
        final ProgressDialog progress = new ProgressDialog(MainActivity.this);
        client.get(url, new TextHttpResponseHandler() {

            @Override
            public void onStart() {
                progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progress.setMessage("Conectando...");
                progress.setCancelable(false);
                progress.show();
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                progress.dismiss();
                if (dato == IMAGEN) {
                    galeria_imagenes = responseString.split("\n");
                    indice_imagenes = -1;
                    cargarImagenes();

                } else if (dato == FRASE) {
                    galeria_frases = responseString.split("\n");
                    indice_frases = -1;
                    cargarFrases();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                progress.dismiss();
                if (dato == IMAGEN) {
                    galeria_imagenes = null;
                    imv_res.setImageResource(R.mipmap.ic_launcher);
                    notificarError("Error descargando imágenes");
                    Toast.makeText(MainActivity.this, "Error descargando imágenes", Toast.LENGTH_LONG).show();
                }
                else if (dato == FRASE) {
                    galeria_frases = null;
                    txv_res.setText("");
                    notificarError("Error descargando frases");
                    Toast.makeText(MainActivity.this, "Error descargando frases", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void crearContador() {
        timer = new CountDownTimer(intervalo, 1000) {
            @Override
            public void onTick(long l) {}

            @Override
            public void onFinish() {
                cargarImagenes();
                cargarFrases();
                start();
            }
        }.start();
    }

    private void cargarImagenes() {
        if (galeria_imagenes != null && galeria_imagenes.length > 0) {
            indice_imagenes = (indice_imagenes + 1) % galeria_imagenes.length;
            try {
                Picasso.with(MainActivity.this).load(galeria_imagenes[indice_imagenes]).error(R.mipmap.ic_launcher).into(imv_res);
            } catch (Exception e) {
                notificarError("Error al cargar la imagen " + (indice_imagenes + 1) + ", " + e.getMessage());
            }
        }
    }

    private void cargarFrases() {
        if (galeria_frases != null && galeria_frases.length > 0) {
            indice_frases = (indice_frases + 1) % galeria_frases.length;
            try {
                txv_res.setText(galeria_frases[indice_frases]);
            } catch (Exception e) {
                notificarError("Error al cargar la frase " + (indice_frases + 1) + ", " + e.getMessage());
            }
        }
    }

    private boolean notificarError(String error){
        Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
        final ProgressDialog progress = new ProgressDialog(this);
        RequestParams params = new RequestParams();
        params.put("error", new Date(System.currentTimeMillis()) + ", " + error);
        RestClient.post(ERRORES, params, new TextHttpResponseHandler() {
            @Override
            public void onStart() {
                super.onStart();
                progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progress.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        RestClient.cancelRequests(getApplicationContext(), true);
                    }
                });
                progress.show();
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                progress.dismiss();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                progress.dismiss();
            }
        });
        return true;
    }
}
