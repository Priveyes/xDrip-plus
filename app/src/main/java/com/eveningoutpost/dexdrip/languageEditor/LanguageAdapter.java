package com.eveningoutpost.dexdrip.languageEditor;

import android.content.*;
import android.graphics.*;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.*;

import androidx.annotation.*;
import androidx.appcompat.app.*;
import androidx.recyclerview.widget.*;

import com.eveningoutpost.dexdrip.models.*;
import com.eveningoutpost.dexdrip.models.UserError.*;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.utilitymodels.ShotStateStore;
import com.eveningoutpost.dexdrip.utilitymodels.*;
import com.github.amlcurran.showcaseview.*;
import com.github.amlcurran.showcaseview.targets.*;

import java.util.*;

/**
 * Created by jamorham on 04/07/2016.
 */

public class LanguageAdapter extends RecyclerView.Adapter<LanguageAdapter.MyViewHolder> {

    private static final String TAG = "jamorhamlang";
    private List<LanguageItem> languageList;
    private Context context;
    private boolean showcased_undo = false;
    private boolean showcased_newline = false;
    public int first_run = 0;

    public LanguageAdapter(Context ctx, List<LanguageItem> languageList) {
        this.languageList = languageList;
        this.context = ctx;
        UserError.Log.i(TAG, "New adapter, size: " + languageList.size());
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        RelativeLayout wholeBlock;
        ImageButton elementUndo;
        TextView english_text, id_text;
        EditText local_text;
        int position = -1;

        public MyViewHolder(View view) {
            super(view);

            id_text = (TextView) view.findViewById(R.id.language_id_reference);
            english_text = (TextView) view.findViewById(R.id.language_english_text);
            local_text = (EditText) view.findViewById(R.id.language_translated_text);
            elementUndo = (ImageButton) view.findViewById(R.id.languageElementUndo);
          //  wholeBlock = (RelativeLayout) view.findViewById(R.id.language_list_block);
        }
    }

    private void informThisRowChanged(MyViewHolder holder, TextView v) {
        final int pos = holder.getAdapterPosition();
        try {
            UserError.Log.i(TAG, "informThisRowChanged: " + pos);
            if (pos > -1) {
                LanguageAdapter.this.notifyItemChanged(pos, new LanguageItem(languageList.get(pos).item_name, languageList.get(pos).english_text, v.getText().toString().replace(" ^ ", "\n")));
            }
        } catch (IllegalStateException e) {
            UserError.Log.i(TAG,"informThisRowChanged - cannot calculate during scroll");
        }
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.language_list_row, parent, false);

        final MyViewHolder holder = new MyViewHolder(itemView);

        // allow ime-done on multiline
        holder.local_text.setHorizontallyScrolling(false);
        holder.local_text.setMaxLines(100);

        holder.elementUndo.setOnLongClickListener(v -> {
            LanguageAdapter.this.notifyItemChanged(holder.position, "undo");
            return true;
        });

        holder.local_text.setOnEditorActionListener((v, actionId, event) -> {
            boolean handled = false;
            if (actionId == EditorInfo.IME_ACTION_DONE) {
             informThisRowChanged(holder, v);
                 }
            return handled;
        });

        holder.local_text.setOnFocusChangeListener((v, hasFocus) -> {
           if (!hasFocus) {
               informThisRowChanged(holder, (TextView)v);
           }
        });



     /*   holder.local_text.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                final int pos = holder.getAdapterPosition();
                // has it actually changed or was this recycle element just reused?
                if (!languageList.get(pos).local_text.equals(s.toString()))
                {
                    UserError.Log.i(TAG,"afterTextChanged: "+pos+" :"+s+":"+languageList.get(pos).local_text);
                    LanguageAdapter.this.notifyItemChanged(pos, new LanguageItem(languageList.get(pos).item_name, languageList.get(pos).english_text, s.toString()));

                }
             }
        });
*/
        return holder;
    }


    @Override
    public void onBindViewHolder(@NonNull final MyViewHolder holder, int position) {
        final LanguageItem languageItem = languageList.get(position);
        holder.position = position;


        holder.english_text.setText(languageItem.english_text);


        holder.local_text.setText(languageItem.local_text.replace("\n", " ^ "));
        holder.id_text.setText(languageItem.item_name);


        if (languageItem.customized) {
            holder.local_text.setTextColor(Color.parseColor("#d6a5a7"));
            holder.elementUndo.setVisibility(View.VISIBLE);

            // optimize with flag
            if (!showcased_undo) {
                if (JoH.ratelimit("language-showcase", 2)) {
                    if (!ShotStateStore.hasShot(LanguageEditor.SHOWCASE_LANGUAGE_ELEMENT_UNDO)) {
                        ShowcaseView myShowcase = new ShowcaseView.Builder((AppCompatActivity) context)

                                .setTarget(new ViewTarget(holder.elementUndo))
                                .setStyle(R.style.CustomShowcaseTheme2)
                                .setContentTitle("Item Undo Button") // always in english
                                .setContentText("\n" + "You can Undo a single change by Long-pressing the Undo button.") // always in english
                                .setShowcaseDrawer(new JamorhamShowcaseDrawer(context.getResources(), context.getTheme(), 90, 14))
                                .singleShot(LanguageEditor.oneshot ? LanguageEditor.SHOWCASE_LANGUAGE_ELEMENT_UNDO : -1)
                                .build();

                        myShowcase.setBackgroundColor(Color.TRANSPARENT);
                        myShowcase.show();
                        showcased_undo = true;
                    } else {
                        showcased_undo = true;
                    }
                }
            }

        } else {
            holder.local_text.setTextColor(Color.parseColor("#a5d6a7"));
            holder.elementUndo.setVisibility(View.INVISIBLE);
        }

        if (languageItem.english_text.equals(languageItem.local_text)) {
            holder.id_text.setText(holder.id_text.getText() + "             NEW");
        }

        if (!showcased_newline) {
            if (LanguageEditor.last_filter.isEmpty()) {
                if (holder.local_text.getText().toString().contains(" ^ ")) {
                    if (JoH.ratelimit("language-showcase", 2)) {
                        if (!ShotStateStore.hasShot(LanguageEditor.SHOWCASE_LANGUAGE_ELEMENT_NEWLINE)) {
                            ShowcaseView myShowcase = new ShowcaseView.Builder((AppCompatActivity) context)

                                    .setTarget(new ViewTarget(holder.local_text))
                                    .setStyle(R.style.CustomShowcaseTheme2)
                                    .setContentTitle("Line break and other Symbols") // always in english
                                    .setContentText("\n" + "Symbols like ^ are used internally for advancing to a new line.\n\nPlease be careful to preserve these exactly and also respect other symbols you find like : which may affect the user interface if removed.") // always in english
                                    .setShowcaseDrawer(new JamorhamShowcaseDrawer(context.getResources(), context.getTheme(), 90, 50))
                                    .singleShot(LanguageEditor.oneshot ? LanguageEditor.SHOWCASE_LANGUAGE_ELEMENT_NEWLINE : -1)
                                    .build();

                            myShowcase.setBackgroundColor(Color.TRANSPARENT);
                            myShowcase.show();
                            showcased_newline = true;
                        } else {
                            showcased_newline = true;
                        }
                    }
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return languageList.size();
    }
}
