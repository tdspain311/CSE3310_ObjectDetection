package com.cse3310.team8.objectdetect;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.app.Fragment;

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
//import android.support.v4.Fragment;


/**
 * A simple {@link android.app.Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link com.cse3310.team8.objectdetect.BoundingBoxFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link com.cse3310.team8.objectdetect.BoundingBoxFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BoundingBoxFragment extends Fragment implements CameraBridgeViewBase.CvCameraViewListener2{

    private static final String TAG = "BoundingBox::Fragment";

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

    public static BoundingBoxFragment newInstance() {
        BoundingBoxFragment fragment = new BoundingBoxFragment();
        return fragment;
    }

    public BoundingBoxFragment() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    //@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getActivity().setRequestedOrientation(
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        View view = inflater.inflate(R.layout.fragment_bounding_box, container, false);

        mOpenCvCameraView = (CameraBridgeViewBase) view.findViewById(R.id.surface_view_bounding_box);
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

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
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
        CONTOUR_COLOR = new Scalar(0,255,255,255);

    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        Point point1 = new Point(mRgba.width()/4, 3*mRgba.height()/4);
        Point point2 = new Point(3*mRgba.width()/4, mRgba.height()/4);

        //BOX out of lines
        //Left line
        //Core.line(mRgba, point1, new Point(point1.x, point2.y), new Scalar(255, 0, 255, 255), 4);
        //Right line
        //Core.line(mRgba, point2, new Point(point2.x, point1.y), new Scalar(255, 0, 255, 255), 4);
        //Top line
        //Core.line(mRgba, point1, new Point(point2.x, point1.y), new Scalar(255, 0, 255, 255), 4);
        //Bottom line
        //Core.line(mRgba, point2, new Point(point1.x, point2.y), new Scalar(255, 0, 255, 255), 4);
        Core.rectangle(mRgba, point1, point2, new Scalar(255, 0, 255, 255), 4);



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

                // draw enclosing rectangle (all same color, but you could use variable i to make them unique)
                //Point((rect.x+rect.width, rect.y+rect.height) , new Scalar(255, 0, 0, 255), 3);
                //Core.rectangle(mRgba, rect.tl(), rect.br(), CONTOUR_COLOR, 3, 8, 0);

                if (//Top left of bounding box
                    ( (rect.tl().y > point2.y) && (rect.tl().y < point1.y) &&
                      (rect.tl().x > point1.x) && (rect.tl().x < point2.x) ) &&
                   (//Bottom Right of bounding box
                    ( (rect.br().y > point2.y) && (rect.br().y < point1.y) &&
                      (rect.br().x > point1.x) && (rect.br().x < point2.x)) ) )
                    {
                    Core.putText(mRgba, "Captured!", new Point(rect.tl().x, rect.tl().y-10), Core.FONT_HERSHEY_PLAIN, 2, new Scalar(255, 0, 0, 255), 2);
                    Core.rectangle(mRgba, rect.tl(), rect.br(), new Scalar(255, 0, 0, 255), 3, 8, 0);
                }
                else
                    Core.rectangle(mRgba, rect.tl(), rect.br(), CONTOUR_COLOR, 3, 8, 0);


                //Core.rectangle(mRgba, rect.tl(), rect.br(), CONTOUR_COLOR, -1);
            }
            Log.i(TAG, "Contours count: " + contours.size());
            //Imgproc.findContours(mRgba, contours, );
            //Imgproc.drawContours(mRgba, contours, 3, CONTOUR_COLOR);

            //**TESTING**
            /*
            Mat colorLabel = mRgba.submat(4, 68, 4, 68);
            colorLabel.setTo(mBlobColorRgba);

            Mat spectrumLabel = mRgba.submat(4, 4 + mSpectrum.rows(), 70, 70 + mSpectrum.cols());
            mSpectrum.copyTo(spectrumLabel);
            */
        }

        return mRgba;
    }

    private Scalar convertScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
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
        //**TESTING**
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
        mListener = null;
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
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
