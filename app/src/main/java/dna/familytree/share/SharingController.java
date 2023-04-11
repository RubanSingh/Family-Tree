package dna.familytree.share;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Header;
import org.folg.gedcom.model.Person;
import org.folg.gedcom.model.Submitter;

import java.io.File;

import dna.familytree.BaseController;
import dna.familytree.BuildConfig;
import dna.familytree.Exporter;
import dna.familytree.Global;
import dna.familytree.NewTreeController;
import dna.familytree.PrincipalController;
import dna.familytree.R;
import dna.familytree.Settings;
import dna.familytree.AppUtils;
import dna.familytree.constant.Choice;
import dna.familytree.constant.Extra;
import dna.familytree.list.SubmittersFragment;
import dna.familytree.util.AnalyticsUtil;

public class SharingController extends BaseController {

    Gedcom gc;
    Settings.Tree tree;
    Exporter exporter;
    String submitterName;
    int accessible; // 0 = false, 1 = true
    String dateId;
    String submitterId;
    boolean successfulUpload; // To avoid uploading twice

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.sharing_activity);

        setToolbar();

        final int treeId = getIntent().getIntExtra(Extra.TREE_ID, 1);
        tree = Global.settings.getTree(treeId);

        // Title of the tree
        final EditText editTitle = findViewById(R.id.condividi_titolo);
        editTitle.setText(tree.title);

        if (tree.grade == 10)
            ((TextView)findViewById(R.id.share_submitter_title)).setText(R.string.changes_submitter);

        exporter = new Exporter(this);
        exporter.openTree(treeId);
        gc = Global.gc;
        if (gc != null) {
            displayShareRoot();
            // Submitter name
            final Submitter[] submitter = new Submitter[1];
            // tree in Italy with submitter referenced
            if (tree.grade == 0 && gc.getHeader() != null && gc.getHeader().getSubmitter(gc) != null)
                submitter[0] = gc.getHeader().getSubmitter(gc);
                // in Italy there are authors but none referenced, it takes the last one
            else if (tree.grade == 0 && !gc.getSubmitters().isEmpty())
                submitter[0] = gc.getSubmitters().get(gc.getSubmitters().size() - 1);
                // in Australia there are fresh authors, take one
            else if (tree.grade == 10 && AppUtils.autoreFresco(gc) != null)
                submitter[0] = AppUtils.autoreFresco(gc);
            final EditText editaAutore = findViewById(R.id.condividi_autore);
            submitterName = submitter[0] == null ? "" : submitter[0].getName();
            editaAutore.setText(submitterName);

            // Display an alert for the acknowledgment of sharing
//            if (!Global.settings.shareAgreement) {
//                new MaterialAlertDialogBuilder(this).setTitle(R.string.share_sensitive)
//                        .setMessage(R.string.aware_upload_server)
//                        .setPositiveButton(android.R.string.ok, (dialog, id) -> {
//                            Global.settings.shareAgreement = true;
//                            Global.settings.save();
//                        }).setNeutralButton(R.string.remind_later, null).show();
//            }

            // Collect share data and post to database
            findViewById(R.id.share_button).setOnClickListener(v -> {
                if (successfulUpload)
                    concludeShare();
                else {
                    if (controlla(editTitle, R.string.please_title) || controlla(editaAutore, R.string.please_name))
                        return;

                    v.setEnabled(false);
                    findViewById(R.id.share_wheel).setVisibility(View.VISIBLE);

                    // Title of the tree
                    String titoloEditato = editTitle.getText().toString();
                    if (!tree.title.equals(titoloEditato)) {
                        tree.title = titoloEditato;
                        Global.settings.save();
                    }

                    // Submitter update
                    Header header = gc.getHeader();
                    if (header == null) {
                        header = NewTreeController.createHeader(tree.id + ".json");
                        gc.setHeader(header);
                    } else
                        header.setDateTime(AppUtils.actualDateTime());
                    if (submitter[0] == null) {
                        submitter[0] = SubmittersFragment.createSubmitter(null);
                    }
                    if (header.getSubmitterRef() == null) {
                        header.setSubmitterRef(submitter[0].getId());
                    }
                    String nomeAutoreEditato = editaAutore.getText().toString();
                    if (!nomeAutoreEditato.equals(submitterName)) {
                        submitterName = nomeAutoreEditato;
                        submitter[0].setName(submitterName);
                        AppUtils.updateChangeDate(submitter[0]);
                    }
                    submitterId = submitter[0].getId();
                    AppUtils.saveJson(gc, treeId); // bypassing the preference not to save in automatic

                    // Tree accessibility for app developer
                    CheckBox accessibleTree = findViewById(R.id.condividi_allow);
                    accessible = accessibleTree.isChecked() ? 1 : 0;

                    dateId = tree.title;
                    File fileTree = new File(this.getCacheDir(), tree.title + ".zip");
                    if (this.exporter.exportZipBackup(this.tree.shareRoot, 9, Uri.fromFile(fileTree))) {
                        new ShareTask().execute(this);
                    }
                }
            });
        } else
            findViewById(R.id.share_layout).setVisibility(View.GONE);
    }

    // The person root of the tree
    View rootView;

    void displayShareRoot() {
        String rootId;
        if (tree.shareRoot != null && gc.getPerson(tree.shareRoot) != null)
            rootId = tree.shareRoot;
        else if (tree.root != null && gc.getPerson(tree.root) != null) {
            rootId = tree.root;
            tree.shareRoot = rootId; // to be able to share the tree immediately without changing the root
        } else {
            rootId = AppUtils.trovaRadice(gc);
            tree.shareRoot = rootId;
        }
        Person person = gc.getPerson(rootId);
        if (person != null && tree.grade < 10) { // it is only shown on the first share, not on return
            LinearLayout rootLayout = findViewById(R.id.share_root);
            rootLayout.removeView(rootView);
            rootLayout.setVisibility(View.VISIBLE);
            rootView = AppUtils.linkaPersona(rootLayout, person, 1);
            rootView.setOnClickListener(v -> {
                Intent intent = new Intent(this, PrincipalController.class);
                intent.putExtra(Choice.PERSON, true);
                startActivityForResult(intent, 5007);
            });
        }
    }

    // Check that a field is filled in
    boolean controlla(EditText campo, int msg) {
        String testo = campo.getText().toString();
        if (testo.isEmpty()) {
            campo.requestFocus();
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(campo, InputMethodManager.SHOW_IMPLICIT);
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        AnalyticsUtil.logEventSharingActivity(getFirebaseAnalytics());
    }

    // Carica in ftp il file zip con l'albero condiviso
    @SuppressLint("StaticFieldLeak")
    class ShareTask extends AsyncTask<SharingController, Void, SharingController> {
        protected SharingController doInBackground(SharingController... shareActivity) {
            SharingController questo = shareActivity[0];
            try {
                String nomeZip = tree.title + ".zip";

                File outputFile = new File(questo.getCacheDir(), nomeZip);

                Uri fileUri = FileProvider.getUriForFile(
                        SharingController.this,
                        BuildConfig.APPLICATION_ID + ".provider",
                        outputFile);

                Intent share = new Intent();
                share.setAction(Intent.ACTION_SEND);
                share.setType("application/pdf");
                share.putExtra(Intent.EXTRA_STREAM, fileUri);

                SharingController.this.startActivity(share);
            } catch (Exception e) {
                AppUtils.toast(questo, e.getLocalizedMessage());
            }
            return questo;
        }

        protected void onPostExecute(SharingController questo) {
            if (questo.successfulUpload) {
                Toast.makeText(questo, R.string.correctly_uploaded, Toast.LENGTH_SHORT).show();
                questo.concludeShare();
            } else {
                questo.findViewById(R.id.share_button).setEnabled(true);
                questo.findViewById(R.id.share_wheel).setVisibility(View.INVISIBLE);
            }
        }
    }

    // Mostra le app per condividere il link
    void concludeShare() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.sharing_tree));
        intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.click_this_link,
                "https://www.familygem.app/share.php?tree=" + dateId));
        //startActivity( Intent.createChooser( intent, "Condividi con" ) );
        /* Tornando indietro da una app di messaggistica il requestCode 35417 arriva sempre corretto
            Invece il resultCode può essere RESULT_OK o RESULT_CANCELED a capocchia
            Ad esempio da Gmail ritorna indietro sempre con RESULT_CANCELED sia che l'email è stata inviata o no
            anche inviando un Sms ritorna RESULT_CANCELED anche se l'sms è stato inviato
            oppure da Whatsapp è RESULT_OK sia che il messaggio è stato inviato o no
            In pratica non c'è modo di sapere se nella app di messaggistica il messaggio è stato inviato */
        startActivityForResult(Intent.createChooser(intent, getText(R.string.share_with)), 35417);
        findViewById(R.id.share_button).setEnabled(true);
        findViewById(R.id.share_wheel).setVisibility(View.INVISIBLE);
    }

    // Aggiorna le preferenze così da mostrare la nuova radice scelta in PersonsFragment
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == AppCompatActivity.RESULT_OK) {
            if (requestCode == 5007) {
                tree.shareRoot = data.getStringExtra("idParente");
                Global.settings.save();
                displayShareRoot();
            }
        }
        // Ritorno indietro da qualsiasi app di condivisione, nella quale il messaggio è stato inviato oppure no
        if (requestCode == 35417) {
            // Todo chiudi tastiera
            Toast.makeText(getApplicationContext(), R.string.sharing_completed, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem i) {
        onBackPressed();
        return true;
    }
}
