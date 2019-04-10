package tl.cordova.google.mobile.vision.scanner;

// ----------------------------------------------------------------------------
// |  Android Imports
// ----------------------------------------------------------------------------
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.view.WindowManager;
import android.view.Display;
import android.graphics.Point;

// ----------------------------------------------------------------------------
// |  Google Imports
// ----------------------------------------------------------------------------
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.android.gms.common.images.Size;

// ----------------------------------------------------------------------------
// |  Java Imports
// ----------------------------------------------------------------------------
import java.io.IOException;

// ----------------------------------------------------------------------------
// |  Our Imports
// ----------------------------------------------------------------------------
import tl.cordova.google.mobile.vision.scanner.ui.camera.CameraSource2;
import tl.cordova.google.mobile.vision.scanner.ui.camera.CameraSourcePreview;
import tl.cordova.google.mobile.vision.scanner.ui.camera.GraphicOverlay;

public final class BarcodeCaptureActivity extends    AppCompatActivity
                                          implements BarcodeGraphicTracker.BarcodeUpdateListener {
  // ----------------------------------------------------------------------------
  // |  Public Properties
  // ----------------------------------------------------------------------------
  public              Integer DetectionTypes              ;
  public              double  ViewFinderWidth  = .5       ;
  public              double  ViewFinderHeight = .7       ;
  public static final String  BarcodeObject    = "Barcode";

  // ----------------------------------------------------------------------------
  // |  Private Properties
  // ----------------------------------------------------------------------------  
  private static final String TAG                   = "Barcode-reader";
  private static final int    RC_HANDLE_GMS         = 9001            ;
  private static final int    RC_HANDLE_CAMERA_PERM = 2               ;

  private CameraSource2                  _CameraSource        ;
  private CameraSourcePreview            _Preview             ;
  private GraphicOverlay<BarcodeGraphic> _GraphicOverlay      ;
  private ScaleGestureDetector           _ScaleGestureDetector;
  private GestureDetector                _GestureDetector     ;

  // ----------------------------------------------------------------------------
  // |  Public Functions
  // ----------------------------------------------------------------------------
  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);

    // Hide the status bar and action bar.
    View decorView = getWindow().getDecorView();
    int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
    decorView.setSystemUiVisibility(uiOptions);

    // Remember that you should never show the action bar if the
    // status bar is hidden, so hide that too if necessary.
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    if (getActionBar() != null) {
      getActionBar().hide();
    }

    if (getSupportActionBar() != null) {
      getSupportActionBar().hide();
    }

    setContentView(getResources().getIdentifier("barcode_capture", "layout", getPackageName()));

    _Preview = (CameraSourcePreview) findViewById(getResources().getIdentifier("preview", "id", getPackageName()));
    _Preview.ViewFinderWidth = ViewFinderWidth;
    _Preview.ViewFinderHeight = ViewFinderHeight;
    _GraphicOverlay = (GraphicOverlay<BarcodeGraphic>) findViewById(getResources().getIdentifier("graphicOverlay", "id", getPackageName()));

    // read parameters from the intent used to launch the activity.
    DetectionTypes = getIntent().getIntExtra("DetectionTypes", 1234);
    ViewFinderWidth = getIntent().getDoubleExtra("ViewFinderWidth", .5);
    ViewFinderHeight = getIntent().getDoubleExtra("ViewFinderHeight", .7);

    int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
    if (rc == PackageManager.PERMISSION_GRANTED) {
      createCameraSource(true, false);
    } else {
      requestCameraPermission();
    }

    _GestureDetector = new GestureDetector(this, new CaptureGestureListener());
    _ScaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());
  }


  @Override
  public boolean onTouchEvent(MotionEvent e) {
    boolean b = _ScaleGestureDetector.onTouchEvent(e);
    boolean c = _GestureDetector.onTouchEvent(e);

    return b || c || super.onTouchEvent(e);
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (requestCode != RC_HANDLE_CAMERA_PERM) {
      Log.d(TAG, "Got unexpected permission result: " + requestCode);
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);
      return;
    }

    if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      Log.d(TAG, "Camera permission granted - initialize the camera source");
      // we have permission, so create the camerasource
      DetectionTypes = getIntent().getIntExtra("DetectionTypes", 0);
      ViewFinderWidth = getIntent().getDoubleExtra("ViewFinderWidth", .5);
      ViewFinderHeight = getIntent().getDoubleExtra("ViewFinderHeight", .7);

      createCameraSource(true, false);
      return;
    }

    Log.e(TAG, "Permission not granted: results len = " + grantResults.length + " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

    DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        finish();
      }
    };

    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle("Camera permission required")
        .setMessage(getResources().getIdentifier("no_camera_permission", "string", getPackageName()))
        .setPositiveButton(getResources().getIdentifier("ok", "string", getPackageName()), listener).show();
  }

  @Override
  public void onBarcodeDetected(Barcode barcode) {
    // do something with barcode data returned

    Intent data = new Intent();
    data.putExtra(BarcodeObject, barcode);
    setResult(CommonStatusCodes.SUCCESS, data);
    finish();
  }

  // ----------------------------------------------------------------------------
  // |  Protected Functions
  // ----------------------------------------------------------------------------
  @Override
  protected void onResume() {
    super.onResume();
    startCameraSource();
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (_Preview != null) {
      _Preview.stop();
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (_Preview != null) {
      _Preview.release();
    }
  }

  // ----------------------------------------------------------------------------
  // |  Private Functions
  // ----------------------------------------------------------------------------
  @SuppressLint("InlinedApi")
  private void createCameraSource(boolean autoFocus, boolean useFlash) {
    Context context = getApplicationContext();

    int detectionType = 0;

    if (DetectionTypes == 0 || DetectionTypes == 1234) {
      detectionType = (Barcode.CODE_39 | Barcode.DATA_MATRIX);
    } else {
      detectionType = DetectionTypes;
    }

    BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(context).setBarcodeFormats(detectionType).build();
    BarcodeTrackerFactory barcodeFactory = new BarcodeTrackerFactory(_GraphicOverlay, this);
  
    barcodeDetector.setProcessor(new MultiProcessor.Builder<>(barcodeFactory).build());

    if (!barcodeDetector.isOperational()) {
      Log.w(TAG, "Detector dependencies are not yet available.");
  
      IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
      boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;

      if (hasLowStorage) {
        Toast.makeText(this, getResources().getIdentifier("low_storage_error", "string", getPackageName()),
            Toast.LENGTH_LONG).show();
        Log.w(TAG, getString(getResources().getIdentifier("low_storage_error", "string", getPackageName())));
      }
    }

    CameraSource2.Builder builder = new CameraSource2.Builder(getApplicationContext(), barcodeDetector)
        .setFacing(CameraSource2.CAMERA_FACING_BACK)
        .setRequestedPreviewSize(1600, 1024)
        .setRequestedFps(15.0f);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
      builder = builder.setFocusMode(autoFocus ? Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE : null);
    }

    _CameraSource = builder.setFlashMode(useFlash ? Camera.Parameters.FLASH_MODE_TORCH : null).build();
  }

  private void startCameraSource() throws SecurityException {
    int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getApplicationContext());
    if (code != ConnectionResult.SUCCESS) {
      Dialog dlg = GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
      dlg.show();
    }

    if (_CameraSource != null) {
      try {
        _Preview.start(_CameraSource, _GraphicOverlay);
      } catch (IOException e) {
        Log.e(TAG, "Unable to start camera source.", e);
        _CameraSource.release();
        _CameraSource = null;
      }
    }
  }

  private void requestCameraPermission() {
    Log.w(TAG, "Camera permission is not granted. Requesting permission");

    final String[] permissions = new String[] { Manifest.permission.CAMERA };

    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
      ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
      return;
    }

    final Activity thisActivity = this;

    View.OnClickListener listener = new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        ActivityCompat.requestPermissions(thisActivity, permissions, RC_HANDLE_CAMERA_PERM);
      }
    };

    findViewById(getResources().getIdentifier("topLayout", "id", getPackageName())).setOnClickListener(listener);
    Snackbar
        .make(_GraphicOverlay, getResources().getIdentifier("permission_camera_rationale", "string", getPackageName()),
            Snackbar.LENGTH_INDEFINITE)
        .setAction(getResources().getIdentifier("ok", "string", getPackageName()), listener).show();
  }

  // ----------------------------------------------------------------------------
  // |  Helper classes
  // ----------------------------------------------------------------------------
  private class CaptureGestureListener extends GestureDetector.SimpleOnGestureListener {
    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
      return super.onSingleTapConfirmed(e);
    }
  }

  private class ScaleListener implements ScaleGestureDetector.OnScaleGestureListener {
    @Override
    public boolean onScale(ScaleGestureDetector detector) {
      return false;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
      return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
      _CameraSource.doZoom(detector.getScaleFactor());
    }
  }
}
