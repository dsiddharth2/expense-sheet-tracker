package expensemanager.com.deshpande.expensemanager;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.sheets.v4.SheetsScopes;

import java.util.Arrays;

import expensemanager.com.deshpande.expensemanager.fragments.AuthenticateFragment;
import expensemanager.com.deshpande.expensemanager.fragments.DataEntryFragment;

public class ExpenseManager_Main extends AppCompatActivity implements DataProviderFromActivity {

    GoogleAccountCredential mCredentials = null;
    FragmentManager fm = null;
    private static final String[] SCOPES = { SheetsScopes.SPREADSHEETS };
    private static final String PREF_ACCOUNT_NAME = "accountName";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense_mamager__main);

        mCredentials = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());
        String accountName = getPreferences(Context.MODE_PRIVATE).getString(PREF_ACCOUNT_NAME, null);
        mCredentials.setSelectedAccountName(accountName);

        fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        if(this.mCredentials.getSelectedAccount() != null) {
            ft.add(R.id.auth_container, new DataEntryFragment());
        } else {
            ft.add(R.id.auth_container, new AuthenticateFragment());
        }
        ft.commit();
    }

    @Override
    public void sendCredentials(GoogleAccountCredential mCredentials) {
        this.mCredentials = mCredentials;

        // replace
        FragmentTransaction ft = this.fm.beginTransaction();
        ft.replace(R.id.auth_container, new DataEntryFragment());
        ft.commit();
    }

    @Override
    public GoogleAccountCredential getCredentials() {
        return this.mCredentials;
    }
}
