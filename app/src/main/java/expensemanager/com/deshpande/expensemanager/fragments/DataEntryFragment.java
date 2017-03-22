package expensemanager.com.deshpande.expensemanager.fragments;

import android.app.Dialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import expensemanager.com.deshpande.expensemanager.DataProviderFromActivity;
import expensemanager.com.deshpande.expensemanager.R;
import expensemanager.com.deshpande.expensemanager.Validation;

public class DataEntryFragment extends Fragment {

    TextView mOutputText = null;
    ProgressDialog mProgress = null;

    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    private static final String[] SCOPES = { SheetsScopes.SPREADSHEETS };
    private static final String PREF_ACCOUNT_NAME = "accountName";

    DataProviderFromActivity myActivity = null;
    GoogleAccountCredential mCredentials = null;

    Button txtSave = null;
    TextView txtError = null;
    EditText txtAmount = null;
    EditText txtDescription = null;
    Spinner txtType = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_data_entry,
                container, false);

        myActivity = (DataProviderFromActivity) getActivity();
        mCredentials = myActivity.getCredentials();

        mProgress = new ProgressDialog(getActivity());
        mProgress.setMessage("Saving Expense ...");

        txtError = (TextView) view.findViewById(R.id.txtError);
        txtAmount = (EditText) view.findViewById(R.id.txtAmount);
        txtDescription = (EditText) view.findViewById(R.id.txtDescription);
        txtType = (Spinner) view.findViewById(R.id.txtType);

        txtSave = (Button) view.findViewById(R.id.txtSave);
        txtSave.setEnabled(true);
        txtSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isDeviceOnline()) {
                    txtError .setText("No network connection available.");
                } else {
                    if (checkValidation())
                        new MakeRequestTask(mCredentials).execute();
                    else
                        Toast.makeText(DataEntryFragment.this.getActivity(), "Form contains error", Toast.LENGTH_LONG).show();
                }
            }
        });

        return view;
    }

    private boolean checkValidation() {
        boolean ret = true;
        if (!Validation.hasText(txtAmount)) ret = false;
        return ret;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    /**
     * Checks whether the device currently has a network connection.
     * @return true if the device has a network connection, false otherwise.
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * An asynchronous task that handles the Google Sheets API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class MakeRequestTask extends AsyncTask<Void, Void, String> {
        private com.google.api.services.sheets.v4.Sheets mService = null;
        private Exception mLastError = null;

        MakeRequestTask(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.sheets.v4.Sheets.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Google Sheets API Android Quickstart")
                    .build();
        }

        /**
         * Background task to call Google Sheets API.
         * @param params no parameters needed for this task.
         */
        @Override
        protected String doInBackground(Void... params) {
            try {
                putDataFromAPI();
                return "OK";
            } catch (Exception e) {
                Log.e("ERROR", "" + e.getMessage());
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        private  void putDataFromAPI() throws IOException {
            String spreadsheetId = "1av64Iccfh91Q00GGdZDbQkhR62s7u6HqBnilIMj8RQc";
            String range = "Sheet1!A2:E2";

            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Date date = new Date();

            String accountName = DataEntryFragment.this.getActivity().getPreferences(Context.MODE_PRIVATE)
                    .getString(DataEntryFragment.PREF_ACCOUNT_NAME, null);

            //for the values that you want to input, create a list of object lists
            List<List<Object>> values = new ArrayList<>();

            //Where each value represents the list of objects that is to be written to a range
            //I simply want to edit a single row, so I use a single list of objects
            List<Object> data1 = new ArrayList<>();
            data1.add(accountName);
            data1.add(dateFormat.format(date));
            data1.add(DataEntryFragment.this.txtType.getSelectedItem().toString());
            data1.add(DataEntryFragment.this.txtAmount.getText().toString());
            data1.add(DataEntryFragment.this.txtDescription.getText().toString());
            values.add(data1);

            //Create the value range object and set its fields
            ValueRange valueRange = new ValueRange();
            valueRange.setMajorDimension("ROWS");
            valueRange.setRange(range);
            valueRange.setValues(values);

            this.mService.spreadsheets().values().append(spreadsheetId, range, valueRange)
                    .setValueInputOption("RAW")
                    .execute();
        }

        @Override
        protected void onPreExecute() {
            mProgress.show();
        }

        @Override
        protected void onPostExecute(String output) {
            mProgress.hide();
            if (output == null) {
                txtError.setText("Some Error Occurred.");
            } else {
                txtAmount.setText("");
                txtDescription.setText("");
                txtError.setText("Success in Posting the Expense.");
            }
        }

        @Override
        protected void onCancelled() {
            mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            AuthenticateFragment.REQUEST_AUTHORIZATION);
                } else {
                    txtError.setText("The following error occurred:\n"
                            + mLastError.getMessage());
                }
            } else {
                txtError.setText("Request cancelled.");
            }
        }
    }

    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     * @param connectionStatusCode code describing the presence (or lack of)
     *     Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                getActivity(),
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }
}
