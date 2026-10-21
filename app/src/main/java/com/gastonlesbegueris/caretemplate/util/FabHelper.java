package com.gastonlesbegueris.caretemplate.util;

import android.content.Intent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.gastonlesbegueris.caretemplate.R;
import com.gastonlesbegueris.caretemplate.ui.SubjectListActivity;

/**
 * Helper para reutilizar el menú flotante (FAB speed dial) en todas las pantallas.
 * Gestiona abrir/cerrar, animaciones y navegación a sujetos / agregar evento.
 */
public final class FabHelper {

    private FabHelper() {}

    public static void initFabSpeedDial(AppCompatActivity activity,
                                        int fabMainId,
                                        int fabSubjectId,
                                        int fabEventId,
                                        @Nullable RecyclerView recyclerToAutoClose) {
        View fab = activity.findViewById(fabMainId);
        View fabSubject = activity.findViewById(fabSubjectId);
        View fabEvent = activity.findViewById(fabEventId);
        if (fab == null || fabSubject == null || fabEvent == null) return;

        final boolean[] fabMenuOpen = {false};

        // Iconos fijos: flavor y calendario
        // Para family usar ic_person_add, para otros flavors usar ic_header_flavor
        String appType = activity.getString(R.string.app_type);
        int fabIconRes = "family".equals(appType) 
            ? R.drawable.ic_fab_family 
            : R.drawable.ic_header_flavor;
        ((com.google.android.material.floatingactionbutton.FloatingActionButton) fabSubject)
                .setImageResource(fabIconRes);
        ((com.google.android.material.floatingactionbutton.FloatingActionButton) fabEvent)
                .setImageResource(R.drawable.ic_event);

        fab.setOnClickListener(v -> toggle(fabMenuOpen, fabSubject, fabEvent, fab));

        fabSubject.setOnClickListener(v -> {
            closeMenu(fabMenuOpen, fabSubject, fabEvent, fab);
            Intent intent = new Intent(activity, SubjectListActivity.class);
            activity.startActivity(intent);
        });
        fabEvent.setOnClickListener(v -> {
            closeMenu(fabMenuOpen, fabSubject, fabEvent, fab);
            Intent intent = new Intent(activity, SubjectListActivity.class);
            intent.putExtra("add_event", true);
            activity.startActivity(intent);
        });

        if (recyclerToAutoClose != null) {
            recyclerToAutoClose.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    if (fabMenuOpen[0] && Math.abs(dy) > 6) closeMenu(fabMenuOpen, fabSubject, fabEvent, fab);
                }
            });
        }
    }

    private static void toggle(boolean[] open, View fabSubject, View fabEvent, View main) {
        if (open[0]) {
            closeMenu(open, fabSubject, fabEvent, main);
        } else {
            openMenu(open, fabSubject, fabEvent, main);
        }
    }

    private static void openMenu(boolean[] open, View fabSubject, View fabEvent, View main) {
        open[0] = true;
        showFabWithAnim(fabSubject);
        showFabWithAnim(fabEvent);
        rotateFab(main, true);
    }

    private static void closeMenu(boolean[] open, View fabSubject, View fabEvent, View main) {
        open[0] = false;
        hideFabWithAnim(fabSubject);
        hideFabWithAnim(fabEvent);
        rotateFab(main, false);
    }

    private static void showFabWithAnim(View fab) {
        fab.setVisibility(View.VISIBLE);
        fab.setScaleX(0.9f); fab.setScaleY(0.9f); fab.setAlpha(0f);
        fab.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(150).start();
    }

    private static void hideFabWithAnim(View fab) {
        fab.animate().alpha(0f).scaleX(0.9f).scaleY(0.9f).setDuration(120)
                .withEndAction(() -> fab.setVisibility(View.GONE)).start();
    }

    private static void rotateFab(View main, boolean open) {
        main.animate().rotation(open ? 45f : 0f).setDuration(150).start();
    }
}
