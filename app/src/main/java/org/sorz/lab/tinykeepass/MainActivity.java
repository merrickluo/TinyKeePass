package org.sorz.lab.tinykeepass;

import android.app.KeyguardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.security.keystore.UserNotAuthenticatedException;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import java.io.File;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

import de.slackspace.openkeepass.KeePassDatabase;
import de.slackspace.openkeepass.domain.KeePassFile;
import de.slackspace.openkeepass.exception.KeePassDatabaseUnreadableException;

public class MainActivity extends AppCompatActivity
        implements FingerprintDialogFragment.OnFragmentInteractionListener {
    private final static String TAG = MainActivity.class.getName();
    private final static int REQUEST_CONFIRM_DEVICE_CREDENTIAL = 0;

    private SharedPreferences preferences;
    private KeyguardManager keyguardManager;
    private SecureStringStorage secureStringStorage;

    private File databaseFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        databaseFile = new File(getNoBackupFilesDir(), FetchDatabaseTask.DB_FILENAME);
        try {
            secureStringStorage = new SecureStringStorage(this);
        } catch (SecureStringStorage.SystemException e) {
            throw new RuntimeException(e);
        }

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, new DatabaseLockedFragment())
                    .commit();

            if (KeePassStorage.getKeePassFile() == null && !databaseFile.canRead()) {
                doConfigureDatabase();
                finish();
            } else {
                doUnlockDatabase();
            }
        }
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(view ->
                Snackbar
                    .make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show());
    }

    public void doUnlockDatabase() {
        if (KeePassStorage.getKeePassFile() != null)
            showEntryList();
        else
            openDatabase();
    }

    public void doConfigureDatabase() {
        KeePassStorage.setKeePassFile(null);
        startActivity(new Intent(this, DatabaseSetupActivity.class));
    }


    private void openDatabase() {
        int authMethod = preferences.getInt("key-auth-method", 0);
        switch (authMethod) {
            case 0: // no auth
            case 1: // screen lock
                try {
                    Cipher cipher = secureStringStorage.getDecryptCipher();
                    openDatabase(cipher);
                } catch (UserNotAuthenticatedException e) {
                    // should do authentication
                    Intent intent = keyguardManager.createConfirmDeviceCredentialIntent(
                            getString(R.string.auth_key_title),
                            getString(R.string.auth_key_decription));
                    startActivityForResult(intent, REQUEST_CONFIRM_DEVICE_CREDENTIAL);
                } catch (SecureStringStorage.SystemException e) {
                    throw new RuntimeException(e);
                }
                break;
            case 2: // fingerprint
                getFragmentManager().beginTransaction()
                        .add(FingerprintDialogFragment.newInstance(), "fingerprint")
                        .commit();
                break;
        }
    }

    private void openDatabase(Cipher cipher) {
        try {
            List<String> strings = secureStringStorage.get(cipher);
            KeePassFile db = KeePassDatabase.getInstance(databaseFile).openDatabase(strings.get(0));
            KeePassStorage.setKeePassFile(db);
            showEntryList();
        } catch (SecureStringStorage.SystemException e) {
            throw new RuntimeException(e);
        } catch (BadPaddingException | IllegalBlockSizeException | UserNotAuthenticatedException e) {
            Log.w(TAG, "fail to decrypt keys", e);
            Snackbar.make(findViewById(R.id.toolbar),
                    "Failed to decrypt keys", Snackbar.LENGTH_LONG).show();
        } catch (KeePassDatabaseUnreadableException | UnsupportedOperationException e) {
            Log.w(TAG, "cannot open database.", e);
            Snackbar.make(findViewById(R.id.toolbar),
                    e.getLocalizedMessage(), Snackbar.LENGTH_LONG).show();
        }
    }

    private void showEntryList() {
        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, EntryFragment.newInstance())
                .commit();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONFIRM_DEVICE_CREDENTIAL:
                if (resultCode == RESULT_OK)
                    openDatabase();
                else
                    Snackbar.make(findViewById(R.id.toolbar),
                            "Failed to authenticate user", Snackbar.LENGTH_LONG).show();
                break;
            default:
                break;
        }
    }

    @Override
    public void onFingerprintCancel() {
        Snackbar.make(findViewById(R.id.toolbar),
                "Failed to authenticate user", Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onFingerprintSuccess(Cipher cipher) {
        openDatabase(cipher);
    }
}
