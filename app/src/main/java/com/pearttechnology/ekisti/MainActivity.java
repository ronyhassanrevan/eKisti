package com.pearttechnology.ekisti;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.Manifest;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Environment;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PERMISSIONS = 101;
    private static final int REQUEST_CODE_FILE_PICKER = 102;
    private static final int REQUEST_CODE_CAMERA = 103;
    private WebView webView;
    private ValueCallback<Uri[]> mUploadMessage;
    private Uri mCameraImageUri;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Request necessary permissions at runtime
        requestPermissions();

        webView = findViewById(R.id.webview);

        // Enable JavaScript and other settings
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setAllowContentAccess(true);
        webView.getSettings().setAllowFileAccess(true);

        // Enable mixed content handling if needed
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            webView.getSettings().setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        // Enable zoom controls
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false); // Hides the zoom controls on the screen

        // Enable pinch-to-zoom
        webView.getSettings().setSupportZoom(true);
        // Set WebViewClient to handle page loading and SSL errors
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                Log.d("WebView", "Loading URL: " + url);
                //Toast.makeText(MainActivity.this, "Loading...", Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d("WebView", "Finished loading URL: " + url);
                //Toast.makeText(MainActivity.this, "Finished loading", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, android.webkit.WebResourceError error) {
                super.onReceivedError(view, request, error);
                Log.e("WebView", "Error loading URL: " + error.toString());
                //Toast.makeText(MainActivity.this, "Error loading page", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                // Ignore SSL certificate errors for debugging purposes
                handler.proceed();
                Log.e("WebView", "SSL Error: " + error.toString());
                //Toast.makeText(MainActivity.this, "SSL Error", Toast.LENGTH_SHORT).show();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                view.loadUrl(request.getUrl().toString());
                return false;
            }
        });

        // Set WebChromeClient to handle JavaScript dialogs, favicons, titles, and progress
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (mUploadMessage != null) {
                    mUploadMessage.onReceiveValue(null);
                }
                mUploadMessage = filePathCallback;
                showFileChooser();
                return true;
            }
        });

        // Add JavaScript interface for print
        webView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void print() {
                runOnUiThread(() -> createWebPrintJob(webView));
            }
        }, "AndroidPrint");

        // Load URL
        webView.loadUrl("https://demo.ekisti.com/");
    }


    private void createWebPrintJob(WebView webView) {
        // Get the print manager
        PrintManager printManager = (PrintManager) this.getSystemService(PRINT_SERVICE);

        // Create a print adapter instance
        PrintDocumentAdapter printAdapter = webView.createPrintDocumentAdapter("MyDocument");

        // Print the document with a job name
        String jobName = getString(R.string.app_name) + " Document";
        printManager.print(jobName, printAdapter, null);
    }

    private void requestPermissions() {
        String[] permissions = {
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        boolean allPermissionsGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }

        if (!allPermissionsGranted) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            for (int result : grantResults) {
                if (result != PERMISSION_GRANTED) {
                    // Handle permissions not granted
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == REQUEST_CODE_FILE_PICKER || requestCode == REQUEST_CODE_CAMERA) && resultCode == RESULT_OK) {
            if (mUploadMessage != null) {
                Uri[] results = null;

                // from documents (and video camera)
                if (data != null && data.getData() != null) {
                    results = new Uri[]{data.getData()};
                }

                // we may get clip data for multi-select documents
                else if (data != null && data.getClipData() != null) {
                    ClipData clipData = data.getClipData();
                    ArrayList<Uri> files = new ArrayList<>(clipData.getItemCount());
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        ClipData.Item item = clipData.getItemAt(i);
                        if (item.getUri() != null) {
                            files.add(item.getUri());
                        }
                    }
                    results = files.toArray(new Uri[0]);
                }

                // from camera
                else if (mCameraImageUri != null) {
                    results = new Uri[]{mCameraImageUri};
                }

                mUploadMessage.onReceiveValue(results);
                mUploadMessage = null;
            }
        } else {
            if (mUploadMessage != null) {
                mUploadMessage.onReceiveValue(null);
                mUploadMessage = null;
            }
        }
    }

    private void showFileChooser() {
        // Create an intent to pick a file
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");

        // Create an intent to capture a photo
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the file
                Toast.makeText(this, "Error while creating file", Toast.LENGTH_SHORT).show();
            }
            if (photoFile != null) {
                mCameraImageUri = FileProvider.getUriForFile(this,
                        "com.pearttechnology.ekisti.provider",
                        photoFile);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCameraImageUri);
            }
        }

        // Create a chooser with both intents
        Intent chooser = Intent.createChooser(intent, "Select or capture an image");
        Intent[] intentArray = {cameraIntent};
        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
        startActivityForResult(chooser, REQUEST_CODE_FILE_PICKER);
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
