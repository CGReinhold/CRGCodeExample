package com.example.cgrcodeexample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.WindowManager;

import org.opencv.android.JavaCameraView;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

// OpenCV Classes

public class MainActivity extends AppCompatActivity implements CvCameraViewListener2 {

    // Used for logging success or failure messages
    private static final String TAG = "OCVSample::Activity";

    // Loads camera view of OpenCV for us to use. This lets us see using OpenCV
    private CameraBridgeViewBase mOpenCvCameraView;

    // Used in Camera selection from menu (when implemented)
    private boolean mIsJavaCamera = true;
    private MenuItem mItemSwitchCamera = null;

    // These variables are used (at the moment) to fix camera orientation from 270degree to 0degree
    Mat mRgba;
    Mat mGray;
    Mat mFinal;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.show_camera);

        mOpenCvCameraView = (JavaCameraView) findViewById(R.id.show_camera_activity_java_surface_view);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);

        mOpenCvCameraView.setMaxFrameSize(1000, 800);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8UC4);
    }

    public void onCameraViewStopped() {
        mGray.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        Rect boundBox = Imgproc.boundingRect(mGray);
        int centerImageX = (boundBox.width + boundBox.x) / 2;
        int centerImageY = (boundBox.height + boundBox.y) / 2;

        Imgproc.GaussianBlur(mGray, mGray, new Size(5, 5), 0);
        Imgproc.threshold(mGray, mGray, 0, 255, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU);
        Imgproc.dilate(mGray, mGray, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(11, 11)), new Point(-1, -1), 5);
        Imgproc.morphologyEx(mGray, mGray, Imgproc.MORPH_CLOSE, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(25, 25)));

        ArrayList<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(mGray, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        ArrayList<MatOfInt> hulls = new ArrayList<>();
        List<MatOfPoint> hullPoints = new ArrayList<>();
        for(int idx = 0; idx >= 0; idx = (int) hierarchy.get(0, idx)[0]) {
            MatOfInt hull = new MatOfInt();
            Imgproc.convexHull(contours.get(idx), hull);
            hulls.add(hull);
            Rect boundBoxContour = Imgproc.boundingRect(contours.get(idx));
            int centerX = (boundBoxContour.width + boundBoxContour.x) / 2;
            int centerY = (boundBoxContour.height + boundBoxContour.y) / 2;

            if (idx != 0 && centerX > centerImageX - 220 && centerX < centerImageX + 220 &&
                    centerY > centerImageY - 120 && centerY < centerImageY + 120) {
                hullPoints.add(hull2Points(hull, contours.get(idx)));
            }
        }

//        mFinal = Mat.zeros(mGray.size(), 0);
        if (hullPoints != null) {
//            Imgproc.drawContours(mGray, hullPoints, -1, new Scalar(255, 255, 255));
//            Imgproc.drawContours(mFinal, hullPoints, -1, new Scalar(255, 255, 255), -1);
            Imgproc.drawContours(mRgba, hullPoints, -1, new Scalar(255, 0, 0), -1);
        }

//        return mGray;
//        return mFinal;
        return mRgba;
    }

    MatOfPoint hull2Points(MatOfInt hull, MatOfPoint contour) {
        List<Integer> indexes = hull.toList();
        List<Point> points = new ArrayList<>();
        MatOfPoint point= new MatOfPoint();
        for(Integer index:indexes) {
            points.add(contour.toList().get(index));
        }
        point.fromList(points);
        return point;
    }

//    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
//        mRgba = inputFrame.rgba();
//        mGray = inputFrame.gray();
//        mFinal = inputFrame.gray();
//        Imgproc.GaussianBlur(mGray, mGray, new Size(5, 5), 0);
//        Imgproc.threshold(mGray, mGray, 0, 255, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU);
//        Imgproc.threshold(mFinal, mFinal, 0, 255, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU);
//        Imgproc.dilate(mGray, mGray, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5)), new Point(-1, -1), 5);
//        Imgproc.morphologyEx(mGray, mGray, Imgproc.MORPH_CLOSE, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(25, 25)));
////        Imgproc.erode(mGray, mGray, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5)), new Point(-1, -1), 5);
//
////        ArrayList<MatOfPoint> contours = new ArrayList<>();
////        Mat hierarchy = new Mat();
////        Imgproc.findContours(mGray, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
////        if (contours != null) {
////            Imgproc.drawContours(mFinal, contours, -1, new Scalar(255, 255, 255), -1);
////        }
//
////        for(int idx = 0; idx >= 0; idx = (int) hierarchy.get(0, idx)[0]) {
////            MatOfPoint matOfPoint = contours.get(idx);
////            Rect rect = Imgproc.boundingRect(matOfPoint);
////            Imgproc.rectangle(mRgba, rect.tl(), rect.br(), new Scalar(0, 0, 255));
////        }
//        return mGray;
//    }
}
