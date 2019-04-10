package tl.cordova.google.mobile.vision.scanner;

// ----------------------------------------------------------------------------
// |  Android Imports
// ----------------------------------------------------------------------------
import android.content.Context;
import android.support.annotation.UiThread;

// ----------------------------------------------------------------------------
// |  Google Imports
// ----------------------------------------------------------------------------
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.barcode.Barcode;

// ----------------------------------------------------------------------------
// |  Java Imports
// ----------------------------------------------------------------------------

// ----------------------------------------------------------------------------
// |  Our Imports
// ----------------------------------------------------------------------------
import tl.cordova.google.mobile.vision.scanner.ui.camera.GraphicOverlay;

public class BarcodeGraphicTracker extends Tracker<Barcode> {
  // ----------------------------------------------------------------------------
  // |  Public Properties
  // ----------------------------------------------------------------------------

  // ----------------------------------------------------------------------------
  // |  Private Properties
  // ----------------------------------------------------------------------------  
  private GraphicOverlay<BarcodeGraphic> _Overlay              ;
  private BarcodeGraphic                 _Graphic              ;
  private BarcodeUpdateListener          _BarcodeUpdateListener;

  BarcodeGraphicTracker(GraphicOverlay<BarcodeGraphic> p_Overlay, BarcodeGraphic p_Graphic, Context p_Context) {
    this._Overlay = p_Overlay;
    this._Graphic = p_Graphic;
    if (p_Context instanceof BarcodeUpdateListener) {
      this._BarcodeUpdateListener = (BarcodeUpdateListener) p_Context;
    } else {
      throw new RuntimeException("Hosting activity must implement BarcodeUpdateListener");
    }
  }

  // ----------------------------------------------------------------------------
  // |  Public Functions
  // ----------------------------------------------------------------------------
  @Override
  public void onNewItem(int p_Id, Barcode p_Item) {
    _BarcodeUpdateListener.onBarcodeDetected(p_Item);
  }

  @Override
  public void onUpdate(Detector.Detections<Barcode> p_DetectionResults, Barcode p_Item) {
  }

  @Override
  public void onMissing(Detector.Detections<Barcode> p_DetectionResults) {
  }

  @Override
  public void onDone() {
  }

  // ----------------------------------------------------------------------------
  // |  Protected Functions
  // ----------------------------------------------------------------------------

  // ----------------------------------------------------------------------------
  // |  Private Functions
  // ----------------------------------------------------------------------------

  // ----------------------------------------------------------------------------
  // |  Helper classes
  // ----------------------------------------------------------------------------
  public interface BarcodeUpdateListener {
    @UiThread
    void onBarcodeDetected(Barcode p_Barcode);
  }
}
