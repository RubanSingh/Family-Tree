package dna.familytree.list;

import static dna.familytree.Global.gc;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import org.folg.gedcom.model.Media;
import org.folg.gedcom.model.Person;

import java.util.List;

import dna.familytree.DetailController;
import dna.familytree.util.FileUtils;
import dna.familytree.Global;
import dna.familytree.Memory;
import dna.familytree.PrincipalController;
import dna.familytree.ProfileController;
import dna.familytree.R;
import dna.familytree.AppUtils;
import dna.familytree.constant.Choice;
import dna.familytree.detail.MediaController;
import dna.familytree.visitor.FindStack;
import dna.familytree.visitor.MediaContainerList;
import dna.familytree.visitor.MediaReferences;

/**
 * Adapter for the RecyclerView of media gallery {@link MediaFragment}.
 */
public class MediaAdapter extends RecyclerView.Adapter<MediaAdapter.gestoreVistaMedia> {

    private List<MediaContainerList.MedCont> listaMedia;
    private boolean dettagli;

    public MediaAdapter(List<MediaContainerList.MedCont> listaMedia, boolean dettagli) {
        this.listaMedia = listaMedia;
        this.dettagli = dettagli;
    }

    @Override
    public gestoreVistaMedia onCreateViewHolder(ViewGroup parent, int type) {
        View vista = LayoutInflater.from(parent.getContext()).inflate(R.layout.pezzo_media, parent, false);
        return new gestoreVistaMedia(vista, dettagli);
    }

    @Override
    public void onBindViewHolder(final gestoreVistaMedia gestore, int posizione) {
        gestore.setta(posizione);
    }

    @Override
    public int getItemCount() {
        return listaMedia.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    class gestoreVistaMedia extends RecyclerView.ViewHolder implements View.OnClickListener {
        View vista;
        boolean dettagli;
        Media media;
        Object contenitore;
        ImageView vistaImmagine;
        TextView vistaTesto;
        TextView vistaNumero;

        gestoreVistaMedia(View vista, boolean dettagli) {
            super(vista);
            this.vista = vista;
            this.dettagli = dettagli;
            vistaImmagine = vista.findViewById(R.id.media_img);
            vistaTesto = vista.findViewById(R.id.media_testo);
            vistaNumero = vista.findViewById(R.id.media_num);
        }

        void setta(int posizione) {
            media = listaMedia.get(posizione).media;
            contenitore = listaMedia.get(posizione).container;
            if (dettagli) {
                arredaMedia(media, vistaTesto, vistaNumero);
                vista.setOnClickListener(this);
                ((Activity)vista.getContext()).registerForContextMenu(vista);
                vista.setTag(R.id.tag_object, media);
                vista.setTag(R.id.tag_contenitore, contenitore);
                // Registra menu contestuale
                final AppCompatActivity attiva = (AppCompatActivity)vista.getContext();
                if (vista.getContext() instanceof ProfileController) { // ProfileMediaFragment
                    attiva.getSupportFragmentManager()
                            .findFragmentByTag("android:switcher:" + R.id.profile_pager + ":0") // non garantito in futuro
                            .registerForContextMenu(vista);
                } else if (vista.getContext() instanceof PrincipalController) // MediaFragment
                    attiva.getSupportFragmentManager().findFragmentById(R.id.contenitore_fragment).registerForContextMenu(vista);
                else // nelle AppCompatActivity
                    attiva.registerForContextMenu(vista);
            } else {
                RecyclerView.LayoutParams parami = new RecyclerView.LayoutParams(RecyclerView.LayoutParams.WRAP_CONTENT, AppUtils.dpToPx(110));
                int margin = AppUtils.dpToPx(5);
                parami.setMargins(margin, margin, margin, margin);
                vista.setLayoutParams(parami);
                vistaTesto.setVisibility(View.GONE);
                vistaNumero.setVisibility(View.GONE);
            }
            FileUtils.showImage(media, vistaImmagine, vista.findViewById(R.id.media_circolo));
        }

        @Override
        public void onClick(View v) {
            AppCompatActivity activity = (AppCompatActivity)v.getContext();
            // MediaFragment in modalità scelta dell'object media
            // Restituisce l'ID di un object media a ProfileMediaFragment
            if (activity.getIntent().getBooleanExtra(Choice.MEDIA, false)) {
                Intent intent = new Intent();
                intent.putExtra("mediaId", media.getId());
                activity.setResult(Activity.RESULT_OK, intent);
                activity.finish();
                // MediaFragment in modalità normale apre MediaActivity
            } else {
                Intent intent = new Intent(v.getContext(), MediaController.class);
                if (media.getId() != null) { // tutti i Media record
                    Memory.setLeader(media);
                } else if ((activity instanceof ProfileController && contenitore instanceof Person) // media di primo livello nell'Indi
                        || activity instanceof DetailController) { // normale apertura nei Dettagli
                    Memory.add(media);
                } else { // da MediaFragment tutti i media semplici, o da ProfileMediaFragment i media sotto molteplici livelli
                    new FindStack(Global.gc, media);
                    if (activity instanceof PrincipalController) // Solo in MediaFragment
                        intent.putExtra("daSolo", true); // così poi Immagine mostra la dispensa
                }
                v.getContext().startActivity(intent);
            }
        }
    }

    public static void arredaMedia(Media media, TextView vistaTesto, TextView vistaNumero) {
        String testo = "";
        if (media.getTitle() != null)
            testo = media.getTitle() + "\n";
        if (Global.settings.expert && media.getFile() != null) {
            String file = media.getFile();
            file = file.replace('\\', '/');
            if (file.lastIndexOf('/') > -1) {
                if (file.length() > 1 && file.endsWith("/")) // rimuove l'ultima barra
                    file = file.substring(0, file.length() - 1);
                file = file.substring(file.lastIndexOf('/') + 1);
            }
            testo += file;
        }
        if (testo.isEmpty())
            vistaTesto.setVisibility(View.GONE);
        else {
            if (testo.endsWith("\n"))
                testo = testo.substring(0, testo.length() - 1);
            vistaTesto.setText(testo);
        }
        if (media.getId() != null) {
            MediaReferences mediaReferences = new MediaReferences(gc, media, false);
            vistaNumero.setText(String.valueOf(mediaReferences.num));
            vistaNumero.setVisibility(View.VISIBLE);
        } else
            vistaNumero.setVisibility(View.GONE);
    }

    // Questa serve solo per creare una RecyclerView con le iconcine dei media che risulti trasparente ai click
    // todo però impedisce lo scroll in Dettaglio
    public static class RiciclaVista extends RecyclerView {
        boolean dettagli;

        public RiciclaVista(Context context, boolean dettagli) {
            super(context);
            this.dettagli = dettagli;
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            super.onTouchEvent(e);
            return dettagli; // quando è false la griglia non intercetta il click
        }
    }
}
