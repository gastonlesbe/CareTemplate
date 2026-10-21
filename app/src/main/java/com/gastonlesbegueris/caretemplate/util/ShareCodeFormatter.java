package com.gastonlesbegueris.caretemplate.util;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;

/**
 * TextWatcher que formatea automáticamente el código de compartir mientras se escribe.
 * Formato: XXXX-XXXX-XXXX (mayúsculas y guiones automáticos)
 * Funciona tanto con EditText como con TextInputEditText
 */
public class ShareCodeFormatter implements TextWatcher {
    private static final String TAG = "ShareCodeFormatter";
    private final EditText editText;
    private boolean isFormatting = false;
    private int previousLength = 0;
    private int previousCursorPosition = 0;
    
    public ShareCodeFormatter(EditText editText) {
        this.editText = editText;
    }
    
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        previousLength = s != null ? s.length() : 0;
        previousCursorPosition = editText.getSelectionStart();
    }
    
    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // No hacer nada aquí
    }
    
    @Override
    public void afterTextChanged(Editable s) {
        if (isFormatting) {
            return; // Evitar recursión infinita
        }
        
        isFormatting = true;
        
        try {
            String text = s != null ? s.toString() : "";
            
            // Convertir a mayúsculas y quitar caracteres no válidos (solo letras y números)
            String cleaned = text.toUpperCase().replaceAll("[^A-Z0-9]", "");
            
            // Limitar a 12 caracteres
            if (cleaned.length() > 12) {
                cleaned = cleaned.substring(0, 12);
            }
            
            // Formatear con guiones: XXXX-XXXX-XXXX
            StringBuilder formatted = new StringBuilder();
            for (int i = 0; i < cleaned.length(); i++) {
                if (i > 0 && i % 4 == 0) {
                    formatted.append("-");
                }
                formatted.append(cleaned.charAt(i));
            }
            
            // Si el texto cambió, actualizarlo
            if (!text.equals(formatted.toString())) {
                int currentCursorPosition = editText.getSelectionStart();
                s.replace(0, s.length(), formatted.toString());
                
                // Calcular nueva posición del cursor
                int newPosition = calculateNewCursorPosition(
                        text, 
                        formatted.toString(), 
                        currentCursorPosition,
                        previousCursorPosition,
                        previousLength
                );
                
                if (newPosition >= 0 && newPosition <= formatted.length()) {
                    editText.setSelection(newPosition);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error formateando código", e);
        } finally {
            isFormatting = false;
        }
    }
    
    /**
     * Calcula la nueva posición del cursor después del formateo.
     */
    private int calculateNewCursorPosition(String oldText, String newText, int currentPos, int previousPos, int previousLength) {
        // Contar caracteres válidos (sin guiones) antes del cursor en el texto anterior
        int validCharsBeforeCursor = 0;
        for (int i = 0; i < Math.min(currentPos, oldText.length()); i++) {
            char c = oldText.charAt(i);
            if (c != '-' && Character.isLetterOrDigit(c)) {
                validCharsBeforeCursor++;
            }
        }
        
        // Encontrar la posición en el nuevo texto que corresponde a esos caracteres válidos
        int newPosition = 0;
        int validCharsCounted = 0;
        for (int i = 0; i < newText.length() && validCharsCounted < validCharsBeforeCursor; i++) {
            char c = newText.charAt(i);
            if (c != '-') {
                validCharsCounted++;
            }
            newPosition = i + 1;
        }
        
        // Si estamos justo después de un guión y agregamos un carácter, avanzar el cursor
        if (newText.length() > previousLength && newPosition < newText.length()) {
            if (newText.charAt(newPosition - 1) == '-') {
                newPosition++;
            }
        }
        
        return Math.min(newPosition, newText.length());
    }
}

