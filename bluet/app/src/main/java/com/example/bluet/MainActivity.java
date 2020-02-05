package com.example.bluet;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    public final String TAG=getClass().getSimpleName();
    private static final int REQUEST_CODE = 1 ;
    private BluetoothAdapter bluetooth;
    private ProgressDialog mProgDial;
    private ListView listDevices;
    private BluetoothSocket mBluetoothSocket;
    private BluetoothServerSocket mBluetoothSocketServ;
    private OutputStream mOutputStream;
    private InputStream mInputStream;

    private ArrayList<BluetoothDevice> mDevices= new ArrayList<>();
    private DeviceListAdapter mDeviceListAdapter;
    Button button2;
    Button button1;
    Button button_scr;
    Button but_name_blu;
    Button Sch;
    boolean tmp=false;
    private static final int REQUEST_ENABLE_BT = 1001;
    private Uri uri;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        mDeviceListAdapter = new DeviceListAdapter(this,R.layout.device_item, mDevices);
        but_name_blu = (Button) findViewById(R.id.but_name_blu);
        button2 = (Button) findViewById(R.id.button2);
        button_scr = (Button) findViewById(R.id.button_scr);
        Sch = (Button) findViewById(R.id.search);
        askForPermission("android.permission.READ_EXTERNAL_STORAGE",120);
        askForPermission("android.permission.WRITE_EXTERNAL_STORAGE",122);
        button1 = (Button) findViewById(R.id.button1);
        Sch.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View view) {
                if(bluetooth.isEnabled())
                    seacrh_dev();
            }
        });
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){

                tmp=!tmp;
                if(!bluetooth.isEnabled()) {
                    OnBluetooth();
                    Toast.makeText(getApplicationContext(),"Checked",Toast.LENGTH_LONG).show();
                }
                v.setBackgroundColor((tmp ? Color.BLUE : Color.BLACK));
            }

        });
        button_scr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ScreenName=captureScreen();
                String path="/storage/emulated/0"+ScreenName;
                File file = new File(path);
                try {
                    go_to_file(ScreenName,path,file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        but_name_blu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Name_bluetooth();
            }
        });
        button1.setOnClickListener(new View.OnClickListener() {


            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, FILE_SELECT_CODE);

            }
        });

        bluetooth = BluetoothAdapter.getDefaultAdapter();
    }
    private static final int DISCOVER_DURATION = 300;
    private static final int REQUEST_BLU = 1;
    private static final int FILE_SELECT_CODE = 0;
    private static final String SCREEN_SHOTS_LOCATION = "/storage/emulated/0";

    private String captureScreen() {
        View v = getWindow().getDecorView().getRootView();
        v.setDrawingCacheEnabled(true);
        String ScreenName;
        Bitmap bmp = Bitmap.createBitmap(v.getDrawingCache());
        v.setDrawingCacheEnabled(false);
        try {
            String Time= String.valueOf(System.currentTimeMillis());
            FileOutputStream fos = new FileOutputStream(new File(Environment
                    .getExternalStorageDirectory().toString(), "SCREEN"
                    + Time + ".png"));
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
            ScreenName = "SCREEN" + Time + ".png";
            fos.flush();
            fos.close();
            return ScreenName;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return String.valueOf(0);

        } catch (IOException e) {
            e.printStackTrace();
            return String.valueOf(0);
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        try{
            if(mBluetoothSocket!=null){
                mBluetoothSocket.close();
            }
            if(mOutputStream!=null){
                mOutputStream.close();
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }
    public static String getRealPathFromURI_API19(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }
    private void askForPermission(String permission, Integer requestCode) {
        if (ContextCompat.checkSelfPermission(MainActivity.this, permission) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, permission)) {

                //This is called if user has denied the permission before
                //In this case I am just asking the permission again
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, requestCode);

            } else {

                ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, requestCode);
            }
        } else {
            Toast.makeText(this, "" + permission + " is already granted.", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        String path = null;
        String selectedFile = null;
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {

                    Uri uri = data.getData();
                    path = getRealPathFromURI_API19(this,uri);
                    Log.d(TAG, "File Uri: " + uri.toString());
                    // Get the path
                    Log.d(TAG, "File Path: " + path);
                    //path = getPath(this, uri);
                    // uri = data.getData();
                    File file = new File(path);
                    //path = file.getPath();

                    Log.d(TAG, "File Path: " + path);
                    String fileName = new File(path).getName();
                    Log.d(TAG, "File Name: " + fileName);
                    Intent i = new Intent();
                    i.setAction(Intent.ACTION_SEND);
                    i.setType("*/*");
                    i.putExtra(Intent.EXTRA_STREAM, uri);
                    PackageManager pm = getPackageManager();
                    List<ResolveInfo> list = pm.queryIntentActivities(i, 0);
                    if (list.size() > 0) {
                        Log.d(TAG, "BUU");
                        String packageName = null;
                        String className = null;
                        boolean found = false;

                        for (ResolveInfo info : list) {
                            packageName = info.activityInfo.packageName;
                            if (packageName.equals("com.android.bluetooth")) {
                                className = info.activityInfo.name;
                                found = true;
                                break;
                            }
                        }
                        Log.d(TAG, "BUU");
                        //CHECK BLUETOOTH available or not------------------------------------------------
                        if (!found) {
                            Log.d(TAG, "BUUBB");
                            Toast.makeText(this, "Bluetooth not been found", Toast.LENGTH_LONG).show();
                        } else
                        {
                            try {
                                go_to_file(fileName,path,file);
                            } catch (IOException e) {
                                //e.printStackTrace();
                            }
                        }


                    }
                } else {
                    Toast.makeText(this, "Bluetooth is cancelled", Toast.LENGTH_LONG).show();
                }

                break;
        }
    }
    public void go_to_file(String fileName,String path,File file) throws IOException {
        //File fileTo = new File("/storage/emulated/0/test.jpg");
        int lenFile=(int)fileName.length();
        if(mOutputStream!=null){
            String buf="hello";
            byte[] buffer=fileName.getBytes();
            FileInputStream fileInputStream = new FileInputStream(file);
            Log.d("SIZE", String.valueOf(fileInputStream.available()));
            long k=fileInputStream.available();
            int tmp;
            byte[] LenByte = new byte[8];
            ByteBuffer.wrap(LenByte).putLong(k);
            Log.d("BUUUL", String.valueOf(LenByte));
            fileInputStream.close();
            mOutputStream.write(LenByte);
            FileInputStream fileInputStream2 = new FileInputStream(file);
            //byte[] bytesBuff = new byte[fileInputStream2.available()];
            byte[] bytesBuff = new byte[512];
            int ind = fileInputStream2.read(bytesBuff);
            Log.d("BUUUL", "GOL");
            int KeysFlag =0;
            while(KeysFlag<k){
                mOutputStream.write(bytesBuff);
                bytesBuff = new byte[989];
                ind = fileInputStream2.read(bytesBuff);
                KeysFlag+=989;
            }
            Log.d("BUUUL", bytesBuff.toString());
            //mOutputStream.write(bytesBuff);
            fileInputStream2.close();
            Log.d("BUUUL","flush");
            mOutputStream.flush();
            byte[] buffers = new byte[8];
            Log.d("BUUUL","reads");
            mInputStream.read(buffers);

            Log.d("BUUUL", "GO to read");
            Log.d("URI", buffers.toString());
            ByteBuffer wrapped = ByteBuffer.wrap(buffers); // big-endian by default
            long num  = wrapped.getLong();
            //long num = 259048;
            //File fileScr = new File("storage/emulated/0", "screen.jpg");
            Log.d("URI","GO TO SCREEN");
            // FileOutputStream fOut = openFileOutput("/storage/emulated/0/screen.jpg", MODE_WORLD_READABLE);
            Log.d("File","value = "+num);
            byte[] fileBytes = new byte[989];
            Log.d("File", "value = " + (int) num);
            long iBYTE=0;
            long g = 0;
            String OkFl="ok";
            BufferedOutputStream fileScreen = new BufferedOutputStream(new FileOutputStream("/storage/emulated/0/VK/Screen.jpg"));
            while(g<num) {
                g += mInputStream.read(fileBytes);
                Log.d("File", "value = " + (int) g);
                fileScreen.write(fileBytes);
            }
            fileScreen.close();
            mOutputStream.flush();
//Р’ РџР РРќРЇРўРР Р РћРўРџР РђР’РљР• РќР• РҐР’РђРўРђР•Рў Р‘РђР™РўРћР’.....
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void seacrh_dev() {
        Log.d(TAG,"searchDevices()");
        CheckPermissionLocation();

        if(!bluetooth.isDiscovering()){
            Log.d(TAG,"searchDevices(): РїРѕРёСЃРє РЅР°С‡Р°Р»Рѕ");
            bluetooth.startDiscovery();
        }
        if(bluetooth.isDiscovering()){
            Log.d(TAG,"searchDevices(): РїРѕРёСЃРє РїРµСЂРµР·Р°РїСѓСЃРє");
            bluetooth.cancelDiscovery();
            bluetooth.startDiscovery();
        }
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);//РєРѕРіРґР° С‚РѕР»СЊРєРѕ Р·Р°РїСѓСЃРєР°РµС‚СЃСЏ РїРѕРёСЃРє СѓСЃС‚СЂ-РІР°
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);//РєРѕРіРґР° РїРѕРёСЃРє РїСЂРµРєСЂР°С‰РµРЅ
        filter.addAction(BluetoothDevice.ACTION_FOUND);//РїСЂРё РґРѕР±Р°РІР»РµРЅРёРё РЅРѕРІРѕРіРѕ СѓСЃС‚СЂ-РІР°
        registerReceiver(mRecevier,filter);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void CheckPermissionLocation() {
        if(Build.VERSION.SDK_INT>Build.VERSION_CODES.LOLLIPOP){
            int check=checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            check+=checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            check+=checkSelfPermission("Manifest.permission.READ_EXTERNAL_STORAGE");
            check+=checkSelfPermission("Manifest.permission.WRITE_EXTERNAL_STORAGE");
            if(check!=0){
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},1002);
            }
        }
    }

    private AdapterView.OnItemClickListener onItemClickListener= new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
            BluetoothDevice device =mDevices.get(position);
            StartConnection(device);
        }
    };
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private void StartConnection(BluetoothDevice device) {
        if(device!=null){
            try{
                //Method method = device.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
                //mBluetoothSocket = (BluetoothSocket) method.invoke(device,1);
                mBluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                mBluetoothSocket.connect();
                mOutputStream = mBluetoothSocket.getOutputStream();
                mInputStream = mBluetoothSocket.getInputStream();
                showMesToast("РџРѕРґРєР»СЋС‡РµРЅРёРµ СѓСЃРїРµС€РЅРѕ!");
            } catch (Exception e) {
                showMesToast("РћС€РёР±РєР°!");
                e.printStackTrace();
            }

        }
    }
    private void ShowListDevices(){
        Log.d(TAG,"showListDiv");
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setTitle("РќР°Р№РґРµРЅРЅС‹Рµ СѓСЃС‚СЂРѕР№СЃС‚РІР°");

        View view=getLayoutInflater().inflate(R.layout.list_devices_view,null);
        listDevices=view.findViewById(R.id.list_devices);

        listDevices.setAdapter(mDeviceListAdapter);
        listDevices.setOnItemClickListener(onItemClickListener);
        builder.setView(view);
        builder.setNegativeButton("ok",null);
        builder.create();
        builder.show();

    }
    protected void Name_bluetooth() {
        String status;
        if (bluetooth.isEnabled()) {
            String mydeviceaddress = bluetooth.getAddress();
            String mydevicename = bluetooth.getName();
            status = mydevicename + " : " + mydeviceaddress;

        } else {
            status = "Bluetooth РІС‹РєР»СЋС‡РµРЅ";
        }
        Toast.makeText(this,status,Toast.LENGTH_LONG).show();
    }
    protected void OnBluetooth () {
        // Bluetooth РІС‹РєР»СЋС‡РµРЅ. РџСЂРµРґР»РѕР¶РёРј РїРѕР»СЊР·РѕРІР°С‚РµР»СЋ РІРєР»СЋС‡РёС‚СЊ РµРіРѕ.
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        String status;
        if (bluetooth.isEnabled()) {
            String mydeviceaddress = bluetooth.getAddress();
            String mydevicename = bluetooth.getName();
            status = mydevicename + " : " + mydeviceaddress;
        } else {
            status = "Bluetooth РІС‹РєР»СЋС‡РµРЅ";
        }
        Toast.makeText(this, status, Toast.LENGTH_LONG).show();
    }
    private void showMesToast(String message){
        Toast.makeText(this,message,Toast.LENGTH_SHORT).show();
    }

    private BroadcastReceiver mRecevier= new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action=intent.getAction();
            if(action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)){
                Log.d(TAG,"onReceive: ACTION_DISCOVERY_STARTED");
                showMesToast("РќР°С‡Р°С‚ РїРѕРёСЃРє СѓСЃС‚СЂ-РІ");

                mProgDial=ProgressDialog.show(MainActivity.this,"РџРѕРёСЃРє СѓСЃС‚СЂРѕР№СЃС‚РІ", "РћР¶РёРґР°Р№С‚Рµ");
            }
            if(action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)){
                Log.d(TAG,"onReceive: ACTION_DISCOVERY_FINISHED");
                showMesToast("РџРѕРёСЃРє СѓСЃС‚СЂ-РІ Р·Р°РІРµСЂС€РµРЅ");
                mProgDial.dismiss();

                ShowListDevices();
            }
            if(action.equals(BluetoothDevice.ACTION_FOUND)){
                Log.d(TAG,"onReceive: ACTION_FOUND");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(device!=null){
                    if(!mDevices.contains(device)){
                        Log.d(TAG,"onReceive: nop");
                        mDeviceListAdapter.add(device);
                    }
                }

            }
        }
    };


}