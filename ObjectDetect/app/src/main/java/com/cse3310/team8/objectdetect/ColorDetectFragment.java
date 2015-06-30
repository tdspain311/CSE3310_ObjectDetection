package com.cse3310.team8.objectdetect;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ColorDetectFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ColorDetectFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ColorDetectFragment extends Fragment implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "MotionDetect::Fragment";

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.i(TAG, "opencv initialization failed");
        }
        else {
            Log.i(TAG, "opencv initialization successful");
        }
    }

    private OnFragmentInteractionListener mListener;

    private CameraBridgeViewBase mOpenCvCameraView;
    private Mat                  mRgba;
    private ColorDetector        mColorDetector;

    private boolean              mIsColorSelected = false;

    private Scalar               mBlobColorRgba;
    private Scalar               mBlobColorHsv;
    private Mat                  mSpectrum;
    private Size                 SPECTRUM_SIZE;
    private Scalar               CONTOUR_COLOR;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(super.getActivity()) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "CallBack loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public static ColorDetectFragment newInstance() {
        ColorDetectFragment fragment = new ColorDetectFragment();
        return fragment;
    }

    public ColorDetectFragment() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //**TESTING**


        //**TESTING**
        //super.getActivity().requestWindowFeature(Window.FEATURE_NO_TITLE);
        //super.getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        //**TESTING**
        //mOpenCvCameraView = (CameraBridgeViewBase) super.getActivity().findViewById(R.id.surface_view_color_detect);
        //mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        //mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getActivity().setRequestedOrientation(
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        View view = inflater.inflate(R.layout.fragment_color_detect, container, false);

        //**TESTING**
        //super.getActivity().requestWindowFeature(Window.FEATURE_NO_TITLE);
        //super.getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mOpenCvCameraView = (CameraBridgeViewBase) view.findViewById(R.id.surface_view_color_detect);
        mOpenCvCameraView.setCvCameraViewListener(this);

        view.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                int cols = mRgba.cols();
                int rows = mRgba.rows();

                int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
                int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;

                int x = (int) event.getX() - xOffset;
                int y = (int) event.getY() - yOffset;

                Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");

                if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;

                Rect touchedRect = new Rect();

                touchedRect.x = (x > 4) ? x - 4 : 0;
                touchedRect.y = (y > 4) ? y - 4 : 0;

                touchedRect.width = (x + 4 < cols) ? x + 4 - touchedRect.x : cols - touchedRect.x;
                touchedRect.height = (y + 4 < rows) ? y + 4 - touchedRect.y : rows - touchedRect.y;

                Mat touchedRegionRgba = mRgba.submat(touchedRect);

                Mat touchedRegionHsv = new Mat();
                Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);

                // Calculate average color of touched region
                mBlobColorHsv = Core.sumElems(touchedRegionHsv);
                int pointCount = touchedRect.width * touchedRect.height;
                for (int i = 0; i < mBlobColorHsv.val.length; i++)
                    mBlobColorHsv.val[i] /= pointCount;

                mBlobColorRgba = convertScalarHsv2Rgba(mBlobColorHsv);

                Log.i(TAG, "Touched rgba color: (" + mBlobColorRgba.val[0] + ", " + mBlobColorRgba.val[1] +
                        ", " + mBlobColorRgba.val[2] + ", " + mBlobColorRgba.val[3] + ")");

                mColorDetector.setHsvColor(mBlobColorHsv);

                Imgproc.resize(mColorDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE);

                mIsColorSelected = true;

                touchedRegionRgba.release();
                touchedRegionHsv.release();

                return false; // don't need subsequent touch events
            }
        });

        return view;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    //**TESTING**

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        //**TESTING**
        //getActivity().getActionBar().hide();
        /*
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
        */
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
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);

        //Async initialization with OPENCV Manager
        //OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, super.getActivity(), mLoaderCallback);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }



    //**TESTING**

    @Override
    public void onDetach() {
        super.onDetach();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        mListener = null;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        Log.i(TAG, "height " + mRgba.height() + "width " + mRgba.width());

        mColorDetector = new ColorDetector();
        mSpectrum = new Mat();
        mBlobColorRgba = new Scalar(255);
        mBlobColorHsv = new Scalar(255);
        SPECTRUM_SIZE = new Size(200, 64);
        CONTOUR_COLOR = new Scalar(0,255,0,0);
    }



    @Override
    public void onCameraViewStopped() {
            mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        //Rect boundingBox = new Rect(); // testing
        MatOfPoint2f approxCurve = new MatOfPoint2f();


        if (mIsColorSelected) {
            mColorDetector.process(mRgba);
            List<MatOfPoint> contours = mColorDetector.getContours();

            //MatOfPoint2f contour2f = new MatOfPoint2f( contours.get().toArray() );
            //boundingBox = Imgproc.boundingRect();

            //For each contour found

            for (int i=0; i<contours.size(); i++)
            {
                //Convert contours(i) from MatOfPoint to MatOfPoint2f
                MatOfPoint2f contour2f = new MatOfPoint2f( contours.get(i).toArray() );
                //Processing on mMOP2f1 which is in type MatOfPoint2f
                double approxDistance = Imgproc.arcLength(contour2f, true)*0.02;
                Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);

                //Convert back to MatOfPoint
                MatOfPoint points = new MatOfPoint( approxCurve.toArray() );

                // Get bounding rect of contour
                Rect rect = Imgproc.boundingRect(points);
                //Point center = new Point();

                //center = rect.x+rect.tl();
                // draw enclosing rectangle (all same color, but you could use variable i to make them unique)
                //Point((rect.x+rect.width, rect.y+rect.height) , new Scalar(255, 0, 0, 255), 3);
                Core.putText(mRgba, "Tracking", new Point(rect.tl().x, rect.tl().y-10), Core.FONT_HERSHEY_PLAIN, 2, new Scalar(255, 0, 0, 255));
                Core.rectangle(mRgba, rect.tl(), rect.br(), CONTOUR_COLOR, 3, 8, 0);
            }
            Log.i(TAG, "Contours count: " + contours.size());
            //Imgproc.findContours(mRgba, contours, );
            //Imgproc.drawContours(mRgba, contours, 3, CONTOUR_COLOR);

            Mat colorLabel = mRgba.submat(4, 68, 4, 68);
            colorLabel.setTo(mBlobColorRgba);

            Mat spectrumLabel = mRgba.submat(4, 4 + mSpectrum.rows(), 70, 70 + mSpectrum.cols());
            mSpectrum.copyTo(spectrumLabel);
        }

        return mRgba;
    }

    private Scalar convertScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }

}
