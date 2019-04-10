package tl.cordova.google.mobile.vision.scanner;

// ----------------------------------------------------------------------------
// |  Android Imports
// ----------------------------------------------------------------------------
import android.content.Context;

// ----------------------------------------------------------------------------
// |  Google Imports
// ----------------------------------------------------------------------------
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.barcode.Barcode;

// ----------------------------------------------------------------------------
// |  Our Imports
// ----------------------------------------------------------------------------
import tl.cordova.google.mobile.vision.scanner.ui.camera.GraphicOverlay;

class BarcodeTrackerFactory implements MultiProcessor.Factory<Barcode> {
  // ----------------------------------------------------------------------------
  // | Public Properties
  // ----------------------------------------------------------------------------

  // ----------------------------------------------------------------------------
  // | Private Properties
  // ----------------------------------------------------------------------------
  private GraphicOverlay<BarcodeGraphic> _GraphicOverlay;
  private Context                        _Context       ;

  public BarcodeTrackerFactory(GraphicOverlay<BarcodeGraphic> p_GraphicOverlay, Context p_Context) {
    this._GraphicOverlay = p_GraphicOverlay;
    this._Context        = p_Context       ;
  }

  // ----------------------------------------------------------------------------
  // |  Public Functions
  // ----------------------------------------------------------------------------  
  @Override
  public Tracker<Barcode> create(Barcode p_Barcode) {
    BarcodeGraphic graphic = new BarcodeGraphic(_GraphicOverlay);

    return new BarcodeGraphicTracker(_GraphicOverlay, graphic, _Context);
  }

  // ----------------------------------------------------------------------------
  // |  Protected Functions
  // ----------------------------------------------------------------------------

  // ----------------------------------------------------------------------------
  // |  Private Functions
  // ----------------------------------------------------------------------------
}
