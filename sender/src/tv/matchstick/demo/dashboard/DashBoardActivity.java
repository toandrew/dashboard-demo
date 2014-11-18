package tv.matchstick.demo.dashboard;

import java.io.IOException;

import tv.matchstick.fling.ApplicationMetadata;
import tv.matchstick.fling.ConnectionResult;
import tv.matchstick.fling.Fling;
import tv.matchstick.fling.Fling.ApplicationConnectionResult;
import tv.matchstick.fling.FlingDevice;
import tv.matchstick.fling.FlingManager;
import tv.matchstick.fling.FlingMediaControlIntent;
import tv.matchstick.fling.ResultCallback;
import tv.matchstick.fling.Status;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class DashBoardActivity extends ActionBarActivity {
    private static final String TAG = "MyDashBoardDemo";

    private static final String APP_URL = "http://toandrew.github.io/dashboard-demo/receiver/index.html";

    private Button mSendBtn;
    private EditText mInfoBox;

    private String mCurrentUser;

    private FlingDevice mSelectedDevice;
    private FlingManager mApiClient;
    private Fling.Listener mFlingListener;
    private ConnectionCallbacks mConnectionCallbacks;
    private ConnectionFailedListener mConnectionFailedListener;
    private MediaRouter mMediaRouter;
    private MediaRouteSelector mMediaRouteSelector;
    private MediaRouter.Callback mMediaRouterCallback;
    private DashBoardChannel mDashBoardChannel;

    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dash_board);

        String APPLICATION_ID = "~dashboard";
        Fling.FlingApi.setApplicationId(APPLICATION_ID);

        mDashBoardChannel = new DashBoardChannel();

        mMediaRouter = MediaRouter.getInstance(getApplicationContext());
        mMediaRouteSelector = new MediaRouteSelector.Builder()
                .addControlCategory(
                        FlingMediaControlIntent
                                .categoryForFling(APPLICATION_ID)).build();

        mMediaRouterCallback = new MediaRouterCallback();
        mFlingListener = new FlingListener();
        mConnectionCallbacks = new ConnectionCallbacks();
        mConnectionFailedListener = new ConnectionFailedListener();

        mInfoBox = (EditText) findViewById(R.id.info);
        mSendBtn = (Button) findViewById(R.id.sendBtn);
        mSendBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if (mApiClient != null && mApiClient.isConnected()) {
                    sendMessage(mInfoBox.getText().toString());
                }
            }

        });

        mSendBtn.setEnabled(false);

        mCurrentUser = "guest";
    }

    @Override
    protected void onStart() {
        super.onStart();
        mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback,
                MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        finish();
        super.onPause();
    }

    @Override
    protected void onStop() {
        setSelectedDevice(null);
        mMediaRouter.removeCallback(mMediaRouterCallback);
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * Called when the options menu is first created.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);
        MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
        MediaRouteActionProvider mediaRouteActionProvider = (MediaRouteActionProvider) MenuItemCompat
                .getActionProvider(mediaRouteMenuItem);
        mediaRouteActionProvider.setRouteSelector(mMediaRouteSelector);
        return true;
    }

    private class MediaRouterCallback extends MediaRouter.Callback {
        @Override
        public void onRouteSelected(MediaRouter router, RouteInfo route) {
            Log.d(TAG, "onRouteSelected: " + route);
            DashBoardActivity.this.onRouteSelected(route);
        }

        @Override
        public void onRouteUnselected(MediaRouter router, RouteInfo route) {
            Log.d(TAG, "onRouteUnselected: " + route);
            DashBoardActivity.this.onRouteUnselected(route);
        }
    }

    /**
     * Stop receiver application.
     */
    public void stopApplication() {
        if (!mApiClient.isConnected()) {
            return;
        }

        Fling.FlingApi.stopApplication(mApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status result) {
                        if (result.isSuccess()) {
                            //
                        }
                    }
                });
    }

    private void setSelectedDevice(FlingDevice device) {
        Log.d(TAG, "setSelectedDevice: " + device);
        mSelectedDevice = device;

        if (mSelectedDevice != null) {
            mSendBtn.setText(R.string.connecting);
            try {
                disconnectApiClient();
                connectApiClient();
            } catch (IllegalStateException e) {
                Log.w(TAG, "Exception while connecting API client", e);
                disconnectApiClient();

                mSendBtn.setEnabled(false);
                mSendBtn.setText(R.string.not_connected);
                mSendBtn.setTextColor(Color.RED);
            }
        } else {
            if (mApiClient != null) {
                if (mApiClient.isConnected()) {
                    mDashBoardChannel.leave(mApiClient, mCurrentUser);
                }

                stopApplication();

                disconnectApiClient();
            }

            mMediaRouter.selectRoute(mMediaRouter.getDefaultRoute());

            mSendBtn.setEnabled(false);
            mSendBtn.setText(R.string.not_connected);
            mSendBtn.setTextColor(Color.RED);
        }
    }

    private void connectApiClient() {
        Fling.FlingOptions apiOptions = Fling.FlingOptions.builder(
                mSelectedDevice, mFlingListener).build();
        mApiClient = new FlingManager.Builder(this)
                .addApi(Fling.API, apiOptions)
                .addConnectionCallbacks(mConnectionCallbacks)
                .addOnConnectionFailedListener(mConnectionFailedListener)
                .build();
        mApiClient.connect();
    }

    private void disconnectApiClient() {
        if (mApiClient != null) {
            mApiClient.disconnect();
            mApiClient = null;
        }
    }

    /**
     * Called when a user selects a route.
     */
    private void onRouteSelected(RouteInfo route) {
        Log.d(TAG, "onRouteSelected: " + route.getName());

        FlingDevice device = FlingDevice.getFromBundle(route.getExtras());
        setSelectedDevice(device);
    }

    /**
     * Called when a user unselects a route.
     */
    private void onRouteUnselected(RouteInfo route) {
        if (route != null) {
            Log.d(TAG, "onRouteUnselected: " + route.getName());
        }
        setSelectedDevice(null);
    }

    private class FlingListener extends Fling.Listener {
        @Override
        public void onApplicationDisconnected(int statusCode) {
            Log.d(TAG, "Fling.Listener.onApplicationDisconnected: "
                    + statusCode);

            mSelectedDevice = null;
            mMediaRouter.selectRoute(mMediaRouter.getDefaultRoute());

            if (mApiClient == null) {
                return;
            }

            try {
                Fling.FlingApi.removeMessageReceivedCallbacks(mApiClient,
                        mDashBoardChannel.getNamespace());
            } catch (IOException e) {
                Log.w(TAG, "Exception while launching application", e);
            }
        }
    }

    private class ConnectionCallbacks implements
            FlingManager.ConnectionCallbacks {
        @Override
        public void onConnectionSuspended(int cause) {
            Log.d(TAG, "ConnectionCallbacks.onConnectionSuspended");
        }

        @Override
        public void onConnected(Bundle connectionHint) {
            Log.d(TAG, "ConnectionCallbacks.onConnected");
            Fling.FlingApi.launchApplication(mApiClient, APP_URL)
                    .setResultCallback(new ConnectionResultCallback());
        }
    }

    private class ConnectionFailedListener implements
            FlingManager.OnConnectionFailedListener {
        @Override
        public void onConnectionFailed(ConnectionResult result) {
            Log.d(TAG, "ConnectionFailedListener.onConnectionFailed");
            setSelectedDevice(null);
        }
    }

    private final class ConnectionResultCallback implements
            ResultCallback<ApplicationConnectionResult> {
        @Override
        public void onResult(ApplicationConnectionResult result) {
            Status status = result.getStatus();
            ApplicationMetadata appMetaData = result.getApplicationMetadata();

            if (status.isSuccess()) {
                Log.d(TAG, "ConnectionResultCallback: " + appMetaData.getData());
                try {
                    Fling.FlingApi
                            .setMessageReceivedCallbacks(mApiClient,
                                    mDashBoardChannel.getNamespace(),
                                    mDashBoardChannel);

                    mDashBoardChannel.show(mApiClient, mCurrentUser, "Hi~");

                    mSendBtn.setEnabled(true);
                    mSendBtn.setText(R.string.send);
                    mSendBtn.setTextColor(Color.BLUE);
                } catch (IOException e) {
                    Log.w(TAG, "Exception while launching application", e);
                }
            } else {
                Log.d(TAG,
                        "ConnectionResultCallback. Unable to launch the game. statusCode: "
                                + status.getStatusCode());

                mSendBtn.setEnabled(false);
                mSendBtn.setText(R.string.not_connected);
                mSendBtn.setTextColor(Color.RED);
            }
        }
    }

    private void sendMessage(final String hello) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mDashBoardChannel != null) {
                    mDashBoardChannel.show(mApiClient, mCurrentUser, hello);
                }
            }
        });
    }
}
