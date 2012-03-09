package com.emuneee.camerafem;

import java.io.IOException;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.Face;
import android.hardware.Camera.FaceDetectionListener;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ToggleButton;

public class CameraFemActivity extends Activity implements FaceDetectionListener, SurfaceHolder.Callback {
	private SurfaceView surfaceView;
	private SurfaceHolder surfaceHolder;
	private Camera camera;
	
	private OverlayView overlayView;
    private SurfaceHolder overlayHolder;
    Bitmap mDrawing;
    Canvas tempCanvas = new Canvas();
    Paint paint;
    Face face;
	private int intCanvasWidth;
	private int intCanvasHeight;
	private boolean mRun;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // setup the overlay view
        overlayView = (OverlayView)this.findViewById(R.id.overlayViewCameraFem);
        overlayHolder = overlayView.getHolder();
        overlayHolder.setFormat(PixelFormat.TRANSLUCENT); 
        overlayHolder.addCallback(this);
        
        surfaceView = (SurfaceView) findViewById(R.id.surfaceViewCameraFem);
        // get the surface holder object, will use this for our camera preview
        surfaceHolder = surfaceView.getHolder();
        
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        
        ((ToggleButton) findViewById(R.id.toggleButtonCamera)).setOnCheckedChangeListener(
        		new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if(isChecked) {
					startCamera();
				} else {
					stopCamera();
				}
			}
        });
    }
    
    private void stopCamera() {
    	camera.stopFaceDetection();
		camera.stopPreview();
		camera.release();
	}
    
    public void startCamera() {    	
    	camera = Camera.open(1);
    	camera.setDisplayOrientation(90);
    	try {
			camera.setPreviewDisplay(surfaceHolder);
			camera.startPreview();
			camera.startFaceDetection();
			camera.setFaceDetectionListener(this);
		} catch (IOException e) {
			Log.w(getClass().getSimpleName(), e.getMessage());
		}
    }
    
    @Override
	public void onFaceDetection(Face[] faces, Camera camera) {
		if(faces.length > 0) {
			Log.v(getClass().getSimpleName(), "Face detected!");
			face = faces[0];
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
        intCanvasWidth = width;
        intCanvasHeight = height;
        mDrawing = Bitmap.createBitmap(intCanvasWidth, intCanvasHeight,
                Bitmap.Config.ARGB_8888);
        paint = new Paint();
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (thread.getState() == Thread.State.TERMINATED) {
            thread = new Thread();
        }
        mRun = true;
        thread.start();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		boolean retry = true;
        mRun = false;
        while (retry) {
            try {
                thread.join();
                retry = false;
            } catch (InterruptedException e) {
                // we will try it again and again...
            }
        }
	}
	
	 Thread thread = new Thread() {
	        public void doDraw(Canvas c) {
	        	
	        	if(face != null && face.rect != null) {
	        		Matrix matrix = new Matrix();
	        		
	        		 // Need mirror for front camera.
	        		 boolean mirror = true;
	        		 matrix.setScale(mirror ? -1 : 1, 1);
	        		 // This is the value for android.hardware.Camera.setDisplayOrientation.
	        		 matrix.postRotate(90);
	        		 // Camera driver coordinates range from (-1000, -1000) to (1000, 1000).
	        		 // UI coordinates range from (0, 0) to (width, height).
	        		 matrix.postScale(c.getWidth() / 2000f, c.getHeight() / 2000f);
	        		 matrix.postTranslate(c.getWidth() / 2f, c.getHeight() / 2f);
		            tempCanvas.setBitmap(mDrawing);
		            paint.setColor(Color.rgb(255, 255,255));
		            //tempCanvas.(face.rightEye.x, face.rightEye.x, 10, paint);
		    
		            tempCanvas.drawRect(face.rect, paint);
		            c.drawBitmap(mDrawing, 0, 0, null);
	        	}
	        }

	        @Override
	        public void run() {
	            Canvas c;
	            while (mRun) {
	                c = null;
	                try {
	                    c = overlayHolder.lockCanvas(null);
	                    synchronized (overlayHolder) {
	                        doDraw(c);
	                    }
	                } finally {
	                    // do this in a finally so that if an exception is thrown
	                    // during the above, we don't leave the Surface in an
	                    // inconsistent state
	                    if (c != null) {
	                    	overlayHolder.unlockCanvasAndPost(c);
	                    }
	                }
	            }
	        }
	    };
}