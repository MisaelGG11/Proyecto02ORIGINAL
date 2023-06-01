package com.example.couponsapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.example.couponsapp.controladores.UsuarioControl;
import com.example.couponsapp.email.Message;
import com.example.couponsapp.modelos.Usuario;
import com.example.couponsapp.vistas.CanjearCuponFragment;
import com.example.couponsapp.vistas.GestionarCuponFragment;
import com.example.couponsapp.vistas.GestionarRestauranteFragment;
import com.example.couponsapp.vistas.PDFViewerFragment;
import com.example.couponsapp.vistas.RestauranteFragment;
import com.example.couponsapp.vistas.HomeFragment;
import com.example.couponsapp.vistas.GestionarUsuarioFragment;
import com.example.couponsapp.vistas.MisCuponesFragment;
import com.example.couponsapp.vistas.QRScannerFragment;
import com.example.couponsapp.vistas.UsuarioFragment;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.example.couponsapp.email.SendEmail;
import com.example.couponsapp.email.Config;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

public class InicioActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    GoogleSignInOptions gso;
    GoogleSignInClient gsc;
    TextView nombre, email;
    ImageView foto;

    DrawerLayout drawerLayout;
    NavigationView navigationView;
    Toolbar toolbar;
    ActionBarDrawerToggle toggle;
    Bundle extras;

    UsuarioControl usuarioControl = new UsuarioControl(this);
    Usuario usuario;
    int rol_usuario, id_usuario;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inicio);

        extras = getIntent().getExtras();

        //Obtener datos generales
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        gsc = GoogleSignIn.getClient(this, gso);
        GoogleSignInAccount cuenta = GoogleSignIn.getLastSignedInAccount(this);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);
        nombre = navigationView.getHeaderView(0).findViewById(R.id.nombre_usuario);
        email = navigationView.getHeaderView(0).findViewById(R.id.email_usuario);
        foto = navigationView.getHeaderView(0).findViewById(R.id.foto_user);

        //Mostrar datos del usuario
        if (cuenta != null) {
            String nombreUser = cuenta.getDisplayName();
            String emailUser = cuenta.getEmail();
            int indexUser = cuenta.getEmail().indexOf('@');
            String userString = cuenta.getEmail().substring(0, indexUser);

            if(usuarioControl.userExist(userString, cuenta.getEmail()) == 0){
                usuario=new Usuario(
                        0, //no tiene relevancia para insert
                        3,
                        0,
                        userString,
                        "bixxortnnuis34",
                        cuenta.getEmail(),
                        cuenta.getDisplayName(),
                        "",
                        "",
                        1,
                        ""
                );
                usuarioControl.insertUsuario(usuario);

                //Enviar correo electronico de bienvenida
                String mEmail = cuenta.getEmail();
                String mSubject = "Registro";
                MimeMultipart mContent = new MimeMultipart();
                MimeBodyPart mbp1= new MimeBodyPart();
                Message msg = new Message();
                String htmlText = msg.mensaje(cuenta.getDisplayName());
                try {
                    mbp1.setContent(htmlText,"text/html");
                    mContent.addBodyPart(mbp1);
                } catch (MessagingException e) {
                    e.printStackTrace();
                }

                //Servicio web para obtener correo por defecto
                String url = "http://192.168.1.4/serviciosWeb/correo.php";
                JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url,null, new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        JSONObject object = null;
                        String correo= "", password="";
                        try {
                            object = response.getJSONObject(0);
                            correo = object.getString("EMAIL");
                            password = object.getString("PASSWORD");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        SendEmail javaMailAPI = new SendEmail(getApplicationContext(), mEmail, mSubject, mContent, correo, password);
                        javaMailAPI.execute();
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("error", error.getMessage());
                    }
                });

                RequestQueue requestQueue = Volley.newRequestQueue(this);
                requestQueue.add(request);

            }
            usuario = usuarioControl.traerUsuario(userString, "bixxortnnuis34");
            Uri userPhoto = cuenta.getPhotoUrl();
            nombre.setText(nombreUser);
            email.setText(emailUser);
            rol_usuario = usuario.getId_rol();
            id_usuario = usuario.getId_usuario();
            Glide.with(this).load(String.valueOf(userPhoto)).into(foto);
        }

        if(extras != null){
            String user = extras.getString("username");
            String password = extras.getString("password");
            usuario = usuarioControl.traerUsuario(user, password);
            nombre.setText(usuario.getNombre() + " " + usuario.getApellido());
            email.setText(usuario.getEmail());
            rol_usuario = usuario.getId_rol();
            id_usuario = usuario.getId_usuario();
            if (!usuario.getUri_foto_perfil().equals("")&& id_usuario<3){
                //recuperando la imagen de donde fue almacenada
                String directory = this.getFilesDir().toString();
                File image=new File(directory,usuario.getUri_foto_perfil());
                //guardando temporalmente la uri de el archivo.jpg
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.couponsapp.fileprovider",
                        image);
                //asginando el bitmap a el ImageView en UI
                foto.setImageURI(photoURI);
            }
        }

        //Inicializar en la opcion home
        Bundle data = new Bundle();
        data.putString("username",usuario.getUsername());
        Fragment home=new HomeFragment();
        home.setArguments(data);
        getSupportFragmentManager().beginTransaction().add(R.id.content,home ).commit();
        setTitle("Home");
        setSupportActionBar(toolbar);

        //Opciones para mostrar el icono de hamburger
        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close);
        drawerLayout.addDrawerListener(toggle);

        //Seleccion de opcion
        navigationView.setNavigationItemSelectedListener(this);

        MenuItem opcionAdmin = navigationView.getMenu().findItem(R.id.nav_gestion_usuario);
        MenuItem opcionEncargado = navigationView.getMenu().findItem(R.id.nav_gestion_cupon);
        MenuItem opcionEncargadoSanner = navigationView.getMenu().findItem(R.id.nav_scanner);
        MenuItem opcionCliente = navigationView.getMenu().findItem(R.id.nav_canjear_cupon);
        MenuItem opcionClienteListCupones = navigationView.getMenu().findItem(R.id.nav_mis_cupones);


        //Esconder opciones
        switch (rol_usuario){
            case 1:
                opcionAdmin.setVisible(true);
                opcionEncargado.setVisible(false);
                opcionEncargadoSanner.setVisible(false);
                opcionCliente.setVisible(false);
                opcionClienteListCupones.setVisible(false);
                break;
            case 2:
                opcionEncargado.setVisible(true);
                opcionEncargadoSanner.setVisible(true);
                opcionAdmin.setVisible(false);
                opcionCliente.setVisible(false);
                opcionClienteListCupones.setVisible(false);
                break;
            case 3:
                opcionAdmin.setVisible(false);
                opcionEncargado.setVisible(false);
                opcionEncargadoSanner.setVisible(false);
                opcionCliente.setVisible(true);
                opcionClienteListCupones.setVisible(true);
                break;
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        selectItemNav(item);
        return true;
    }

    @Override
    public void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        toggle.syncState();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        toggle.onConfigurationChanged(newConfig);
    }

    private void selectItemNav(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nav_gestion_cupon:
                replaceFragment(new GestionarCuponFragment());
                setTitle("Gestionar cupon");
                break;
            case R.id.nav_scanner:
                replaceFragment(new QRScannerFragment());
                setTitle("Escanear cupon");
                break;
            case R.id.nav_home:
                replaceFragment(new HomeFragment());
                setTitle("Home");
                break;
            case R.id.nav_gestion_usuario:
                replaceFragment(new GestionarUsuarioFragment());
                setTitle("Gestionar usuario");
                break;
            case R.id.nav_canjear_cupon:
                replaceFragment(new CanjearCuponFragment());
                setTitle("Cupones");
                break;
            case R.id.nav_mis_cupones:
                replaceFragment(new MisCuponesFragment());
                setTitle("Mis cupones");
                break;
            case R.id.nav_cerrar_sesion:
                cerrarSesion();
                break;
            case R.id.nav_restaurante:
                replaceFragment(new GestionarRestauranteFragment());
                setTitle("Restaurantes");
                break;
            case R.id.nav_perfil:
                replaceFragment(new UsuarioFragment());
                setTitle("Perfil");
                break;
        }
        setTitle(item.getTitle());
        drawerLayout.closeDrawers();
    }

    private void replaceFragment(Fragment fragment) {
        Bundle data = new Bundle();
        data.putInt("id_user", id_usuario);
        data.putInt("id_usuario",usuario.getId_usuario());
        fragment.setArguments(data);
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fm.beginTransaction();
        fragmentTransaction.replace(R.id.content, fragment);
        fragmentTransaction.commit();
    }

    void cerrarSesion() {
        gsc.signOut().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(Task<Void> task) {
                finish();
                Intent intent = new Intent(InicioActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (toggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}