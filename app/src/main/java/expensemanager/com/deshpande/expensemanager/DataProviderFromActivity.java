package expensemanager.com.deshpande.expensemanager;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;

public interface DataProviderFromActivity {
    void sendCredentials(GoogleAccountCredential mCredentials);
    GoogleAccountCredential getCredentials();
}
